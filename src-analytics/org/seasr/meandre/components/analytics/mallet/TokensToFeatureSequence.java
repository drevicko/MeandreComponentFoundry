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

package org.seasr.meandre.components.analytics.mallet;

import org.meandre.annotations.Component;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.datatypes.core.DataTypeParser;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.abstracts.AbstractStreamingExecutableComponent;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;

@Component(
        name = "Tokens To Mallet Feature Sequence",
        creator = "Ian Wood",
        baseURL = "meandre://seasr.org/components/foundry/",
        firingPolicy = FiringPolicy.all,
        mode = Mode.compute,
        rights = Licenses.UofINCSA,
        tags = "#TRANSFORM, token, text, convert, mallet, feature",
        description = "This component converts a collection of tokens into a Mallet feature sequence. "+
        		"A cc.mallet.types.Alphabet object is kept for the duration of a stream to store the vocabulary.",
        dependency = {"protobuf-java-2.2.0.jar"}
)
public class TokensToFeatureSequence extends AbstractStreamingExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

	@ComponentInput(
			name = Names.PORT_TOKENS,
			description = "The tokens to convert to text" +
    			 "<br>TYPE: java.lang.String" +
                 "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings" +
                 "<br>TYPE: byte[]" +
                 "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Bytes" +
                 "<br>TYPE: java.lang.Object"
	)
	protected static final String IN_TOKENS = Names.PORT_TOKENS;

    //------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            name = "mallet_feature_sequence",
            description = "A Mallet feature sequence containing the data from the tuples" +
                "<br>TYPE: cc.mallet.types.FeatureSequence"
    )
    protected static final String OUT_FEATURE_SEQ = "mallet_feature_sequence";

    //----------------------------- PROPERTIES ---------------------------------------------------

    //--------------------------------------------------------------------------------------------

    protected Alphabet _alphabet;


    //--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        super.initializeCallBack(ccp);
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
		String[] tokens = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_TOKENS));

        int capacity = tokens.length;
        Alphabet alphabet = (_alphabet != null) ? _alphabet : new Alphabet();
        FeatureSequence featureSequence = new FeatureSequence(alphabet, capacity);
        for (String token : tokens) {
        	featureSequence.add(token);
        }

        cc.pushDataComponentToOutput(OUT_FEATURE_SEQ, featureSequence);
    }

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
    }

    //--------------------------------------------------------------------------------------------

    @Override
    public boolean isAccumulator() {
        return true;
    }

    @Override
    public void startStream() throws Exception {
        _alphabet = new Alphabet();
    }

    @Override
    public void endStream() throws Exception {
        _alphabet = null;
    }
}
