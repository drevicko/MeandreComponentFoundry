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

import java.util.ArrayList;
import java.util.Arrays;
import org.meandre.annotations.Component;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.meandre.components.abstracts.AbstractExecutableComponent;
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
public class InstanceListToText extends AbstractExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            name = "mallet_instance_list",
            description = "The Mallet instance list" +
                "<br>TYPE: cc.mallet.types.InstanceList"
    )
    protected static final String IN_INSTANCE_LIST = "mallet_instance_list";

    
    //------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            name = "mallet_instance_list",
            description = "The list of accumulated machine learning instances, " +
                "typically used for training or testing of a machine learning algorithm" +
                "<br>TYPE: cc.mallet.types.InstanceList"
    )
    protected static final String OUT_INSTANCE_LIST = "mallet_instance_list";
    
    @ComponentOutput(
            name = "mallet_instance_list",
            description = "The list of accumulated machine learning instances, " +
                "typically used for training or testing of a machine learning algorithm" +
                "<br>TYPE: cc.mallet.types.InstanceList"
    )
    protected static final String OUT_INSTANCE_LIST = "mallet_instance_list";
    

    //----------------------------- PROPERTIES ---------------------------------------------------

    //--------------------------------------------------------------------------------------------


	protected Integer _minFrequency;


	private boolean _removeEmpty;


    //--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        String minFrequency = getPropertyOrDieTrying(PROP_MIN_FREQENCY, ccp);
        //use getPropertyOrDieTrying(propName, true, false, context) if we're ok with empty minFrequency
        _minFrequency = minFrequency.length() > 0 ? Integer.parseInt(minFrequency) : 0;
        _removeEmpty  = Boolean.parseBoolean(getPropertyOrDieTrying(PROP_REMOVE_EMPTY, ccp));
        console.config(String.format("removing words occuring < %d; removing empty instances %b", _minFrequency, _removeEmpty ));
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
    	InstanceList _instanceList = (InstanceList) cc.getDataComponentFromInput(IN_INSTANCE_LIST);
        
        Alphabet inAlphabet = _instanceList.getAlphabet();
        
        // first count the features
        int[] iFrequentCounts = new int[inAlphabet.size()];
        Arrays.fill(iFrequentCounts,0);
        for (Instance instance : _instanceList)
        	for (int f : ((FeatureSequence)instance.getData()).getFeatures() ) 
        		iFrequentCounts[f]++;
        
        // then convert to doubles and find out how many frequent ones
        int frequentCounts = 0;
        double[] dFrequentCounts = new double[inAlphabet.size()];
        int i=0;
        for (int count:iFrequentCounts) {
        	if (count >= _minFrequency) frequentCounts ++;
        	dFrequentCounts[i++]=(double)count;
        }
        
        // now prune the FeatureSequences
        ArrayList<Instance> remove = new ArrayList<Instance>();
        Alphabet newAlphabet = new Alphabet(frequentCounts);
        for (Instance instance : _instanceList) {
        	FeatureSequence seq = (FeatureSequence) instance.getData();
        	seq.prune(dFrequentCounts, newAlphabet, _minFrequency);
        	if (_removeEmpty && seq.getLength() == 0) {
        		remove.add(instance);
        	}
        }
        console.fine(String.format("removing %d instances", remove.size()));
        for (Instance instance : remove) {
        	_instanceList.remove(instance);
        }

		cc.pushDataComponentToOutput(OUT_INSTANCE_LIST, _instanceList);
        
    }

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
    }

    //--------------------------------------------------------------------------------------------

}
