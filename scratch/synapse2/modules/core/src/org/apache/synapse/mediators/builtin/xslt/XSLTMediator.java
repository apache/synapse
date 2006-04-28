package org.apache.synapse.mediators.builtin.xslt;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;


import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;

import org.apache.synapse.api.Mediator;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAbstractFactory;

/**
 *
 * @see org.apache.synapse.mediators.base.builtin.xslt.XSLTProcessorConfigurator
 * <p> This class is the class that transforms messages using XSLT. 
 *   

 *
 */
public class XSLTMediator implements Mediator {

    private Transformer tran = null;

    private boolean isBody = false;

    public boolean mediate(SynapseMessage smc) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter xsw;
        try {
            xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(baos);

        if (isBody) smc.getEnvelope().getBody().serialize(xsw);
        else smc.getEnvelope().serialize(xsw);

        Source src = new StreamSource(new ByteArrayInputStream(baos.toByteArray()));
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos2);
        tran.transform(src, result);
        StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(baos2.toByteArray()));
        OMElement nw = builder.getDocumentElement();
        if (isBody) smc.getEnvelope().setFirstChild(nw);
        //TODO don't assume SOAP 1.1
        else smc.setEnvelope(OMAbstractFactory.getSOAP11Factory().createSOAPEnvelope(builder));

        } catch (Exception e) {
            throw new SynapseException(e);
        }
        return true;
    }

    /**
     * @param b
     * <p> If isBody is true then the XSLT is applied to the Body of the SOAP message, otherwise to the whole env
     */
    public void setIsBody(boolean b) {
        isBody  = b;
    }


    /**
     * @param is
     * <p>
     * This sets the correct XSL transform
     */
    public void setXSLInputStream(InputStream is) {
        try {
            Source src = new StreamSource(is);
            tran = TransformerFactory.newInstance().newTransformer(src);
        } catch (Exception e) {
            throw new SynapseException(e);

        }
    }

}

	
