/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.util.jaxp;

import org.w3c.dom.ls.LSInput;

import java.io.InputStream;
import java.io.Reader;

/**
 * External schema resource holder for {@link org.apache.synapse.util.jaxp.SchemaResourceResolver}
 * This will use to store {@link java.io.InputStream} of external schema resource resolved by
 * {@link org.apache.synapse.util.jaxp.SchemaResourceResolver}
 *
 * Current implementation is only using {@link java.io.InputStream} to store external schema resource. Methods other
 * than {@link org.apache.synapse.util.jaxp.SchemaResourceLSInput#getByteStream()} and
 * {@link org.apache.synapse.util.jaxp.SchemaResourceLSInput#setByteStream(java.io.InputStream)} are just place holders.
 */
public class SchemaResourceLSInput implements LSInput {

    InputStream byteStream = null;
    String systemId = null;
    String publicId = null;
    String baseURI = null;

    public Reader getCharacterStream() {
        return null;
    }

    public void setCharacterStream(Reader characterStream) {

    }

    public InputStream getByteStream() {
        return byteStream;
    }

    public void setByteStream(InputStream byteStream) {
        this.byteStream = byteStream;
    }

    public String getStringData() {
        return null;
    }

    public void setStringData(String stringData) {

    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    public String getEncoding() {
        return null;
    }

    public void setEncoding(String encoding) {

    }

    public boolean getCertifiedText() {
        return false;
    }

    public void setCertifiedText(boolean certifiedText) {

    }
}
