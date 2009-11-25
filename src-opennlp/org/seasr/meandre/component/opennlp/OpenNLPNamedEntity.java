/**
*
* University of Illinois/NCSA
* Open Source License
*
* Copyright (c) 2008, NCSA.  All rights reserved.
*
* Developed by:
* The Automated Learning Group
* University of Illinois at Urbana-Champaign
* http://www.seasr.org
*
* Permission is hereby granted, free of charge, to any person obtaining
* a copy of this software and associated documentation files (the
* "Software"), to deal with the Software without restriction, including
* without limitation the rights to use, copy, modify, merge, publish,
* distribute, sublicense, and/or sell copies of the Software, and to
* permit persons to whom the Software is furnished to do so, subject
* to the following conditions:
*
* Redistributions of source code must retain the above copyright
* notice, this list of conditions and the following disclaimers.
*
* Redistributions in binary form must reproduce the above copyright
* notice, this list of conditions and the following disclaimers in
* the documentation and/or other materials provided with the distribution.
*
* Neither the names of The Automated Learning Group, University of
* Illinois at Urbana-Champaign, nor the names of its contributors may
* be used to endorse or promote products derived from this Software
* without specific prior written permission.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE
* FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
* WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE SOFTWARE.
*
*/

package org.seasr.meandre.component.opennlp;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import opennlp.maxent.io.BinaryGISModelReader;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.util.Span;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.seasr.datatypes.BasicDataTypesTools;
import org.seasr.datatypes.BasicDataTypes.Strings;
import org.seasr.datatypes.BasicDataTypes.StringsArray;
import org.seasr.datatypes.BasicDataTypes.StringsMap;
import org.seasr.meandre.components.tools.Names;
import org.seasr.meandre.support.components.datatype.parsers.DataTypeParser;
import org.seasr.meandre.support.components.tuples.SimpleTuple;
import org.seasr.meandre.support.components.tuples.SimpleTuplePeer;


/**
 * This component perform Named Entity Extraction via OpenNLP.
 *
 * @author Mike Haberman;
 *
 */

//
// General Path:  Text -> SentenceDetector -> SentenceTokenizer -> NETagger
//

@Component(
		name = "OpenNLP Named Entity",
		creator = "Mike Haberman",
		baseURL = "meandre://seasr.org/components/foundry/",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		tags = "semantic, tools, text, opennlp, tokenizer, sentences, tagging",
		description = "This component tags the incoming set of tokenized sentences " +
				      "unsing OpenNLP named entity facilities.",
		dependency = {"trove-2.0.3.jar","protobuf-java-2.2.0.jar", "maxent-models.jar", "seasr-commons.jar"}
)
public class OpenNLPNamedEntity extends OpenNLPBaseUtilities {

    //------------------------------ INPUTS ------------------------------------------------------

	@ComponentInput(
			name = Names.PORT_TOKENIZED_SENTENCES,
			description = "The sequence of tokenized sentences"
	)
	protected static final String IN_TOKENS = Names.PORT_TOKENIZED_SENTENCES;

    //------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
			name = Names.PORT_TUPLES,
			description = "set of tuples: (sentenceId,type,textStart,textEnd,text)"
	)
	protected static final String OUT_TUPLES = Names.PORT_TUPLES;

	@ComponentOutput(
			name = Names.PORT_META_TUPLE,
			description = "meta data for tuples: (sentenceId,type,textStart,textEnd,text)"
	)
	protected static final String OUT_META_TUPLE = Names.PORT_META_TUPLE;

	//----------------------------- PROPERTIES ---------------------------------------------------

	@ComponentProperty(
			name = "NETypes",
			description = "Named Entties types (e.g person,location,date)",
		    defaultValue = "person,location,date,organization"
		)
	protected static final String PROP_NE_TYPES = "NETypes";

	//--------------------------------------------------------------------------------------------


	// money, percentage, time
	String[] finderTypes = {"person", "location", "date", "organization"};

    NameFinderME[] finders = null;

	//--------------------------------------------------------------------------------------------


    public static NameFinderME[] build(String sOpenNLPDir, String[] finderTypes)
    throws IOException
    {
    	NameFinderME[] finders = new NameFinderME[finderTypes.length];
		for (int i = 0; i < finderTypes.length; i++) {
			String tagPath          = // e.g.  /opennlp/models/English/namefind/location.bin.gz
			    sOpenNLPDir + "namefind" + File.separator + finderTypes[i] + ".bin.gz";
			File tagFile     = new File(tagPath);
			if (! tagFile.canRead()) {
				throw new IOException("Failed to open tag file for " + tagPath);
			}

			System.out.println("loaded " + tagPath);
			BinaryGISModelReader reader = new BinaryGISModelReader(tagFile);
			NameFinderME finder = new NameFinderME(reader.getModel());
			finders[i] = finder;
		}
		return finders;
    }

	@Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {

		super.initializeCallBack(ccp);


		// parse up the Named Entity types
		String types = ccp.getProperty(PROP_NE_TYPES).trim();
		StringTokenizer tokens = new StringTokenizer(types,",");
		int count = tokens.countTokens();
		finderTypes = new String[count];
		for (int i = 0; i < count; i++) {
			finderTypes[i] = tokens.nextToken();
			console.info("added type " + finderTypes[i]);
		}


		try {
			finders = build(sOpenNLPDir, finderTypes);
		}
		catch ( Throwable t ) {
			console.severe("Failed to open tokenizer model for " + sLanguage);
			throw new ComponentExecutionException(t);
		}

		//
		// build the tuple (output) data
		//
		String[] fields =
			new String[] {SENTENCE_ID_FIELD, TYPE_FIELD,
				          TEXT_START_FIELD, TEXT_END_FIELD, TEXT_FIELD};

		tuplePeer = new SimpleTuplePeer(fields);

		TYPE_IDX        = tuplePeer.getIndexForFieldName(TYPE_FIELD);
		SENTENCE_ID_IDX = tuplePeer.getIndexForFieldName(SENTENCE_ID_FIELD);
		TEXT_START_IDX  = tuplePeer.getIndexForFieldName(TEXT_START_FIELD);
		TEXT_END_IDX    = tuplePeer.getIndexForFieldName(TEXT_END_FIELD);
		TEXT_IDX        = tuplePeer.getIndexForFieldName(TEXT_FIELD);


	}

	SimpleTuplePeer tuplePeer;
	public static final String TYPE_FIELD        = "type";
	public static final String SENTENCE_ID_FIELD = "sentenceId";
	public static final String TEXT_START_FIELD  = "textStart";
	public static final String TEXT_END_FIELD    = "textEnd";
	public static final String TEXT_FIELD        = "text";

	int TYPE_IDX        ;
	int SENTENCE_ID_IDX ;
	int TEXT_START_IDX  ;
	int TEXT_END_IDX    ;
	int TEXT_IDX        ;

	public static TextSpan label(String sentence, String[] tokens, Span span) {

		TextSpan out = new TextSpan();
		return label(sentence, tokens, span, out);
	}

	public static TextSpan label(String sentence, String[] tokens, Span span, TextSpan out)
	{

		int s = span.getStart();
		int e = span.getEnd();

		String first = tokens[s];
		String last = first;
		if (e != s) {
	       last = tokens[e-1];
		}
		int beginIndex = sentence.indexOf(first,out.getEnd());
		// always go from where the first token was found
		int endIndex   = sentence.indexOf(last, beginIndex) + last.length();


		out.setStart(beginIndex);
		out.setEnd(endIndex);
		out.setSpan(sentence);
		return out;


		/*
		 // another way to derive text span
		 // but needs to be smarter about
		 // processing punctuation, spaces, etc
		StringBuffer sb = new StringBuffer();
		for (int ti = s; ti < e; ti++) {
			sb.append(tokens[ti]);
			if (ti + 1 < e)sb.append(" ");
		}
		console.info(sb.toString());
		*/


	}

	public static List<TextSpan> findURLS(String sentence) {

		List<TextSpan> list = new ArrayList<TextSpan>();

		StringTokenizer tokens = new StringTokenizer(sentence);
		int sIdx = 0;
		int eIdx = 0;

		while(tokens.hasMoreTokens()) {
			String s = tokens.nextToken();

			int idx = s.toLowerCase().indexOf("http");
			if ( idx >= 0) {

				String sub = s.substring(idx);

				sIdx = sentence.indexOf(sub, eIdx);
				eIdx = sIdx + sub.length();

				TextSpan span = new TextSpan();
				span.setStart(sIdx);
				span.setEnd(eIdx);
				span.setSpan(sentence);

				list.add(span);

			}

		}
	    return list;
	}

	@SuppressWarnings("unchecked")
	@Override
    public void executeCallBack(ComponentContext cc) throws Exception
	{
		List<Strings> output = new ArrayList<Strings>();

		// input was encoded via :
		// cc.pushDataComponentToOutput(OUT_TOKENS, BasicDataTypesTools.stringToStrings(ta));
		//

		//
		// NEED a parser here ? DataTypeParser.parseAsMap ???
		//
		StringsMap input = (StringsMap) cc.getDataComponentFromInput(IN_TOKENS);

		int count = input.getKeyCount();
		console.fine("processing " + count);
		int globalOffset = 0;

		List<SimpleTuple> list = new ArrayList<SimpleTuple>();
		TextSpan textSpan = new TextSpan();

		for (int i = 0; i < count; i++) {
			String key    = input.getKey(i);    // this is the entire sentence
			Strings value = input.getValue(i);  // this is the set of tokens for that sentence

			String[] tokens = DataTypeParser.parseAsString(value);
			// console.info("Tokens " + tokens.length + " .. " + tokens[0]);

			list.clear();
			for (int j = 0; j < finders.length; j++) {

				String type = finderTypes[j];
				Span[] span = finders[j].find(tokens);

				textSpan.reset();
				for (int k = 0; k < span.length; k++) {

					textSpan = label(key, tokens, span[k], textSpan);
					int beginIndex = textSpan.getStart();
					int endIndex   = textSpan.getEnd();
					String text    = textSpan.getText();

					SimpleTuple tuple = tuplePeer.createTuple();
					tuple.setValue(TYPE_IDX,        type);
					tuple.setValue(SENTENCE_ID_IDX, i);
					tuple.setValue(TEXT_START_IDX,  beginIndex + globalOffset);
					tuple.setValue(TEXT_END_IDX,    endIndex + globalOffset);
					tuple.setValue(TEXT_IDX,        text);

					list.add(tuple);

				}


			} // end finders

			//
			// bonus find the urls
			//
			// TODO: add a property to or finderTypes a URL type
			//

			List<TextSpan> urls = findURLS(key);
			for (TextSpan s : urls) {

				SimpleTuple tuple = tuplePeer.createTuple();
				tuple.setValue(TYPE_IDX,        "URL");
				tuple.setValue(SENTENCE_ID_IDX, i);
				tuple.setValue(TEXT_START_IDX,  s.getStart() + globalOffset);
				tuple.setValue(TEXT_END_IDX,    s.getEnd()   + globalOffset);
				tuple.setValue(TEXT_IDX,        s.getText());

				list.add(tuple);
			}


			//
			// at this point, sort all the tuples based
			// on their beginIndex
			// then add them to the output
			Collections.sort(list, new Comparator() {

				       public int compare(Object a, Object b) {
				    	   SimpleTuple at = (SimpleTuple)a;
				    	   SimpleTuple bt = (SimpleTuple)b;
				    	   int v = Integer.parseInt(at.getValue(TEXT_START_IDX));
				    	   int u = Integer.parseInt(bt.getValue(TEXT_START_IDX));
				    	   return v-u;
				       }

					}
             );

			// add the sorted tuples to the output buffer
			for (SimpleTuple t:list) {
				output.add(t.convert());
			}

			globalOffset += key.length();


		}


		// push the whole collection, protocol safe
	    Strings[] results = new Strings[output.size()];
	    output.toArray(results);

	    StringsArray outputSafe = BasicDataTypesTools.javaArrayToStringsArray(results);
	    cc.pushDataComponentToOutput(OUT_TUPLES, outputSafe);

	    //
		// metaData for this tuple producer
		//
	    cc.pushDataComponentToOutput(OUT_META_TUPLE, tuplePeer.convert());
	}

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
        super.disposeCallBack(ccp);
    }



}
