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

package org.seasr.meandre.components.analytics.mallet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.LabelSequence;
import de.schlichtherle.io.FileOutputStream;

/**
 * @author Boris Capitanu
 */

@Component(
        name = "Load Topic Model State",
        creator = "Ian Wood",
        baseURL = "meandre://seasr.org/components/foundry/",
        firingPolicy = FiringPolicy.all,
        mode = Mode.compute,
        rights = Licenses.UofINCSA,
        tags = "#output, mallet, topic model, state",
        description = "This component loads the state resulting from running topic modeling" ,
        dependency = {"protobuf-java-2.2.0.jar"}
)
public class LoadTopicModelState extends AbstractExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            name = Names.PORT_LOCATION,
            description = "The URL or file name of the state file to be read" +
                "<br>TYPE: java.net.URI" +
                "<br>TYPE: java.net.URL" +
                "<br>TYPE: java.lang.String" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String IN_LOCATION = Names.PORT_LOCATION;

    //------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            name = Names.PORT_LOCATION,
            description = "The location of the state file created" +
                    "<br>TYPE: org.seasr.datatypes.BasicDataTypes.Strings"
    )
    protected static final String OUT_LOCATION = Names.PORT_LOCATION;

    @ComponentOutput(
            name = "topic_model",
            description = "The topic model (same as input)" +
                "<br>TYPE: cc.mallet.topics.ParallelTopicModel"
    )
    protected static final String OUT_TOPIC_MODEL = "topic_model";

    //----------------------------- PROPERTIES ---------------------------------------------------

    @ComponentProperty(
            name = Names.PROP_DEFAULT_FOLDER,
            description = "The folder to write to. If the specified location " +
                    "is not an absolute path, it will be assumed relative to the " +
                    "published_resources folder.",
            defaultValue = ""
    )
    protected static final String PROP_DEFAULT_FOLDER = Names.PROP_DEFAULT_FOLDER;

    @ComponentProperty(
            name = "use_compression",
            description = "Use compression?",
            defaultValue = "false"
    )
    protected static final String PROP_USE_COMPRESSION = "use_compression";

    //--------------------------------------------------------------------------------------------


    protected static final String NEWLINE = System.getProperty("line.separator");
    protected String _defaultFolder, _publicResourcesDir;
    protected boolean _appendTimestamp, _useCompression;


    //--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        _defaultFolder = getPropertyOrDieTrying(PROP_DEFAULT_FOLDER, true, false, ccp);
        if (_defaultFolder.length() == 0)
            _defaultFolder = ccp.getPublicResourcesDirectory();
        else
            if (!_defaultFolder.startsWith(File.separator))
                _defaultFolder = new File(ccp.getPublicResourcesDirectory(), _defaultFolder).getAbsolutePath();

        console.fine("Default folder set to: " + _defaultFolder);

        _useCompression = Boolean.parseBoolean(getPropertyOrDieTrying(PROP_USE_COMPRESSION, ccp));

        _publicResourcesDir = new File(ccp.getPublicResourcesDirectory()).getAbsolutePath();
        if (!_publicResourcesDir.endsWith(File.separator)) _publicResourcesDir += File.separator;
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
    	// FIXME: THIS IS NOT REALLY IMPLEMENTED YET!! DO NOT USE IT!!
    	throw new Exception("Component Not Implemented!");
    	
        String location = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_LOCATION))[0];

        File file = getLocation(location, _defaultFolder);
        File parentDir = file.getParentFile();

        if (!parentDir.exists()) {
            if (parentDir.mkdirs())
                console.finer("Created directory: " + parentDir);
        } else
            if (!parentDir.isDirectory())
                throw new IOException(parentDir.toString() + " must be a directory!");

        console.fine(String.format("Reading mallet state file %s", file));

        InputStream stream = new BufferedInputStream(new FileInputStream(file));
        if (_useCompression)
        	stream = new GZIPInputStream(stream);
         
        ParallelTopicModel topicModel = topicModel;

        //PrintStream stream = new PrintStream(is);
        try {
            //topicModel.printState(stream);
        	// instead of calling the above printState which doesn't provide
        	// appropriate delimiters for words with spaces - I've copied the code here
        	// and modified it to use tabs -Loretta
        	//FIXME : this is copied from SaveTopicModelState - changing it to read the state is unfinished! 
    		stream.readln ("#doc\tsource\tpos\ttypeindex\ttype\ttopic");
    		stream.read("#alpha : ");
    		for (int topic = 0; topic < topicModel.numTopics; topic++) {
    			stream.read(topicModel.alpha[topic] + " ");
    		}
    		stream.readln();
    		stream.readln("#beta : " + topicModel.beta);

    		for (int doc = 0; doc < topicModel.data.size(); doc++) {
    			FeatureSequence tokenSequence =	(FeatureSequence) topicModel.data.get(doc).instance.getData();
    			LabelSequence topicSequence =	(LabelSequence) topicModel.data.get(doc).topicSequence;

    			String source = "NA";
    			if (topicModel.data.get(doc).instance.getSource() != null) {
    				source = topicModel.data.get(doc).instance.getSource().toString();
    			}

    			Formatter output = new Formatter(new StringBuilder(), Locale.US);

    			for (int pi = 0; pi < topicSequence.getLength(); pi++) {
    				int type = tokenSequence.getIndexAtPosition(pi);
    				int topic = topicSequence.getIndexAtPosition(pi);

    				output.format("%d\t%s\t%d\t%d\t%s\t%d\n", doc, source, pi, type, topicModel.alphabet.lookupObject(type), topic);
    			}
    			stream.read(output);
    		}
    	
            console.fine("State file created");
        }
        finally {
        	console.fine("Closing stream (FileInputStream)");
            stream.close();
            console.fine("Finished closing sreams in finally clause");
        }

        console.fine("Sending location to "+OUT_LOCATION);
        cc.pushDataComponentToOutput(OUT_LOCATION, BasicDataTypesTools.stringToStrings(file.toURI().toURL().toString()));
        console.fine("Sending topic model to "+OUT_TOPIC_MODEL);
        cc.pushDataComponentToOutput(OUT_TOPIC_MODEL, topicModel);
        console.fine("All done, end of executeCallBack()");
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
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    protected File getLocation(String location, String defaultFolder) throws MalformedURLException, URISyntaxException {
        // Check if the location is a fully-specified URL
        URL locationURL;
        try {
            locationURL = new URI(location).toURL();
        }
        catch (IllegalArgumentException e) {
            // Not a fully-specified URL, check if absolute location
            if (location.startsWith(File.separator) || location.startsWith(":" + File.separator, 1))
                locationURL = new File(location).toURI().toURL();
            else
                // Relative location
                locationURL = new File(defaultFolder, location).toURI().toURL();
        }

        return new File(locationURL.toURI());
    }
}
