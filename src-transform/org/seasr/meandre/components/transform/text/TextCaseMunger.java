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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * @author Ian Wood
 */

@Component(
        creator = "Ian Wood",
        description = "Changes to lower case except for words in ALL CAPS. ",
        name = "Text Case Munger",
        tags = "#TRANSFORM, text, remove, replace, case",
        rights = Licenses.UofINCSA,
        baseURL = "meandre://seasr.org/components/foundry/",
        dependency = {"protobuf-java-2.2.0.jar"}
)
public class TextCaseMunger extends AbstractExecutableComponent {

	//------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            description = "The text to change case." +
                          "<br>TYPE: java.lang.String" +
                          "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings" +
                          "<br>TYPE: byte[]" +
                          "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Bytes" +
                          "<br>TYPE: java.lang.Object",
            name = Names.PORT_TEXT
    )
    protected static final String IN_TEXT = Names.PORT_TEXT;

    //------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            description = "The processed text." +
                          "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings",
            name = Names.PORT_TEXT
    )
    protected static final String OUT_TEXT = Names.PORT_TEXT;

    //------------------------------ PROPERTIES --------------------------------------------------

    static final String MIXED_CASE_PATTERN = "\\w*[a-z]+[A-Z]+\\w*|\\w*[A-Z]+[a-z]+\\w*";
	@ComponentProperty(
	        description = "Optional regular expression to find mixed case words. " +
            	          "If this property is left blank, the pattern '" + MIXED_CASE_PATTERN + "' " +
            	          "is used. " +
            	          "Note that in this case, the underscore character '_' is treated as part of a word.",
            name = Names.PROP_FIND,
            defaultValue = ""
	)
	protected static final String PROP_FIND = Names.PROP_FIND;

	//--------------------------------------------------------------------------------------------
	
	Pattern mixedCaseFinder;
	
	//--------------------------------------------------------------------------------------------

	@Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
		String newCasePattern = getPropertyOrDieTrying(PROP_FIND, false, false, ccp);
		if (newCasePattern.length() == 0) 
			newCasePattern = MIXED_CASE_PATTERN;
		mixedCaseFinder = Pattern.compile(newCasePattern);
	}

	@Override
    public void executeCallBack(ComponentContext cc) throws Exception {
		String[] input = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_TEXT));
		String[] output = new String[input.length];

		for (int i = 0, iMax = input.length; i < iMax; i++) {
		    String text = input[i];
		    
		    Matcher matcher = mixedCaseFinder.matcher(text);
	        while (matcher.find()) {
	        	String replace = matcher.group().toLowerCase();
	        	matcher.reset();
	        	text = matcher.replaceFirst(replace);
	        	matcher = mixedCaseFinder.matcher(text);
	        	console.fine(String.format("Munged mixed case to %s", replace));
	        }
	        
		    output[i] = text;
		}

		cc.pushDataComponentToOutput(OUT_TEXT, BasicDataTypesTools.stringToStrings(output));
	}

	@Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
	}
}
