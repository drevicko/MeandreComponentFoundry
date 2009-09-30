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

package org.seasr.meandre.components.tools.xml.io;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.components.abstracts.AbstractExecutableComponent;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;

import org.seasr.datatypes.BasicDataTypesTools;
import org.seasr.meandre.components.tools.Names;
import org.seasr.meandre.support.components.datatype.parsers.DataTypeParser;
@Component(
		name = "XML To XML With XSL",
		creator = "Lily Dong",
		baseURL = "meandre://seasr.org/components/tools/",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		tags = "xml, xsl, filter",
		description = "This component reads a XML and a XSL. It filters XML with XSL "+
		"and outputs the filtered XML.",
		dependency = {"protobuf-java-2.2.0.jar"}
)

public class XMLToXMLWithXSLT extends AbstractExecutableComponent {
	//------------------------------ INPUTS ------------------------------------------------------

	@ComponentInput(
			name = Names.PORT_XML,
			description = "The XML doucment to read"
	)
	protected static final String IN_XML = Names.PORT_XML;

	@ComponentInput(
			name = Names.PORT_XSL,
			description = "The XSL document to read"
	)
	protected static final String IN_XSL = Names.PORT_XSL;

	//------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
			name = Names.PORT_XML,
			description = "The XML document to output after filtering"
		)
	private final static String OUT_XML = Names.PORT_XML;

	//--------------------------------------------------------------------------------------------

	public void executeCallBack(ComponentContext cc)
	throws Exception {
		String inXml = DataTypeParser.parseAsString(
    			cc.getDataComponentFromInput(IN_XML))[0];
		String inXsl = DataTypeParser.parseAsString(
    			cc.getDataComponentFromInput(IN_XSL))[0];

		StringReader xmlReader = new StringReader(inXml);
		StringReader xslReader = new StringReader(inXsl);

		Source xmlSource = new StreamSource(xmlReader);
		Source xslSource = new StreamSource(xslReader);

		StringWriter xmlWriter = new StringWriter();
		StreamResult xmlResult = new StreamResult(xmlWriter);

		TransformerFactory factory = TransformerFactory.newInstance();
    	Transformer transformer = factory.newTransformer(xslSource);
    	transformer.transform(xmlSource, xmlResult);
    	String outXml = xmlWriter.getBuffer().toString();

		cc.pushDataComponentToOutput(OUT_XML, BasicDataTypesTools.stringToStrings(outXml));

		xmlReader.close();
		xslReader.close();
		xmlWriter.close();
	}

	public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
	}

	public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
	}
}