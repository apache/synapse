package org.apache.sandesha2.msgprocessors;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisOperationFactory;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.Sequence;
import org.ietf.jgss.MessageProp;

public class LastMessageProcessor  implements MsgProcessor {

	
	
	
	public boolean processInMessage(RMMsgContext rmMsgCtx, Transaction transaction) throws AxisFault {
		processLastMessage(rmMsgCtx);
		return true;
	}

	public boolean processOutMessage(RMMsgContext rmMsgCtx) throws AxisFault {
		// TODO Auto-generated method stub
		return false;
	}

	public static void processLastMessage(RMMsgContext rmMsgCtx) throws AxisFault {
		
		if (!Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(rmMsgCtx.getRMSpecVersion()))
			return;
		
		Sequence sequence = (Sequence) rmMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
		String sequenceId = sequence.getIdentifier().getIdentifier();
		
		ConfigurationContext configurationContext = rmMsgCtx.getConfigurationContext();
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(
							configurationContext, configurationContext.getAxisConfiguration());
		
		RMDBeanMgr rmdMgr = storageManager.getRMDBeanMgr();
		RMDBean rmdBean = rmdMgr.retrieve(sequenceId);
		String outBoundInternalSequence = rmdBean.getOutboundInternalSequence();
		
		RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();
		RMSBean findBean = new RMSBean ();
		findBean.setInternalSequenceID(outBoundInternalSequence);
		RMSBean rmsBean = rmsBeanMgr.findUnique (findBean);
		
		if (rmsBean!=null && rmsBean.getLastOutMessage()<=0) {
			//there is a RMS sequence without a LastMsg entry
			
			MessageContext msgContext = rmMsgCtx.getMessageContext();
//			MessageContext outMessage = MessageContextBuilder.createOutMessageContext(msgContext);
			
			MessageContext outMessageContext = new MessageContext ();
			outMessageContext.setServerSide(true);
			
			outMessageContext.setTransportOut(msgContext.getTransportOut());
			outMessageContext.setProperty (Constants.OUT_TRANSPORT_INFO, msgContext.getProperty(Constants.OUT_TRANSPORT_INFO));
			outMessageContext.setProperty (MessageContext.TRANSPORT_OUT, msgContext.getProperty(MessageContext.TRANSPORT_OUT));

			//add the SOAP envelope with body null
			SOAPFactory factory = (SOAPFactory) msgContext.getEnvelope().getOMFactory();
			SOAPEnvelope envelope = factory.getDefaultEnvelope();
			outMessageContext.setEnvelope(envelope);
			
			//set the LastMessageAction and the property
			if (outMessageContext.getOptions()==null)
				outMessageContext.setOptions(new Options ());
			
			outMessageContext.setConfigurationContext(msgContext.getConfigurationContext());
			outMessageContext.setServiceContext(msgContext.getServiceContext());
			outMessageContext.setAxisService(msgContext.getAxisService());
			
			AxisOperation operation = SpecSpecificConstants.getWSRMOperation(Sandesha2Constants.MessageTypes.LAST_MESSAGE, 
																	rmMsgCtx.getRMSpecVersion() , msgContext.getAxisService());
			
			OperationContext operationContext = new OperationContext (operation,msgContext.getServiceContext());
			operationContext.addMessageContext(outMessageContext);
			
			String inboundSequenceId = (String) msgContext.getProperty(Sandesha2Constants.MessageContextProperties.INBOUND_SEQUENCE_ID);
			operationContext.setProperty(Sandesha2Constants.MessageContextProperties.INBOUND_SEQUENCE_ID, 
					inboundSequenceId);
			
			Long inboundMSgNo = (Long) msgContext.getProperty(Sandesha2Constants.MessageContextProperties.INBOUND_MESSAGE_NUMBER);
			operationContext.setProperty(Sandesha2Constants.MessageContextProperties.INBOUND_MESSAGE_NUMBER, 
					inboundMSgNo);
			
			outMessageContext.setAxisOperation(operation);
			outMessageContext.setOperationContext(operationContext);
			
			outMessageContext.getOptions().setAction(Sandesha2Constants.SPEC_2005_02.Actions.ACTION_LAST_MESSAGE);

			//says that the inbound msg of this was a LastMessage - so the new msg will also be a LastMessage
			outMessageContext.setProperty(Sandesha2Constants.MessageContextProperties.INBOUND_LAST_MESSAGE, Boolean.TRUE);
			
			AxisEngine engine = new AxisEngine (rmMsgCtx.getConfigurationContext());
			engine.send(outMessageContext);
			
		}
		
		
		
	}

}
