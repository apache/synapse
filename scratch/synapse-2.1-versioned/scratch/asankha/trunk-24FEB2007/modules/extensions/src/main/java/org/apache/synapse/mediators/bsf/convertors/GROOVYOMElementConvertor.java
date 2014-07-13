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

/**
 * Converts between AXIOM OMElement and Groovy Node objects
 * TODO: find a more efficent way to do this using STaX instead of going via a String
 */
public class GROOVYOMElementConvertor extends DefaultOMElementConvertor {

    public GROOVYOMElementConvertor() {
    }

    /**
     * Converts an OMElement into a groovy.util.Node
     */
    public Object toScript(OMElement o) {
        try {

            Node xmlNode = new XmlParser().parseText(o.toString());
            return xmlNode;

        } catch (Exception e) {
            throw new SynapseException(e);
        }        
    }

    /**
     * Converts a Groovy object into a OMElement
     */
    public OMElement fromScript(Object o) {
        try {

            OMElement omElement;

            if (o instanceof Node) {
                omElement = nodeToOMElement((Node)o);
            } else if (o instanceof Writable){
                omElement = writableToOMElement((Writable)o);
            } else {
                throw new SynapseException("unknown type: " + o);
            }

            return omElement;

        } catch (Exception e) {
            throw new SynapseException(e);
        }
    }

    protected OMElement writableToOMElement(Writable writable) throws IOException, XMLStreamException {
        Writer out = new StringWriter();
        writable.writeTo(out);
        out.close();
        StAXOMBuilder builder = new StAXOMBuilder(out.toString());
        OMElement omElement = builder.getDocumentElement();
        return omElement;
    }

    protected OMElement nodeToOMElement(Node node) throws XMLStreamException {
        StringWriter out = new StringWriter();
        new XmlNodePrinter(new PrintWriter(out)).print(node);
        String xmlString = out.toString();
        StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(xmlString.getBytes()));
        OMElement omElement = builder.getDocumentElement();
        return omElement;
    }

}
