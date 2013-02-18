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

package org.seasr.meandre.components.tools.db;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.seasr.datatypes.core.BasicDataTypesTools;
import org.seasr.datatypes.core.DataTypeParser;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.abstracts.AbstractStreamingExecutableComponent;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.system.components.ext.StreamInitiator;
import org.meandre.core.system.components.ext.StreamTerminator;

import com.google.gwt.dev.util.Strings;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;


/**
 *
 * @author Ian Wood
 *
 */

@Component(
		name = "Query MongoDb",
		creator = "Ian Wood",
		baseURL = "meandre://seasr.org/components/foundry/",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		tags = "#INPUT, io, read, json, mongo, mongodb",
		description = "This component queries a mongodb database. " +
				"The results are returned separately, wrapped as a stream.",
		dependency = {"protobuf-java-2.2.0.jar", "mongo-2.10.1.jar"}
)
public class QueryMongoDb extends AbstractStreamingExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

	@ComponentInput(
			name = MongoDBClient.MONGODB_CONN_CLIENT,
			description = "A mongodb Java client" +
                "<br>TYPE: com.mongodb.MongoClient"
	)
	protected static final String IN_MONGO_CLIENT = MongoDBClient.MONGODB_CONN_CLIENT;

	@ComponentInput(
			name = Names.PORT_QUERY,
			description = "The mongodb query to perform (as a JSON string)." + 
                "<br>TYPE: String" //+
                //"<br>TYPE: org.json.JSONObject.JSONObject"
	)
	protected static final String IN_QUERY = Names.PORT_QUERY;

	@ComponentInput(
			name = "projection",
			description = "The mongodb projection (as a JSON string) - this specifies the fields to be included " +
					"from the retreived objects." + 
                "<br>TYPE: String"// +
                //"<br>TYPE: org.json.JSONObject.JSONObject"
	)
	protected static final String IN_PROJECTION = "projection";

	//------------------------------ OUTPUTS -----------------------------------------------------

//    @ComponentOutput(
//            name = MongoDBClient.MONGODB_CONN_CLIENT,
//            description = "The mongodb Java client" +
//                "<br>TYPE: com.mongodb.MongoClient"
//    )
//    protected static final String OUT_MONGODB_CONN_CLIENT = MongoDBClient.MONGODB_CONN_CLIENT;

	@ComponentOutput(
			name = MongoDBClient.BSON_STRING,
			description = "The URL or file name containing the model read" +
                "<br>TYPE: com.mongodb.BasicDBObject"
	)
	protected static final String OUT_BSON = MongoDBClient.BSON_STRING;

    //------------------------------ PROPERTIES --------------------------------------------------

	// Inherited ignoreErrors (PROP_IGNORE_ERRORS) from AbstractExecutableComponent

	@ComponentProperty(
            name = "database_name",
            description = "The name of the database within mongodb server.",
            defaultValue = ""
    )
    protected static final String PROP_DB_NAME = "database_name";

	@ComponentProperty(
            name = "collection_name",
            description = "The name of the collection from which to draw data.",
            defaultValue = ""
    )
    protected static final String PROP_COLLECTION_NAME = "collection_name";

	@ComponentProperty(
            name = "wrap_stream",
            description = "Should the output be wrapped in a stream?",
            defaultValue = "true"
    )
    protected static final String PROP_WRAP_STREAM = "wrap_stream";

	//--------------------------------------------------------------------------------------------


	MongoClient _client = null;
	String _dbName = null;
	DB _db = null;
	String _collectionName = null;
	DBCollection _collection = null;
    protected boolean _wrapStream;


	//--------------------------------------------------------------------------------------------

	@Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        super.initializeCallBack(ccp);
        
		_dbName = getPropertyOrDieTrying(PROP_DB_NAME, ccp);
		_collectionName = getPropertyOrDieTrying(PROP_COLLECTION_NAME, ccp);
        _wrapStream = Boolean.parseBoolean(getPropertyOrDieTrying(PROP_WRAP_STREAM, ccp));
	}

	@Override
    public void executeCallBack(ComponentContext cc) throws Exception {

        if (cc.isInputAvailable(IN_MONGO_CLIENT)) {
			if (_client == null) {
				_client = (MongoClient) cc.getDataComponentFromInput(IN_MONGO_CLIENT);
				_db = _client.getDB(_dbName);
				_collection = _db.getCollection(_collectionName);
			}
        }
		
		DBObject query = (DBObject) JSON.parse(Strings.join(DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_QUERY))," "));
		DBObject projection = (DBObject) JSON.parse(Strings.join(DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_PROJECTION))," "));
		
        if (_wrapStream)
            cc.pushDataComponentToOutput(OUT_BSON, new StreamInitiator(streamId));

		DBCursor cursor = _collection.find(query, projection);		
		try {
		   while(cursor.hasNext()) {
			   cc.pushDataComponentToOutput(OUT_BSON, cursor.next());
		       System.out.println();
		   }
		} finally {
		   cursor.close();
		}
		        
        if (_wrapStream)
            cc.pushDataComponentToOutput(OUT_BSON, new StreamTerminator(streamId));
	}

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
    }
    

	@Override
	public boolean isAccumulator() {
		return false;
	}
}
