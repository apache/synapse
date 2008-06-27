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

package org.apache.synapse.mediators.transform;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.om.impl.dom.jaxp.DocumentBuilderFactoryImpl;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.util.xpath.SourceXPathSupport;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.apache.synapse.util.AXIOMUtils;
import org.apache.synapse.util.TemporaryData;
import org.apache.synapse.util.TextFileDataSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The XSLT mediator performs an XSLT transformation requested, using
 * the current message. The source attribute (if available) specifies the source element
 * on which the transformation would be applied. It will default to the first child of
 * the messages' SOAP body, if it is omitted.
 *
 * Additional properties passed into this mediator would become parameters for XSLT.
 * Additional features passed into this mediator would become features except for
 * "http://ws.apache.org/ns/synapse/transform/feature/dom" for the Transformer Factory, which
 * is used to decide between using DOM and Streams during the transformation process. By default
 * this is turned on as an optimization, but should be set to false if issues are detected
 *
 *  Note: Set the TransformerFactory system property to generate and use translets
 *  -Djavax.xml.transform.TransformerFactory=org.apache.xalan.xsltc.trax.TransformerFactoryImpl
 * 
 */
public class XSLTMediator extends AbstractMediator {
    
    private static class ErrorListenerImpl implements ErrorListener {
        private final SynapseLog synLog;
        private final String activity;
        
        public ErrorListenerImpl(SynapseLog synLog, String activity) {
            this.synLog = synLog;
            this.activity = activity;
        }
        
        public void warning(TransformerException e) throws TransformerException {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebugWarn("Warning encountered during " + activity + " : " + e);
            }
        }
        
        public void error(TransformerException e) throws TransformerException {
            synLog.error("Error occured in " + activity + " : " + e);
            throw e;
        }
        
        public void fatalError(TransformerException e) throws TransformerException {
            synLog.error("Fatal error occured in " + activity + " : " + e);
            throw e;
        }
    }
    
    /**
     * The feature for which deciding swiching between DOM and Stream during the
     * transformation process
     */
    public static final String USE_DOM_SOURCE_AND_RESULTS =
        "http://ws.apache.org/ns/synapse/transform/feature/dom";
    /**
     * The resource key/name which refers to the XSLT to be used for the transformation
     */
    private String xsltKey = null;

    /**
     * The (optional) XPath expression which yields the source element for a transformation
     */
    private final SourceXPathSupport source = new SourceXPathSupport();

    /**
     * The name of the message context property to store the transformation result  
     */
    private String targetPropertyName = null;

    /**
     * Any parameters which should be passed into the XSLT transformation
     */
    private List<MediatorProperty> properties = new ArrayList<MediatorProperty>();

    /**
     * Any features which should be set to the TransformerFactory explicitly
     */
    private List<MediatorProperty> transformerFactoryFeatures = new ArrayList<MediatorProperty>();

    /**
     * Any attributes which should be set to the TransformerFactory explicitly
     */
    private List<MediatorProperty> transformerFactoryAttributes
                = new ArrayList<MediatorProperty>();

    /**
     * The Template instance used to create a Transformer object. This is  thread-safe
     *
     * @see javax.xml.transform.Templates
     */
    private Templates cachedTemplates = null;

    /**
     * The TransformerFactory instance which use to create Templates...This is not thread-safe.
     * @see javax.xml.transform.TransformerFactory
     */
    private final TransformerFactory transFact = TransformerFactory.newInstance();

    /**
     * Lock used to ensure thread-safe creation and use of the above Transformer
     */
    private final Object transformerLock = new Object();

    /**
     *  Is it need to use DOMSource and DOMResult?
     */
    private boolean useDOMSourceAndResults = false;

    /**
     * Transforms this message (or its element specified as the source) using the
     * given XSLT transformation
     *
     * @param synCtx the current message where the transformation will apply
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {

        SynapseLog synLog = getLog(synCtx);

        synLog.traceOrDebug("Start : XSLT mediator");
        if (synLog.isTraceTraceEnabled()) {
            synLog.traceTrace("Message : " + synCtx.getEnvelope());
        }

        try {
            performXSLT(synCtx, synLog);

        } catch (Exception e) {
            handleException("Unable to perform XSLT transformation using : " + xsltKey +
                " against source XPath : " + source, e, synCtx);

        }

        synLog.traceOrDebug("End : XSLT mediator");

        return true;
    }

    /**
     * Perform actual XSLT transformation
     * @param synCtx current message
     * @param synLog the logger to be used
     */
    private void performXSLT(MessageContext synCtx, SynapseLog synLog) {

        OMNode sourceNode = source.selectOMNode(synCtx, synLog);
        TemporaryData tempTargetData = null;
        OutputStream osForTarget;
        boolean isSoapEnvelope = (sourceNode == synCtx.getEnvelope());
        boolean isSoapBody = (sourceNode == synCtx.getEnvelope().getBody());

        if (synLog.isTraceTraceEnabled()) {
            synLog.traceTrace("Transformation source : " + sourceNode.toString());
        }

        Source transformSrc;
        Result transformTgt;

        if (useDOMSourceAndResults) {
            synLog.traceOrDebug("Using a DOMSource for transformation");

            // for fast transformations create a DOMSource - ** may not work always though **
            transformSrc = new DOMSource(
                ((Element) ElementHelper.importOMElement((OMElement) sourceNode,
                DOOMAbstractFactory.getOMFactory())).getOwnerDocument());
            DocumentBuilderFactoryImpl.setDOOMRequired(true);

            try {
                transformTgt = new DOMResult(
                    DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder().newDocument());
            } catch (ParserConfigurationException e) {
                throw new SynapseException("Error creating a DOMResult for the transformation," +
                    " Consider setting optimization feature : " + USE_DOM_SOURCE_AND_RESULTS +
                    " off", e, synLog);
            }

        } else {
            synLog.traceOrDebug("Using byte array serialization for transformation");

            transformSrc = AXIOMUtils.asSource(sourceNode);
            
            tempTargetData = synCtx.getEnvironment().createTemporaryData();
            osForTarget = tempTargetData.getOutputStream();
            transformTgt = new StreamResult(osForTarget);
        }

        // build transformer - if necessary
        Entry dp = synCtx.getConfiguration().getEntryDefinition(xsltKey);

        // if the xsltKey refers to a dynamic resource
        boolean reCreate = dp != null && dp.isDynamic() && (!dp.isCached() || dp.isExpired());

        synchronized (transformerLock) {
            if (reCreate || cachedTemplates == null) {
                // Set an error listener (SYNAPSE-307).
                transFact.setErrorListener(new ErrorListenerImpl(synLog, "stylesheet parsing"));
                try {
                    cachedTemplates = transFact.newTemplates(
                        SynapseConfigUtils.getStreamSource(synCtx.getEntry(xsltKey)));
                    if (cachedTemplates == null) {
                        handleException("Error compiling the XSLT with key : " + xsltKey, synCtx);
                    }
                } catch (Exception e) {
                    handleException("Error creating XSLT transformer using : "
                        + xsltKey, e, synCtx);
                }
            }
        }

        try {
            // perform transformation
            Transformer transformer = cachedTemplates.newTransformer();
            if (!properties.isEmpty()) {
                // set the parameters which will pass to the Transformation
                applyProperties(transformer, synCtx, synLog);
            }

            transformer.setErrorListener(new ErrorListenerImpl(synLog, "XSLT transformation"));
            
            transformer.transform(transformSrc, transformTgt);

            synLog.traceOrDebug("Transformation completed - processing result");

            // get the result OMElement
            OMElement result;
            if (transformTgt instanceof DOMResult) {

                Node node = ((DOMResult) transformTgt).getNode();
                if (node == null) {
                    synLog.traceOrDebug("Transformation result (DOMResult) was null");
                    return;
                }

                Node resultNode = node.getFirstChild();
                if (resultNode == null) {
                    synLog.traceOrDebug("Transformation result (DOMResult) was empty");
                    return;
                }

                result = ElementHelper.importOMElement(
                    (OMElement) resultNode, OMAbstractFactory.getOMFactory());

            } else {

                String outputMethod = transformer.getOutputProperty(OutputKeys.METHOD);
                String encoding = transformer.getOutputProperty(OutputKeys.ENCODING);

                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("output method: " + outputMethod
                            + "; encoding: " + encoding);
                }
                
                if ("text".equals(outputMethod)) {
                    result = handleNonXMLResult(tempTargetData, Charset.forName(encoding), synLog);
                } else {
                    try {
                        XMLStreamReader reader = StAXUtils.createXMLStreamReader(
                            tempTargetData.getInputStream());
                        if (isSoapEnvelope) {
                            result = new StAXSOAPModelBuilder(reader).getSOAPEnvelope();
                        } else {
                            result = new StAXOMBuilder(reader).getDocumentElement();
                        }                        
    
                    } catch (XMLStreamException e) {
                        throw new SynapseException(
                            "Error building result element from XSLT transformation", e, synLog);
    
                    } catch (IOException e) {
                        throw new SynapseException("Error reading temporary data", e, synLog);
                    }
                }
            }

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Transformation result : " + result.toString());
            }

            if (targetPropertyName != null) {
                // add result XML as a message context property to the message
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Adding result as message context property : " +
                        targetPropertyName);
                }
                synCtx.setProperty(targetPropertyName, result);
            } else {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Replace " +
                        (isSoapEnvelope ? "SOAP envelope" : isSoapBody ? "SOAP body" : "node")
                        + " with result");
                }

                if (isSoapEnvelope) {
                    try {
                        synCtx.setEnvelope((SOAPEnvelope) result);
                    } catch (AxisFault ex) {
                        handleException("Unable to replace SOAP envelope with result", ex, synCtx);
                    }

                } else if (isSoapBody) {
                    for (Iterator iter = synCtx.getEnvelope().getBody().getChildElements();
                        iter.hasNext(); ) {
                        OMElement child = (OMElement) iter.next();
                        child.detach();
                    }

                    for (Iterator iter = result.getChildElements(); iter.hasNext(); ) {
                        OMElement child = (OMElement) iter.next();
                        synCtx.getEnvelope().getBody().addChild(child);
                    }

                } else {
                    sourceNode.insertSiblingAfter(result);
                    sourceNode.detach();
                }
            }

        } catch (TransformerException e) {
            handleException("Error performing XSLT transformation using : " + xsltKey, e, synCtx);
        }
    }

    public SynapseXPath getSource() {
        return source.getXPath();
    }

    public void setSource(SynapseXPath source) {
        this.source.setXPath(source);
    }

    public String getXsltKey() {
        return xsltKey;
    }

    public void setXsltKey(String xsltKey) {
        this.xsltKey = xsltKey;
    }

    public void addProperty(MediatorProperty p) {
        properties.add(p);
    }
    
    /**
     * Set the properties defined in the mediator as parameters on the stylesheet.
     * 
     * @param transformer
     * @param synCtx
     * @param synLog
     */
    private void applyProperties(Transformer transformer, MessageContext synCtx,
                                 SynapseLog synLog) {
        for (MediatorProperty prop : properties) {
            if (prop != null) {
                String value;
                if (prop.getValue() != null) {
                    value = prop.getValue();
                } else {
                    value = prop.getExpression().stringValueOf(synCtx);
                }
                if (synLog.isTraceOrDebugEnabled()) {
                    if (value == null) {
                        synLog.traceOrDebug("Not setting parameter '" + prop.getName() + "'");
                    } else {
                        synLog.traceOrDebug("Setting parameter '" + prop.getName() + "' to '"
                                + value + "'");
                    }
                }
                if (value != null) {
                    transformer.setParameter(prop.getName(), value);
                }
            }
        }
    }
    
    /**
     * Add a feature to be set on the {@link TransformerFactory} used by this mediator instance.
     * This method can also be used to enable some Synapse specific optimizations and
     * enhancements as described in the documentation of this class.
     * 
     * @param featureName The name of the feature
     * @param isFeatureEnable the desired state of the feature
     * 
     * @see TransformerFactory#setFeature(String, boolean)
     * @see XSLTMediator
     */
    public void addFeature(String featureName, boolean isFeatureEnable) {
        try {
            MediatorProperty mp = new MediatorProperty();
            mp.setName(featureName);
            if (isFeatureEnable) {
                mp.setValue("true");
            } else {
                mp.setValue("false");
            }
            transformerFactoryFeatures.add(mp);
            if (USE_DOM_SOURCE_AND_RESULTS.equals(featureName)) {
                useDOMSourceAndResults = isFeatureEnable;
            } else {
                transFact.setFeature(featureName, isFeatureEnable);
            }
        } catch (TransformerConfigurationException e) {
            String msg = "Error occured when setting features to the TransformerFactory";
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }
    }

    /**
     * Add an attribute to be set on the {@link TransformerFactory} used by this mediator instance.
     * This method can also be used to enable some Synapse specific optimizations and
     * enhancements as described in the documentation of this class.
     * 
     * @param name The name of the feature
     * @param value should this feature enable?
     * 
     * @see TransformerFactory#setAttribute(String, Object)
     * @see XSLTMediator
     */
    public void addAttribute(String name, String value) {
        MediatorProperty mp = new MediatorProperty();
        mp.setName(name);
        mp.setValue(value);
        transformerFactoryAttributes.add(mp);
        try {
            transFact.setAttribute(name, value);
        } catch (IllegalArgumentException e) {
            String msg = "Error occured when setting attribute to the TransformerFactory";
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }
    }

    /**
     * If the transformation results in a non-XML payload, use standard wrapper elements
     * to wrap the text payload so that other mediators could still process the result
     * @param tempData the encoded text payload
     * @param charset the encoding of the payload
     * @param synLog the logger to be used
     * @return an OMElement wrapping the text payload
     */
    private OMElement handleNonXMLResult(TemporaryData tempData, Charset charset,
                                         SynapseLog synLog) {

        synLog.traceOrDebug("Processing non SOAP/XML (text) transformation result");
        synLog.traceTrace("Wrapping text transformation result");

        return TextFileDataSource.createOMSourcedElement(tempData, charset);
    }

    /**
     * @return Return the features explicitly set to the TransformerFactory through this mediator.
     */
    public List<MediatorProperty> getFeatures(){
        return transformerFactoryFeatures;
    }

    /**
     * @return Return the attributes explicitly set to the TransformerFactory through this mediator.
     */
    public List<MediatorProperty> getAttributes(){
        return transformerFactoryAttributes;
    }

    public void addAllProperties(List<MediatorProperty> list) {
        properties.addAll(list);
    }

    public List<MediatorProperty> getProperties() {
        return properties;
    }

    public void setSourceXPathString(String sourceXPathString) {
        this.source.setXPathString(sourceXPathString);
    }

    public String getTargetPropertyName() {
        return targetPropertyName;
    }

    public void setTargetPropertyName(String targetPropertyName) {
        this.targetPropertyName = targetPropertyName;
    }
    
}

	
