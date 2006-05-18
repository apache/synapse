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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.AbstractListMediator;
import org.jaxen.JaxenException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Validate a message or an element against a schema
 */
public class ValidateMediator extends AbstractListMediator {

    private static final Log log = LogFactory.getLog(ValidateMediator.class);

    /** A space or comma delimitered list of schemas to validate the source element against */
    private String schemaUrl = null;
    /**
     * An XPath expression to be evaluated against the message to find the element to be validated.
     * If this is not specified, the validation will occur against the first child element of the SOAP body
     */
    private AXIOMXPath source = null;

    /**
     * Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking).
     */
    private static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";

    /**
     * Honour all schema locations feature id (http://apache.org/xml/features/honour-all-schemaLocations).
     */
    private static final String HONOUR_ALL_SCHEMA_LOCATIONS_ID = "http://apache.org/xml/features/honour-all-schemaLocations";

    /**
     * Default schema language (http://www.w3.org/2001/XMLSchema).
     */
    private static final String DEFAULT_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";


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

    /**
     * Return the node to be validated. If a source XPath is not specified, this will
     * default to the first child of the SOAP body
     * @param synCtx the message context
     * @return the OMNode against which validation should be performed
     */
    private OMNode getValidateSource(MessageContext synCtx) {

        AXIOMXPath sourceXPath = source;
        // do not change the source XPath if not specified, as it is shared..
        // and will cause confusion to concurrent messages and erroneous results

        if (sourceXPath == null) {
            log.debug("validation source was not specified.. defaulting to SOAP Body");
            try {
                sourceXPath = new AXIOMXPath("//SOAP-ENV:Body/child::*");
                sourceXPath.addNamespace("SOAP-ENV", synCtx.isSOAP11() ?
                    SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI : SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
            } catch (JaxenException e) {
                // this should not cause a runtime exception!
            }
        }

        try {
            Object o = sourceXPath.evaluate(synCtx.getEnvelope());
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

        try {
            // create a byte array output stream and serialize the source node into it
            ByteArrayOutputStream baosForSource = new ByteArrayOutputStream();
            XMLStreamWriter xsWriterForSource = XMLOutputFactory.newInstance().createXMLStreamWriter(baosForSource);

            // serialize the validation target and get an input stream into it
            OMNode sourceNode = getValidateSource(synCtx);
            sourceNode.serialize(xsWriterForSource);
            baisFromSource = new ByteArrayInputStream(baosForSource.toByteArray());

        } catch (Exception e) {
            String msg = "Error accessing source element for validation : " + source;
            log.error(msg);
            throw new SynapseException(msg, e);
        }

        try {
            // this is our custom validation handler
            SynapseValidator handler = new SynapseValidator();

            // Create SchemaFactory and configure
            SchemaFactory factory = SchemaFactory.newInstance(DEFAULT_SCHEMA_LANGUAGE);
            factory.setErrorHandler(handler);
            factory.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, true);
            factory.setFeature(HONOUR_ALL_SCHEMA_LOCATIONS_ID, true);

            // Build Schema from schemaUrl
            Schema schema = null;
            if (schemaUrl != null) {
                StringTokenizer st = new StringTokenizer(schemaUrl, " ,");
                int sourceCount = st.countTokens();

                if (sourceCount == 0) {
                    log.debug("Schemas have not been specified..");
                    schema = factory.newSchema();
                } else {
                    StreamSource[] sources = new StreamSource[sourceCount];
                    for (int j = 0; j < sourceCount; ++j) {
                        sources[j] = new StreamSource(st.nextToken());
                    }
                    schema = factory.newSchema(sources);
                }
            } else {
                log.debug("Schemas have not been specified..");
                schema = factory.newSchema();
            }

            // Setup validator and input source.
            Validator validator = schema.newValidator();
            validator.setErrorHandler(handler);
            validator.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, true);
            validator.setFeature(HONOUR_ALL_SCHEMA_LOCATIONS_ID, true);

            XMLReader reader = XMLReaderFactory.createXMLReader();
            SAXSource source = new SAXSource(reader, new InputSource(baisFromSource));
            validator.validate(source);

            if (handler.isValidationError()) {
                log.debug("Validation of element : " + source + " failed against : " + schemaUrl +
                    " Message : " + handler.getSaxParseException().getMessage() + " Executing 'on-fail' sequence");
                log.debug("Failed message envelope : " + synCtx.getEnvelope());
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
    private class SynapseValidator extends DefaultHandler {

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
