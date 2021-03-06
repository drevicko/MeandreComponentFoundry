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

package org.seasr.meandre.components.transform.totext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat.CSVFormatBuilder;
import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.datatypes.core.BasicDataTypesTools;
import org.seasr.datatypes.core.Names;
import org.seasr.datatypes.core.BasicDataTypes.DoublesMap;

/**
 * This component tokenizes the text contained in the input model using OpenNLP.
 *
 * @author Ian Wood
 *
 */

@Component(
		name = "Token Double Values To Text",
		creator = "Ian Wood",
		baseURL = "meandre://seasr.org/components/foundry/",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		tags = "token count, text, convert, double, token value, #transform,csv",
		description = "Given a collection of token values (as doubles), this component converts it " +
				      "into csv text. The default separator is a comma, so make sure tokens " +
				      "do not have commas or change the separator.",
		dependency = {"protobuf-java-2.2.0.jar"}
)
public class TokenDoubleValuesToText extends AnalysisToText {

    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            name = Names.PORT_TOKEN_DOUBLE_VALUES,
            description = "The tokens and associated double values to convert to text" +
            "<br>TYPE: org.seasr.datatypes.core.BasicDataTypes.DoublesMap"
    )
    protected static final String INPUT_TOKEN_VALUES = Names.PORT_TOKEN_DOUBLE_VALUES;

    //------------------------------ PROPERTIES --------------------------------------------------

	@ComponentProperty(
			name = "header",
			description = "The csv header row. " +
					"A comma-separated list of attribute names. The commas will be replaced " +
					"by the separator specified in the " + PROP_TEXT_SEPARATOR + " property. " +
					"Defaults to 'tokens,counts'. If this property is empty, " +
					"no header will be used.",
			defaultValue = "tokens,counts"
	)
	protected static final String PROP_HEADER = "header";

	//--------------------------------------------------------------------------------------------

	private CSVFormat format;
	
	//--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
    	super.initializeCallBack(ccp);
        CSVFormatBuilder fmtBuilder = CSVFormat.newBuilder(textSep.charAt(0)).withRecordSeparator(System.getProperty("line.separator"));
        if (bHeaderAdded) fmtBuilder = fmtBuilder.withHeader(sHeader.split(","));
        format = fmtBuilder.build();
    }
    
	@Override
    public void executeCallBack(ComponentContext cc) throws Exception {
	    Map<String, Double> tokenValues = BasicDataTypesTools.DoubleMapToMap((DoublesMap)cc.getDataComponentFromInput(INPUT_TOKEN_VALUES));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos,false,encoding);

	    CSVPrinter printer = new CSVPrinter(ps,format);
	    for (Entry<String, Double> token : tokenValues.entrySet()) {
	    	printer.printRecord(token.getKey(),token.getValue());
	    }
	    printer.close();
	    
//		printDoublesMap(ps, tokenValues, this.iCount, this.iOffset);
//		System.out.println(String.format("TokenDoubleValuesToText: printed %d values making %d chars of output",tokenValues.getKeyCount()-this.iOffset,baos.size()));
		console.fine(String.format("printed %d values making %d chars of output",tokenValues.size()-this.iOffset,baos.size()));

		cc.pushDataComponentToOutput(OUT_TEXT, BasicDataTypesTools.stringToStrings(baos.toString(encoding)));
	}
}
