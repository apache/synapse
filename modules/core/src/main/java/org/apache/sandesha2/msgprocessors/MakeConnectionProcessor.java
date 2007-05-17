package org.apache.sandesha2.msgprocessors;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.workers.SenderWorker;
import org.apache.sandesha2.wsrm.Address;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.MakeConnection;
import org.apache.sandesha2.wsrm.MessagePending;

/**
 * This class is responsible for processing MakeConnection request messages that come to the system.
 * MakeConnection is only supported by WSRM 1.1
 * Here a client can ask for reply messages using a polling mechanism, so even clients without real
 * endpoints can ask for reliable response messages.
 */
public class MakeConnectionProcessor implements MsgProcessor {

	private static final Log log = LogFactory.getLog(MakeConnectionProcessor.class);

	/**
	 * Prosesses incoming MakeConnection request messages.
	 * A message is selected by the set of SenderBeans that are waiting to be sent.
	 * This is processed using a SenderWorker. 
	 */
	public boolean processInMessage(RMMsgContext rmMsgCtx) throws AxisFault {
		if(log.isDebugEnabled()) log.debug("Enter: MakeConnectionProcessor::processInMessage " + rmMsgCtx.getSOAPEnvelope().getBody());

		MakeConnection makeConnection = (MakeConnection) rmMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.MAKE_CONNECTION);
		Address address = makeConnection.getAddress();
		Identifier identifier = makeConnection.getIdentifier();
		
		ConfigurationContext configurationContext = rmMsgCtx.getConfigurationContext();
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
		
		SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();
		
		//selecting the set of SenderBeans that suit the given criteria.
		SenderBean findSenderBean = new SenderBean ();
		findSenderBean.setSend(true);
		findSenderBean.setTransportAvailable(false);
		
		if (address!=null)
			findSenderBean.setToAddress(address.getAddress());
		
		if (identifier!=null)
			findSenderBean.setSequenceID(identifier.getIdentifier());
		
		//finding the beans that go with the criteria of the passed SenderBean
		//The reSend flag is ignored for this selection, so there is no need to
		//set it.
		Collection collection = senderBeanMgr.find(findSenderBean);
		
		//removing beans that does not pass the resend test
		for (Iterator it=collection.iterator();it.hasNext();) {
			SenderBean bean = (SenderBean) it.next();
			if (!bean.isReSend() && bean.getSentCount()>0)
				it.remove();
		}
		
		//selecting a bean to send RANDOMLY. TODO- Should use a better mechanism.
		int size = collection.size();
		int itemToPick=-1;
		
		boolean pending = false;
		if (size>0) {
			Random random = new Random ();
			itemToPick = random.nextInt(size);
		}

		if (size>1)
			pending = true;  //there are more than one message to be delivered using the makeConnection.
							 //So the MessagePending header should have value true;
		
		Iterator it = collection.iterator();
		
		SenderBean senderBean = null;
		for (int item=0;item<size;item++) {
		    senderBean = (SenderBean) it.next();
			if (item==itemToPick)
				break;
		}

		if (senderBean==null) {
			if(log.isDebugEnabled()) log.debug("Exit: MakeConnectionProcessor::processInMessage, no matching message found");
			return false;
		}
		
		replyToPoll(rmMsgCtx, senderBean, storageManager, pending, makeConnection.getNamespaceValue());
		
		if(log.isDebugEnabled()) log.debug("Exit: MakeConnectionProcessor::processInMessage");
		return false;
	}
	
	public static void replyToPoll(RMMsgContext pollMessage,
			SenderBean matchingMessage,
			StorageManager storageManager,
			boolean pending,
			String namespace)
	throws AxisFault
	{
		if(log.isDebugEnabled()) log.debug("Enter: MakeConnectionProcessor::replyToPoll");
		TransportOutDescription transportOut = pollMessage.getMessageContext().getTransportOut();
		if (transportOut==null) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cantSendMakeConnectionNoTransportOut);
			if(log.isDebugEnabled()) log.debug(message);
			throw new SandeshaException (message);
		}
			
		String messageStorageKey = matchingMessage.getMessageContextRefKey();
		MessageContext returnMessage = storageManager.retrieveMessageContext(messageStorageKey,pollMessage.getConfigurationContext());
		if (returnMessage==null) {
			String message = "Cannot find the message stored with the key:" + messageStorageKey;
			if(log.isDebugEnabled()) log.debug(message);
			// Someone else has either removed the sender & message, or another make connection got here first.
			return;
		}
		
		if(pending) addMessagePendingHeader(returnMessage, namespace);
		
		RMMsgContext returnRMMsg = MsgInitializer.initializeMessage(returnMessage);
		if(returnRMMsg.getRMNamespaceValue()==null){
			//this is the case when a stored application response msg was not sucecsfully returned 
			//on the sending transport's backchannel. Since the msg was stored without a sequence header
			//we need to lookup the namespace using the RMS bean
			if(log.isDebugEnabled()) log.debug("Looking up rmNamespace from RMS bean");
			String sequenceID = matchingMessage.getSequenceID();
			if(sequenceID!=null){
				RMSBean rmsBean = new RMSBean();
				rmsBean.setSequenceID(sequenceID);
				rmsBean = storageManager.getRMSBeanMgr().findUnique(rmsBean);
				if(rmsBean!=null){
					returnRMMsg.setRMNamespaceValue(SpecSpecificConstants.getRMNamespaceValue(rmsBean.getRMVersion()));
				}
			}
		}
		setTransportProperties (returnMessage, pollMessage);
		
		// Link the response to the request
		OperationContext context = pollMessage.getMessageContext().getOperationContext();
		if(context == null) {
			AxisOperation oldOperation = returnMessage.getAxisOperation();

			context = ContextFactory.createOperationContext(oldOperation, returnMessage.getServiceContext()); //new OperationContext(oldOperation);

			context.addMessageContext(pollMessage.getMessageContext());
			pollMessage.getMessageContext().setOperationContext(context);
		}
		context.addMessageContext(returnMessage);
		returnMessage.setOperationContext(context);
		
		returnMessage.setProperty(Sandesha2Constants.MAKE_CONNECTION_RESPONSE, Boolean.TRUE);
		
		//running the MakeConnection through a SenderWorker.
		//This will allow Sandesha2 to consider both of following senarios equally.
		//  1. A message being sent by the Sender thread.
		//  2. A message being sent as a reply to an MakeConnection.
		SenderWorker worker = new SenderWorker (pollMessage.getConfigurationContext(), matchingMessage, returnRMMsg.getRMSpecVersion());
		worker.setMessage(returnRMMsg);
		worker.run();
		
		if(log.isDebugEnabled()) log.debug("Exit: MakeConnectionProcessor::replyToPoll");
	}
	
	private static void addMessagePendingHeader (MessageContext returnMessage, String namespace) throws SandeshaException {
		MessagePending messagePending = new MessagePending(namespace);
		messagePending.setPending(true);
		messagePending.toSOAPEnvelope(returnMessage.getEnvelope());
	}

	public boolean processOutMessage(RMMsgContext rmMsgCtx) {
		return false;
	}

	private static void setTransportProperties (MessageContext returnMessage, RMMsgContext makeConnectionMessage) {
        returnMessage.setProperty(MessageContext.TRANSPORT_OUT,makeConnectionMessage.getProperty(MessageContext.TRANSPORT_OUT));
        returnMessage.setProperty(Constants.OUT_TRANSPORT_INFO,makeConnectionMessage.getProperty(Constants.OUT_TRANSPORT_INFO));
        
        Object contentType = makeConnectionMessage.getProperty(Constants.Configuration.CONTENT_TYPE);
        returnMessage.setProperty(Constants.Configuration.CONTENT_TYPE, contentType);

        returnMessage.setTransportOut(makeConnectionMessage.getMessageContext().getTransportOut());
	}
}
