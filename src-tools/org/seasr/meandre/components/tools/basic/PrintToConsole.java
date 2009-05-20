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

package org.seasr.meandre.components.tools.basic;

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
import org.seasr.meandre.components.tools.Names;
import org.seasr.meandre.support.parsers.DataTypeParser;

/**
 * Prints an object to the console
 *
 * @author Xavier Llor&agrave
 * @author Boris Capitanu
 */
@Component(
		name = "Print to console",
		creator = "Xavier Llora",
		baseURL = "meandre://seasr.org/components/tools/",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		tags = "io, print, console",
		dependency = {"protobuf-java-2.0.3.jar"},
		description = "This component takes the input and prints it to the console. "
)
public class PrintToConsole extends AbstractExecutableComponent {

    //--------------------------------------------------------------------------------------------

	@ComponentInput(
			name = Names.PORT_OBJECT,
			description = "The object to print"
		)
	protected static final String IN_OBJECT = Names.PORT_OBJECT;

	@ComponentOutput(
			name = Names.PORT_OBJECT,
			description = "The object printed"
		)
	protected static final String OUT_OBJECT = Names.PORT_OBJECT;

	//--------------------------------------------------------------------------------------------

    @ComponentProperty(
            name = Names.PROP_WRAP_STREAM,
            description = "Should the printed object be wrapped as a stream? ",
            defaultValue = "false"
        )
    protected static final String PROP_WRAP_STREAM = Names.PROP_WRAP_STREAM;

	//--------------------------------------------------------------------------------------------

	/** Should be wrapped */
	private boolean bWrapped;

	private Logger _console;

	//--------------------------------------------------------------------------------------------

	public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
	    _console = getConsoleLogger();
		bWrapped = Boolean.parseBoolean(ccp.getProperty(PROP_WRAP_STREAM));
	}

	public void executeCallBack(ComponentContext cc) throws Exception {
		Object data = cc.getDataComponentFromInput(IN_OBJECT);
        _console.fine("Got input of type: " + data.getClass().toString());

        cc.getOutputConsole().println(DataTypeParser.parseAsString(data));

		cc.pushDataComponentToOutput(OUT_OBJECT, data);
	}

	public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
	    bWrapped = false;
	}

	/* (non-Javadoc)
	 * @see org.meandre.components.abstracts.AbstractExecutableComponent#handleStreamInitiators(org.meandre.core.ComponentContext, java.util.Set)
	 */
	@Override
	protected void handleStreamInitiators(ComponentContext cc, Set<String> inputPortsWithInitiators)
	        throws ComponentContextException {
	    if (bWrapped)
	           printStreamDelimiter(cc, cc.getDataComponentFromInput(IN_OBJECT));
	}

	/* (non-Javadoc)
	 * @see org.meandre.components.abstracts.AbstractExecutableComponent#handleStreamTerminators(org.meandre.core.ComponentContext, java.util.Set)
	 */
	@Override
	protected void handleStreamTerminators(ComponentContext cc, Set<String> inputPortsWithTerminators)
	        throws ComponentContextException {
	   if (bWrapped)
	       printStreamDelimiter(cc, cc.getDataComponentFromInput(IN_OBJECT));
	}

	/** Prints a stream delimiter
	 *
	 * @param cc The component context
	 * @param obj The delimiter to print
	 */
	private void printStreamDelimiter(ComponentContext cc, Object obj) {
		StringBuffer sb = new StringBuffer();
		String [] saName = obj.getClass().getName().split("\\"+".");
		sb.append(saName[saName.length-1]+" [ ");
		StreamDelimiter sd = (StreamDelimiter) obj;
		for ( String sKey:sd.keySet())
			sb.append("("+sKey+"="+sd.get(sKey)+") ");
		sb.append("]");
		cc.getOutputConsole().println(sb);
	}

	//-----------------------------------------------------------------------------------


}
