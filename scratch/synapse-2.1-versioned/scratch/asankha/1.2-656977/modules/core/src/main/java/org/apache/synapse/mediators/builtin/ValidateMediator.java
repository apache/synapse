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

package org.apache.synapse.mediators.builtin;

import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.util.AXIOMUtils;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validate a message or an element against a schema
 * <p/>
 * This internally uses the Xerces2-j parser, which cautions a lot about thread-safety and
 * memory leaks. Hence this initial implementation will create a single parser instance
 * for each unique mediator instance, and re-use it to validate multiple messages - even
 * concurrently - by synchronizing access
 */
public class ValidateMediator extends AbstractListMediator {

    /**
     * A list of property keys, referring to the schemas to be used for the validation
     */
    private List<String> schemaKeys = new ArrayList<String>();

    /**
     * An XPath expression to be evaluated against the message to find the element to be validated.
     * If this is not specified, the validation will occur against the first child element of the
     * SOAP body
     */
    private SynapseXPath source = null;

    /**
     * A Map containing features to be passed to the actual validator (Xerces)
     */
    private List<MediatorProperty> explicityFeatures = new ArrayList<MediatorProperty>();

    /**
     * This is the actual schema instance used to create a new schema
     * This is a thred-safe instance.
     */
    private Schema cachedSchema;

    /**
     * Lock used to ensure thread-safe creation and use of the above Validator
     */
    private final Object validatorLock = new Object();

    /**
     * The SchemaFactory used to create new schema instances.
     */
    private  SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

//    private static final String DEFAULT_XPATH = "//s11:Envelope/s11:Body/child::*[position()=1] | " +
//        "//s12:Envelope/s12:Body/child::*[position()=1]";

    public static final String DEFAULT_XPATH = "s11:Body/child::*[position()=1] | " +
        "s12:Body/child::*[position()=1]";
    
    public ValidateMediator() {
        // create the default XPath
        try {
            this.source = new SynapseXPath(DEFAULT_XPATH);
            this.source.addNamespace("s11", SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
            this.source.addNamespace("s12", SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
        } catch (JaxenException e) {
            String msg = "Error creating default source XPath expression : " + DEFAULT_XPATH;
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }
    }

    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Validate mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        // Input source for the validation
        Source validateSrc = getValidationSource(synCtx, traceOrDebugOn, traceOn);

        // flag to check if we need to initialize/re-initialize the schema
        boolean reCreate = false;
        // if any of the schemas are not loaded, or have expired, load or re-load them
        for (String propKey : schemaKeys) {
            Entry dp = synCtx.getConfiguration().getEntryDefinition(propKey);
            if (dp != null && dp.isDynamic()) {
                if (!dp.isCached() || dp.isExpired()) {
                    reCreate = true;       // request re-initialization of Validator
                }
            }
        }

        // This is the reference to the DefaultHandler instance
        MyErrorHandler errorHandler = new MyErrorHandler();

        // do not re-initialize schema unless required
        synchronized (validatorLock) {
            if (reCreate || cachedSchema == null) {

                factory.setErrorHandler(errorHandler);
                StreamSource[] sources = new StreamSource[schemaKeys.size()];
                int i = 0;
                for (String propName : schemaKeys) {
                    sources[i++] = SynapseConfigUtils.getStreamSource(synCtx.getEntry(propName));
                }

                try {
                    cachedSchema = factory.newSchema(sources);
                } catch (SAXException e) {
                    handleException("Error creating a new schema objects for " +
                        "schemas : " + schemaKeys.toString(), e, synCtx);
                }

                if (errorHandler.isValidationError()) {
                    //reset the errorhandler state
                    errorHandler.setValidationError(false);

                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Error creating a new schema objects for " +
                            "schemas : " + schemaKeys.toString());
                    }
                }
            }
        }

        // no need to synchronize, schema instances are thread-safe
        try {
            Validator validator = cachedSchema.newValidator();
            validator.setErrorHandler(errorHandler);

            // perform actual validation
            validator.validate(validateSrc);

            if (errorHandler.isValidationError()) {

                if (traceOrDebugOn) {
                    String msg = "Validation of element returned by XPath : " + source +
                        " failed against the given schema(s) " + schemaKeys +
                        "with error : " + errorHandler.getSaxParseException().getMessage() +
                        " Executing 'on-fail' sequence";
                    traceOrDebug(traceOn, msg);

                    // write a warning to the service log
                    synCtx.getServiceLog().warn(msg);

                    if (traceOn && trace.isTraceEnabled()) {
                        log.debug("Failed message envelope : " + synCtx.getEnvelope());
                    }
                }

                // set error message and detail (stack trace) into the message context
                synCtx.setProperty(SynapseConstants.ERROR_MESSAGE,
                    errorHandler.getSaxParseException().getMessage());
                synCtx.setProperty(SynapseConstants.ERROR_EXCEPTION,
                    errorHandler.getSaxParseException());
                synCtx.setProperty(SynapseConstants.ERROR_DETAIL,
                    FaultHandler.getStackTrace(errorHandler.getSaxParseException()));

                // super.mediate() invokes the "on-fail" sequence of mediators
                return super.mediate(synCtx);
            }
        } catch (SAXException e) {
            handleException("Error validating " + source + " element", e, synCtx);
        } catch (IOException e) {
            handleException("Error validating " + source + " element", e, synCtx);
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Validation of element returned by the XPath expression : "
                + source + " succeeded against the given schemas and the current message");
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Validate mediator");
        }

        return true;
    }

    /**
     * Get the validation Source for the message context
     *
     * @param synCtx the current message to validate
     * @param traceOrDebugOn is tracing or debugging on?
     * @param traceOn is tracing on?
     * @return the validation Source for the current message
     */
    private Source getValidationSource(MessageContext synCtx,
        boolean traceOrDebugOn, boolean traceOn) {

        try {
            OMNode validateSource = getValidateSource(synCtx);
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Validation source : " + validateSource.toString());
            }

            return AXIOMUtils.asSource(validateSource);

        } catch (Exception e) {
            handleException("Error accessing source element : " + source, e, synCtx);
        }
        return null; // never reaches here
    }

    /**
     * This class handles validation errors to be used for the error reporting
     */
    private class MyErrorHandler extends DefaultHandler {

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
        
        /**
         * To set explicitly validation error condition
         * @param validationError  is occur validation error?
         */
        public void setValidationError(boolean validationError) {
            this.validationError = validationError;
        }
    }

    /**
     * Return the OMNode to be validated. If a source XPath is not specified, this will
     * default to the first child of the SOAP body i.e. - //*:Envelope/*:Body/child::*
     *
     * @param synCtx the message context
     * @return the OMNode against which validation should be performed
     */
    private OMNode getValidateSource(MessageContext synCtx) {

        try {
            Object o = source.evaluate(synCtx);
            if (o instanceof OMNode) {
                return (OMNode) o;
            } else if (o instanceof List && !((List) o).isEmpty()) {
                return (OMNode) ((List) o).get(0);  // Always fetches *only* the first
            } else {
                handleException("The evaluation of the XPath expression "
                    + source + " did not result in an OMNode : " + o, synCtx);
            }
        } catch (JaxenException e) {
            handleException("Error evaluating XPath expression : " + source, e, synCtx);
        }
        return null;
    }

    // setters and getters

    /**
     * Get a mediator feature. The common use case is a feature for the
     * underlying Xerces validator
     *
     * @param key property key / feature name
     * @return property string value (usually true|false)
     */
    public Object getFeature(String key) {
        for (MediatorProperty prop : explicityFeatures) {
            if (key.equals(prop.getName())) {
                return prop.getValue();
            }
        }
        return null;
    }

    /**
     * add a feature which need to set for the Schema Factory
     *
     * @param  featureName The name of the feature
     * @param isFeatureEnable should this feature enable?(true|false)
     * @see #getFeature(String)
     * @throws SAXException on an unknown feature
     */
   public void addFeature(String featureName, boolean isFeatureEnable) throws SAXException {
        MediatorProperty mp = new MediatorProperty();
        mp.setName(featureName);
        if (isFeatureEnable) {
            mp.setValue("true");
        } else {
            mp.setValue("false");
        }
        explicityFeatures.add(mp);
        factory.setFeature(featureName, isFeatureEnable);
    }

    /**
     * Set a list of local property names which refer to a list of schemas to be
     * used for validation
     *
     * @param schemaKeys list of local property names
     */
    public void setSchemaKeys(List<String> schemaKeys) {
        this.schemaKeys = schemaKeys;
    }

    /**
     * Set the given XPath as the source XPath
     * @param source an XPath to be set as the source
     */
    public void setSource(SynapseXPath source) {
       this.source = source;
    }

    /**
     * Get the source XPath which yields the source element for validation
     * @return the XPath which yields the source element for validation
     */
    public SynapseXPath getSource() {
        return source;
    }

    /**
     * The keys for the schema resources used for validation
     * @return schema registry keys
     */
    public List<String> getSchemaKeys() {
        return schemaKeys;
    }

    /**
     * Features for the actual Xerces validator
     * @return explicityFeatures to be passed to the Xerces validator
     */
    public List<MediatorProperty> getFeatures() {
        return explicityFeatures;
    }
}
