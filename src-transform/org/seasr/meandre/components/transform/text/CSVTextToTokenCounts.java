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

package org.seasr.meandre.components.transform.text;

import java.io.StringReader;
import java.util.Hashtable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVFormat.CSVFormatBuilder;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
//import org.apache.commons.csv.CSVStrategy;
import org.meandre.annotations.Component;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.datatypes.core.BasicDataTypesTools;
import org.seasr.datatypes.core.DataTypeParser;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.abstracts.AbstractExecutableComponent;

/**
 *
 * @author Boris Capitanu
 *
 */

@Component(
        creator = "Boris Capitanu,Ian Wood",
        description = "Converts CSV text to token counts structure. It respects double quoted fields (which may contin" +
        		"delimiters). Double quotes within a quoted field may be represented by two double quote characters.",
        name = "CSV Text To Token Counts",
        tags = "#TRANSFORM, CSV, text, token count",
        rights = Licenses.UofINCSA,
        baseURL = "meandre://seasr.org/components/foundry/",
        dependency = {"protobuf-java-2.2.0.jar"}
)
public class CSVTextToTokenCounts extends AbstractExecutableComponent{

    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            name = Names.PORT_TEXT,
            description = "The text to be converted" +
                "<br>TYPE: java.lang.String" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings" +
                "<br>TYPE: byte[]" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Bytes" +
                "<br>TYPE: java.lang.Object"
    )
    protected static final String IN_TEXT = Names.PORT_TEXT;

    //------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
			name = Names.PORT_TOKEN_COUNTS,
			description = "The token counts" +
			    "<br>TYPE: org.seasr.datatypes.BasicDataTypes.IntegersMap"
	)
	protected static final String OUT_TOKEN_COUNTS = Names.PORT_TOKEN_COUNTS;

    //----------------------------- PROPERTIES ---------------------------------------------------

    @ComponentProperty(
            name = Names.PROP_HEADER,
            description = "Does the input contain a header?",
            defaultValue = "true"
    )
    protected static final String PROP_HEADER = Names.PROP_HEADER;

    @ComponentProperty(
            name = "tokenSeparator",
            description = "The token to use to separate the field values. Use \\t if the separator is the tab character. " +
            		"In all other cases, all characters after the first character are discarded.",
            defaultValue = ","
    )
    protected static final String PROP_TOKEN_SEPARATOR = "tokenSeparator";

    @ComponentProperty(
            name = "token_pos",
            description = "The position of the token (the 'token' column) in the CSV (0=first, 1=second, etc.)",
            defaultValue = "0"
    )
    protected static final String PROP_TOKEN_POS = "token_pos";

    @ComponentProperty(
            name = "count_pos",
            description = "The position of the count (the 'count' column) in the CSV (0=first, 1=second, etc.). The " +
            		"count field of the csv text should contain only decimal digits 0-9.",
            defaultValue = "1"
    )
    protected static final String PROP_COUNT_POS = "count_pos";

    @ComponentProperty(
            name = Names.PROP_ORDERED,
            description = "Should the resulting token counts be ordered?",
            defaultValue = "true"
    )
    protected static final String PROP_ORDERED = Names.PROP_ORDERED;

	//--------------------------------------------------------------------------------------------


    private boolean bHeader, bOrdered;
    private char separator;
    private int tokenPos, countPos;
//    private CSVStrategy strategy;
    private CSVFormat format;
//    private static String[] uninitialisedLine = {};


    //--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        bHeader = Boolean.parseBoolean(getPropertyOrDieTrying(PROP_HEADER, true, true, ccp));
        bOrdered = Boolean.parseBoolean(getPropertyOrDieTrying(PROP_ORDERED, true, true, ccp));
        separator = getPropertyOrDieTrying(PROP_TOKEN_SEPARATOR, false, true, ccp).replaceAll("\\\\t", "\t").charAt(0);
//        strategy = new CSVStrategy(separator, '"', CSVStrategy.COMMENTS_DISABLED);
        CSVFormatBuilder fmtBuilder = CSVFormat.newBuilder(separator);
        if (bHeader) fmtBuilder = fmtBuilder.withHeader();
        format = fmtBuilder.build();
        tokenPos = Integer.parseInt(getPropertyOrDieTrying(PROP_TOKEN_POS, ccp));
        countPos = Integer.parseInt(getPropertyOrDieTrying(PROP_COUNT_POS, ccp));
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
    	Hashtable<String,Integer> htCounts = new Hashtable<String,Integer>();

    	for (String text : DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_TEXT))) {
//    	    boolean skippedHeader = false;
    	    //String[][] data = ... .getAllValues();
//    	    CSVParser parser = new CSVParser(new StringReader(text), strategy); 
//    	    CSVParser parser = new CSVParser(new StringReader(text), format); 
//    	    String[] tokens = uninitialisedLine;
//    	    while (tokens != null) {
    		console.finer("received text:\n"+text+"\n");
    	    for (CSVRecord tokens : format.parse(new StringReader(text))) {
//    	    	tokens = parser.getLine();
//    	    	if (tokens == null) break;
//    	        if (bHeader && !skippedHeader) {
//    	            skippedHeader = true;
//    	            continue;
//    	        }
//    	        String token = tokens[tokenPos];
    	    	console.fine("processing row "+tokens.toString());
    	    	if (tokens.size() <= tokenPos || tokens.size() <= countPos) {
    	    		console.warning(String.format("csv row %d too short (%d) for count pos %d or token pos %d - discarding",tokens.getRecordNumber(),tokens.size(),countPos,tokenPos));
    	    		continue;
    	    	}
    	        String token = tokens.get(tokenPos);
    	        int count = 0;
    	        try {
    	        	count = Integer.parseInt(tokens.get(countPos));
    	        } catch (NumberFormatException e) {
    	        	console.warning(String.format("Token '%s' had malformed count '%s' - assigning zero!", token, tokens.get(countPos)));
    	        }

    	        if (htCounts.containsKey(token))
    	            console.warning(String.format("Token '%s' occurs more than once in the dataset - replacing previous count %d with %d...", token,htCounts.get(token),count));

    	        htCounts.put(token, count);
    	    }
        }
    	cc.pushDataComponentToOutput(OUT_TOKEN_COUNTS, BasicDataTypesTools.mapToIntegerMap(htCounts, bOrdered));
    }

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
    }
}
