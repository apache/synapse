package org.apache.synapse.mediators.transform;


import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.mediators.AbstractMediator;
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

    /**
     * Transforms this message (or its element specified as the source) using the
     * given XSLT or XQuery transformation
     * @param synMsg the current message where the transformation will apply
     * @return true always
     */
    public boolean mediate(SynapseMessage synMsg) {
        log.debug(getType() + " mediate()");

        if (xsltUrl != null) {
            performXLST(synMsg);
            return true;

        } else if (xQueryUrl != null) {
            //TODO later revisit later
            System.err.println("Unimplemented functionality..");
            return true;

        } else {
            log.error("Invalid configuration - xslt/xq not specified");
            return false;
        }
    }

    private void performXLST(SynapseMessage synMsg) {
        try {
            // create a transformer
            Transformer transformer = TransformerFactory.newInstance().newTransformer(
                new StreamSource(xsltUrl.openStream()));

            // create a byte array output stream and serialize the source node into it
            ByteArrayOutputStream baosForSource = new ByteArrayOutputStream();
            XMLStreamWriter xsWriterForSource = XMLOutputFactory.newInstance().createXMLStreamWriter(baosForSource);

            OMNode sourceNode = getTransformSource(synMsg);
            sourceNode.serialize(xsWriterForSource);
            Source transformSrc = new StreamSource(new ByteArrayInputStream(baosForSource.toByteArray()));

            // create a new Stream result over a new BAOS..
            ByteArrayOutputStream baosForTarget = new ByteArrayOutputStream();
            StreamResult transformTgt = new StreamResult(baosForTarget);

            // perform transformation
            transformer.transform(transformSrc, transformTgt);

            StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(baosForTarget.toByteArray()));
            OMContainer parent = sourceNode.getParent();

            if (parent instanceof SOAPEnvelope) {
                ((SOAPEnvelope) parent).getBody().getFirstOMChild().detach();
                ((SOAPEnvelope) parent).getBody().setFirstChild(builder.getDocumentElement());
            } else {
                parent.getFirstOMChild().detach();
                parent.addChild(builder.getDocumentElement());
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

    private OMNode getTransformSource(SynapseMessage synMsg) {

        if (source == null) {
            try {
                source = new AXIOMXPath("//SOAP-ENV:Body");
                source.addNamespace("SOAP-ENV", synMsg.isSOAP11() ?
                    SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI : SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
            } catch (JaxenException e) {}
        }

        try {
            Object o = source.evaluate(synMsg.getEnvelope());;
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
        log.error(msg);
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

}

	
