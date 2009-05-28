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

package org.seasr.meandre.components.analytics.text.io.console;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.seasr.datatypes.BasicDataTypesTools;
import org.seasr.meandre.components.tools.Names;
import org.seasr.meandre.support.parsers.DataTypeParser;


/** This component tokenizes the text contained in the input model using OpenNLP.
 *
 * @author Xavier Llor&agrave;
 * @author Boris Capitanu
 *
 */

@Component(
		name = "Tokens To Text",
		creator = "Xavier Llora",
		baseURL = "meandre://seasr.org/components/tools/",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		tags = "semantic, tools, text, tokenizer, counting",
		description = "Given a collection of tokens, this component converts it " +
				      "into text.",
		dependency = {"protobuf-java-2.0.3.jar"}
)
public class TokensToText extends AnalysisToText {

    //------------------------------ INPUTS ------------------------------------------------------

	@ComponentInput(
			name = Names.PORT_TOKENS,
			description = "The tokens to convert to text"
	)
	protected static final String IN_TOKENS = Names.PORT_TOKENS;

	//------------------------------ PROPERTIES --------------------------------------------------

    @ComponentProperty(
            name = Names.PROP_MESSAGE,
            description = "The header to use.",
            defaultValue = "Available tokens"
    )
    protected static final String PROP_MESSAGE = Names.PROP_MESSAGE;


	//--------------------------------------------------------------------------------------------

	public void executeCallBack(ComponentContext cc) throws Exception {
		String[] tokens = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_TOKENS));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);

		printStrings(ps, tokens, this.iCount, this.iOffset);

		cc.pushDataComponentToOutput(OUT_TEXT, BasicDataTypesTools.stringToStrings(baos.toString()));
	}
}
