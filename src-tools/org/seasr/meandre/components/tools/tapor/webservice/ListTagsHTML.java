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

package org.seasr.meandre.components.tools.tapor.webservice;

import javax.xml.rpc.Call;
import javax.xml.rpc.Service;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceFactory;
import javax.xml.rpc.ParameterMode;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.Licenses;

import org.seasr.meandre.components.abstracts.AbstractExecutableComponent;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.seasr.datatypes.core.BasicDataTypesTools;
import org.seasr.datatypes.core.DataTypeParser;
import org.seasr.datatypes.core.Names;

@Component(creator="Lily Dong",
        description="Demonstrates how to construct a interface to " +
        "consume web service of list_Tags_HTML of Tapor at " +
        "http://tada.mcmaster.ca/view/Main/TAPoRware#Using_TAPoRware_as_a_web_service.",
        name="List HTML Tags",
        rights = Licenses.UofINCSA,
        tags="tag web service",
        dependency={"FastInfoset.jar", "jaxrpc-impl.jar", "jaxrpc-spi.jar", "jsr173_api.jar", "saaj-impl.jar", "protobuf-java-2.2.0.jar"},
        baseURL="meandre://seasr.org/components/foundry/"
)

public class ListTagsHTML extends AbstractExecutableComponent {

	//------------------------------ INPUTS ------------------------------------------------------

	@ComponentInput(
			name= Names.PORT_TEXT,
			description="Input text to be analyzed." +
			"<br>TYPE: java.lang.String" +
            "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings" +
            "<br>TYPE: byte[]" +
            "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Bytes" +
            "<br>TYPE: java.lang.Object"
    )
    public final static String IN_TEXT = Names.PORT_TEXT;

	//------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
	        name = Names.PORT_XML,
	        description = "Output XML document generated by TAPoR." +
	        "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
	)
	protected static final String OUT_XML = Names.PORT_XML;

	//--------------------------------------------------------------------------------------------

	private static String qnameService = "TaporwareServices";
	private static String qnamePort = "TaporwareService_xml";
	private static String bodyNamespaceValue = "http://taporware.mcmaster.ca/~taporware/webservice";
	private static String endPoint = "http://taporware.mcmaster.ca:9982";

	private static String ENCODING_STYLE_PROPERTY = "javax.xml.rpc.encodingstyle.namespace.uri";
	private static String NS_XSD = "http://www.w3.org/2001/XMLSchema";
	private static String URI_ENCODING =  "http://schemas.xmlsoap.org/soap/encoding/";

	//--------------------------------------------------------------------------------------------

	@Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
	}

	@Override
	public void executeCallBack(ComponentContext cc)
	throws Exception {
		String htmlInput = DataTypeParser.parseAsString(
				cc.getDataComponentFromInput(IN_TEXT))[0];

	    try {
	    	ServiceFactory factory = ServiceFactory.newInstance();
	        Service service = factory.createService(
	        		new QName(qnameService));
	        QName port = new QName(qnamePort);

	        Call call = service.createCall(port);
	        call.setTargetEndpointAddress(endPoint);

	        call.setProperty(Call.SOAPACTION_USE_PROPERTY, new Boolean(false));
	        call.setProperty(Call.SOAPACTION_URI_PROPERTY, "");
	        call.setProperty(ENCODING_STYLE_PROPERTY, URI_ENCODING);

	        QName QNAME_TYPE_STRING = new QName(NS_XSD, "string");
	        call.setReturnType(QNAME_TYPE_STRING);

	        call.setOperationName(new QName(bodyNamespaceValue,"list_Tags_HTML"));

	        call.addParameter("htmlInput", QNAME_TYPE_STRING, ParameterMode.IN);
	    	call.addParameter("sorting", QNAME_TYPE_STRING, ParameterMode.IN);
	    	call.addParameter("outFormat", QNAME_TYPE_STRING, ParameterMode.IN);

			String[] params = { htmlInput, "2", "4" };
		    String result = (String)call.invoke(params);

		    java.io.PrintWriter pw = new java.io.PrintWriter("result.html");
			pw.println(result);
			pw.flush();
			pw.close();

			cc.pushDataComponentToOutput(
        			OUT_XML,
        			BasicDataTypesTools.stringToStrings(result));
		} catch (Exception ex) {
			throw new ComponentExecutionException(ex);
		}
	}

	@Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
	}
}
