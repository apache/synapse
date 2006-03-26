package org.apache.synapse.processors.builtin.xslt;

import java.io.InputStream;

import javax.xml.namespace.QName;

import org.apache.synapse.xml.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;



import org.apache.synapse.xml.AbstractProcessorConfigurator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;


/**
 *
 * @see org.apache.synapse.processors.builtin.xslt.XSLTProcessor
 * <p> This class configures the XSLT transformer 
 * <p> The tag looks like this
 * <xmp><xslt name="x" xsl="file.xsl" type="body|envelope"/></xmp>
 *  Perform the given XSLT on the SOAP-Envelope or Body 
 *  <p>If type is not present, assumed to be body
 */
public class XSLTProcessorConfigurator extends AbstractProcessorConfigurator {
    private static final QName tagName = new QName(Constants.SYNAPSE_NAMESPACE, "xslt");

    public Processor createProcessor(SynapseEnvironment se, OMElement el) {
        XSLTProcessor xp = new XSLTProcessor();
        super.setNameOnProcessor(se,el,xp);

        OMAttribute type = el.getAttribute(new QName("type"));
        if (type != null && type.getAttributeValue().trim().toLowerCase().equals("envelope")) xp.setIsBody(false);
        else xp.setIsBody(true);

        OMAttribute xsl = el.getAttribute(new QName("xsl"));
        if (xsl == null) throw new SynapseException("no xsl attribute on: "+el.toString());

        InputStream xslStream =  se.getClassLoader().getResourceAsStream(xsl.getAttributeValue());
        xp.setXSLInputStream(xslStream);

        return xp;



    }

    public QName getTagQName() {

        return tagName;
    }

}
