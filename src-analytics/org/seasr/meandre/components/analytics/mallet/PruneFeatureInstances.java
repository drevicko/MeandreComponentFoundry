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

import gnu.trove.TObjectIntHashMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.meandre.annotations.Component;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.abstracts.AbstractExecutableComponent;
import org.seasr.meandre.components.abstracts.AbstractStreamingExecutableComponent;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

@Component(
        name = "Prune Mallet Instances",
        creator = "Ian Wood",
        baseURL = "meandre://seasr.org/components/foundry/",
        firingPolicy = FiringPolicy.all,
        mode = Mode.compute,
        rights = Licenses.UofINCSA,
        tags = "#TRANSFORM, mallet, instance, feature",
        description = "This component removes infrequent tokens from a Mallet instance list. " +
        		"It assumes each instance is a Mallet feature sequence and aborts if this is not the case. " +
        		"This is particularly useful for toic modelling, where highly infrequent tokens contribute little to the model." ,
        dependency = {"protobuf-java-2.2.0.jar"}
)
public class PruneFeatureInstances extends AbstractExecutableComponent {

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
    

    //----------------------------- PROPERTIES ---------------------------------------------------

    @ComponentProperty(
            name = "min_freqency",
            description = "Tokens ocurring less than this number in the instance list will be removed.",
            defaultValue = "10"
    )
    protected static final String PROP_MIN_FREQENCY = "min_freqency";
    
    //--------------------------------------------------------------------------------------------


	protected Integer _minFrequency;


    //--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        String minFrequency = getPropertyOrDieTrying(PROP_MIN_FREQENCY, ccp);
        //use getPropertyOrDieTrying(propName, true, false, context) if we're ok with empty minFrequency
        _minFrequency = minFrequency.length() > 0 ? Integer.parseInt(minFrequency) : 0;
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
        Alphabet newAlphabet = new Alphabet(frequentCounts);
        for (Instance instance : _instanceList) {
        	FeatureSequence seq = (FeatureSequence) instance.getData();
        	seq.prune(dFrequentCounts, newAlphabet, _minFrequency);
        }

		cc.pushDataComponentToOutput(OUT_INSTANCE_LIST, _instanceList);
        
    }

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
    }

    //--------------------------------------------------------------------------------------------

}
