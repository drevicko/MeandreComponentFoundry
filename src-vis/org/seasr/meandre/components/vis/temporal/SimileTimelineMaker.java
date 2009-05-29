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

package org.seasr.meandre.components.vis.temporal;

import java.io.File;
import java.io.Writer;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.annotations.Component.Licenses;
import org.meandre.components.abstracts.AbstractExecutableComponent;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.datatypes.BasicDataTypesTools;
import org.seasr.meandre.components.tools.Names;
import org.seasr.meandre.support.html.VelocityTemplateService;
import org.seasr.meandre.support.io.IOUtils;
import org.seasr.meandre.support.parsers.DataTypeParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Lily Dong
 * @author Boris Capitanu
 */

@Component(
        creator = "Lily Dong",
        description = "Generates the necessary HTML and XML files " +
                      "for viewing timeline and store them on the local machine. " +
                      "The two files will be stored under public/resources/timeline/file/. " +
                      "will be stored under public/resources/timeline/js/. " +
                      "For fast browse, dates are grouped into different time slices. " +
                      "The number of time slices is designed as a property. " +
                      "If granularity is not appropriate, adjusts this porperty.",
        name = "Simile Timeline Generator",
        tags = "simile, timeline",
        rights = Licenses.UofINCSA,
        baseURL="meandre://seasr.org/components/tools/",
        dependency = {"protobuf-java-2.0.3.jar", "velocity-1.6.1-dep.jar"},
        resources = {"SimileTimelineMaker.vm"}
)
public class SimileTimelineMaker extends AbstractExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

	@ComponentInput(
	        description = "The source XML document",
	        name = Names.PORT_XML
	)
    protected static final String IN_XML = Names.PORT_XML;

    //------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
	        description = "The HTML for the Simile Timeline viewer",
	        name = Names.PORT_HTML
	)
	protected static final String OUT_HTML = Names.PORT_HTML;

    //------------------------------ PROPERTIES --------------------------------------------------

	@ComponentProperty(
	        defaultValue = "10",
            description = "The number of time slices desired",
            name = Names.PROP_N_SLICES
	)
    protected static final String DATA_PROPERTY = Names.PROP_N_SLICES;

    //--------------------------------------------------------------------------------------------


	protected static final String simileVelocityTemplate =
	    "org/seasr/meandre/components/vis/temporal/SimileTimelineMaker.vm";

    /** Store document title */
    private String docTitle;

    /** Store the minimum value of year */
    private int minYear;

    /** Store the maximum value of year */
    private int maxYear;

    private VelocityContext _context;


    //--------------------------------------------------------------------------------------------

    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        _context = VelocityTemplateService.getInstance().getNewContext();
        _context.put("ccp", ccp);
    }

    public void executeCallBack(ComponentContext cc) throws Exception {
        Document doc = DataTypeParser.parseAsDomDocument(cc.getDataComponentFromInput(IN_XML));

        String dir = cc.getPublicResourcesDirectory() + File.separator;
        dir += "timeline" + File.separator;

        console.finest("Set storage location to " + dir);

        String webUiUrl = cc.getWebUIUrl(true).toString();
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String htmlFileName = "myhtml" + formatter.format(now) + ".html",
               xmlFileName = "myxml" + formatter.format(now) + ".xml";
        String htmlLocation = webUiUrl + "public/resources/timeline/" + htmlFileName,
               xmlLocation  = webUiUrl + "public/resources/timeline/" + xmlFileName;

        console.finest("htmlFileName=" + htmlFileName);
        console.finest("xmlFileName=" + xmlFileName);
        console.finest("htmlLocation=" + htmlLocation);
        console.finest("xmlLocation=" + xmlLocation);

        URI xmlURI = DataTypeParser.parseAsURI(dir + xmlFileName);
        URI htmlURI = DataTypeParser.parseAsURI(dir + htmlFileName);

        Writer xmlWriter = IOUtils.getWriterForResource(xmlURI);
        xmlWriter.write(generateXML(doc));
        xmlWriter.close();

        String simileHtml = generateHTML(xmlFileName);

        Writer htmlWriter = IOUtils.getWriterForResource(htmlURI);
        htmlWriter.write(simileHtml);
        htmlWriter.close();

        console.info("The Simile Timeline HTML content was created at " + htmlLocation);
        console.info("The Simile Timeline XML content was created at " + xmlLocation);

        cc.pushDataComponentToOutput(OUT_HTML, BasicDataTypesTools.stringToStrings(simileHtml));
    }

    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
    }

    //--------------------------------------------------------------------------------------------

    private String generateHTML(String xmlFileName) throws Exception {
        VelocityTemplateService velocity = VelocityTemplateService.getInstance();
        _context.put("maxYear", maxYear);
        _context.put("minYear", minYear);
        _context.put("xmlFileName", xmlFileName);

        return velocity.generateOutput(_context, simileVelocityTemplate);
    }

    private String generateXML(Document doc) {
    	minYear = Integer.MAX_VALUE;
    	maxYear = Integer.MIN_VALUE;

    	StringBuffer buf = new StringBuffer(); //Store XML
    	buf.append("<data>\n");

		doc.getDocumentElement().normalize();
		docTitle = doc.getDocumentElement().getAttribute("docID");
		console.finest("Root element : " + docTitle);
		NodeList nodeLst = doc.getElementsByTagName("date");
		//getConsoleOut().println("Information of date");
		for (int k = 0; k < nodeLst.getLength(); k++) {
			Node fstNode = nodeLst.item(k);
			String aDate = fstNode.getTextContent();

			//standardize date
			//getConsoleOut().println("time : " + aDate);

			String month = null,
				   day   = null,
				   year  = null;

			String startMonth = null,
					 endMonth = null;

			Pattern datePattern = Pattern.compile("(january|jan|feburary|feb|march|mar|" + //look for month
					"april|apr|may|june|jun|july|jul|august|aug|september|sept|october|oct|"+
					"november|nov|december|dec)");
			Matcher dateMatcher = datePattern.matcher(aDate);
			if(dateMatcher.find()) { //look for month
				month = dateMatcher.group(1);
			} else { //look for season
				datePattern = Pattern.compile("(spring|summer|fall|winter)");
				dateMatcher = datePattern.matcher(aDate);
				if(dateMatcher.find()) {
					String season = dateMatcher.group(1);
					if(season.equalsIgnoreCase("spring")) {
						startMonth = "Mar 21";
						endMonth = "June 20";
					} else if(season.equalsIgnoreCase("summer")) {
						startMonth = "June 21";
						endMonth = "Sept 20";
					} else if(season.equalsIgnoreCase("fall")) {
						startMonth = "Sept 21";
						endMonth = "Dec 20";
					} else { //winter
						startMonth = "Dec 21";
						endMonth = "Mar 20";
					}
				}
			}

			datePattern = Pattern.compile("(\\b\\d{1}\\b)"); //look for day	like 5
			dateMatcher = datePattern.matcher(aDate);
			if(dateMatcher.find()) {
				day = dateMatcher.group(1);
			} else {
				datePattern = Pattern.compile("(\\b\\d{2}\\b)"); //look for day	like 21
				dateMatcher = datePattern.matcher(aDate);
				if(dateMatcher.find()) {
					day = dateMatcher.group(1);
				}
			}

			//datePattern = Pattern.compile("(\\d{4})"); //look for year
			datePattern = Pattern.compile("(\\d{3,4})"); //look for year with 3 or 4 digits
			dateMatcher = datePattern.matcher(aDate);
			if(dateMatcher.find()) { //look for year
				NamedNodeMap nnp = fstNode.getAttributes();
		        String sentence = nnp.getNamedItem("sentence").getNodeValue();

		        //escape invalid xml characters
//		        sentence = sentence.replaceAll("[&]",  "&amp;");
//		        sentence = sentence.replaceAll("[<]",  "&lt;");
//		        sentence = sentence.replaceAll("[>]",  "&gt;");
//		        sentence = sentence.replaceAll("[\"]", "&quot; ");
//		        sentence = sentence.replaceAll("[\']", "&#39;");

		        sentence = StringEscapeUtils.escapeXml(sentence);

		        StringTokenizer st = new StringTokenizer(sentence, "|");
		        StringBuffer sb = new StringBuffer();
		        int nr = 0;
		        while(st.hasMoreTokens()) {
		        	String nt = st.nextToken();
		        	int pos = nt.toLowerCase().indexOf(aDate);
		        	String str = "&lt;/font&gt;";
		        	int offset = pos+aDate.length();
		        	nt = new StringBuffer(nt).insert(offset, str).toString();
		        	offset = pos;
		        	str = "&lt;font color=&#39;red&#39;&gt;";
		        	nt = new StringBuffer(nt).insert(offset, str).toString();
		        	sb.append("&lt;div onclick=&#39;toggleVisibility(this)&#39; style=&#39;position:relative&#39; ALIGN=&#39;LEFT&#39;&gt;&lt;b&gt;Sentence ").append(++nr).append("&lt;/b&gt;");
		        	sb.append("&lt;span style=&#39;display: &#39; ALIGN=&#39;LEFT&#39;&gt; &lt;table &gt;&lt;tr&gt;&lt;td&gt;").append(nt).append("&lt;/td&gt;&lt;/tr&gt;&lt;/table&gt;&lt;/span&gt;&lt;/div&gt;");
		        }
		        sentence = sb.toString();

				year = dateMatcher.group(1);
				minYear = Math.min(minYear, Integer.parseInt(year));
				maxYear = Math.max(maxYear, Integer.parseInt(year));

				//year or month year or month day year
				if(day == null)
					if(month == null) { //season year
						if(startMonth != null) {//spring or summer or fall or winter year
							buf.append("<event start=\"").append(startMonth + " " + year).append("\" end=\"").append(endMonth + " " + year).append("\" title=\"").append(aDate+"("+nr+")").append("\">\n").append(sentence).append("\n");
							buf.append("</event>\n");
						} else { //year
							//if(Integer.parseInt(year) != 1832) {
							buf.append("<event start=\"").append(year).append("\" title=\"").append(aDate+"("+nr+")").append("\">\n").append(sentence).append("\n");
							buf.append("</event>\n");//}
						}
					} else { //month year
						String startDay = month + " 01";
						int m = 1;
						if(month.startsWith("feb"))
							m = 2;
						else if(month.startsWith("mar"))
							m = 3;
						else if(month.startsWith("apr"))
							m = 4;
						else if(month.startsWith("may"))
							m = 5;
						else if(month.startsWith("jun"))
							m = 6;
						else if(month.startsWith("jul"))
							m = 7;
						else if(month.startsWith("aug"))
							m = 8;
						else if(month.startsWith("sept"))
							m = 9;
						else if(month.startsWith("oct"))
							m = 10;
						else if(month.startsWith("nov"))
							m = 11;
						else if(month.startsWith("dec"))
							m = 12;
						int y = Integer.parseInt(year);
						int numberOfDays = 31;
						if (m == 4 || m == 6 || m == 9 || m == 11)
							  numberOfDays = 30;
						else if (m == 2) {
							boolean isLeapYear = (y % 4 == 0 && y % 100 != 0 || (y % 400 == 0));
							if (isLeapYear)
								numberOfDays = 29;
							else
								numberOfDays = 28;
						}
						String endDay = month + " " + Integer.toString(numberOfDays);
						buf.append("<event start=\"").append(startDay + " " + year).append("\" end=\"").append(endDay + " " + year).append("\" title=\"").append(aDate+"("+nr+")").append("\">\n").append(sentence).append("\n");
				    	buf.append("</event>\n");
					}
				else {
					if(month == null) {//year
						buf.append("<event start=\"").append(year).append("\" title=\"").append(aDate+"("+nr+")").append("\">\n").append(sentence).append("\n");
						buf.append("</event>\n");
					} else { //month day year
						buf.append("<event start=\"").append(month + " " + day + " " + year).append("\" title=\"").append(aDate+"("+nr+")").append("\">\n").append(sentence).append("\n");
						buf.append("</event>\n");
					}
				}
			}
		}
		buf.append("</data>");

    	return buf.toString();
    }
}
