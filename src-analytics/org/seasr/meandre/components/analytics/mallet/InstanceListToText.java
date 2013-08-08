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
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.system.components.ext.StreamInitiator;
import org.meandre.core.system.components.ext.StreamTerminator;
import org.seasr.datatypes.core.BasicDataTypesTools;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.abstracts.AbstractStreamingExecutableComponent;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

@Component(
        name = "Instances to Text",
        creator = "Ian Wood",
        baseURL = "meandre://seasr.org/components/foundry/",
        firingPolicy = FiringPolicy.all,
        mode = Mode.compute,
        rights = Licenses.UofINCSA,
        tags = "#TRANSFORM, mallet, instance, feature",
        description = "This component generates a stream of strings from a mallet instance list. " +
        		"It assumes each instance is a Mallet feature sequence and aborts if this is not the case. " ,
        dependency = {"protobuf-java-2.2.0.jar"}
)
public class InstanceListToText extends AbstractStreamingExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            name = "mallet_instance_list",
            description = "The Mallet instance list" +
                    "typically used for training or testing of a machine learning algorithm. " +
                "<br>TYPE: cc.mallet.types.InstanceList"
    )
    protected static final String IN_INSTANCE_LIST = "mallet_instance_list";

    
    //------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            name = "mallet_instance_list",
            description = "The Mallet instance list that was input. " +
                "<br>TYPE: cc.mallet.types.InstanceList"
    )
    protected static final String OUT_INSTANCE_LIST = "mallet_instance_list";
    
    @ComponentOutput(
            name = Names.PORT_TOKENS,
            description = "A stream of arrays of tokens extracted from the instance list." +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String OUT_TOKENS = Names.PORT_TOKENS;
    

    //----------------------------- PROPERTIES ---------------------------------------------------
    
	@ComponentProperty(
            name = "wrap_stream",
            description = "Should the output be wrapped in a stream?",
            defaultValue = "true"
    )
    protected static final String PROP_WRAP_STREAM = "wrap_stream";

    //--------------------------------------------------------------------------------------------

	protected String[] tokens = new String[1000];
	protected int tokenLength = 0;
    protected boolean _wrapStream;
    

    //--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        super.initializeCallBack(ccp);
        _wrapStream = Boolean.parseBoolean(getPropertyOrDieTrying(PROP_WRAP_STREAM, ccp));
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
        if (_wrapStream)
            cc.pushDataComponentToOutput(OUT_TOKENS, new StreamInitiator(streamId));

		InstanceList _instanceList = (InstanceList) cc.getDataComponentFromInput(IN_INSTANCE_LIST);
		cc.pushDataComponentToOutput(OUT_INSTANCE_LIST, _instanceList);

        Alphabet inAlphabet = _instanceList.getAlphabet();
        if (inAlphabet == null && _instanceList.size() > 0)
        		inAlphabet = _instanceList.get(0).getDataAlphabet();
        console.info(String.format("Got instance list alphabet of length %s ",inAlphabet.size()));
        for (Instance instance : _instanceList) {
        	tokenLength = 0;
        	FeatureSequence features = (FeatureSequence)instance.getData();
        	if (features.getLength() > tokens.length) tokens = new String[features.getLength()+100];
        	inAlphabet.lookupObjects( features.getFeatures(), tokens);
        	cc.pushDataComponentToOutput(OUT_TOKENS, BasicDataTypesTools.stringToStrings(tokens));
        }
        
        if (_wrapStream)
            cc.pushDataComponentToOutput(OUT_TOKENS, new StreamTerminator(streamId));
        
    }

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
    }

    //--------------------------------------------------------------------------------------------
    
	@Override
	public boolean isAccumulator() {
		return false;
	}
}
