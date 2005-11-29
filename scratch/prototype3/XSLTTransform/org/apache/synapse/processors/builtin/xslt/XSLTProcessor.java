package org.apache.synapse.processors.builtin.xslt;



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

import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;

import org.apache.synapse.processors.AbstractProcessor;

/**
 * @author Paul Fremantle
 * @see org.apache.synapse.processors.builtin.xslt.XSLTProcessorConfigurator
 * <p> This class is the class that transforms messages using XSLT. 
 *   

 *
 */
public class XSLTProcessor extends AbstractProcessor {

	private Transformer tran = null;

	private boolean isBody = false;

	/* (non-Javadoc)
	 * @see org.apache.synapse.Processor#process(org.apache.synapse.SynapseEnvironment, org.apache.synapse.SynapseMessage)
	 */
	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
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

	
