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

package org.seasr.meandre.components.analytics.psychometrics;

import java.util.Hashtable;
import java.util.Map;

import org.meandre.annotations.Component;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.datatypes.core.BasicDataTypes.DoublesMap;
import org.seasr.datatypes.core.BasicDataTypesTools;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.abstracts.AbstractExecutableComponent;

import drevicko.psycholingua.WordClassDictionary;
import drevicko.psycholingua.WordCounter.WordClassFloatCount;

/**
 * This component calculates LIWC scores from token distributions.
 *
 * @author Ian Wood;
 *
 */

@Component(
		name = "LIWC on Token Distributions",
		creator = "Ian Wood",
		baseURL = "meandre://seasr.org/components/foundry/",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		tags = "token count, text, convert, double, token value, LIWC, #ANALYTICS",
		description = "Given a collection of word frequecies (as doubles), this component calculates " +
				      "LIWC scores for the stream.",
		dependency = {"protobuf-java-2.2.0.jar,drevicko-LIWC.jar"}
)
public class LIWCOnTokenDoubleValues extends AbstractExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            name = "topic_word_distributions",
            description = "The words and associated probabilities as double values " +
            "<br>TYPE: org.seasr.datatypes.core.BasicDataTypes.DoublesMap"
    )
    protected static final String INPUT_WORD_DISTRIBUTION = "topic_word_distributions";

    //------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
	        name = "LIWC_scores",
	        description = "The calculated LIWC scores." +
                "<br>TYPE: org.seasr.datatypes.core.BasicDataTypes.DoublesMap"
	)
    protected static final String OUT_LIWC_SCORES = "LIWC_scores";

	@ComponentOutput(
	        name = "topic_size",
	        description = "The calculated LIWC scores." +
                "<br>TYPE: org.seasr.datatypes.core.BasicDataTypes.DoublesMap"
	)
    protected static final String OUT_SIZE = "topic_size";
	@ComponentOutput(
	        name = "topic_word_distributions",
	        description = "The word distribution that was input." +
	            "<br>TYPE: org.seasr.datatypes.core.BasicDataTypes.DoublesMap"
	)
    protected static final String OUT_WORD_DISTRIBUTION = "topic_word_distributions";

    //------------------------------ PROPERTIES --------------------------------------------------

	@ComponentProperty(
			name = Names.PROP_FILENAME,
			description = "The location of the LIWC dictionary file.",
		    defaultValue = ""
	)
	protected static final String PROP_LIWC_DICT = Names.PROP_FILENAME;

	//--------------------------------------------------------------------------------------------

	protected String _dictFileName;
	private WordClassDictionary dict;
	
	//--------------------------------------------------------------------------------------------

	@Override
	public void initializeCallBack(ComponentContextProperties ccp)
			throws Exception {
        _dictFileName = getPropertyOrDieTrying(PROP_LIWC_DICT, ccp);
        dict = new WordClassDictionary(_dictFileName);
	}

	@Override
    public void executeCallBack(ComponentContext cc) throws Exception {
	    DoublesMap tokenValues = (DoublesMap)cc.getDataComponentFromInput(INPUT_WORD_DISTRIBUTION);	   
	    
	    WordClassFloatCount[] LIWC_Values = null;
	    try {
	    	LIWC_Values = dict.countFloatClasses(BasicDataTypesTools.DoubleMapToMap(tokenValues));
	    } catch (IllegalArgumentException e) {
	    	console.warning(String.format("Failed to calculate LIWC values! : %s", e.getMessage()));
	    	LIWC_Values = new WordClassFloatCount[0];
	    }
//		System.out.print("LIWCOnTokenDoubleValues:");
		Map<String, Double> out = new Hashtable<String, Double>();
		for (WordClassFloatCount fc : LIWC_Values) {
			out.put(dict.getClassName(fc.classId),fc.countFloat);
//			System.out.print(fc);
		}
		
//		System.out.println();
//		System.out.println(String.format("LIWCOnTokenDoubleValues: found %d classes",out.size()));
		console.fine(String.format("LIWC float counter found %d classes from LIWC_Values array of %d",out.size(),LIWC_Values.length));
		
		cc.pushDataComponentToOutput(OUT_LIWC_SCORES, BasicDataTypesTools.mapToDoubleMap(out, false));
		cc.pushDataComponentToOutput(OUT_WORD_DISTRIBUTION, tokenValues);
	}

	@Override
	public void disposeCallBack(ComponentContextProperties ccp)
			throws Exception {
		// FIXME Auto-generated method stub
		
	}
}
