package org.apache.synapse.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;

import org.apache.axis2.engine.MessageReceiver;
import org.apache.synapse.Constants;

public class EmptyMessageReceiver implements MessageReceiver {

	public void receive(MessageContext mc) throws AxisFault {
		mc.setProperty(Constants.MEDIATOR_RESPONSE_PROPERTY, Boolean
				.valueOf(true));
	}

}
