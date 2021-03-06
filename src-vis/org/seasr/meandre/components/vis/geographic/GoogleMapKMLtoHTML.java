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

package org.seasr.meandre.components.vis.geographic;

import java.net.URL;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.meandre.annotations.Component;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.datatypes.core.DataTypeParser;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.abstracts.AbstractStreamingExecutableComponent;
import org.seasr.meandre.support.generic.html.VelocityTemplateService;

/**
 * @author Boris Capitanu
 */

@Component(
        creator="Boris Capitanu",
        description="Generates a Google Maps HTML document containing the KML layer(s) received in the input.",
        name="Google Map KML To HTML",
        tags="#VIS, google map, kml, html",
        mode=Mode.compute,
        rights = Licenses.UofINCSA,
        baseURL="meandre://seasr.org/components/foundry/",
        resources = {"GoogleMapKMLToHTML.vm"}
)
public class GoogleMapKMLtoHTML extends AbstractStreamingExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            name = Names.PORT_LOCATION,
            description = "The URL of the KML file" +
                "<br>TYPE: java.net.URI" +
                "<br>TYPE: java.net.URL" +
                "<br>TYPE: java.lang.String" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String IN_LOCATION = Names.PORT_LOCATION;

    @ComponentInput(
            description = "The label associated with the KML file" +
                          "<br>TYPE: java.lang.String" +
                          "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings" +
                          "<br>TYPE: byte[]" +
                          "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Bytes" +
                          "<br>TYPE: java.lang.Object",
            name = Names.PORT_TEXT
    )
    protected static final String IN_TEXT = Names.PORT_TEXT;

    //------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            name = Names.PORT_HTML,
            description = "The HTML for the Google Map viewer" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String OUT_HTML = Names.PORT_HTML;

    //----------------------------- PROPERTIES ---------------------------------------------------

    static final String DEFAULT_TEMPLATE = "org/seasr/meandre/components/vis/geographic/GoogleMapKMLToHTML.vm";
    @ComponentProperty(
            description = "The template name",
            name = Names.PROP_TEMPLATE,
            defaultValue = DEFAULT_TEMPLATE
    )
    protected static final String PROP_TEMPLATE = Names.PROP_TEMPLATE;

    @ComponentProperty(
            defaultValue = "",
            description = "This property sets the Google Maps API key of your web site.",
            name = Names.PROP_GOOGLE_APIS_KEY
    )
    protected static final String PROP_GOOGLE_KEY = Names.PROP_GOOGLE_APIS_KEY;

    @ComponentProperty(
            defaultValue = "SATELLITE",
            description = "This property controls the type of map displayed by default. " +
                    "Valid values are:<br><ul>" +
                    "<li>ROADMAP - displays the normal, default 2D tiles of Google Maps.</li>" +
                    "<li>SATELLITE displays photographic tiles.</li>" +
                    "<li>HYBRID displays a mix of photographic tiles and a tile layer for prominent features (roads, city names).</li>" +
                    "<li>TERRAIN displays physical relief tiles for displaying elevation and water features (mountains, rivers, etc.).</li>" +
                    "</ul>",
            name = "map_type"
    )
    protected static final String PROP_MAP_TYPE = "map_type";

    //--------------------------------------------------------------------------------------------


    protected String _template;
    protected VelocityContext _context;
    protected boolean _isStreaming = false;
    protected boolean _deleteKml;

    protected JSONArray _kmlFiles;


    //--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        super.initializeCallBack(ccp);

        _context = VelocityTemplateService.getInstance().getNewContext();
        _context.put("key", getPropertyOrDieTrying(PROP_GOOGLE_KEY, ccp));
        _context.put("map_type", getPropertyOrDieTrying(PROP_MAP_TYPE, ccp));

        _template = getPropertyOrDieTrying(PROP_TEMPLATE, ccp);
        _kmlFiles = new JSONArray();
        _isStreaming = false;
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
        URL kmlUrl = DataTypeParser.parseAsURI(cc.getDataComponentFromInput(IN_LOCATION)).toURL();
        String label = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_TEXT))[0];

        JSONObject kmlObj = new JSONObject();
        kmlObj.put("url", kmlUrl.toString());
        kmlObj.put("label", label);
        _kmlFiles.put(kmlObj);

        if (!_isStreaming)
            endStream();
    }

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
        _context = null;
        _kmlFiles = null;
    }

    //--------------------------------------------------------------------------------------------

    @Override
    public boolean isAccumulator() {
        return true;
    }

    @Override
    public void startStream() throws Exception {
        _isStreaming = true;
        _kmlFiles = new JSONArray();
    }

    @Override
    public void endStream() throws Exception {
        _context.put("kmlFiles", _kmlFiles.toString());

        String html = VelocityTemplateService.getInstance().generateOutput(_context, _template);
        componentContext.pushDataComponentToOutput(OUT_HTML, html);

        _isStreaming = false;
        _kmlFiles = new JSONArray();
    }
}
