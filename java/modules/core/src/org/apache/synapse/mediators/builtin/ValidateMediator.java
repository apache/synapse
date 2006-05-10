/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse.mediators.builtin;

import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.jaxen.JaxenException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Iterator;

/**
 * Validate a message or an element against a schema
 */
public class ValidateMediator extends AbstractListMediator {

    private static final Log log = LogFactory.getLog(ValidateMediator.class);

    private String schemaUrl = null;
    private AXIOMXPath source = null;

    private static final String SCHEMA_LOCATION_NO_NS =
        "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";
    private static final String SCHEMA_LOCATION_NS =
        "http://apache.org/xml/properties/schema/external-schemaLocation";
    private static final String FULL_CHECKING = "http://apache.org/xml/features/validation/schema-full-checking";
    private static final String SCHEMA_VALIDATION = "http://apache.org/xml/features/validation/schema";
    private static final String VALIDATION = "http://xml.org/sax/features/validation";

    public String getSchemaUrl() {
        return schemaUrl;
    }

    public void setSchemaUrl(String schemaUrl) {
        this.schemaUrl = schemaUrl;
    }

    public AXIOMXPath getSource() {
        return source;
    }

    public void setSource(AXIOMXPath source) {
        this.source = source;
    }

    private OMNode getValidateSource(MessageContext synCtx) {

        if (source == null) {
            try {
                source = new AXIOMXPath("//SOAP-ENV:Body");
                source.addNamespace("SOAP-ENV", synCtx.isSOAP11() ?
                    SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI : SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
            } catch (JaxenException e) {}
        }

        try {
            Object o = source.evaluate(synCtx.getEnvelope());
            if (o instanceof OMNode) {
                return (OMNode) o;
            } else if (o instanceof List && !((List) o).isEmpty()) {
                return (OMNode) ((List) o).get(0);  // Always fetches *only* the first
            } else {
                String msg = "The evaluation of the XPath expression " + source + " must result in an OMNode";
                log.error(msg);
                throw new SynapseException(msg);
            }

        } catch (JaxenException e) {
            String msg = "Error evaluating XPath " + source + " on message";
            log.error(msg);
            throw new SynapseException(msg, e);
        }
    }

    public boolean mediate(MessageContext synCtx) {

        ByteArrayInputStream baisFromSource = null;
        StringBuffer nsLocations = new StringBuffer();

        try {
            // create a byte array output stream and serialize the source node into it
            ByteArrayOutputStream baosForSource = new ByteArrayOutputStream();
            XMLStreamWriter xsWriterForSource = XMLOutputFactory.newInstance().createXMLStreamWriter(baosForSource);

            // save the list of defined namespaces for validation against the schema
            OMNode sourceNode = getValidateSource(synCtx);
            if (sourceNode instanceof OMElement) {
                Iterator iter = ((OMElement) sourceNode).getAllDeclaredNamespaces();
                while (iter.hasNext()) {
                    OMNamespace omNS = (OMNamespace) iter.next();
                    nsLocations.append(omNS.getName() + " " + getSchemaUrl());
                }
            }
            sourceNode.serialize(xsWriterForSource);
            baisFromSource = new ByteArrayInputStream(baosForSource.toByteArray());

        } catch (Exception e) {
            String msg = "Error accessing source element for validation : " + source;
            log.error(msg);
            throw new SynapseException(msg, e);
        }

        try {
            SAXParserFactory spFactory = SAXParserFactory.newInstance();
            spFactory.setNamespaceAware(true);
            spFactory.setValidating(true);
            SAXParser parser = spFactory.newSAXParser();

            parser.setProperty(VALIDATION, Boolean.TRUE);
            parser.setProperty(SCHEMA_VALIDATION, Boolean.TRUE);
            parser.setProperty(FULL_CHECKING, Boolean.TRUE);
            parser.setProperty(SCHEMA_LOCATION_NS, nsLocations.toString());
            parser.setProperty(SCHEMA_LOCATION_NO_NS, getSchemaUrl());

            Validator handler = new Validator();
            parser.parse(baisFromSource, handler);

            if (handler.isValidationError()) {
                log.debug("Validation failed :" + handler.getSaxParseException().getMessage());
                // super.mediate() invokes the "on-fail" sequence of mediators
                return super.mediate(synCtx);
            }

        }
        catch (Exception e) {
            String msg = "Error validating " + source + " against schema : " + schemaUrl + " : " + e.getMessage();
            log.error(msg);
            throw new SynapseException(msg, e);
        }

        return true;
    }

    /**
     * This class handles validation errors to be used for error reporting
     */
    private class Validator extends DefaultHandler {

        private boolean validationError = false;
        private SAXParseException saxParseException = null;

        public void error(SAXParseException exception) throws SAXException {
            validationError = true;
            saxParseException = exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            validationError = true;
            saxParseException = exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
        }

        public boolean isValidationError() {
            return validationError;
        }

        public SAXParseException getSaxParseException() {
            return saxParseException;
        }
    }

}
