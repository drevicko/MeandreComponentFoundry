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

package org.seasr.meandre.support.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;

/**
 * @author Boris Capitanu
 */
public class IOUtils {

    /**
     * Opens the location from where to read.
     *
     * @param uri The location to read from (can be a URL or a local file)
     * @return The reader for this location
     * @throws IOException Thrown if a problem occurred when creating the reader
     */
    public static Reader getReaderForResource(URI uri) throws IOException  {
        try {
            return new InputStreamReader(uri.toURL().openStream());
        } catch (IllegalArgumentException e) {
            // URI not absolute - trying as local file
            return new FileReader(uri.toString());
        }
    }

    /**
     * Gets a Writer that can be used to write to a resource
     *
     * @param uri The location to write to (can be URL or local file)
     * @return The writer for this location
     * @throws IOException Thrown if a problem occurred when creating the writer
     */
    public static Writer getWriterForResource(URI uri) throws IOException {
        return new OutputStreamWriter(StreamUtils.getOutputStreamForResource(uri));
    }

    /**
     * @param uri The resource location (can be a URL or a local path)
     * @return The text contained in the resource
     * @throws IOException Thrown if a problem occurs when pulling data from the resource
     */
    public static String getTextFromReader(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        String sLine;
        while ((sLine = br.readLine()) != null)
            pw.println(sLine);

        reader.close();
        return sw.toString();
    }

}
