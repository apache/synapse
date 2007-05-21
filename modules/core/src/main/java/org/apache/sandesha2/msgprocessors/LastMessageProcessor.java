package org.apache.sandesha2.msgprocessors;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;

public class LastMessageProcessor  {

	public static void processLastMessage(RMMsgContext rmMsgCtx) throws AxisFault {
		
		if (!Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(rmMsgCtx.getRMSpecVersion()))
			return;
		
		MessageContext msgContext = rmMsgCtx.getMessageContext();
		MessageContext outMessage = MessageContextBuilder.createOutMessageContext(msgContext);
		
		//add the SOAP envelope with body null
		SOAPFactory factory = (SOAPFactory) msgContext.getEnvelope().getOMFactory();
		SOAPEnvelope envelope = factory.getDefaultEnvelope();
		outMessage.setEnvelope(envelope);
		
		//set the LastMessageAction and the property
		if (outMessage.getOptions()==null)
			outMessage.setOptions(new Options ());
		
		outMessage.getOptions().setAction(Sandesha2Constants.SPEC_2005_02.Actions.ACTION_LAST_MESSAGE);
		
		AxisEngine engine = new AxisEngine (rmMsgCtx.getConfigurationContext());
		engine.send(outMessage);
		
	}

}
