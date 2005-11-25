package org.apache.synapse.xml;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMNamespace;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.processors.rules.XPathProcessor;

/**
 * @author Paul Fremantle
 * 
 * <p>
 * This class executes a test and then processes all subsequent rules/mediations
 * if the test is true
 * 
 */
public class XPathProcessorConfigurator extends
		AbstractListProcessorConfigurator {
	private static final String XPATH = "xpath";

	private static final QName XPATH_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"xpath");

	private static final QName XPATH_EXPRESSION_ATT_Q = new QName("expr");

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.synapse.spi.Processor#compile(org.apache.synapse.api.SynapseEnvironment,
	 *      org.apache.axis2.om.OMElement)
	 */
	public Processor compile(SynapseEnvironment se, OMElement el) {
		XPathProcessor xp = new XPathProcessor();

		super.compile(se, el, xp);

		OMAttribute expr = el.getAttribute(XPATH_EXPRESSION_ATT_Q);
		if (expr == null) {
			throw new SynapseException(XPATH + " must have "
					+ XPATH_EXPRESSION_ATT_Q + " attribute: " + el.toString());
		}

		xp.setXPathExpr(expr.getAttributeValue());
		Iterator it = el.getAllDeclaredNamespaces();
		while (it.hasNext()) {
			OMNamespace n = (OMNamespace) it.next();
			xp.addXPathNamespace(n.getPrefix(), n.getName());
		}

		return xp;
	}

	public QName getTagQName() {

		return XPATH_Q;
	}

}
