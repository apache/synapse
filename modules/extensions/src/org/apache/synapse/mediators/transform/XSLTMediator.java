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
package org.apache.synapse.mediators.transform;

import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.DynamicProperty;
import org.apache.synapse.config.Util;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.ArrayList;

/**
 * The XSLT mediator performs an XSLT transformation requested, using
 * the current message. The source attribute (if available) spcifies the source element
 * on which the transformation would be applied. It will default to the first child of
 * the messages' SOAP body, if it is omitted. Additional properties passed into this
 * mediator would become parameters for XSLT
 */
public class XSLTMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(XSLTMediator.class);

    /** The property key/name which refers to the XSLT to be used for the transformation */
    private String xsltKey = null;

    /** The (optional) XPath expression which yeilds the source element for a transformation */
    private AXIOMXPath source = null;

    /** Any parameters which should be passed into the XSLT transformation */
    private List properties = new ArrayList();

    /**
     * The Transformer instance used to perform XSLT transformations. This is not thread-safe
     * @see javax.xml.transform.Transformer
     */
    private Transformer transformer = null;

    /** Lock used to ensure thread-safe creation and use of the above Transformer */
    private final Object transformerLock = new Object();

    private static final String DEFAULT_XPATH = "//s11:Envelope/s11:Body/child::*[position()=1] | " +
        "//s12:Envelope/s12:Body/child::*[position()=1]";

    public XSLTMediator() {
        // create the default XPath
        try {
            this.source = new AXIOMXPath(DEFAULT_XPATH);
            this.source.addNamespace("s11", SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
            this.source.addNamespace("s12", SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
        } catch (JaxenException e) {
            handleException("Error creating source XPath expression", e);
        }
    }

    /**
     * Transforms this message (or its element specified as the source) using the
     * given XSLT transformation
     * @param synCtx the current message where the transformation will apply
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {
        log.debug("XSLT mediator mediate()");

        log.debug("Performing XSLT transformation against property with key : " + xsltKey);
        performXLST(synCtx);
        return true;
    }

    private void performXLST(MessageContext msgCtx) {

        Source transformSrc = null;
        ByteArrayOutputStream baosForTarget = new ByteArrayOutputStream();

        // create a new Stream result over a new BAOS..
        StreamResult transformTgt = new StreamResult(baosForTarget);

        OMNode sourceNode = getTransformSource(msgCtx);
        if (log.isDebugEnabled()) {
            log.debug("Transformation source : " + sourceNode);
        }

        try {
            // create a byte array output stream and serialize the source node into it
            ByteArrayOutputStream baosForSource = new ByteArrayOutputStream();
            XMLStreamWriter xsWriterForSource = XMLOutputFactory.newInstance().
                createXMLStreamWriter(baosForSource);

            sourceNode.serialize(xsWriterForSource);
            transformSrc = new StreamSource(new ByteArrayInputStream(baosForSource.toByteArray()));

        } catch (XMLStreamException e) {
            handleException("Error gettting transform source " + e.getMessage(), e);
        }

        // build transformer - if necessary
        DynamicProperty dp = msgCtx.getConfiguration().getDynamicProperty(xsltKey);

        // if the xsltKey refers to a dynamic property
        if (dp != null) {
            if (!dp.isCached() || dp.isExpired()) {
                synchronized(transformerLock) {
                    try {
                        transformer = TransformerFactory.newInstance().
                            newTransformer(Util.getStreamSource(
                                msgCtx.getConfiguration().getProperty(xsltKey)
                            ));
                    } catch (TransformerConfigurationException e) {
                        handleException("Error creating XSLT transformer using : " + xsltKey, e);
                    }
                }
            }

        // if the property is not a DynamicProperty, we will create a transformer only once
        } else {
            if (transformer == null) {
                synchronized(transformerLock) {
                    try {
                        transformer = TransformerFactory.newInstance().
                            newTransformer(
                                Util.getStreamSource(
                                msgCtx.getConfiguration().getProperty(xsltKey)));
                    } catch (TransformerConfigurationException e) {
                        handleException("Error creating XSLT transformer using : " + xsltKey, e);
                    }
                }
            }
        }

        try {
            // perform transformation
            transformer.transform(transformSrc, transformTgt);

            StAXOMBuilder builder = new StAXOMBuilder(
                new ByteArrayInputStream(baosForTarget.toByteArray()));
            OMElement result = builder.getDocumentElement();

            if (log.isDebugEnabled()) {
                log.debug("Transformation result : " + result);
            }

            OMContainer parent = sourceNode.getParent();
            if (parent instanceof SOAPEnvelope) {
                ((SOAPEnvelope) parent).getBody().getFirstOMChild().detach();
                ((SOAPEnvelope) parent).getBody().setFirstChild(result);
            } else {
                parent.getFirstOMChild().detach();
                parent.addChild(result);
            }

        } catch (TransformerException e) {
            handleException("Error performing XSLT transformation " + xsltKey, e);
        } catch (XMLStreamException e) {
            handleException("Error building result from XSLT transformation", e);
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
                    + source + " must result in an OMNode");
            }
        } catch (JaxenException e) {
            handleException("Error evaluating XPath " + source + " on message");
        }
        return null;
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
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

    public void addAllProperties(List list) {
        properties.addAll(list);
    }

}

	
