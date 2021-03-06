/**
 * University of Illinois/NCSA
 * Open Source License
 *
 * Copyright (c) 2008, Board of Trustees-University of Illinois.
 * All rights reserved.
 *
 * Developed by:
 *
 * Automated Learning Group
 * National Center for Supercomputing Applications
 * http://www.seasr.org
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal with the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimers.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimers in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the names of Automated Learning Group, The National Center for
 *    Supercomputing Applications, or University of Illinois, nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this Software without specific prior written permission.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * WITH THE SOFTWARE.
 */

package org.seasr.meandre.components.vis.d3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.meandre.annotations.Component;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.system.components.ext.StreamInitiator;
import org.meandre.core.system.components.ext.StreamTerminator;
import org.seasr.datatypes.core.DataTypeParser;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.vis.html.VelocityTemplateToHTML;

/**
 * @author Boris Capitanu
 */

@Component(
        creator = "Boris Capitanu",
        description = "This components uses the D3 library to generate a tag cloud. "+
        "In the properties setting, you can also indicate the rotation for the text, "+
        "for instance, 'rotation=0' means all words are horizontal; "+
        "'rotation=90' means all words are horizontal or vertical. "+
        "References: D3: Data-Driven Documents, Michael Bostock, Vadim Ogievetsky, Jeffrey Heer, "+
        "IEEE Trans. Visualization & Comp. Graphics (Proc. InfoVis), 2011. " +
		"D3 tag cloud code from http://github.com/jasondavies/d3-cloud.",
        name = "Tag Cloud",
        tags = "#VIS, visualization, d3, tag cloud",
        rights = Licenses.UofINCSA,
        firingPolicy = FiringPolicy.any,
        baseURL = "meandre://seasr.org/components/foundry/",
        dependency = { "velocity-1.7-dep.jar", "d3-v2.8.1.jar", "d3.layout.cloud.jar" },
        resources  = { "TagCloud.vm" }
)
public class TagCloud extends AbstractD3CloudLayoutComponent {

    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            name = Names.PORT_TOKEN_COUNTS,
            description = "The token counts to use for creating the tag cloud" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.IntegersMap" +
                "<br>TYPE: java.util.Map<java.lang.String, java.lang.Integer>"
    )
    protected static final String IN_TOKEN_COUNTS = Names.PORT_TOKEN_COUNTS;

    @ComponentInput(
            name = Names.PORT_DOC_TITLE,
            description = "A label to show abovbe the tag cloud" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings" +
                "<br>TYPE: java.lang.String"
    )
    protected static final String IN_LABEL = Names.PORT_DOC_TITLE;

   //------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            name = Names.PORT_HTML,
            description = "The HTML for the tag cloud" +
            "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String OUT_HTML = Names.PORT_HTML;

    //------------------------------ PROPERTIES --------------------------------------------------

    @ComponentProperty(
            description = "The title for the page",
            name = Names.PROP_TITLE,
            defaultValue = "Tag Cloud"
    )
    protected static final String PROP_TITLE = Names.PROP_TITLE;

    private static final String DEFAULT_TEMPLATE = "org/seasr/meandre/components/vis/d3/TagCloud.vm";
    @ComponentProperty(
            description = "The template name. The default is "+DEFAULT_TEMPLATE,
            name = VelocityTemplateToHTML.PROP_TEMPLATE,
            defaultValue = DEFAULT_TEMPLATE
    )
    protected static final String PROP_TEMPLATE = VelocityTemplateToHTML.PROP_TEMPLATE;

    @ComponentProperty(
            defaultValue = "1000",
            description = "Tag cloud width",
            name = Names.PROP_WIDTH
    )
    protected static final String PROP_WIDTH = Names.PROP_WIDTH;

    @ComponentProperty(
            defaultValue = "1000",
            description = "Tag cloud height",
            name = Names.PROP_HEIGHT
    )
    protected static final String PROP_HEIGHT = Names.PROP_HEIGHT;

    @ComponentProperty(
            defaultValue = "",
            description = "Name of the font to use for the words in the tag cloud",
            name = Names.PROP_FONT_NAME
    )
    protected static final String PROP_FONT_NAME = Names.PROP_FONT_NAME;

    @ComponentProperty(
            defaultValue = "150",
            description = "Maximum font size",
            name = Names.PROP_MAX_SIZE
    )
    protected static final String PROP_FONT_MAX_SIZE = Names.PROP_MAX_SIZE;

    @ComponentProperty(
            defaultValue = "20",
            description = "Minimum font size",
            name = Names.PROP_MIN_SIZE
    )
    protected static final String PROP_FONT_MIN_SIZE = Names.PROP_MIN_SIZE;

    @ComponentProperty(
            defaultValue = "linear",
            description = "The scale to use, one of:" +
            		"linear, log, sqrt",
            name = "scale"
    )
    protected static final String PROP_SCALE = "scale";

    @ComponentProperty(
            defaultValue = "category20",
            description = "Color palette to use:<br><dl>" +
            		"<dt>category10 - ten categorical colors: </dt><dd><font color='#1f77b4'>#1f77b4</font> <font color='#ff7f0e'>#ff7f0e</font> <font color='#2ca02c'>#2ca02c</font> <font color='#d62728'>#d62728</font> <font color='#9467bd'>#9467bd</font> <font color='#8c564b'>#8c564b</font> <font color='#e377c2'>#e377c2</font> <font color='#7f7f7f'>#7f7f7f</font> <font color='#bcbd22'>#bcbd22</font> <font color='#17becf'>#17becf</font></dd>" +
            		"<dt>category20 - twenty categorical colors: </dt><dd><font color='#1f77b4'>#1f77b4</font> <font color='#aec7e8'>#aec7e8</font> <font color='#ff7f0e'>#ff7f0e</font> <font color='#ffbb78'>#ffbb78</font> <font color='#2ca02c'>#2ca02c</font> <font color='#98df8a'>#98df8a</font> <font color='#d62728'>#d62728</font> <font color='#ff9896'>#ff9896</font> <font color='#9467bd'>#9467bd</font> <font color='#c5b0d5'>#c5b0d5</font> <font color='#8c564b'>#8c564b</font> <font color='#c49c94'>#c49c94</font> <font color='#e377c2'>#e377c2</font> <font color='#f7b6d2'>#f7b6d2</font> <font color='#7f7f7f'>#7f7f7f</font> <font color='#c7c7c7'>#c7c7c7</font> <font color='#bcbd22'>#bcbd22</font> <font color='#dbdb8d'>#dbdb8d</font> <font color='#17becf'>#17becf</font> <font color='#9edae5'>#9edae5</font></dd>" +
            		"<dt>category20b - twenty categorical colors: </dt><dd><font color='#393b79'>#393b79</font> <font color='#5254a3'>#5254a3</font> <font color='#6b6ecf'>#6b6ecf</font> <font color='#9c9ede'>#9c9ede</font> <font color='#637939'>#637939</font> <font color='#8ca252'>#8ca252</font> <font color='#b5cf6b'>#b5cf6b</font> <font color='#cedb9c'>#cedb9c</font> <font color='#8c6d31'>#8c6d31</font> <font color='#bd9e39'>#bd9e39</font> <font color='#e7ba52'>#e7ba52</font> <font color='#e7cb94'>#e7cb94</font> <font color='#843c39'>#843c39</font> <font color='#ad494a'>#ad494a</font> <font color='#d6616b'>#d6616b</font> <font color='#e7969c'>#e7969c</font> <font color='#7b4173'>#7b4173</font> <font color='#a55194'>#a55194</font> <font color='#ce6dbd'>#ce6dbd</font> <font color='#de9ed6'>#de9ed6</font></dd>" +
            		"<dt>category20c - twenty categorical colors: </dt><dd><font color='#3182bd'>#3182bd</font> <font color='#6baed6'>#6baed6</font> <font color='#9ecae1'>#9ecae1</font> <font color='#c6dbef'>#c6dbef</font> <font color='#e6550d'>#e6550d</font> <font color='#fd8d3c'>#fd8d3c</font> <font color='#fdae6b'>#fdae6b</font> <font color='#fdd0a2'>#fdd0a2</font> <font color='#31a354'>#31a354</font> <font color='#74c476'>#74c476</font> <font color='#a1d99b'>#a1d99b</font> <font color='#c7e9c0'>#c7e9c0</font> <font color='#756bb1'>#756bb1</font> <font color='#9e9ac8'>#9e9ac8</font> <font color='#bcbddc'>#bcbddc</font> <font color='#dadaeb'>#dadaeb</font> <font color='#636363'>#636363</font> <font color='#969696'>#969696</font> <font color='#bdbdbd'>#bdbdbd</font> <font color='#d9d9d9'>#d9d9d9</font></dd>" +
            		"</dl>",
            name = "color_palette"
    )
    protected static final String PROP_COLOR_PALETTE = "color_palette";

    //--------------------------------------------------------------------------------------------
    
    protected Boolean labelsConnected;
    protected HashMap<String,Set<Integer>> initiatorsReceived = new HashMap<String,Set<Integer>>();
    protected HashMap<String,Set<Integer>> terminatorsReceived = new HashMap<String,Set<Integer>>();
    
    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        super.initializeCallBack(ccp);
        
        for (String input : ccp.getConnectedInputs()) {
        	initiatorsReceived.put(input,new TreeSet<Integer>());
        	terminatorsReceived.put(input,new TreeSet<Integer>());
        }

        context.put("title", getPropertyOrDieTrying(PROP_TITLE, ccp));
        context.put("width", getPropertyOrDieTrying(PROP_WIDTH, ccp));
        context.put("height", getPropertyOrDieTrying(PROP_HEIGHT, ccp));
        context.put("fontMin", getPropertyOrDieTrying(PROP_FONT_MIN_SIZE, ccp));
        context.put("fontMax", getPropertyOrDieTrying(PROP_FONT_MAX_SIZE, ccp));
        context.put("scale", getPropertyOrDieTrying(PROP_SCALE, ccp));
        context.put("colorPalette", getPropertyOrDieTrying(PROP_COLOR_PALETTE, ccp));

        String fontName = getPropertyOrDieTrying(PROP_FONT_NAME, true, false, ccp);
        if (fontName.length() == 0) fontName = null;

        context.put("fontName", fontName);
        //TODO: isComponentInputConnected() is returning true when it's not connected ):
        //labelsConnected = isComponentInputConnected(IN_LABEL);
        labelsConnected = Arrays.asList(ccp.getConnectedInputs()).contains(IN_LABEL);
        console.finest("Label input "+(labelsConnected ? "is" : "is not")+" connected, connected inputs "+Arrays.toString(ccp.getConnectedInputs()));
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
	    for (String portName : new String[] { IN_LABEL, IN_TOKEN_COUNTS })
	        componentInputCache.storeIfAvailable(cc, portName);
    	console.finest("execute callback entry, caches have sizes label: "+componentInputCache.getDataCount(IN_LABEL)+", count:"+componentInputCache.getDataCount(IN_TOKEN_COUNTS));

	    while (componentInputCache.hasData(IN_TOKEN_COUNTS) && (!labelsConnected || componentInputCache.hasData(IN_LABEL) )) {
	    	console.finest("execute callback loop,  caches have sizes label: "+componentInputCache.getDataCount(IN_LABEL)+", count:"+componentInputCache.getDataCount(IN_TOKEN_COUNTS));
	    	String label = null;
	    	if (labelsConnected) {
	    		Object input = componentInputCache.retrieveNext(IN_LABEL);
	    		if (input != null) {
	    			label = DataTypeParser.parseAsString(input)[0];
	    			console.finest("Setting up label '"+label+"' from connected inputs "+Arrays.toString(cc.getConnectedInputs()));
	    		} else 
	    			console.warning("No label when it was expected!");
	    	}

	    	console.finest("Generating tag cloud");

	    	Map<String, Integer> tokenCounts = DataTypeParser.parseAsStringIntegerMap(componentInputCache.retrieveNext(IN_TOKEN_COUNTS));

	    	JSONArray words = new JSONArray();
	    	JSONArray counts = new JSONArray();

	    	for (Map.Entry<String, Integer> entry : tokenCounts.entrySet()) {
	    		String word = entry.getKey();
	    		Integer count = entry.getValue();

	    		words.put(word);
	    		counts.put(count);
	    	}

	    	JSONObject data = new JSONObject();
	    	data.put("words", words);
	    	data.put("counts", counts);

	    	context.put("data", data.toString());
	    	if (labelsConnected) {
	    		context.put("label", label);
	    	}
	    	console.info("Put " + data.toString().length() + " of data"+(labelsConnected ? " with label '"+label+"'" : " without label") );

	    	super.executeCallBack(componentContext);
	    }
	    console.finest("execute callback exit,  caches have sizes label: "+componentInputCache.getDataCount(IN_LABEL)+", count:"+componentInputCache.getDataCount(IN_TOKEN_COUNTS));
    }
    
    @Override
    public void handleStreamInitiators() throws Exception {
        console.entering(getClass().getName(), "handleStreamInitiators", inputPortsWithInitiators);
        for (String portWithInitiator : inputPortsWithInitiators) {
        	StreamInitiator si = (StreamInitiator) componentContext.getDataComponentFromInput(portWithInitiator);
        	if (!initiatorsReceived.containsKey(portWithInitiator)) {
        		console.warning("uninitialised initiator list "+portWithInitiator+"! Others are: "+initiatorsReceived.keySet());
        		initiatorsReceived.put(portWithInitiator, new TreeSet<Integer>());
        	}
        	Set<Integer> initiatorSet = initiatorsReceived.get(portWithInitiator);
        	initiatorSet.add(si.getStreamId());
        	Boolean allPorts = true;
        	for (Entry<String, Set<Integer>> portEntry : initiatorsReceived.entrySet()) {
        		if (!portEntry.getValue().contains(si.getStreamId()) ) {
        			allPorts = false;
        			break;
        		}
        	}
        	if (allPorts) {
        		for (Entry<String, Set<Integer>> portEntry : initiatorsReceived.entrySet()) {
        			portEntry.getValue().remove(si.getStreamId());
        		}
        		for (String portName : componentContext.getOutputNames()) {
//        			componentContext.getConnectedOutputs();
        			if (portName.equals(OUT_ERROR)) continue;
        			componentContext.pushDataComponentToOutput(portName, si);
        		}
        	}
        }
        console.exiting(getClass().getName(), "handleStreamInitiators");
    }

    @Override
    public void handleStreamTerminators() throws Exception {
        console.entering(getClass().getName(), "handleStreamTerminators", inputPortsWithTerminators);
        
        for (String portWithTerminator : inputPortsWithTerminators) {
        	StreamTerminator st = (StreamTerminator) componentContext.getDataComponentFromInput(portWithTerminator);
        	if (!terminatorsReceived.containsKey(portWithTerminator)) {
        		console.warning("uninitialised terminator list "+portWithTerminator+"! Others are: "+terminatorsReceived.keySet());
        		terminatorsReceived.put(portWithTerminator, new TreeSet<Integer>());
        	}
        	terminatorsReceived.get(portWithTerminator).add(st.getStreamId());
        	Boolean allPorts = true;
        	for (Entry<String, Set<Integer>> portEntry : terminatorsReceived.entrySet()) {
        		if (!portEntry.getValue().contains(st.getStreamId()) ) {
        			allPorts = false;
        			break;
        		}
        	}
        	if (allPorts) {
        		for (Entry<String, Set<Integer>> portEntry : terminatorsReceived.entrySet()) {
        			portEntry.getValue().remove(st.getStreamId());
        		}
        		for (String portName : componentContext.getOutputNames()) {
//        			componentContext.getConnectedOutputs();
        			if (portName.equals(OUT_ERROR)) continue;
        			componentContext.pushDataComponentToOutput(portName, st);
        		}
        	}
        }
        console.exiting(getClass().getName(), "handleStreamTerminators");
    }

}
