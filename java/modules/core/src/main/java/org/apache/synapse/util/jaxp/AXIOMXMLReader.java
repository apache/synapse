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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * SAX {@link XMLReader} implementation that reads the supplied AXIOM tree and invokes the
 * callback methods on the configured {@link ContentHandler}.
 * <p>
 * NOTE: THIS IS WORK IN PROGRESS!
 */
public class AXIOMXMLReader implements XMLReader {
    private final OMElement element;
    private final AttributesAdapter attributesAdapter = new AttributesAdapter();
    
    private boolean namespaces = true;
    private boolean namespacePrefixes = false;
    
    private ContentHandler contentHandler;
    private DTDHandler dtdHandler;
    private EntityResolver entityResolver;
    private ErrorHandler errorHandler;
    
    public AXIOMXMLReader(OMElement element) {
        this.element = element;
    }

    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    public DTDHandler getDTDHandler() {
        return dtdHandler;
    }

    public void setDTDHandler(DTDHandler dtdHandler) {
        this.dtdHandler = dtdHandler;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public boolean getFeature(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException(name);
    }

    public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        
        if ("http://xml.org/sax/features/namespaces".equals(name)) {
            namespaces = value;
        } else if ("http://xml.org/sax/features/namespace-prefixes".equals(name)) {
            namespacePrefixes = value;
        } else {
            throw new SAXNotRecognizedException(name);
        }
    }

    public Object getProperty(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        
        if ("http://xml.org/sax/features/namespaces".equals(name)) {
            return namespaces;
        } else if ("http://xml.org/sax/features/namespace-prefixes".equals(name)) {
            return namespacePrefixes;
        } else {
            throw new SAXNotRecognizedException(name);
        }
    }

    public void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException(name);
    }

    public void parse(InputSource input) throws IOException, SAXException {
        parse();
    }

    public void parse(String systemId) throws IOException, SAXException {
        parse();
    }
    
    private void parse() throws SAXException {
        contentHandler.startDocument();
        generateEvents(element);
        contentHandler.endDocument();
    }
    
    private void generateEvents(OMElement omElement) throws SAXException {
        OMNamespace omNamespace = omElement.getNamespace();
        String uri;
        String prefix;
        if (omNamespace != null) {
            uri = omNamespace.getNamespaceURI();
            prefix = omNamespace.getPrefix();
        } else {
            uri = "";
            prefix = null;
        }
        String localName = omElement.getLocalName();
        String qName;
        if (prefix == null || prefix.length() == 0) {
            qName = localName;
        } else {
            qName = prefix + ":" + localName;
        }
        // For performance reasons, we always reuse the same instance of AttributesAdapter.
        // This is explicitely allowed by the specification of the startElement method.
        attributesAdapter.setAttributes(omElement);
        contentHandler.startElement(uri, localName, qName, attributesAdapter);
        for (Iterator it = omElement.getChildren(); it.hasNext(); ) {
            OMNode node = (OMNode)it.next();
            switch (node.getType()) {
                case OMNode.ELEMENT_NODE:
                    generateEvents((OMElement)node);
                    break;
                case OMNode.TEXT_NODE:
                case OMNode.CDATA_SECTION_NODE:
                    generateEvents((OMText)node);
            }
        }
        contentHandler.endElement(uri, localName, qName);
    }
    
    private void generateEvents(OMText omText) throws SAXException {
        char[] ch = omText.getTextCharacters();
        contentHandler.characters(ch, 0, ch.length);
    }

    protected static class AttributesAdapter implements Attributes {
        private OMElement element;
        private List<OMAttribute> attributes = new ArrayList<OMAttribute>(5);

        public void setAttributes(OMElement element) {
            this.element = element;
            attributes.clear();
            for (Iterator it = element.getAllAttributes(); it.hasNext(); ) {
                attributes.add((OMAttribute)it.next());
            }
        }

        public int getLength() {
            return attributes.size();
        }

        public int getIndex(String name) {
            throw new UnsupportedOperationException();
        }

        public int getIndex(String uri, String localName) {
            throw new UnsupportedOperationException();
        }

        public String getLocalName(int index) {
            return attributes.get(index).getLocalName();
        }

        public String getQName(int index) {
            OMAttribute attribute = attributes.get(index);
            OMNamespace ns = attribute.getNamespace();
            if (ns == null) {
                return attribute.getLocalName();
            } else {
                String prefix = ns.getPrefix();
                if (prefix == null || prefix.length() == 0) {
                    return attribute.getLocalName();
                } else {
                    return ns.getPrefix() + ":" + attribute.getLocalName();
                }
            }
        }

        public String getType(int index) {
            return attributes.get(index).getAttributeType();
        }

        public String getType(String name) {
            throw new UnsupportedOperationException();
        }

        public String getType(String uri, String localName) {
            throw new UnsupportedOperationException();
        }

        public String getURI(int index) {
            OMNamespace ns = attributes.get(index).getNamespace();
            return ns == null ? "" : ns.getNamespaceURI();
        }

        public String getValue(int index) {
            return attributes.get(index).getAttributeValue();
        }

        public String getValue(String name) {
            throw new UnsupportedOperationException();
        }

        public String getValue(String uri, String localName) {
            throw new UnsupportedOperationException();
        }
    }
}
