/**
 * University of Illinois/NCSA
 * Open Source License
 *
 * Copyright (c) 2008, Board of Trustees-University of Illinois.
 * All rights reserved.
 *
 * Developed by:
 *
 * Automated Learning Group
 * National Center for Supercomputing Applications
 * http://www.seasr.org
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal with the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimers.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimers in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the names of Automated Learning Group, The National Center for
 *    Supercomputing Applications, or University of Illinois, nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this Software without specific prior written permission.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * WITH THE SOFTWARE.
 */


package org.seasr.meandre.components.vis.protovis;

import org.meandre.annotations.Component;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.datatypes.core.DataTypeParser;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.vis.html.VelocityTemplateToHTML;

/**
 *
 * @author Loretta Auvil
 *
 */

@Component(
        creator = "Loretta Auvil",
        description = "This components creates a Protovis line chart with focus plus context. " +
                    "Original code from Protovis project with slight modifications. "+
                    "The JSON data input needs to be in an appropriate format as mentioned in the input description.",
        name = "Line Chart",
        tags = "#VIS, visualization, protovis, chart",
        rights = Licenses.UofINCSA,
        baseURL = "meandre://seasr.org/components/foundry/",
        dependency = { "velocity-1.7-dep.jar", "protovis-r3.2.jar" },
        resources  = { "LineChart.vm" }
)
public class LineChart extends AbstractProtovisComponent {

	//------------------------------ INPUTS -----------------------------------------------------

    @ComponentInput(
	            name = Names.PORT_JSON,
	            description = "JSON input data with the following form with x values increasing"+
	            "'[{x: 1900, y: 10},{x: 1910, y: 20},...];'" +
	            "<br>TYPE: java.lang.String" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings" +
                "<br>TYPE: byte[]" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Bytes" +
                "<br>TYPE: java.lang.Object"
    )
	protected static final String IN_JSON = Names.PORT_JSON;

    @ComponentInput(
            name = Names.PORT_TEXT,
            description = "The label to display" +
                    "<br>TYPE: java.lang.String" +
                    "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings" +
                    "<br>TYPE: byte[]" +
                    "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Bytes" +
                    "<br>TYPE: java.lang.Object"
    )
    protected static final String IN_LABEL = Names.PORT_TEXT;

	//------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
	           name = Names.PORT_HTML,
	           description = "Text containing the transformed input to html via a velocity template" +
	           "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
	)
	protected static final String OUT_HTML = Names.PORT_HTML;

	//------------------------------ PROPERTIES --------------------------------------------------

	@ComponentProperty(
	        description = "The title for the page",
	        name = Names.PROP_TITLE,
	        defaultValue = "Line Chart"
	)
	protected static final String PROP_TITLE = Names.PROP_TITLE;

	private static final String DEFAULT_TEMPLATE = "org/seasr/meandre/components/vis/protovis/LineChart.vm";
	@ComponentProperty(
	        description = "The template name",
	        name = VelocityTemplateToHTML.PROP_TEMPLATE,
	        defaultValue = DEFAULT_TEMPLATE
	)
    protected static final String PROP_TEMPLATE = VelocityTemplateToHTML.PROP_TEMPLATE;

	//--------------------------------------------------------------------------------------------

	@Override
	public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
	    super.initializeCallBack(ccp);

	    context.put("title", getPropertyOrDieTrying(PROP_TITLE, ccp));
	}

	@Override
	public void executeCallBack(ComponentContext cc) throws Exception {
	    String json = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_JSON))[0];
	    String label = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_LABEL))[0];

	    context.put("data", json);
	    context.put("label", label);

	    console.finest("data: " + json);

		super.executeCallBack(cc);
	}
}
