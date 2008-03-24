/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.util;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDataSourceExt;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.ds.OMDataSourceExtBase;
import org.apache.axiom.om.impl.llom.OMSourcedElementImpl;
import org.apache.axiom.om.impl.serialize.StreamingOMSerializer;
import org.apache.synapse.SynapseException;
import org.apache.synapse.transport.base.BaseConstants;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.nio.charset.Charset;

public class TextFileDataSource extends OMDataSourceExtBase {

    private static final byte[] empty =
        "<text xmlns=\"http://ws.apache.org/commons/ns/payload\"/>".getBytes();
    private final TemporaryData temporaryData;
    private final Charset charset;

    public TextFileDataSource(TemporaryData temporaryData, Charset charset) {
        this.temporaryData = temporaryData;
        this.charset = charset;
    }
    
    public static OMSourcedElement createOMSourcedElement(TemporaryData temporaryData, Charset charset) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        TextFileDataSource txtFileDS = new TextFileDataSource(temporaryData, charset);
        return new OMSourcedElementImpl(BaseConstants.DEFAULT_TEXT_WRAPPER, fac, txtFileDS);
    }

    public void serialize(OutputStream out, OMOutputFormat format) throws XMLStreamException {
        try {
            InputStream is = temporaryData.getInputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw new SynapseException("Error serializing TextFileDataSource to an OutputStream", e);
        }
    }

    public void serialize(Writer writer, OMOutputFormat format) throws XMLStreamException {
        try {
            writer.write(new String(empty));
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    public void serialize(XMLStreamWriter xmlWriter) throws XMLStreamException {
        StreamingOMSerializer serializer = new StreamingOMSerializer();
        serializer.serialize(getReader(), xmlWriter);
    }

    public XMLStreamReader getReader() throws XMLStreamException {
        InputStream is;
        try {
            is = temporaryData.getInputStream();
        }
        catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
        return new WrappedTextNodeStreamReader(BaseConstants.DEFAULT_TEXT_WRAPPER, new InputStreamReader(is, charset));
    }

    public Object getObject() {
        return temporaryData;
    }

    public boolean isDestructiveRead() {
        return false;
    }

    public boolean isDestructiveWrite() {
        return false;
    }
    
    public byte[] getXMLBytes(String encoding) throws UnsupportedEncodingException {
        throw new UnsupportedOperationException();
    }

    public void close() {
    }

    public OMDataSourceExt copy() {
        return new TextFileDataSource(temporaryData, charset);
    }
}
