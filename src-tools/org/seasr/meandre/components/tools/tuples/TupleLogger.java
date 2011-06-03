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

package org.seasr.meandre.components.tools.tuples;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
import org.seasr.datatypes.core.BasicDataTypes.Strings;
import org.seasr.datatypes.core.BasicDataTypes.StringsArray;
import org.seasr.datatypes.core.BasicDataTypesTools;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.abstracts.AbstractExecutableComponent;
import org.seasr.meandre.support.components.tuples.SimpleTuple;
import org.seasr.meandre.support.components.tuples.SimpleTuplePeer;

/**
 *
 * @author Mike Haberman;
 * @author Boris Capitanu
 *
 */

@Component(
		name = "Tuple Logger",
		creator = "Mike Haberman",
		baseURL = "meandre://seasr.org/components/foundry/",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		tags = "tuple, tools, text, filter",
		description = "This component prints the incoming set of tuples to the console (level info) " ,
		dependency = {"trove-2.0.3.jar","protobuf-java-2.2.0.jar"}
)
public class TupleLogger extends AbstractExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

	@ComponentInput(
			name = Names.PORT_TUPLES,
			description = "The tuple(s)" +
			    "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings" +
			    "<br>TYPE: org.seasr.datatypes.BasicDataTypes.StringsArray"
	)
	protected static final String IN_TUPLES = Names.PORT_TUPLES;

	@ComponentInput(
			name = Names.PORT_META_TUPLE,
			description = "The meta data for tuples" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
	)
	protected static final String IN_META_TUPLE = Names.PORT_META_TUPLE;

    //------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
			name = Names.PORT_TUPLES,
			description = "The tuple(s) (same as input)" +
			    "<br>TYPE: same as input"
	)
	protected static final String OUT_TUPLES = Names.PORT_TUPLES;

	@ComponentOutput(
			name = Names.PORT_META_TUPLE,
			description = "The meta data for the tuple(s) (same as input)" +
			    "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
	)
	protected static final String OUT_META_TUPLE = Names.PORT_META_TUPLE;

    //----------------------------- PROPERTIES ---------------------------------------------------

	@ComponentProperty(
			name = "columnSet",
			description = "optional, specifiy a subset of fields to print (e.g 1,3,5) ",
		    defaultValue = ""
	)
	protected static final String PROP_COL = "columnSet";

	//--------------------------------------------------------------------------------------------


	List<Integer> idxList = null;
	String[] values = null;


	//--------------------------------------------------------------------------------------------

	@Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception
    {
		String cols = ccp.getProperty(PROP_COL);
		if (cols != null && cols.trim().length() > 0) {
			idxList = new ArrayList<Integer>();
			StringTokenizer tokens = new StringTokenizer(cols.trim(), ",");
			while (tokens.hasMoreTokens()) {
				int i = Integer.parseInt(tokens.nextToken());
				idxList.add(new Integer(i));
			}

			values = new String[idxList.size()];
		}
	}

	@Override
    public void executeCallBack(ComponentContext cc) throws Exception
    {
        Strings inputMeta = (Strings) cc.getDataComponentFromInput(IN_META_TUPLE);
        SimpleTuplePeer inPeer  = new SimpleTuplePeer(inputMeta);

        Object input = cc.getDataComponentFromInput(IN_TUPLES);
        Strings[] tuples;

        if (input instanceof StringsArray)
            tuples = BasicDataTypesTools.stringsArrayToJavaArray((StringsArray) input);

        else

        if (input instanceof Strings) {
            Strings inTuple = (Strings)input;
            tuples = new Strings[] { inTuple };
        }
        else
            throw new ComponentExecutionException("Don't know how to handle input of type: " + input.getClass().getName());


		console.info(inPeer.toString());

		SimpleTuple tuple = inPeer.createTuple();

		for (int i = 0; i < tuples.length; i++) {
			tuple.setValues(tuples[i]);
			if (idxList == null) {
			   console.info(tuple.toString());
			}
			else {
				for (int j = 0; j < idxList.size(); j++) {
					int idx = idxList.get(j);
					if (idx < inPeer.size()) {
					  values[j] = tuple.getValue(idx);
					}
					else {
						console.warning("index beyond tuple field");
					}
				}

				console.info(SimpleTuplePeer.toString(values));
			}
		}

		cc.pushDataComponentToOutput(OUT_TUPLES, input);
		cc.pushDataComponentToOutput(OUT_META_TUPLE, inputMeta);
	}

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
        idxList = null;
        values = null;
    }

    //--------------------------------------------------------------------------------------------

    @Override
    public void handleStreamInitiators() throws Exception {
        console.finer("### BEGIN marker ###");
        super.handleStreamInitiators();
    }

    @Override
    public void handleStreamTerminators() throws Exception {
        console.finer("### END marker ###");
        super.handleStreamTerminators();
    }
}
