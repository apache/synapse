package org.apache.synapse.processors.rules;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMNamespace;
import org.apache.axis2.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.processors.AllProcessor;
import org.jaxen.JaxenException;

/**
 * @author Paul Fremantle
 * 
 * <p>
 * This class executes a test and then processes all subsequent rules/mediations
 * if the test is true
 * 
 */
public class XPathProcessor extends AllProcessor {
	private static final String XPATH = "xpath";

	private static final QName XPATH_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"xpath");

	private static final QName XPATH_EXPRESSION_ATT_Q = new QName("expr");

	private Log log = LogFactory.getLog(getClass());

	private AXIOMXPath xp = null;

	/* (non-Javadoc)
	 * @see org.apache.synapse.spi.Processor#compile(org.apache.synapse.api.SynapseEnvironment, org.apache.axis2.om.OMElement)
	 */
	public void compile(SynapseEnvironment se, OMElement el) {
		super.compile(se, el);
		OMAttribute xpath = el.getAttribute(XPATH_EXPRESSION_ATT_Q);
		if (xpath == null) {
			throw new SynapseException(XPATH + " must have "
					+ XPATH_EXPRESSION_ATT_Q + " attribute: " + el.toString());
		}

		try {
			xp = new AXIOMXPath(xpath.getAttributeValue());
			Iterator it = el.getAllDeclaredNamespaces();
			while (it.hasNext()) {
				OMNamespace n = (OMNamespace) it.next();
				xp.addNamespace(n.getPrefix(), n.getName());
			}
		} catch (JaxenException e) {
			throw new SynapseException("Problem with xpath expression "
					+ xpath.getAttributeValue(), e);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.synapse.spi.Processor#process(org.apache.synapse.api.SynapseEnvironment, org.apache.synapse.api.SOAPMessageContext)
	 */
	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
		if (xp == null) {
			log.debug("trying to process xpath without being set");
			return true;
		}
		try {
			if (xp.booleanValueOf(smc.getEnvelope())) {
				log.debug("matched xpath: " + xp.toString());
				// now do "all"
				return super.process(se, smc);
			}

		} catch (JaxenException je) {
			throw new SynapseException("Problem evaluating " + xp.toString(),
					je);
		}
		return true;
	}

	public QName getTagQName() {

		return XPATH_Q;
	}

}
