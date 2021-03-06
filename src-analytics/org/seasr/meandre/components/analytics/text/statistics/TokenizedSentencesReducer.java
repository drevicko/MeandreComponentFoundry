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

package org.seasr.meandre.components.analytics.text.statistics;

import java.util.Hashtable;
import java.util.Map;

import org.meandre.annotations.Component;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.datatypes.core.BasicDataTypesTools;
import org.seasr.datatypes.core.DataTypeParser;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.abstracts.AbstractStreamingExecutableComponent;

/**
 * This class accumulates tokenized sentences
 *
 * @author Boris Capitanu
 *
 */

@Component(
        name = "Tokenized Sentences Reducer",
        creator = "Boris Capitanu",
        baseURL = "meandre://seasr.org/components/foundry/",
        firingPolicy = FiringPolicy.all,
        mode = Mode.compute,
        rights = Licenses.UofINCSA,
        tags = "#TRANSFORM, accumulate, reduce, sentence, tokenized sentence",
        description = "This component is intended to work on wrapped model streams. " +
                      "Given a sequence of wrapped models, it will create a new model that " +
                      "accumulates/reduces the tokenized sentences and then pushes the resulting model. " +
                      "If no wrapped model is provided it will act as a simple pass through. This " +
                      "component is based on Wrapped models reducer.",
        dependency = {"protobuf-java-2.2.0.jar"}
)
public class TokenizedSentencesReducer extends AbstractStreamingExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            name = Names.PORT_TOKENIZED_SENTENCES,
            description = "The tokenized sentences to accumulate" +
                "<br>TYPE: java.util.Map<java.lang.String, java.lang.String[]>" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.StringsMap"
    )
    protected static final String IN_TOKENIZED_SENTENCES = Names.PORT_TOKENIZED_SENTENCES;

    //------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            name = Names.PORT_TOKENIZED_SENTENCES,
            description = "The accumulated tokenized sentences" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.StringsMap"
    )
    protected static final String OUT_TOKENIZED_SENTENCES = Names.PORT_TOKENIZED_SENTENCES;

    //------------------------------ PROPERTIES --------------------------------------------------

    // Inherited ignoreErrors (PROP_IGNORE_ERRORS) from AbstractExecutableComponent

    //--------------------------------------------------------------------------------------------


    private boolean _isStreaming;
    private Map<String, String[]> _tokenizedSentences;


    //--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        super.initializeCallBack(ccp);

        _isStreaming = false;
        _tokenizedSentences = new Hashtable<String, String[]>();
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
        Object input = cc.getDataComponentFromInput(IN_TOKENIZED_SENTENCES);

        if (!_isStreaming) {
            cc.pushDataComponentToOutput(OUT_TOKENIZED_SENTENCES, input);
            return;
        }

        _tokenizedSentences.putAll(DataTypeParser.parseAsStringStringArrayMap(input));
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
        if (_isStreaming)
            throw new Exception("Stream error - start stream marker already received!");

        _isStreaming = true;
        _tokenizedSentences.clear();
    }

    @Override
    public void endStream() throws Exception {
        if (!_isStreaming)
            throw new Exception("Stream error - received end stream marker without start stream!");

        componentContext.pushDataComponentToOutput(OUT_TOKENIZED_SENTENCES, BasicDataTypesTools.mapToStringMap(_tokenizedSentences));

        _isStreaming = false;
        _tokenizedSentences.clear();
    }
}
