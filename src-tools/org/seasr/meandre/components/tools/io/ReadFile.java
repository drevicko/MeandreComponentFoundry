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

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.meandre.annotations.Component;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.ComponentProperty;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.datatypes.core.BasicDataTypesTools;
import org.seasr.datatypes.core.DataTypeParser;
import org.seasr.datatypes.core.Names;
import org.seasr.meandre.components.abstracts.AbstractExecutableComponent;

/**
 * Writes the given data to a file
 *
 * @author Ian Wood
 */

@Component(
        name = "Read From File",
        creator = "Ian Wood",
        baseURL = "meandre://seasr.org/components/foundry/",
        firingPolicy = FiringPolicy.all,
        mode = Mode.compute,
        rights = Licenses.UofINCSA,
        tags = "#INPUT, io, file, write, bytes",
        description = "This component reads a single object saved to a file via the java.io.Serializable interface." +
        		"For text files (including HTML, XML, ..) use the Universal Text Extractor or Read Text components.",
        dependency = {"protobuf-java-2.2.0.jar"}
)
public class ReadFile extends AbstractExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            name = Names.PORT_LOCATION,
            description = "The URL or file name specifying from where the data will be read. " +
                "<br>TYPE: java.net.URI" +
                "<br>TYPE: java.net.URL" +
                "<br>TYPE: java.lang.String" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String IN_LOCATION = Names.PORT_LOCATION;

    //------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            name = Names.PORT_LOCATION,
            description = "The URL or file name containing the read data. The location can be " +
            "a full file:/// URL, or an absolute or relative pathname." +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String OUT_LOCATION = Names.PORT_LOCATION;

    @ComponentOutput(
            name = "data",
            description = "The data read" +
                "<br>TYPE: that of the object that was previously serialised to the file"
    )
    protected static final String OUT_DATA = "data";

    //------------------------------ PROPERTIES --------------------------------------------------

    @ComponentProperty(
            name = Names.PROP_DEFAULT_FOLDER,
            description = "The folder containing the file to be read. If the specified location " +
            		"is not a valid URL or an absolute path, it will be assumed relative to the " +
            		"published_resources folder.",
            defaultValue = ""
    )
    protected static final String PROP_DEFAULT_FOLDER = Names.PROP_DEFAULT_FOLDER;

    //--------------------------------------------------------------------------------------------


    private String defaultFolder, publicResourcesDir;
    private File file = null;

    //--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        defaultFolder = getPropertyOrDieTrying(PROP_DEFAULT_FOLDER, true, false, ccp);
        if (defaultFolder.length() == 0)
            defaultFolder = ccp.getPublicResourcesDirectory();
        else
            if (!defaultFolder.startsWith(File.separator))
                defaultFolder = new File(ccp.getPublicResourcesDirectory(), defaultFolder).getAbsolutePath();

        console.fine("Default folder set to: " + defaultFolder);

        publicResourcesDir = new File(ccp.getPublicResourcesDirectory()).getAbsolutePath();
        if (!publicResourcesDir.endsWith(File.separator)) publicResourcesDir += File.separator;
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
        String location = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_LOCATION))[0];
        Object outData = null;

        file = getLocation(location, defaultFolder);

        console.fine(String.format("Reading file %s", file));

        ObjectInputStream ois = null;
        try {
        	ois = new ObjectInputStream (new FileInputStream(file));
        	outData = ois.readObject();
	        }
//        catch (FileNotFoundException e) {
//        	throw("");
//        }
        finally {
        	if (ois != null)
        		ois.close();
        	else {
//        		throw(new );
        	}
        }

        if (file.getAbsolutePath().startsWith(publicResourcesDir)) {
            String publicLoc = file.getAbsolutePath().substring(publicResourcesDir.length());
            URL outputURL = new URL(cc.getWebUIUrl(true), "/public/resources/" + publicLoc);
            console.info("File read from: " + outputURL);
            cc.pushDataComponentToOutput(OUT_LOCATION, BasicDataTypesTools.stringToStrings(outputURL.toString()));
        }
        else
            cc.pushDataComponentToOutput(OUT_LOCATION, BasicDataTypesTools.stringToStrings(file.toString()));

        cc.pushDataComponentToOutput(OUT_DATA, outData);
    }

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
    }

    //--------------------------------------------------------------------------------------------

    /**
     * Gets a file reference to the location specified
     *
     * @param location The location; can be a full file:/// URL, or an absolute or relative pathname
     * @param defaultFolder The folder to use as base for relatively specified pathnames, or null to use current folder
     * @return The File reference to the location
     */
    protected File getLocation(String location, String defaultFolder) {
        // Check if the location is a fully-specified URL
        boolean found = false;
        File processedLocation = null;
        try {
            processedLocation = new File(new URI(location).toURL().toURI());
            found = true;
        }
        catch (IllegalArgumentException e) {}
        catch (MalformedURLException e) {}
        catch (URISyntaxException e) {}
        
        // in java 7, we could do this in a single catch() clause, but to save mess, I've used a flag
        if (!found) {
        	// Not a fully-specified URL, check if absolute location
        	if (location.startsWith(File.separator) || location.startsWith(":" + File.separator, 1))
        		processedLocation = new File(location);
        	else
        		// Relative location
        		processedLocation = new File(defaultFolder, location);
        }
        
        return processedLocation;
    }
}
