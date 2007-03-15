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

package org.apache.synapse.mediators.bsf.convertors;

import java.io.ByteArrayInputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.bsf.BSFEngine;
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The DefaultOMElementConvertor converts between OMElements and Strings
 */
public class DefaultOMElementConvertor implements OMElementConvertor {

    private static final Log log = LogFactory.getLog(DefaultOMElementConvertor.class);

    public OMElement fromScript(Object o) {
        if (o == null) {
            handleException("Cannot convert null JavaScript Object to an OMElement");
        }

        try {
            StAXOMBuilder builder = new StAXOMBuilder(
                new ByteArrayInputStream(o.toString().getBytes()));
            return builder.getDocumentElement();

        } catch (XMLStreamException e) {
            handleException("Error converting Object of type : " + o.getClass().getName() + " to String");
        }
        return null;
    }

    public Object toScript(OMElement omElement) {
        return omElement.toString();
    }

    public void setEngine(BSFEngine e) {
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
