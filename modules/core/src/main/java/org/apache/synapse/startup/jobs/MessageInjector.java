package org.apache.synapse.startup.jobs;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.startup.Job;
import org.apache.synapse.util.PayloadHelper;

public class MessageInjector implements Job, ManagedLifecycle {
	private Log log = LogFactory.getLog(MessageInjector.class);

	private OMElement message;

	String to;

	private SynapseEnvironment synapseEnvironment;

	public void init(SynapseEnvironment se) {
		synapseEnvironment = se;
	}

	public void setMessage(OMElement el) {
		log.debug("set message " + el.toString());
		message = el;
	}

	public void setTo(String url) {
		to = url;
	}

	public void execute() {
		log.debug("execute");
		if (synapseEnvironment == null) {
			log.error("Synapse Environment not set");
			return;
		}
		if (message == null) {
			log.error("message not set");
			return;

		}
		if (to == null) {
			log.error("to address not set");
			return;

		}
		MessageContext mc = synapseEnvironment.createMessageContext();
		mc.setTo(new EndpointReference(to));
		PayloadHelper.setXMLPayload(mc, message.cloneOMElement());
		synapseEnvironment.injectMessage(mc);

	}

	public void destroy() {
	}

}
