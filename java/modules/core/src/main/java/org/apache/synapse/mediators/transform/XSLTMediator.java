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
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.apache.synapse.util.AXIOMUtils;
import org.apache.synapse.util.TemporaryData;
import org.apache.synapse.util.TextFileDataSource;
import org.jaxen.JaxenException;
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

    /** Variable to hold source XPath string to use for debugging */
    private String sourceXPathString = null;

    /**
     * The (optional) XPath expression which yields the source element for a transformation
     */
    private SynapseXPath source = null;

    /**
     * The name of the message context property to store the transformation result  
     */
    private String targetPropertyName = null;

    /**
     * Any parameters which should be passed into the XSLT transformation
     */
    private List<MediatorProperty> properties = new ArrayList<MediatorProperty>();

    /**
     * Any features which should be set to the TransformerFactory by explicitly
     */
    private List<MediatorProperty> explicitFeatures = new ArrayList<MediatorProperty>();

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
     * Default XPath for the selection of the element for the evaluation of the XSLT over
     */
    public static final String DEFAULT_XPATH = "s11:Body/child::*[position()=1] | " +
            "s12:Body/child::*[position()=1]";

    public XSLTMediator() {
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

    /**
     * Transforms this message (or its element specified as the source) using the
     * given XSLT transformation
     *
     * @param synCtx the current message where the transformation will apply
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : XSLT mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        try {
            performXSLT(synCtx, traceOrDebugOn, traceOn);

        } catch (Exception e) {
            handleException("Unable to perform XSLT transformation using : " + xsltKey +
                " against source XPath : " +
                (sourceXPathString == null ? DEFAULT_XPATH : " source XPath : " +
                 sourceXPathString), e, synCtx);

        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : XSLT mediator");
        }

        return true;
    }

    /**
     * Perform actual XSLT transformation
     * @param synCtx current message
     * @param traceOrDebugOn is trace or debug on?
     * @param traceOn is trace on?
     */
    private void performXSLT(MessageContext synCtx, final boolean traceOrDebugOn,
        final boolean traceOn) {

        boolean reCreate = false;
        OMNode sourceNode = getTransformSource(synCtx);
        TemporaryData tempTargetData = null;
        OutputStream osForTarget;
        boolean isSoapEnvelope = (sourceNode == synCtx.getEnvelope());
        boolean isSoapBody = (sourceNode == synCtx.getEnvelope().getBody());

        if (traceOrDebugOn) {
            trace.trace("Transformation source : " + sourceNode.toString());
        }

        Source transformSrc;
        Result transformTgt = null;

        if (useDOMSourceAndResults) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Using a DOMSource for transformation");
            }

            // for fast transformations create a DOMSource - ** may not work always though **
            transformSrc = new DOMSource(
                ((Element) ElementHelper.importOMElement((OMElement) sourceNode,
                DOOMAbstractFactory.getOMFactory())).getOwnerDocument());
            DocumentBuilderFactoryImpl.setDOOMRequired(true);

            try {
                transformTgt = new DOMResult(
                    DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder().newDocument());
            } catch (ParserConfigurationException e) {
                handleException("Error creating a DOMResult for the transformation," +
                    " Consider setting optimization feature : " + USE_DOM_SOURCE_AND_RESULTS +
                    " off", e, synCtx);
            }

        } else {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Using byte array serialization for transformation");
            }

            transformSrc = AXIOMUtils.asSource(sourceNode);
            
            tempTargetData = synCtx.getEnvironment().createTemporaryData();
            osForTarget = tempTargetData.getOutputStream();
            transformTgt = new StreamResult(osForTarget);
        }

        if (transformTgt == null) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Was unable to get a javax.xml.transform.Result created");
            }
            return;
        }

        // build transformer - if necessary
        Entry dp = synCtx.getConfiguration().getEntryDefinition(xsltKey);

        // if the xsltKey refers to a dynamic resource
        if (dp != null && dp.isDynamic()) {
            if (!dp.isCached() || dp.isExpired()) {
                reCreate = true;
            }
        }

        synchronized (transformerLock) {
            if (reCreate || cachedTemplates == null) {
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
                for (MediatorProperty prop : properties) {
                    if (prop != null) {
                        if (prop.getValue() != null) {
                            transformer.setParameter(prop.getName(), prop.getValue());
                        } else {
                            transformer.setParameter(prop.getName(),
                                    prop.getExpression().stringValueOf(synCtx));
                        }
                    }
                }
            }

            transformer.setErrorListener(new ErrorListener() {

                public void warning(TransformerException e) throws TransformerException {

                    if (traceOrDebugOn) {
                        traceOrDebugWarn(
                                traceOn, "Warning encountered during transformation : " + e);
                    }
                }
                
                public void error(TransformerException e) throws TransformerException {
                    log.error("Error occured in XSLT transformation : " + e);
                    throw e;
                }
                
                public void fatalError(TransformerException e) throws TransformerException {
                    log.error("Fatal error occured in the XSLT transformation : " + e);
                    throw e;
                }
            });
            
            transformer.transform(transformSrc, transformTgt);

            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Transformation completed - processing result");
            }

            // get the result OMElement
            OMElement result = null;
            if (transformTgt instanceof DOMResult) {

                Node node = ((DOMResult) transformTgt).getNode();
                if (node == null) {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, ("Transformation result (DOMResult) was null"));
                    }
                    return;
                }

                Node resultNode = node.getFirstChild();
                if (resultNode == null) {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, ("Transformation result (DOMResult) was empty"));
                    }
                    return;
                }

                result = ElementHelper.importOMElement(
                    (OMElement) resultNode, OMAbstractFactory.getOMFactory());

            } else {

                String outputMethod = transformer.getOutputProperty(OutputKeys.METHOD);
                String encoding = transformer.getOutputProperty(OutputKeys.ENCODING);

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "output method: " + outputMethod
                            + "; encoding: " + encoding);
                }
                
                if ("text".equals(outputMethod)) {
                    result = handleNonXMLResult(tempTargetData, Charset.forName(encoding),
                                                traceOrDebugOn, traceOn);
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
                        handleException(
                            "Error building result element from XSLT transformation", e, synCtx);
    
                    } catch (IOException e) {
                        handleException("Error reading temporary data", e, synCtx);
                    }
                }
            }

            if (result == null) {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Transformation result was null");
                }
                return;
            } else {
                if (traceOn && trace.isTraceEnabled()) {
                    trace.trace("Transformation result : " + result.toString());
                }
            }

            if (targetPropertyName != null) {
                // add result XML as a message context property to the message
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Adding result as message context property : " +
                        targetPropertyName);
                }
                synCtx.setProperty(targetPropertyName, result);
            } else {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Replace " +
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

    /**
     * Return the OMNode to be used for the transformation. If a source XPath is not specified,
     * this will default to the first child of the SOAP body i.e. - //*:Envelope/*:Body/child::*
     *
     * @param synCtx the message context
     * @return the OMNode against which the transformation should be performed
     */
    private OMNode getTransformSource(MessageContext synCtx) {
                                
        try {
            Object o = source.evaluate(synCtx);
            if (o instanceof OMNode) {
                return (OMNode) o;
            } else if (o instanceof List && !((List) o).isEmpty()) {
                return (OMNode) ((List) o).get(0);  // Always fetches *only* the first
            } else {
                handleException("The evaluation of the XPath expression "
                        + source + " did not result in an OMNode", synCtx);
            }
        } catch (JaxenException e) {
            handleException("Error evaluating XPath expression : " + source, e, synCtx);
        }
        return null;
    }

    public SynapseXPath getSource() {
        return source;
    }

    public void setSource(SynapseXPath source) {
        this.source = source;
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
     * to add a feature which need to set to the TransformerFactory
     * @param  featureName The name of the feature
     * @param isFeatureEnable should this feature enable?
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
            explicitFeatures.add(mp);
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
     * If the transformation results in a non-XML payload, use standard wrapper elements
     * to wrap the text payload so that other mediators could still process the result
     * @param tempData the encoded text payload
     * @param charset the encoding of the payload
     * @param traceOrDebugOn is tracing on debug logging on?
     * @param traceOn is tracing on?
     * @return an OMElement wrapping the text payload
     */
    private OMElement handleNonXMLResult(TemporaryData tempData, Charset charset,
        boolean traceOrDebugOn, boolean traceOn) {

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Processing non SOAP/XML (text) transformation result");
        }
        if (traceOn && trace.isTraceEnabled()) {
            trace.trace("Wrapping text transformation result");
        }

        return TextFileDataSource.createOMSourcedElement(tempData, charset);
    }

    /**
     *
     * @return Returns the features explicitly  set to the TransformerFactory through this mediator
     */
    public List<MediatorProperty> getFeatures(){
        return explicitFeatures;
    }

    public void addAllProperties(List<MediatorProperty> list) {
        properties.addAll(list);
    }

    public List<MediatorProperty> getProperties() {
        return properties;
    }

    public void setSourceXPathString(String sourceXPathString) {
        this.sourceXPathString = sourceXPathString;
    }

    public String getTargetPropertyName() {
        return targetPropertyName;
    }

    public void setTargetPropertyName(String targetPropertyName) {
        this.targetPropertyName = targetPropertyName;
    }
    
}

	
