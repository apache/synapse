package org.apache.synapse.processors.rules;

import org.apache.axis2.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.processors.ListProcessor;
import org.jaxen.JaxenException;

/**
 * @author Paul Fremantle
 * 
 * <p>
 * This class executes a test and then processes all subsequent rules/mediations
 * if the test is true
 * 
 */
public class XPathProcessor extends ListProcessor {

	private Log log = LogFactory.getLog(getClass());

	private AXIOMXPath xp = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.synapse.spi.Processor#process(org.apache.synapse.api.SynapseEnvironment,
	 *      org.apache.synapse.api.SOAPMessageContext)
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

	public void setXPathExpr(String expr) {
		try {
			xp = new AXIOMXPath(expr);
		} catch (JaxenException je) {
			throw new SynapseException(je);
		}
	}

	public String getXPathExpr() {
		return xp.toString();
	}

	public void addXPathNamespace(String prefix, String uri) {
		try {
			xp.addNamespace(prefix, uri);
		} catch (JaxenException je) {
			throw new SynapseException(je);
		}

	}

}
