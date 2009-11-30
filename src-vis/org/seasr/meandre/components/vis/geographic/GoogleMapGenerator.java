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

import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.annotations.Component.Licenses;
import org.meandre.components.abstracts.AbstractExecutableComponent;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.meandre.components.tools.Names;
import org.seasr.meandre.support.components.datatype.parsers.DataTypeParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Lily Dong
 * @author Boris Capitanu
 * @author Mike Haberman
 */



@Component(
        creator = "Lily Dong",
        description = "Calculates latitude and longitude for an address contained in the input XML document.",
        name = "Google Map Generator",
        tags = "google map, latitude, longitude",
        rights = Licenses.UofINCSA,
        baseURL="meandre://seasr.org/components/foundry/",
        dependency = {"protobuf-java-2.2.0.jar"}
)
public class GoogleMapGenerator	extends AbstractExecutableComponent
{


    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            description = "The source XML document",
            name = Names.PORT_XML
    )
    protected static final String IN_XML = Names.PORT_XML;

    //------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            description = "Output latitude",
	        name = Names.PORT_LATITUDE_VECTOR
    )
	protected static final String OUT_LATITUDE = Names.PORT_LATITUDE_VECTOR;

    @ComponentOutput(
            description = "Output longitude",
            name = Names.PORT_LONGITUDE_VECTOR
    )
	protected static final String OUT_LONGITUDE = Names.PORT_LONGITUDE_VECTOR;

    @ComponentOutput(
            description = "Output location.",
            name = Names.PORT_LOCATION_VECTOR
    )
	protected static final String OUT_LOCATION = Names.PORT_LOCATION_VECTOR;

    @ComponentOutput(
            description = "Output context",
            name = Names.PORT_CONTEXT_VECTOR
    )
	protected static final String OUT_CONTEXT = Names.PORT_CONTEXT_VECTOR;



    //------------------------------ PROPERTIES --------------------------------------------------
    @ComponentProperty(
            defaultValue = GeoLocation.defaultAPIKey,
            description = "This property sets Yahoo API ID. The default value is applicable to all applications.",
            name = Names.PROP_YAHOO_API_KEY
    )
    protected static final String PROP_YAHOO_KEY = Names.PROP_YAHOO_API_KEY;

    //--------------------------------------------------------------------------------------------


    private final static String STRING_DELIMITER = System.getProperty("line.separator");

    private String yahooAPIKey;

    protected class MapData {
        public Vector<String> lat_vector;
        public Vector<String> lon_vector;
        public Vector<String> location_vector;
        public Vector<String> context_vector;
    }


    //--------------------------------------------------------------------------------------------

    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        yahooAPIKey = ccp.getProperty(PROP_YAHOO_KEY);
    }

    public void executeCallBack(ComponentContext cc) throws Exception {
        Document doc = DataTypeParser.parseAsDomDocument(cc.getDataComponentFromInput(IN_XML));

        MapData mapData = extractMapDataFromDocument(doc);

		cc.pushDataComponentToOutput(OUT_LATITUDE, mapData.lat_vector);
		cc.pushDataComponentToOutput(OUT_LONGITUDE, mapData.lon_vector);
		cc.pushDataComponentToOutput(OUT_LOCATION, mapData.location_vector);
		cc.pushDataComponentToOutput(OUT_CONTEXT, mapData.context_vector);
    }

    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
    }




    //--------------------------------------------------------------------------------------------

    protected MapData extractMapDataFromDocument(Document document) throws Exception {
        Vector<String> lat = new Vector<String>();
        Vector<String> lon = new Vector<String>();
        Vector<String> location = new Vector<String>();
        Vector<String> context = new Vector<String>();

        document.getDocumentElement().normalize();
        console.finer("Root element : " + document.getDocumentElement().getNodeName());

        NodeList nodeLst = document.getElementsByTagName("location");
        console.finer("Information of all addresses");

        for (int k = 0; k < nodeLst.getLength(); k++) {
            Element elEntity = (Element)nodeLst.item(k);
            String aLoc = elEntity.getAttribute("value");

            Pattern p = Pattern.compile("[a-zA-Z .]+");
            Matcher m = p.matcher(aLoc);
            if(!m.matches()) //illegal characters
                continue;

            //
            // start of the refactored code
            //
            try {
            	GeoLocation geo = GeoLocation.getLocation(aLoc, yahooAPIKey);
            	if (geo.isValid()) {
            		lat.add(String.valueOf(geo.getLatitude()));
            		lon.add(String.valueOf(geo.getLongitude()));
            	}
            }catch(java.io.IOException e) {
            	console.info("unable to find location " + aLoc);
            	continue;
            }

            StringBuffer sbHtml = new StringBuffer();
            int nr = 0;

		    NodeList sentenceNodes = elEntity.getElementsByTagName("sentence");
		    for (int idx = 0, idxMax = sentenceNodes.getLength(); idx < idxMax; idx++) {
		        Element elSentence = (Element)sentenceNodes.item(idx);
		        String docTitle = elSentence.getAttribute("docTitle");
		        String theSentence = elSentence.getTextContent();

		        theSentence = theSentence.replaceAll("\t|\r|\n", " ");
		        aLoc = aLoc.replaceAll("\t|\r|\n", " ");

		        String normalizedSentence = theSentence.toLowerCase();
		        String normalizedDate = aLoc.toLowerCase();

		        int datePos = normalizedSentence.indexOf(normalizedDate);
		        if (datePos < 0) {
		            console.warning("Could not find the position of the date in the sentence! This should not happen!");
		            console.warning("   sentence: '" + normalizedSentence + "'");
		            console.warning("   date: '" + normalizedDate + "'");
		        }

		        String sentBefore = theSentence.substring(0, datePos);
		        String sentAfter = theSentence.substring(datePos + aLoc.length());
		        theSentence = StringEscapeUtils.escapeHtml(sentBefore) +
		                      "<font color='red'>" + StringEscapeUtils.escapeHtml(aLoc) + "</font>" +
		                      StringEscapeUtils.escapeHtml(sentAfter);
                sbHtml.append("<div onclick='toggleVisibility(this)' style='position:relative' align='left'><b>Sentence ").append(++nr);
                if (docTitle != null && docTitle.length() > 0)
                    sbHtml.append(" from '" + StringEscapeUtils.escapeHtml(docTitle) + "'");
                sbHtml.append("</b><span style='display: ' align='left'><table><tr><td>").append(theSentence).append("</td></tr></table></span></div>");
		    }
		    String sentence = sbHtml.toString();

            location.add(aLoc+"("+nr+")");
            context.add(sentence);
        }

        MapData mapData = new MapData();
        mapData.lat_vector = lat;
        mapData.lon_vector = lon;
        mapData.context_vector = context;
        mapData.location_vector = location;

        return mapData;
    }
}

