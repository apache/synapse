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
 * @see org.apache.synapse.mediators.base.builtin.xslt.XSLTProcessorConfigurator
 *      <p> This class is the class that transforms messages using XSLT.
 */
public class XSLTMediator extends AbstractMediator {

    private URL xsltUrl = null;
    private URL xqUrl = null;
    private String source = null;

    public boolean mediate(SynapseMessage synMsg) {
        log.debug(getType() + " mediate()");
        if (xsltUrl != null) {
            performXLST(synMsg);
            return true;
        } else if (xqUrl != null) {
            //TODO later
            return true;
        } else {
            log.error("Invalid configuration - xslt/xq not specified");
            return false;
        }
    }

    private void performXLST(SynapseMessage synMsg) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer(
                new StreamSource(xsltUrl.openStream()));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLStreamWriter xsWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(baos);

            AXIOMXPath xp = new AXIOMXPath(source);
            OMNode sourceNode = (OMNode) xp.evaluate(synMsg.getEnvelope());
            sourceNode.serialize(xsWriter);

            synMsg.getEnvelope().getBody().serialize(xsWriter);

            Source transformSrc = new StreamSource(new ByteArrayInputStream(baos.toByteArray()));
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            StreamResult transformTgt = new StreamResult(baos2);
            transformer.transform(transformSrc, transformTgt);

            StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(baos2.toByteArray()));
            sourceNode.getParent().addChild(builder.getDocumentElement());

        } catch (MalformedURLException mue) {
            throw new SynapseException(mue);
        } catch (TransformerConfigurationException tce) {
            throw new SynapseException(tce);
        } catch (XMLStreamException xse) {
            throw new SynapseException(xse);
        } catch (JaxenException je) {
            throw new SynapseException(je);
        } catch (TransformerException te) {
            throw new SynapseException(te);
        } catch (IOException ioe) {
            throw new SynapseException(ioe);
        }
    }

}

	
