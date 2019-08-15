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
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.ds.AbstractPullOMDataSource;
import org.apache.axiom.util.blob.OverflowBlob;
import org.apache.axiom.util.stax.WrappedTextNodeStreamReader;
import org.apache.axis2.transport.base.BaseConstants;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class TextFileDataSource extends AbstractPullOMDataSource {
    private final OverflowBlob overflowBlob;
    private final Charset charset;

    public TextFileDataSource(OverflowBlob overflowBlob, Charset charset) {
        this.overflowBlob = overflowBlob;
        this.charset = charset;
    }
    
    public static OMSourcedElement createOMSourcedElement(OverflowBlob overflowBlob, Charset charset) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        TextFileDataSource txtFileDS = new TextFileDataSource(overflowBlob, charset);
        return fac.createOMElement(txtFileDS, BaseConstants.DEFAULT_TEXT_WRAPPER);
    }

    public XMLStreamReader getReader() throws XMLStreamException {
        InputStream is;
        try {
            is = overflowBlob.getInputStream();
        }
        catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
        return new WrappedTextNodeStreamReader(
                BaseConstants.DEFAULT_TEXT_WRAPPER, new InputStreamReader(is, charset));
    }

    public Object getObject() {
        return overflowBlob;
    }

    public boolean isDestructiveRead() {
        return false;
    }

    public OMDataSourceExt copy() {
        return new TextFileDataSource(overflowBlob, charset);
    }
}
