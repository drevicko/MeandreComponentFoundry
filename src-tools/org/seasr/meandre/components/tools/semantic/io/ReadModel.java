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

package org.seasr.meandre.components.tools.semantic.io;

import java.net.URI;
import java.util.Set;
import java.util.logging.Logger;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.components.abstracts.AbstractExecutableComponent;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.system.components.ext.StreamDelimiter;
import org.meandre.core.system.components.ext.StreamInitiator;
import org.meandre.core.system.components.ext.StreamTerminator;
import org.seasr.datatypes.BasicDataTypesTools;
import org.seasr.meandre.components.tools.Names;
import org.seasr.meandre.support.io.ModelUtils;
import org.seasr.meandre.support.parsers.DataTypeParser;

/**
 * Reads a Jena Model from a location (remote or local)
 *
 * @author Xavier Llor&agrave;
 * @author Boris Capitanu
 */
@Component(
		name = "Read Semantic Model",
		creator = "Xavier Llora",
		baseURL = "meandre://seasr.org/components/tools/",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		dependency = {"protobuf-java-2.0.3.jar"},
		tags = "semantic, io, read, model",
		description = "This component reads a RDF model. The model name is specified " +
				      "in the input. Also, it is able to read from URLs and local files " +
				      "using URL of file syntax. The component outputs the semantic model " +
				      "read. A property allows to control the behaviour of the component in " +
				      "front of an IO error, allowing to continue pushing and empty model or " +
				      "throwing and exception forcing the finalization of the flow execution."
)
public class ReadModel extends AbstractExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

	@ComponentInput(
			name = Names.PORT_LOCATION,
			description = "The URL or file name containing the model to read"
	)
	protected static final String IN_LOCATION = Names.PORT_LOCATION;

    //------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
			name = Names.PORT_LOCATION,
			description = "The URL or file name containing the model read"
	)
	protected static final String OUT_LOCATION = Names.PORT_LOCATION;

	@ComponentOutput(
			name = Names.PORT_DOCUMENT,
			description = "The model containing the semantic document read"
	)
	protected static final String OUT_DOCUMENT = Names.PORT_DOCUMENT;

    //------------------------------ PROPERTIES --------------------------------------------------

    @ComponentProperty(
            name=Names.PROP_BASE_URI,
            description = "The base URI to be used when converting relative URI's to absolute URI's. " +
                          "The base URI may be null if there are no relative URIs to convert. " +
                          "A base URI of \"\" may permit relative URIs to be used in the model.",
            defaultValue = "seasr://seasr.org/document/base"
    )
    protected static final String PROP_BASE_URI = Names.PROP_BASE_URI;

	//--------------------------------------------------------------------------------------------


	/** The base url to use */
	private String sBaseURI;

	private Logger _console;


	//--------------------------------------------------------------------------------------------

	public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
	    _console = getConsoleLogger();

		this.sBaseURI = ccp.getProperty(PROP_BASE_URI);
	}

	public void executeCallBack(ComponentContext cc) throws Exception {
		URI location = DataTypeParser.parseAsURI(cc.getDataComponentFromInput(IN_LOCATION));

		cc.pushDataComponentToOutput(OUT_LOCATION, BasicDataTypesTools.stringToStrings(location.toString()));
		cc.pushDataComponentToOutput(OUT_DOCUMENT, ModelUtils.getModel(location, sBaseURI));
	}

    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
        this.sBaseURI = null;
    }

    //-----------------------------------------------------------------------------------

    @Override
    protected void handleStreamInitiators(ComponentContext cc, Set<String> inputPortsWithInitiators)
            throws ComponentContextException {
        pushDelimiters(cc, (StreamInitiator)cc.getDataComponentFromInput(IN_LOCATION));
    }

    @Override
    protected void handleStreamTerminators(ComponentContext cc, Set<String> inputPortsWithTerminators)
            throws ComponentContextException {
        pushDelimiters(cc, (StreamTerminator)cc.getDataComponentFromInput(IN_LOCATION));
    }

    //-----------------------------------------------------------------------------------

	/**
	 * Push the delimiters
	 *
	 * @param cc The component context
	 * @param sdLoc The delimiter object
	 * @throws ComponentContextException
	 */
	private void pushDelimiters(ComponentContext cc, StreamDelimiter sdLoc)
			throws ComponentContextException {
		cc.pushDataComponentToOutput(OUT_LOCATION, sdLoc);
		try {
			StreamDelimiter sd = (StreamDelimiter) sdLoc.getClass().newInstance();
			for ( String sKey:sd.keySet() )
				sd.put(sKey, sdLoc.get(sKey));
			cc.pushDataComponentToOutput(OUT_DOCUMENT, sd);
		} catch (Exception e) {
			_console.warning("Failed to create a new delimiter - reusing existing one");
			cc.pushDataComponentToOutput(OUT_DOCUMENT, sdLoc);
		}
	}
}
