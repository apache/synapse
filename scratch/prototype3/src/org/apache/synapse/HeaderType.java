package org.apache.synapse;

import org.apache.axis2.addressing.EndpointReference;

public class HeaderType {

	private final static int TO = 1, FROM = 2, FAULT = 3, ACTION = 4,
			REPLYTO = 5;

	public final static String STRTO = "to", STRFROM = "from",
			STRFAULT = "faultto", STRACTION = "action", STRREPLYTO = "replyto";

	private int headerType = 0;

	public void setHeaderType(String header) {
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
			throw new SynapseException("unknown header type");
	}

	public String getHeaderType() {
		switch (headerType) {

		case TO:
			return STRTO;
		case FROM:
			return STRFROM;
		case FAULT:
			return STRFAULT;
		case ACTION:
			return STRACTION;
		case REPLYTO:
			return STRREPLYTO;

		}
		return null;
	}

	public String getHeader(SynapseMessage sm) {
		switch (headerType) {
		case TO: {
			if (sm.getTo() != null)
				return sm.getTo().getAddress();

		}
		case FROM: {
			if (sm.getFrom() != null)
				return sm.getFrom().getAddress();
			break;
		}
		case FAULT: {
			if (sm.getFaultTo() != null)
				return sm.getFaultTo().getAddress();
			break;
		}
		case ACTION: {
			if (sm.getWSAAction() != null)
				return sm.getWSAAction();
			break;
		}
		case REPLYTO: {
			if (sm.getReplyTo() != null)
				return sm.getReplyTo().getAddress();
			break;
		}
		}

		return null;
	}

	public void setHeader(SynapseMessage sm, String value) {
		switch (headerType) {
		case 0: {
			throw new SynapseException(
					"headerType=0 in setHeader. Assume called setHeader before setHeaderType");
		}

		case TO: {

			sm.setTo(new EndpointReference(value));
			break;
		}
		case FROM: {
			sm.setFrom(new EndpointReference(value));
			break;
		}
		case REPLYTO: {
			sm.setReplyTo(new EndpointReference(value));
			break;
		}
		case ACTION: {
			sm.setWSAAction(value);
			break;
		}

		}
	}
}
