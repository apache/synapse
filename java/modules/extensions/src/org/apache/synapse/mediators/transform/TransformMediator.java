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
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

/**
 * The transform mediator performs an XSLT or XQuery transformation requested, using
 * the current message. The source attribute (if available) spcifies the source element
 * on which the transformation would be applied. Additional properties passed into this
 * mediator would become parameters (for XSLT) or variables (XQuery).
 */
public class TransformMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(TransformMediator.class);

    private URL xsltUrl = null;
    private URL xQueryUrl = null;
    private AXIOMXPath source = null;
    private List properties = new ArrayList();

    /**
     * Transforms this message (or its element specified as the source) using the
     * given XSLT or XQuery transformation
     * @param synCtx the current message where the transformation will apply
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {
        log.debug("Transform mediator mediate()");

        if (xsltUrl != null) {
            log.debug("Performing XSLT transformation against : " + xsltUrl);
            performXLST(synCtx);
            return true;

        } else if (xQueryUrl != null) {
            //TODO later revisit later
            log.error("Unimplemented functionality : XQuery transformation");
            throw new UnsupportedOperationException("XQuery transformation is not yet supported");

        } else {
            log.error("Invalid configuration - xslt/xq not specified");
            return false;
        }
    }

    private void performXLST(MessageContext synCtx) {
        try {
            // create a transformer
            Transformer transformer = TransformerFactory.newInstance().newTransformer(
                new StreamSource(xsltUrl.openStream()));

            // create a byte array output stream and serialize the source node into it
            ByteArrayOutputStream baosForSource = new ByteArrayOutputStream();
            XMLStreamWriter xsWriterForSource = XMLOutputFactory.newInstance().createXMLStreamWriter(baosForSource);

            OMNode sourceNode = getTransformSource(synCtx);
            log.debug("Transformation source : " + sourceNode);
            sourceNode.serialize(xsWriterForSource);
            Source transformSrc = new StreamSource(new ByteArrayInputStream(baosForSource.toByteArray()));

            // create a new Stream result over a new BAOS..
            ByteArrayOutputStream baosForTarget = new ByteArrayOutputStream();
            StreamResult transformTgt = new StreamResult(baosForTarget);

            // perform transformation
            transformer.transform(transformSrc, transformTgt);

            StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(baosForTarget.toByteArray()));
            OMContainer parent = sourceNode.getParent();
            OMElement result = builder.getDocumentElement();
            log.debug("Transformation result : " + result);

            if (parent instanceof SOAPEnvelope) {
                ((SOAPEnvelope) parent).getBody().getFirstOMChild().detach();
                ((SOAPEnvelope) parent).getBody().setFirstChild(result);
            } else {
                parent.getFirstOMChild().detach();
                parent.addChild(result);
            }

        } catch (MalformedURLException mue) {
            handleException(mue);
        } catch (TransformerConfigurationException tce) {
            handleException(tce);
        } catch (XMLStreamException xse) {
            handleException(xse);
        } catch (TransformerException te) {
            handleException(te);
        } catch (IOException ioe) {
            handleException(ioe);
        }
    }

    private OMNode getTransformSource(MessageContext synCtx) {

        // Do not change source, if none was specified. else will cause issues
        // for concurrent messages and reuse of the mediator instance
        AXIOMXPath sourceXPath = source;

        if (sourceXPath == null) {
            try {
                log.debug("Transform source XPath was not specified.. defaulting to SOAP Body");
                sourceXPath = new AXIOMXPath("//SOAP-ENV:Body");
                sourceXPath.addNamespace("SOAP-ENV", synCtx.isSOAP11() ?
                    SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI : SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
            } catch (JaxenException e) {}
        }

        try {
            log.debug("Transformation against source element evaluated by : " + sourceXPath);
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

    private void handleException(Exception e) {
        String msg = "Error performing XSLT/XQ transformation " + e.getMessage();
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    public AXIOMXPath getSource() {
        return source;
    }

    public void setSource(AXIOMXPath source) {
        this.source = source;
    }

    public URL getXsltUrl() {
        return xsltUrl;
    }

    public void setXsltUrl(URL xsltUrl) {
        this.xsltUrl = xsltUrl;
    }

    public URL getXQueryUrl() {
        return xQueryUrl;
    }

    public void setXQueryUrl(URL xQueryUrl) {
        this.xQueryUrl = xQueryUrl;
    }

    public void addProperty(MediatorProperty p) {
        properties.add(p);
    }

    public void addAllProperties(List list) {
        properties.addAll(list);
    }

}

	
