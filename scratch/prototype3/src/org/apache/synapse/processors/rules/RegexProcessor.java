package org.apache.synapse.processors.rules;

import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.api.SynapseEnvironment;
import org.apache.synapse.processors.AllProcessor;

/**
 * @author Paul Fremantle
 * 
 */
public class RegexProcessor extends AllProcessor {
	private static final String REGEX = "regex";

	private static final QName REGEX_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			REGEX);

	private static final QName REGEX_PATTERN_ATT_Q = new QName("pattern");

	private static final QName REGEX_PROPERTY_ATT_Q = new QName("property");

	private static final QName REGEX_HEADER_ATT_Q = new QName("message-address");

	private Pattern pattern = null;

	private Log log = LogFactory.getLog(getClass());

	private int headerType = 0;

	private String property = null;

	private final static int TO = 1, FROM = 2, FAULT = 3, ACTION = 4;

	private final static String STRTO = "to", STRFROM = "from",
			STRFAULT = "faultto", STRACTION = "action";

	public void compile(SynapseEnvironment se, OMElement el) {
		super.compile(se, el);
		OMAttribute patt = el.getAttribute(REGEX_PATTERN_ATT_Q);
		if (patt == null) {
			throw new SynapseException(REGEX + " must have "
					+ REGEX_PATTERN_ATT_Q + " attribute: " + el.toString());
		}

		OMAttribute prop = el.getAttribute(REGEX_PROPERTY_ATT_Q);
		OMAttribute head = el.getAttribute(REGEX_HEADER_ATT_Q);
		if (prop == null && head == null) {
			throw new SynapseException(REGEX + " must have either "
					+ REGEX_PROPERTY_ATT_Q + " or " + REGEX_HEADER_ATT_Q
					+ " attributes: " + el.toString());
		}
		pattern = Pattern.compile(patt.getAttributeValue());
		if (prop != null) {
			property = prop.getAttributeValue();
		} else {
			String header = head.getAttributeValue();
			if (header.equalsIgnoreCase(STRTO))
				headerType = TO;
			else if (header.equalsIgnoreCase(STRFROM))
				headerType = FROM;
			else if (header.equalsIgnoreCase(STRFAULT))
				headerType = FAULT;
			else if (header.equalsIgnoreCase(STRACTION))
				headerType = ACTION;
			else
				throw new SynapseException(
						"unknown header attribute value in regex: " + header);

		}
	}

	public boolean process(SynapseEnvironment se, SOAPMessageContext smc) {

		if (pattern == null) {
			log.debug("trying to process with empty pattern");
			return true;
		}
		String toMatch = null;
		if (property != null) {
			toMatch = smc.getProperty(property).toString();
		} else {
			// must be header type if we got here

			switch (headerType) {
			case 0: {
				log.debug("trying to process with empty property and header");
				return true;
			}
			case TO: {
				if (smc.getTo() == null)
					return true;
				toMatch = smc.getTo().getAddress();
				break;
			}
			case FROM: {
				if (smc.getFrom() == null)
					return true;
				toMatch = smc.getFrom().getAddress();
				break;
			}
			case FAULT: {
				if (smc.getFaultTo() == null)
					return true;
				toMatch = smc.getFaultTo().getAddress();
				break;
			}
			case ACTION: {
				if (smc.getWSAAction() == null)
					return true;
				toMatch = smc.getWSAAction();
				break;
			}
			}
		}
		if (pattern.matcher(toMatch).matches()) {
			log.debug("Regex pattern " + pattern.toString() + " matched "
					+ toMatch);
			return super.process(se, smc);
		}
		return true;
	}

	public QName getTagQName() {
		return REGEX_Q;
	}

}
