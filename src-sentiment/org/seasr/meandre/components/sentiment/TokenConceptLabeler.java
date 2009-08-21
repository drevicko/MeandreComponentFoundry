package org.seasr.meandre.components.sentiment;







import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.StringTokenizer;



import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.components.abstracts.AbstractExecutableComponent;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.seasr.datatypes.BasicDataTypes;
import org.seasr.datatypes.BasicDataTypesTools;
import org.seasr.datatypes.BasicDataTypes.Strings;
import org.seasr.datatypes.BasicDataTypes.StringsMap;
import org.seasr.meandre.components.tools.Names;

import org.seasr.meandre.support.parsers.DataTypeParser;
import org.seasr.meandre.support.tuples.DynamicTuple;
import org.seasr.meandre.support.tuples.DynamicTuplePeer;

import org.seasr.meandre.support.sentiment.PathMetric;
import org.seasr.meandre.support.sentiment.PathMetricFinder;




/**
 *
 * @author Mike Haberman;
 *
 */

//
// General Path:   PosTagger -->PosTokenFrequencyCounter --> TokenConceptLabler
//


@Component(
		name = "Token Concept Labeler",
		creator = "Mike Haberman",
		baseURL = "meandre://seasr.org/components/tools/",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		tags = "semantic, tools, text, opennlp, tokenizer, sentences, pos, tagging",
		description = "This component counts the incoming set of pos tuples " ,
		dependency = {"trove-2.0.3.jar","protobuf-java-2.0.3.jar"}
)
public class TokenConceptLabeler  extends AbstractExecutableComponent {
	

    //------------------------------ INPUTS ------------------------------------------------------
	
	@ComponentInput(
			name = Names.PORT_TUPLES,
			description = "set of tuples"
	)
	protected static final String IN_TUPLES = Names.PORT_TUPLES;
	
	@ComponentInput(
			name = Names.PORT_META_TUPLE,
			description = "meta data for tuples"
	)
	protected static final String IN_META_TUPLE = Names.PORT_META_TUPLE;
	

    //------------------------------ OUTPUTS -----------------------------------------------------
	
	@ComponentOutput(
			name = Names.PORT_TUPLES,
			description = "set of filtered tuples"
	)
	protected static final String OUT_TUPLES = Names.PORT_TUPLES;
	
	@ComponentOutput(
			name = Names.PORT_META_TUPLE,
			description = "meta data for the tuples (same as input, concept)"
	)
	protected static final String OUT_META_TUPLE = Names.PORT_META_TUPLE;
	
	
	
	
	//----------------------------- PROPERTIES ---------------------------------------------------
	@ComponentProperty(description = "synNet host",
			   name = "host",
			   defaultValue = "http://services.seasr.org/synnet/")
	protected static final String DATA_PROPERTY_HOST = "host";
	 
	 
   @ComponentProperty(description = "concepts",
		   name = "concepts",
		   defaultValue = "love={lovable};anger={hateful,angry}")
    protected static final String DATA_PROPERTY_CONCEPTS = "concepts";
      
   
   @ComponentProperty(description = "cache filename of concepts to write/read",
           name = "conceptCacheFile",
           defaultValue = "posConceptCache.csv")
    protected static final String DATA_PROPERTY_CACHE = "conceptCacheFile";
  
   
   static String NO_VALUE = "N.A.";
   
   
	//--------------------------------------------------------------------------------------------

    HashMap<String,List<String>> conceptMap;
	HashMap<String,String> reverseMap;
	List<String> allLabels;
	String cacheFileName;
	String noConceptFileName = "ignorePos.csv";
	Map<String,String> wordToConceptMap;
	Map<String,String> noConceptMap;
	
	PathMetricFinder finder;
	
	public void initializeCallBack(ComponentContextProperties ccp) throws Exception 
	{
		//
		// init the synNet host
		//
		String host   = ccp.getProperty(DATA_PROPERTY_CACHE);
		this.finder = new PathMetricFinder(host);
		
		
		// create/build the cache file name
		this.cacheFileName    = ccp.getProperty(DATA_PROPERTY_CACHE);
		this.wordToConceptMap = readFromFile(cacheFileName);
		this.noConceptMap     = readFromFile(noConceptFileName);
		
		//
		// build the map for concepts
		//
		String toParse = ccp.getProperty(DATA_PROPERTY_CONCEPTS);
        conceptMap = new HashMap<String,List<String>>();
        reverseMap = new HashMap<String,String>();
        allLabels = new ArrayList<String>();
        
        StringTokenizer tokens = new StringTokenizer(toParse, ";");
        while (tokens.hasMoreTokens()){
        	String kv = tokens.nextToken();
        	int idx = kv.indexOf('=');
        	if (idx > 0) {
        		String key   = kv.substring(0,idx).trim();  /// concept = {a,b,c}
        		String value = kv.substring(idx+1).replaceAll("[}{]", "");
        		StringTokenizer labels = new StringTokenizer(value, ",");
        	
        		ArrayList<String> labelList = new ArrayList<String>();
        		while(labels.hasMoreTokens()) {
        			String label = labels.nextToken().trim();
        			// console.info("MAP value " + label + ":" + key);
        			allLabels.add(label);
        			labelList.add(label);
        			reverseMap.put(label, key);
        		}
        		conceptMap.put(key, labelList);
        	}
        }
        
	}

	public void executeCallBack(ComponentContext cc) throws Exception 
	{
		// TODO: pull from poperties
		
		
		Strings inputMeta = (Strings) cc.getDataComponentFromInput(IN_META_TUPLE);
		String[] meta = DataTypeParser.parseAsString(inputMeta);
		String fields = meta[0];
		DynamicTuplePeer inPeer = new DynamicTuplePeer(fields);
		DynamicTuplePeer outPeer = new DynamicTuplePeer(inPeer, new String[]{"concept"});		
		
		Strings input = (Strings) cc.getDataComponentFromInput(IN_TUPLES);
		String[] tuples = DataTypeParser.parseAsString(input);
		DynamicTuple tuple = inPeer.createTuple();
		
		int TOKEN_IDX   = inPeer.getIndexForFieldName("token");
		int CONCEPT_IDX = outPeer.getIndexForFieldName("concept");
		
		DynamicTuple outTuple = outPeer.createTuple();
		
		console.info("tuple count to label " + tuples.length);
		
		List<String> output = new ArrayList<String>();
		for (int i = 0; i < tuples.length; i++) {
			
			tuple.setValues(tuples[i]);	
			String token = tuple.getValue(TOKEN_IDX);
			
			String concept = wordToConceptMap.get(token);
			if (concept == null) {
				
				if (noConceptMap.get(token) != null) {
					// word has no concept
					continue;
				}
				
			
			   console.info("need to label " + token);
			   List<PathMetric> all = finder.getAllMetric(token, allLabels);
			   if (all == null) {
			    	console.info("Unable to label, service down");
			    	break;
			   }
			
			   PathMetric metric    = finder.getBestMetric(all);
			   if (metric != null) {
			      concept = reverseMap.get(metric.end);
			      console.info(token + " ==> " + concept);
			      wordToConceptMap.put(token, concept);
			      // console.info(metric.toString());
			      // label the tuple and save it
			   }
			   else {
				   noConceptMap.put(token, NO_VALUE);
			   }
			}
			
			if (concept != null) {
				outTuple.setValue(tuple);
				outTuple.setValue(CONCEPT_IDX, concept);
				output.add(outTuple.toString());
			}
		}
		
		//
		// push the whole collection, protocol safe
		//
	    String[] results = new String[output.size()];
	    output.toArray(results);
	    Strings outputSafe = BasicDataTypesTools.stringToStrings(results);
	    cc.pushDataComponentToOutput(OUT_TUPLES, outputSafe);

	    
	    //
		// metaData for this tuple producer
		//
	    Strings metaData;
		metaData = BasicDataTypesTools.stringToStrings(outPeer.getFieldNames());
	    cc.pushDataComponentToOutput(OUT_META_TUPLE, metaData);
		
	}

    public void disposeCallBack(ComponentContextProperties ccp) throws Exception 
    {
    	writeToFile(wordToConceptMap, cacheFileName);
    	writeToFile(noConceptMap, noConceptFileName);
    }
    
    
    private void writeToFile(Map<String,String>map, String filename) {
		try {
	        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
	        Iterator<String> it = map.keySet().iterator();
	        while(it.hasNext()) {
	        	String key = it.next();
	        	String concept = map.get(key);
	        	out.write(key +"," + concept + "\n");
	        }
	        out.close();
	    } catch (IOException e) {}
	}
	
	private Map<String,String> readFromFile(String filename)
	{
		Map<String,String> map = new HashMap<String,String>();
		
		try {
			File file = new File(filename);
			console.info("reading from " + filename);
			
			if (file.exists()) {
				
				BufferedReader input =  new BufferedReader(new FileReader(file));
			      try {
			        String line = null;
			        /*
			        * readLine is a bit quirky :
			        * it returns the content of a line MINUS the newline.
			        * it returns null only for the END of the stream.
			        * it returns an empty String if two newlines appear in a row.
			        */
			        
			        while (( line = input.readLine()) != null){
			        	
			        	// word,concept (word is the key, concept is the value)
			        	int idx = line.indexOf(',');
			        	String word    = line.substring(0,idx).trim();
			        	String concept = line.substring(idx+1).trim();
			        	
			        	map.put(word,concept);
			        }
			       
			      }
			      finally {
			        input.close();
			      }
			}
			else {
				// no file exists to read
				console.info("file is empty: " + filename);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return map;
	}
	
}