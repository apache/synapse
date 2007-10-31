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
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.om.impl.dom.jaxp.DocumentBuilderFactoryImpl;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.impl.llom.OMSourcedElementImpl;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.util.FixedByteArrayOutputStream;
import org.apache.synapse.util.TextFileDataSource;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.transport.base.BaseConstants;
import org.jaxen.JaxenException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.namespace.QName;
import javax.activation.FileDataSource;
import javax.activation.DataHandler;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The XSLT mediator performs an XSLT transformation requested, using
 * the current message. The source attribute (if available) spcifies the source element
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

    /** Maximum size of a byte array stream attempted in-memory before file serialization is used */
    private static final int BYTE_ARRAY_SIZE = 8192;
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
     * The (optional) XPath expression which yeilds the source element for a transformation
     */
    private AXIOMXPath source = null;

    /**
     * Any parameters which should be passed into the XSLT transformation
     */
    private List properties = new ArrayList();

    /**
     * Any features which should be set to the TransformerFactory by explicitly
     */
    private List explicitFeatures = new ArrayList();

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

    // todo - this is a hack to get the handler module case working - ruwan
    //    public static final String DEFAULT_XPATH = "//s11:Envelope/s11:Body/child::*[position()=1] | " +
    //            "//s12:Envelope/s12:Body/child::*[position()=1]";
    public static final String DEFAULT_XPATH = "s11:Body/child::*[position()=1] | " +
            "s12:Body/child::*[position()=1]";

    public XSLTMediator() {
        // create the default XPath
        try {
            this.source = new AXIOMXPath(DEFAULT_XPATH);
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
            performXLST(synCtx, traceOrDebugOn, traceOn);

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
    private void performXLST(MessageContext synCtx, boolean traceOrDebugOn, boolean traceOn) {

        boolean reCreate = false;
        OMNode sourceNode = getTransformSource(synCtx);
        OutputStream osForTarget = null;
        InputStream  isForSource = null;
        ByteArrayOutputStream baosForTarget = new FixedByteArrayOutputStream(BYTE_ARRAY_SIZE);
        File tempTargetFile = null;
        File tempSourceFile = null;

        if (traceOrDebugOn) {
            trace.trace("Transformation source : " + sourceNode.toString());
        }

        Source transformSrc = null;
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

            try {
                // create a byte array output stream and serialize the source node into it
                ByteArrayOutputStream baosForSource = new FixedByteArrayOutputStream(BYTE_ARRAY_SIZE);
                XMLStreamWriter xsWriterForSource = XMLOutputFactory.newInstance().
                    createXMLStreamWriter(baosForSource);

                sourceNode.serialize(xsWriterForSource);
                isForSource = new ByteArrayInputStream(baosForSource.toByteArray());
                transformSrc = new StreamSource(isForSource);
                transformTgt = new StreamResult(baosForTarget);

            } catch (XMLStreamException e) {
                handleException("Error creating a StreamResult for the transformation", e, synCtx);

            } catch (SynapseException x) {

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Error creating a StreamResult using a byte array" +
                        " - attempting using temporary files for serialization");
                }

                OutputStream osForSource = null;

                try {
                    // create a output stream and serialize the source node into it
                    tempSourceFile = File.createTempFile("xs_", ".xml");
                    tempTargetFile = File.createTempFile("xt_", ".xml");

                    osForSource = new FileOutputStream(tempSourceFile);
                    osForTarget = new FileOutputStream(tempTargetFile);

                    XMLStreamWriter xsWriterForSource =
                        XMLOutputFactory.newInstance().createXMLStreamWriter(osForSource);

                    sourceNode.serialize(xsWriterForSource);
                    transformSrc = new StreamSource(tempSourceFile);
                    transformTgt = new StreamResult(osForTarget);

                } catch (XMLStreamException e) {
                    handleException("Error creating a StreamResult for the transformation", e, synCtx);
                } catch (IOException e) {
                    handleException("Error using a temporary file/s for the transformation", e, synCtx);
                } finally {
                    try {
                        osForSource.close();
                    } catch (IOException ignore) {}
                }
            }
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

                } catch (TransformerConfigurationException e) {
                    handleException("Error creating XSLT transformer using : " + xsltKey, e, synCtx);
                }
            }
        }

        try {
            // perform transformation
            Transformer transformer = cachedTemplates.newTransformer();
            if (!properties.isEmpty()) {
                // set the parameters which will pass to the Transformation
                for (int i = 0; i < properties.size(); i++) {
                    MediatorProperty prop = (MediatorProperty) properties.get(i);
                    if (prop != null) {
                        if (prop.getValue() != null) {
                            transformer.setParameter(prop.getName(), prop.getValue());
                        } else {
                            transformer.setParameter(prop.getName(),
                                Axis2MessageContext.getStringValue(prop.getExpression(), synCtx));
                        }
                    }
                }
            }

            try {
                transformer.transform(transformSrc, transformTgt);

            } catch (TransformerException x) {
                // did we exceed the in-memory BYTE_ARRAY_SIZE? if so, use a file for output
                try {
                    tempTargetFile = File.createTempFile("xt_", ".xml");
                    osForTarget  = new FileOutputStream(tempTargetFile);
                    transformTgt = new StreamResult(osForTarget);

                    // retry transformation again
                    isForSource.reset();
                    transformer.reset();
                    transformer.transform(transformSrc, transformTgt);

                } catch (IOException e) {
                    handleException("Error using a temporary file/s for the transformation", e, synCtx);
                }
            }

            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Transformation completed - processing result");
            }

            if (tempSourceFile != null) {
                boolean deleted = tempSourceFile.delete();
                if (!deleted) {
                    tempSourceFile.deleteOnExit();
                }
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

                // if we used a temporary file for the output of the transformation, read from it
                if (tempTargetFile != null) {
                    try {
                        StAXOMBuilder builder = new StAXOMBuilder(new FileInputStream(tempTargetFile));
                        result = builder.getDocumentElement();

                    } catch (XMLStreamException e) {
                        handleException(
                            "Error building result element from XSLT transformation", e, synCtx);

                    } catch (Exception e) {
                        result = handleNonXMLResult(tempTargetFile, traceOrDebugOn, traceOn);

                    } finally {
                        boolean deleted = tempTargetFile.delete();
                        if (!deleted) {
                            tempTargetFile.deleteOnExit();
                        }
                    }

                } else {
                    // read the Fixed byte array stream
                    try {
                        StAXOMBuilder builder = new StAXOMBuilder(
                            new ByteArrayInputStream(baosForTarget.toByteArray()));
                        result = builder.getDocumentElement();

                    } catch (XMLStreamException e) {
                        handleException(
                            "Error building result element from XSLT transformation", e, synCtx);

                    } catch (Exception e) {
                        result = handleNonXMLResult(baosForTarget.toString(), traceOrDebugOn, traceOn);
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

            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Replace source node with result");
            }

            // replace the sourceNode with the result.
            sourceNode.insertSiblingAfter(result);
            sourceNode.detach();

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
            Object o = source.evaluate(synCtx.getEnvelope());
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

    public AXIOMXPath getSource() {
        return source;
    }

    public void setSource(AXIOMXPath source) {
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
     * @param file the text payload file
     * @param traceOrDebugOn is tracing on debug logging on?
     * @param traceOn is tracing on?
     * @return an OMElement wrapping the text payload
     */
    private OMElement handleNonXMLResult(File file, boolean traceOrDebugOn, boolean traceOn) {

        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement wrapper = null;

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Processing non SOAP/XML (text) transformation result");
        }
        if (traceOn && trace.isTraceEnabled()) {
            trace.trace("Wrapping text transformation result from : " + file);
        }

        if (file != null) {
            TextFileDataSource txtFileDS = new TextFileDataSource(new FileDataSource(file));
            wrapper = new OMSourcedElementImpl(BaseConstants.DEFAULT_TEXT_WRAPPER, fac, txtFileDS);
        }

        return wrapper;
    }

    /**
     * If the transformation results in a non-XML payload, use standard wrapper elements
     * to wrap the text payload so that other mediators could still process the result
     * @param textPayload the text payload to wrap
     * @param traceOrDebugOn is tracing on debug logging on?
     * @param traceOn is tracing on?
     * @return an OMElement wrapping the text payload
     */
    private OMElement handleNonXMLResult(String textPayload, boolean traceOrDebugOn, boolean traceOn) {

        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement wrapper = null;

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Processing non SOAP/XML (text) transformation result");
        }
        if (traceOn && trace.isTraceEnabled()) {
            trace.trace("Wrapping text transformation result : " + textPayload);
        }

        if (textPayload != null) {
            OMTextImpl textData = (OMTextImpl) fac.createOMText(textPayload);
            wrapper = fac.createOMElement(BaseConstants.DEFAULT_TEXT_WRAPPER, null);
            wrapper.addChild(textData);
        }

        return wrapper;
    }

    /**
     *
     * @return Returns the features explicitly  set to the TransformerFactory through this mediator
     */
    public List getFeatures(){
        return explicitFeatures;
    }

    public void addAllProperties(List list) {
        properties.addAll(list);
    }

    public List getProperties() {
        return properties;
    }

    public void setSourceXPathString(String sourceXPathString) {
        this.sourceXPathString = sourceXPathString;
    }
}

	
