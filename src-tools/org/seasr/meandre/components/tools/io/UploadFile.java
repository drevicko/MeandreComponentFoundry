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

package org.seasr.meandre.components.tools.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.meandre.annotations.Component;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.system.components.ext.StreamInitiator;
import org.meandre.core.system.components.ext.StreamTerminator;
import org.meandre.webui.WebUIException;
import org.seasr.datatypes.core.BasicDataTypesTools;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.abstracts.util.ComponentUtils;
import org.seasr.meandre.components.tools.text.io.GenericTemplate;
import org.seasr.meandre.support.generic.io.JARInstaller.InstallStatus;

import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

@Component(
        creator = "Lily Dong",
        description = "Uploads and submits files from user's own machine. " +
        "This component works with Fluid Infusion 1.2.",
        name = "Upload File",
        tags = "#INPUT, file input upload text",
        rights = Licenses.UofINCSA,
        mode = Mode.webui,
        baseURL = "meandre://seasr.org/components/foundry/",
        dependency={ "velocity-1.7-dep.jar", "protobuf-java-2.2.0.jar", "infusion-1.2.jar" },
        resources={ "UploadFile.vm" }
)

public class UploadFile extends GenericTemplate {

	//------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            name = Names.PORT_FILENAME,
            description = "The file path of the uploaded file(s)" +
                "<br>TYPE: java.lang.String"
    )
    protected static final String OUT_FILE_PATH = Names.PORT_FILENAME;

    @ComponentOutput(
            name = Names.PORT_MIME_TYPE,
            description = "The MIME type of the uploaded file(s)" +
                "<br>TYPE: java.lang.String"
    )
    protected static final String OUT_MIME_TYPE = Names.PORT_MIME_TYPE;

    @ComponentOutput(
            name = Names.PORT_RAW_DATA,
            description = "The content of the uploaded file(s)" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Bytes"
    )
    protected static final String OUT_RAW_DATA = Names.PORT_RAW_DATA;

    //------------------------------ PROPERTIES --------------------------------------------------

	@ComponentProperty(
	        description = "The template name",
	        name = GenericTemplate.PROP_TEMPLATE,
	        defaultValue = "org/seasr/meandre/components/tools/text/io/UploadFile.vm"
	)
    protected static final String PROP_TEMPLATE = GenericTemplate.PROP_TEMPLATE;

    @ComponentProperty(
            name = Names.PROP_MAX_SIZE,
            description = "Maximum file size accepted (in MB) default: 10MB",
            defaultValue = "10"
    )
    protected static final String PROP_MAX_SIZE = Names.PROP_MAX_SIZE;

    @ComponentProperty(
            name = Names.PROP_WRAP_STREAM,
            description = "Wrap output as stream?",
            defaultValue = "false"
    )
    protected static final String PROP_WRAP_STREAM = Names.PROP_WRAP_STREAM;

    @ComponentProperty(
            name = "stream_id",
            description = "The stream id",
            defaultValue = ""
    )
    protected static final String PROP_STREAM_ID = "stream_id";

	//--------------------------------------------------------------------------------------------


    private int maxFileSize;
    private boolean wrapStream;
    private Integer streamId;


    //--------------------------------------------------------------------------------------------

    @Override
	public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
	    super.initializeCallBack(ccp);

	    streamId = Integer.parseInt(getPropertyOrDieTrying(PROP_STREAM_ID, ccp));
	    wrapStream = Boolean.parseBoolean(getPropertyOrDieTrying(PROP_WRAP_STREAM, ccp));
	    maxFileSize = Integer.parseInt(getPropertyOrDieTrying(PROP_MAX_SIZE, ccp));
	    maxFileSize *= 1024 * 1024;

        String sFluidDir = ccp.getPublicResourcesDirectory() + File.separator + "infusion";
	    InstallStatus status = ComponentUtils.installJARContainingResource(getClass(),
	            "infusion-1.2/components/uploader/js/Uploader.js", sFluidDir, false);

        if (status == InstallStatus.SKIPPED)
            console.fine("Installation skipped - Fluid components already installed");

        if (status == InstallStatus.FAILED)
            throw new ComponentContextException("Failed to install Fluid components at " + new File(sFluidDir).getAbsolutePath());

	    context.put("title", "File Uploader");
	    context.put("FPath", "/public/resources/infusion/infusion-1.2");
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
	    if (wrapStream) pushInitiator();

	    super.executeCallBack(cc);

	    if (wrapStream) pushTerminator();
	}

  //--------------------------------------------------------------------------------------------

	@Override
    protected boolean processRequest(HttpServletRequest request) throws IOException {
		if (!expectMoreRequests(request)) return false;

	    // TODO: look at using commons-fileupload lib for parsing upload requests - has friendlier license than oreilly
	    // ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
        // List lstItems = upload.parseRequest(request);

	    String type = null;

	    String type1 = request.getHeader("Content-Type");
	    String type2 = request.getContentType();

	    // If one value is null, choose the other value
	    if (type1 == null && type2 != null) {
	      type = type2;
	    }
	    else if (type2 == null && type1 != null) {
	      type = type1;
	    }
	    // If neither value is null, choose the longer value
	    else if (type1 != null && type2 != null) {
	      type = (type1.length() > type2.length() ? type1 : type2);
	    }

	    if(type != null) {
	    	MultipartParser mp = new MultipartParser(request, maxFileSize);
	    	Part part;
	    	while ((part = mp.readNextPart()) != null) {
	    		String name = part.getName();
	    		if (part.isParam()) {
	    			// it's a parameter part
	    			ParamPart paramPart = (ParamPart) part;
	    			String value = paramPart.getStringValue();
	    			console.finest("param; name=" + name + ", value=" + value);
	    		}
	    		else if (part.isFile()) {
	    			// it's a file part
	    			FilePart filePart = (FilePart) part;
	    			String fileName = filePart.getFileName();
	    			if (fileName != null) {
	    				// the part actually contained a file
	    				ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
	    				long size = filePart.writeTo(dataStream);
	    				String filePath = filePart.getFilePath();
	    				String contentType = filePart.getContentType();

	    				console.info("Received " + filePath + " (" + contentType + "), size: " + size + " bytes");

	    				try {
	    					componentContext.pushDataComponentToOutput(OUT_FILE_PATH, filePath);
	    					componentContext.pushDataComponentToOutput(OUT_MIME_TYPE, contentType);
	    					componentContext.pushDataComponentToOutput(OUT_RAW_DATA,
                                BasicDataTypesTools.byteArrayToBytes(dataStream.toByteArray()));
	    				}
	    				catch (ComponentContextException e) {
	    					throw new IOException(e.toString());
	    				}

	    				dataStream.close();
	    			}
	    			else {
	    				// the field did not contain a file
	    				console.info("file; name=" + name + "; EMPTY");
	    			}
	    		}
	    	}
	    }

		return true;
    }

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response) throws WebUIException {
		console.entering(getClass().getName(), "handle", response);

	    StringBuffer sb = request.getRequestURL();
	    sb.append(" <Query Data> ").append(request.getQueryString());
	    console.info("Request: " + sb.toString());

	    //
	    // this is the workhorse method
	    // process the input, determine any errors,
	    // set up any output that will be pushed
	    //
	    try {
	    	if (processRequest(request)) {
	    		// regenerate the template
	            generateContent(request, response);
	            console.exiting(getClass().getName(), "handle/generateContent");
	            return;
	        }
	    } catch(IOException ioe) {
	    	throw new WebUIException(ioe);
	    }

	    //
	    // see if this component can handle multiple requests before
	    // releasing the semaphore,
	    //
	    if (!expectMoreRequests(request)) {
	    	console.fine("done = true");
	    	done = true;

	    	// No Errors,
	    	// just push the browser to the "next" component
	    	try{
	    		generateMetaRefresh(response);
	    	} catch (IOException e) {
	    		throw new WebUIException("Unable to generate redirect response", e);
	    	}

	    	console.exiting(getClass().getName(), "handle");
	    }
	}

    //--------------------------------------------------------------------------------------------

	/**
     * Pushes an initiator.
     *
     * @throws Exception Something went wrong when pushing
     */
    private void pushInitiator() throws Exception {
        console.fine("Pushing " + StreamInitiator.class.getSimpleName());

        StreamInitiator si = new StreamInitiator(streamId);
        componentContext.pushDataComponentToOutput(OUT_FILE_PATH, si);
        componentContext.pushDataComponentToOutput(OUT_MIME_TYPE, si);
        componentContext.pushDataComponentToOutput(OUT_RAW_DATA, si);
    }

    /**
     * Pushes a terminator.
     *
     * @throws Exception Something went wrong when pushing
     */
    private void pushTerminator() throws Exception {
        console.fine("Pushing " + StreamTerminator.class.getSimpleName());

        StreamTerminator st = new StreamTerminator(streamId);
        componentContext.pushDataComponentToOutput(OUT_FILE_PATH, st);
        componentContext.pushDataComponentToOutput(OUT_MIME_TYPE, st);
        componentContext.pushDataComponentToOutput(OUT_RAW_DATA, st);
    }
}
