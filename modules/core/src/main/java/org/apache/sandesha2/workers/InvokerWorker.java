package org.apache.sandesha2.workers;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beans.InvokerBean;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.wsrm.Sequence;

public class InvokerWorker extends SandeshaWorker implements Runnable {

	ConfigurationContext configurationContext = null;
	String messageContextKey;
	boolean ignoreNextMsg = false;
	
	Log log = LogFactory.getLog(InvokerWorker.class);
	
	public InvokerWorker (ConfigurationContext configurationContext, String messageContextKey, boolean ignoreNextMsg) {
		this.configurationContext = configurationContext;
		this.messageContextKey = messageContextKey;
		this.ignoreNextMsg = ignoreNextMsg;
	}
	
	public void run() {
		if(log.isDebugEnabled()) log.debug("Enter: InvokerWorker::run");
		
		Transaction transaction = null;
		MessageContext msgToInvoke = null;
		
		try {
			
			StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
			InvokerBeanMgr invokerBeanMgr = storageManager.getInvokerBeanMgr();
			
			//starting a transaction
			transaction = storageManager.getTransaction();
			
			InvokerBean invokerBean = invokerBeanMgr.retrieve(messageContextKey);
			
			msgToInvoke = storageManager.retrieveMessageContext(messageContextKey, configurationContext);
			RMMsgContext rmMsg = MsgInitializer.initializeMessage(msgToInvoke);

			// ending the transaction before invocation.
			if(transaction != null) {
				transaction.commit();
				transaction = null;
			}
					
			//starting a transaction for the invocation work.
			transaction = storageManager.getTransaction();
			// Lock the RMD Bean just to avoid deadlocks
			SandeshaUtil.getRMDBeanFromSequenceId(storageManager, invokerBean.getSequenceID());
			// Depending on the transaction  support, the service will be invoked only once. 
			// Therefore we delete the invoker bean and message now, ahead of time
			invokerBeanMgr.delete(messageContextKey);
			// removing the corresponding message context as well.
			storageManager.removeMessageContext(messageContextKey);

			try {

				boolean postFailureInvocation = false;

				// StorageManagers should st following property to
				// true, to indicate that the message received comes
				// after a failure.
				String postFaulureProperty = (String) msgToInvoke
						.getProperty(Sandesha2Constants.POST_FAILURE_MESSAGE);
				if (postFaulureProperty != null
						&& Sandesha2Constants.VALUE_TRUE.equals(postFaulureProperty))
					postFailureInvocation = true;

				AxisEngine engine = new AxisEngine(msgToInvoke.getConfigurationContext());
				if (postFailureInvocation) {
					makeMessageReadyForReinjection(msgToInvoke);
					if (log.isDebugEnabled())
						log.debug("Receiving message, key=" + messageContextKey + ", msgCtx="
								+ msgToInvoke.getEnvelope().getHeader());
					engine.receive(msgToInvoke);
				} else {
					if (log.isDebugEnabled())
						log.debug("Resuming message, key=" + messageContextKey + ", msgCtx="
								+ msgToInvoke.getEnvelope().getHeader());
					msgToInvoke.setPaused(false);
					engine.resumeReceive(msgToInvoke);
				}
				
				if(transaction!=null){
					transaction.commit();
					transaction = storageManager.getTransaction();
				}
			} catch (Exception e) {
				if (log.isDebugEnabled())
					log.debug("Exception :", e);

				handleFault(rmMsg, e);
			}


			
			if (rmMsg.getMessageType() == Sandesha2Constants.MessageTypes.APPLICATION) {
				Sequence sequence = (Sequence) rmMsg
						.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
				
				boolean highestMessage = false;
				if (sequence.getLastMessage() != null) {
					//this will work for RM 1.0 only
					highestMessage = true;
				} else {
					RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, invokerBean.getSequenceID());
					
					if (rmdBean!=null && rmdBean.isTerminated()) {
						long highestInMsgNo = rmdBean.getHighestInMessageNumber();
						if (invokerBean.getMsgNo()==highestInMsgNo)
							highestMessage = true;
					}
				}
				
				if (highestMessage) {
					//do cleaning stuff that hs to be done after the invocation of the last message.
					TerminateManager.cleanReceivingSideAfterInvocation(invokerBean.getSequenceID(), storageManager);
					// exit from current iteration. (since an entry
					// was removed)
					if(log.isDebugEnabled()) log.debug("Exit: InvokerWorker::run Last message return");					
					return;
				}
			}
			
			if(!ignoreNextMsg){
				// updating the next msg to invoke
				RMDBean rMDBean = storageManager.getRMDBeanMgr().retrieve(invokerBean.getSequenceID());
				long nextMsgNo = rMDBean.getNextMsgNoToProcess();
				
				if (!(invokerBean.getMsgNo()==nextMsgNo)) {
					String message = "Operated message number is different from the Next Message Number to invoke";
					throw new SandeshaException (message);
				}
				
				nextMsgNo++;
				rMDBean.setNextMsgNoToProcess(nextMsgNo);
				storageManager.getRMDBeanMgr().update(rMDBean);
			}
			
			if(transaction != null && transaction.isActive()) transaction.commit();
			transaction = null;
			
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.toString(), e);
		} finally {
			if (workId !=null && lock!=null) {
				lock.removeWork(workId);
			}

			if (transaction!=null && transaction.isActive()) {
				try {
					transaction.rollback();
				} catch (SandeshaStorageException e) {
					if (log.isWarnEnabled())
						log.warn("Caught exception rolling back transaction", e);
				}
			}
		}
		
		if(log.isDebugEnabled()) log.debug("Exit: InvokerWorker::run");
	}

	private void makeMessageReadyForReinjection(MessageContext messageContext) {
		messageContext.setProperty(AddressingConstants.WS_ADDRESSING_VERSION, null);
		messageContext.getOptions().setMessageId(null);
		messageContext.getOptions().setTo(null);
		messageContext.getOptions().setAction(null);
		messageContext.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, Sandesha2Constants.VALUE_TRUE);
	}

	private void handleFault(RMMsgContext inRMMsgContext, Exception e) {
		MessageContext inMsgContext = inRMMsgContext.getMessageContext();
		AxisEngine engine = new AxisEngine(inMsgContext.getConfigurationContext());
		try {					
			MessageContext faultContext = MessageContextBuilder.createFaultMessageContext(inMsgContext, e);
			// Copy some of the parameters to the new message context.
			faultContext.setProperty(Constants.Configuration.CONTENT_TYPE, inMsgContext
					.getProperty(Constants.Configuration.CONTENT_TYPE));

			EndpointReference faultEPR = inRMMsgContext.getFaultTo();
			if (faultEPR==null)
				faultEPR = inRMMsgContext.getReplyTo();
			
			//we handler the WSRM Anon InOut scenario differently here
			if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(inRMMsgContext.getRMSpecVersion())
					&& (faultEPR==null || faultEPR.hasAnonymousAddress())) {
				RequestResponseTransport requestResponseTransport = (RequestResponseTransport) inRMMsgContext.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
				
				//this will cause the fault to be thrown out of thread waiting on this transport object.
				AxisFault fault = new AxisFault ("Sandesha2 got a fault when doing the invocation", faultContext);
				requestResponseTransport.signalFaultReady(fault);
			} else	
				engine.sendFault(faultContext);
			
		} catch (AxisFault e1) {
			if (log.isErrorEnabled())
				log.error("Unable to send fault message ", e1);
		}
	}
	
}
