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

package org.apache.synapse.util.jaxp;

import java.nio.charset.Charset;

import javax.xml.transform.sax.SAXResult;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;

/**
 * {@link ResultBuilder} implementation that relies on {@link OMContainer#getSAXResult()}.
 */
public class AXIOMResultBuilder implements ResultBuilder {
	private final OMDocument document = OMAbstractFactory.getOMFactory().createOMDocument();
    
    public SAXResult getResult() {
        return document.getSAXResult();
    }

    public OMElement getNode(Charset charset) {
        return document.getOMDocumentElement();
    }

    public void release() {
    }
}
