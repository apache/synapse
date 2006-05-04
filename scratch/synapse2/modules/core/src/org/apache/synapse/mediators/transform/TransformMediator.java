package org.apache.synapse.mediators.transform;


import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.mediators.AbstractMediator;
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

/**
 * The transform mediator performs an XSLT or XQuery transformation requested, using
 * the current message. The source attribute (if available) spcifies the source element
 * on which the transformation would be applied. Additional properties passed into this
 * mediator would become parameters (for XSLT) or variables (XQuery).
 */
public class TransformMediator extends AbstractMediator {

    private URL xsltUrl = null;
    private URL xqUrl = null;
    private String source = null;

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

        } else if (xqUrl != null) {
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

            AXIOMXPath xp = new AXIOMXPath(source);
            Object result = xp.evaluate(synMsg.getEnvelope());
            OMNode sourceNode = null;

            if (result instanceof OMNode) {
                sourceNode = (OMNode) result;
            } else {
                String msg = "XPath evaluation of the source element did not return an OMNode";
                log.error(msg);
                throw new SynapseException(msg);
            }
            sourceNode.serialize(xsWriterForSource);
            Source transformSrc = new StreamSource(new ByteArrayInputStream(baosForSource.toByteArray()));

            // create a new Stream result over a new BAOS..
            ByteArrayOutputStream baosForTarget = new ByteArrayOutputStream();
            StreamResult transformTgt = new StreamResult(baosForTarget);

            // perform transformation
            transformer.transform(transformSrc, transformTgt);

            StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(baosForTarget.toByteArray()));
            sourceNode.getParent().addChild(builder.getDocumentElement());

        } catch (MalformedURLException mue) {
            handleException(mue);
        } catch (TransformerConfigurationException tce) {
            handleException(tce);
        } catch (XMLStreamException xse) {
            handleException(xse);
        } catch (JaxenException je) {
            handleException(je);
        } catch (TransformerException te) {
            handleException(te);
        } catch (IOException ioe) {
            handleException(ioe);
        }
    }

    private void handleException(Exception e) {
        String msg = "Error performing XSLT/XQ transformation " + e.getMessage();
        log.error(msg);
        throw new SynapseException(msg, e);
    }

}

	
