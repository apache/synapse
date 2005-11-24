package org.apache.synapse.processors.mediators;

import javax.xml.namespace.QName;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.api.SynapseEnvironment;

import org.apache.synapse.processors.AbstractProcessor;

/**
 * @author Paul Fremantle
 *         <p>
 *         <xmp><synapse:header type="to|from|faultto|replyto|action"
 *         value="newvalue"/> </xmp>
 * 
 * 
 */
public class HeaderProcessor extends AbstractProcessor {
	private static final QName HEADER_Q = new QName(
			Constants.SYNAPSE_NAMESPACE, "header");

	private Log log = LogFactory.getLog(getClass());

	private int headerType = 0;

	private String value = null;

	private static final QName TYPE_ATT_Q = new QName("type"),
			VALUE_ATT_Q = new QName("value");

	private final static int TO = 1, FROM = 2, FAULT = 3, ACTION = 4,
			REPLYTO = 5;

	private final static String STRTO = "to", STRFROM = "from",
			STRFAULT = "faultto", STRACTION = "action", STRREPLYTO = "replyto";

	public void compile(SynapseEnvironment se, OMElement el) {
		super.compile(se, el);

		OMAttribute val = el.getAttribute(VALUE_ATT_Q);
		OMAttribute type = el.getAttribute(TYPE_ATT_Q);
		if (val == null || type == null) {
			throw new SynapseException("<header> must have both " + VALUE_ATT_Q
					+ " and " + TYPE_ATT_Q + " attributes: " + el.toString());
		}

		String header = type.getAttributeValue();
		if (header.equalsIgnoreCase(STRTO))
			headerType = TO;
		else if (header.equalsIgnoreCase(STRFROM))
			headerType = FROM;
		else if (header.equalsIgnoreCase(STRFAULT))
			headerType = FAULT;
		else if (header.equalsIgnoreCase(STRACTION))
			headerType = ACTION;
		else if (header.equalsIgnoreCase(STRREPLYTO))
			headerType = REPLYTO;
		else
			throw new SynapseException(
					"unknown header attribute value in <header>: " + header);
		value = val.getAttributeValue();
	}

	public boolean process(SynapseEnvironment se, SOAPMessageContext smc) {

		switch (headerType) {
		case 0: {
			log.debug("unknown header type");
			return true;
		}

		case TO: {
			log.debug("set to: " + value);
			smc.setTo(new EndpointReference(value));
			break;
		}
		case FROM: {
			log.debug("set from: " + value);
			smc.setFrom(new EndpointReference(value));
			break;
		}
		case REPLYTO: {
			log.debug("set replyto: " + value);
			smc.setReplyTo(new EndpointReference(value));
			break;
		}
		case ACTION: {
			log.debug("set action: " + value);
			smc.setWSAAction(value);
			break;
		}

		}

		return true;
	}

	public QName getTagQName() {
		return HEADER_Q;
	}

}
