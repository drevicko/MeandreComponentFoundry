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

package org.seasr.meandre.components.analytics.text.readability;

import java.net.URI;
import java.util.Vector;

import org.apache.velocity.VelocityContext;
import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.components.abstracts.AbstractExecutableComponent;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.system.components.ext.StreamInitiator;
import org.meandre.core.system.components.ext.StreamTerminator;
import org.seasr.datatypes.BasicDataTypesTools;
import org.seasr.meandre.components.tools.Names;
import org.seasr.meandre.support.components.datatype.parsers.DataTypeParser;
import org.seasr.meandre.support.generic.html.VelocityTemplateService;
import org.seasr.meandre.support.generic.text.analytics.ReadabilityMeasure;

/**
 * This class implements the Flesch Kincaid Readability measure as explained
 * at http://en.wikipedia.org/wiki/Flesch-Kincaid_Readability_Test. The code is
 * based on the work done by Daniel Shiffman at
 * http://www.shiffman.net/teaching/a2z/week1/
 *
 * @author Xavier Llor&agrave;
 * @author Loretta Auvil
 * @author Boris Capitanu
 */

@Component(
		creator = "Xavier Llora",
		description = "Computes the Flesch Kincaid readability measure as explained at http://en.wikipedia.org/wiki/Flesch-Kincaid_Readability_Test. The code is based on the work done by Daniel Shiffman at http://www.shiffman.net/teaching/a2z/week1/",
		name = "Flesch Kincaid Readability Measure",
		tags = "text, readability, measure, flesch, kincaid",
		rights = Licenses.UofINCSA,
		mode = Mode.compute,
		firingPolicy = FiringPolicy.all,
		baseURL = "meandre://seasr.org/components/foundry/",
		dependency = {"protobuf-java-2.2.0.jar"},
		resources = {"FleschKincaidReadabilityMeasure.vm"}
)
public class FleschKincaidReadabilityMeasure extends AbstractExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

	@ComponentInput(
	        name = Names.PORT_TEXT,
			description = "Text content of the url page." +
                "<br>TYPE: java.lang.String" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings" +
                "<br>TYPE: byte[]" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Bytes" +
                "<br>TYPE: java.lang.Object"
	)
	protected static final String IN_CONTENT = Names.PORT_TEXT;

	@ComponentInput(
			description = "Item title",
			name = Names.PORT_DOC_TITLE
	)
	protected static final String IN_ITEM_TITLE = Names.PORT_DOC_TITLE;

	@ComponentInput(
	        name = Names.PORT_LOCATION,
			description = "Item location" +
                "<br>TYPE: java.lang.String" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings" +
                "<br>TYPE: byte[]" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Bytes" +
                "<br>TYPE: java.lang.Object"
	)
	protected static final String IN_ITEM_URL = Names.PORT_LOCATION;

    //------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
	        name = Names.PORT_HTML,
			description = "A report of the Flesch Kincaid readability measures." +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
	)
	protected static final String OUT_HTML_REPORT = Names.PORT_HTML;

	//------------------------------ PROPERTIES --------------------------------------------------

    @ComponentProperty(
            defaultValue = "org/seasr/meandre/components/analytics/text/readability/FleschKincaidReadabilityMeasure.vm",
            description = "The template to use for wrapping the HTML input.",
            name = Names.PROP_TEMPLATE
    )
    protected static final String PROP_TEMPLATE = Names.PROP_TEMPLATE;

    //--------------------------------------------------------------------------------------------


    private static final String FLESCH_KINCAID_WIKIPEDIA_URL = "http://en.wikipedia.org/wiki/Flesch-Kincaid_Readability_Test";

    private VelocityContext _context;
    private String _template;
    private Vector<FleschDocument> _fleschDocs = new Vector<FleschDocument>();
    private boolean _gotInitiator;

    public class FleschDocument {
        private final String _title;
        private final URI _location;
        private final ReadabilityMeasure _measure;

        public FleschDocument(String title, URI location, ReadabilityMeasure measure) {
            _title = title;
            _location = location;
            _measure = measure;
        }

        public String getTitle() {
            return _title;
        }

        public String getLocation() {
            return _location.toString();
        }

        public ReadabilityMeasure getMeasure() {
            return _measure;
        }
    }


    //--------------------------------------------------------------------------------------------

	@Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
	    _template = ccp.getProperty(PROP_TEMPLATE);
	    _gotInitiator = false;

	    _context = VelocityTemplateService.getInstance().getNewContext();
        _context.put("ccp", ccp);
        _context.put("FLESCH_KINCAID_WIKIPEDIA_URL", FLESCH_KINCAID_WIKIPEDIA_URL);
        _context.put("String", String.class);
        _context.put("Math", Math.class);
	}

	@Override
    public void executeCallBack(ComponentContext cc) throws Exception {
		String title = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_ITEM_TITLE))[0];
		URI location = DataTypeParser.parseAsURI(cc.getDataComponentFromInput(IN_ITEM_URL));
		String content = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_CONTENT))[0];

		console.fine(String.format("Processing '%s' from '%s'", title, location.toString()));

		ReadabilityMeasure measure = ReadabilityMeasure.computeFleschReadabilityMeasure(content);
		_fleschDocs.add(new FleschDocument(title, location, measure));

		if (!_gotInitiator) {
		    cc.pushDataComponentToOutput(OUT_HTML_REPORT,
		            BasicDataTypesTools.stringToStrings(generateReport()));
		    _fleschDocs.clear();
		}
	}

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
    }

    //--------------------------------------------------------------------------------------------

    @Override
    protected void handleStreamInitiators() throws Exception {
        if (_gotInitiator)
            throw new UnsupportedOperationException("Cannot process multiple streams at the same time!");

        if (inputPortsWithInitiators.contains(IN_ITEM_TITLE) &&
            inputPortsWithInitiators.contains(IN_ITEM_URL) &&
            inputPortsWithInitiators.contains(IN_CONTENT)) {

            _fleschDocs = new Vector<FleschDocument>();
            _gotInitiator = true;
        }
        else
            throw new Exception("Unbalanced or unexpected StreamInitiator received");
    }

    @Override
    protected void handleStreamTerminators() throws Exception {
        if (!_gotInitiator)
            throw new Exception("Received StreamTerminator without receiving StreamInitiator");

        if (inputPortsWithTerminators.contains(IN_ITEM_TITLE) &&
            inputPortsWithTerminators.contains(IN_ITEM_URL) &&
            inputPortsWithTerminators.contains(IN_CONTENT)) {

                componentContext.pushDataComponentToOutput(OUT_HTML_REPORT, new StreamInitiator());
                componentContext.pushDataComponentToOutput(OUT_HTML_REPORT,
                        BasicDataTypesTools.stringToStrings(generateReport()));
                componentContext.pushDataComponentToOutput(OUT_HTML_REPORT, new StreamTerminator());
                _gotInitiator = false;
                _fleschDocs.clear();
            }
            else
                throw new Exception("Unbalanced or unexpected StreamTerminator received");
    }

    //--------------------------------------------------------------------------------------------

    private String generateReport() throws Exception {
        VelocityTemplateService velocity = VelocityTemplateService.getInstance();
        _context.put("fleschDocs", _fleschDocs);
        return velocity.generateOutput(_context, _template);
    }
}
