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

package org.seasr.meandre.components.transform.bson;

import org.meandre.annotations.Component;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.datatypes.core.BasicDataTypesTools;
import org.seasr.meandre.components.abstracts.AbstractExecutableComponent;
import org.seasr.meandre.components.tools.db.mongodb.MongoDBClient;
import com.mongodb.BasicDBObject;

/**
 * @author Ian Wood
 */

@Component(
        name = "Select BSon Field(s)",
        creator = "Ian Wood",
        baseURL = "meandre://seasr.org/components/foundry/",
        firingPolicy = FiringPolicy.all,
        mode = Mode.compute,
        rights = Licenses.UofINCSA,
        tags = "#TRANSFORM, bson, select fields, mongo, mongodb",
        description = "This component extracts BSon fields matching a BSon projection expression and outputs the result as a string.",
        dependency = {"protobuf-java-2.2.0.jar"}
)
public class SelectBsonFields extends AbstractExecutableComponent {
	//------------------------------ STRINGS -----------------------------------------------------
	
    protected static final String PARAM_OUT_STRING = "json_or_string";
    protected static final String PARAM_FIELDS = "fields";

    //------------------------------ INPUTS -----------------------------------------------------

    @ComponentInput(
            name = MongoDBClient.BSON_STRING,
            description = "The BSON object to filter/convert" +
                "<br>TYPE: com.mongodb.BasicDBObject"
    )
    protected static final String IN_BSON = MongoDBClient.BSON_STRING;

    //------------------------------ OUTPUTS -----------------------------------------------------
    
    @ComponentOutput(
            name = "json_or_string1",
            description = "The String or JSON string result." +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String OUT_STRING1 = "json_or_string1";

    @ComponentOutput(
            name = "json_or_string2",
            description = "The String or JSON string result." +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String OUT_STRING2 = "json_or_string2";

    @ComponentOutput(
            name = "json_or_string3",
            description = "The String or JSON string result." +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String OUT_STRING3 = "json_or_string3";

    @ComponentOutput(
            name = "json_or_string4",
            description = "The String or JSON string result." +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String OUT_STRING4 = "json_or_string4";

    @ComponentOutput(
            name = "json_or_string5",
            description = "The String or JSON string result." +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String OUT_STRING5 = "json_or_string5";

    //----------------------------- PROPERTIES ---------------------------------------------------

    @ComponentProperty(
            name = PARAM_FIELDS,
            description = "A comma separated list of fields to extract from the BSON. " +
            		"Up to 5 fields can be extracted and the are output in the order " +
            		"they appear (ie: the first field is pushed to port '"+OUT_STRING1+"'." +
            				"Beware: whitespace is NOT trimmed!",
            defaultValue = ""
    )
    protected static final String PROP_FIELD = PARAM_FIELDS;

    //--------------------------------------------------------------------------------------------


	private String[] _fields = null;


    //--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
    	_fields = getPropertyOrDieTrying(PROP_FIELD, ccp).split(",");
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
        BasicDBObject bson = (BasicDBObject) cc.getDataComponentFromInput(IN_BSON);
        for (int i=0; i < _fields.length; i++) {
        	cc.pushDataComponentToOutput(PARAM_OUT_STRING+(i+1), BasicDataTypesTools.stringToStrings(bson.getString(_fields[i])));
        }
    }

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
    }

    //--------------------------------------------------------------------------------------------
}
