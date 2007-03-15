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

import groovy.lang.Writable;
import groovy.util.Node;
import groovy.util.XmlNodePrinter;
import groovy.util.XmlParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Converts between AXIOM OMElement and Groovy Node objects
 * TODO: find a more efficent way to do this using STaX instead of going via a String
 */
public class GROOVYOMElementConvertor extends DefaultOMElementConvertor {

    private static final Log log = LogFactory.getLog(GROOVYOMElementConvertor.class);

    /**
     * Converts an OMElement into a groovy.util.Node
     */
    public Object toScript(OMElement o) {
        try {
            return new XmlParser().parseText(o.toString());

        } catch (Exception e) {
            handleException("Error converting OMElement to Groovy object", e);
        }
        return null;
    }

    /**
     * Converts a Groovy object into a OMElement
     */
    public OMElement fromScript(Object o) {

        if (o == null) {
            handleException("Cannot convert null Groovy Object to an OMElement");
        }

        try {
            if (o instanceof Node) {
                return nodeToOMElement((Node)o);
            } else if (o instanceof Writable){
                return writableToOMElement((Writable)o);
            } else {
                handleException("Unknown Groovy object : " + o.getClass().getName() +
                " to be converted to an OMElement");
            }

        } catch (Exception e) {
            handleException("Error convering Groovy object : " + o.getClass().getName() +
                " into an OMElement" + e);
        }
        return null;
    }

    private OMElement writableToOMElement(Writable writable) throws IOException, XMLStreamException {
        Writer out = new StringWriter();
        writable.writeTo(out);
        out.close();
        StAXOMBuilder builder = new StAXOMBuilder(out.toString());
        return builder.getDocumentElement();
    }

    private OMElement nodeToOMElement(Node node) throws XMLStreamException {
        StringWriter out = new StringWriter();
        new XmlNodePrinter(new PrintWriter(out)).print(node);
        String xmlString = out.toString();
        StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(xmlString.getBytes()));
        return builder.getDocumentElement();
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
