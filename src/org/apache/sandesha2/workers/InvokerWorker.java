package org.apache.sandesha2.workers;

import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.NextMsgBeanMgr;
import org.apache.sandesha2.storage.beans.InvokerBean;
import org.apache.sandesha2.storage.beans.NextMsgBean;
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
		if(log.isDebugEnabled()) log.debug("Entry: InvokerWorker::run");
		
		Transaction transaction = null;
		MessageContext msgToInvoke = null;
		
		try {
			
			StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
			InvokerBeanMgr invokerBeanMgr = storageManager.getStorageMapBeanMgr();
			NextMsgBeanMgr nextMsgMgr = storageManager.getNextMsgBeanMgr();
			
			//starting a transaction
			transaction = storageManager.getTransaction();
			
			InvokerBean invokerBean = invokerBeanMgr.retrieve(messageContextKey);
			
			String sequenceId = invokerBean.getSequenceID();
			long messageNo = invokerBean.getMsgNo();
			
			msgToInvoke = storageManager.retrieveMessageContext(messageContextKey, configurationContext);
			RMMsgContext rmMsg = MsgInitializer.initializeMessage(msgToInvoke);

			String sequencePropertyKey = SandeshaUtil.getSequencePropertyKey(rmMsg);
			
			//endint the transaction before invocation.
			transaction.commit();
				
			boolean invoked = false;
			
			try {

				// Invocation is not done within a transation. This
				// may get changed when WS-AT is available.
				
				// Invoking the message.
				msgToInvoke.setProperty(Sandesha2Constants.WITHIN_TRANSACTION,
						Sandesha2Constants.VALUE_TRUE);

				boolean postFailureInvocation = false;

				// StorageManagers should st following property to
				// true, to indicate that the message received comes
				// after a failure.
				String postFaulureProperty = (String) msgToInvoke
						.getProperty(Sandesha2Constants.POST_FAILURE_MESSAGE);
				if (postFaulureProperty != null
						&& Sandesha2Constants.VALUE_TRUE.equals(postFaulureProperty))
					postFailureInvocation = true;

				AxisEngine engine = new AxisEngine(configurationContext);
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
				
				invoked = true;

			} catch (Exception e) {
				if (log.isDebugEnabled())
					log.debug("Exception :", e);

				handleFault(msgToInvoke, e);

				// throw new SandeshaException(e);
			}
				
			//starting a transaction for the post-invocation work.
			transaction = storageManager.getTransaction();
			
			// Service will be invoked only once. I.e. even if an
			// exception get thrown in invocation
			// the service will not be invoked again.
			invokerBeanMgr.delete(messageContextKey);

			// removing the corresponding message context as well.
			storageManager.removeMessageContext(messageContextKey);
			
			if (rmMsg.getMessageType() == Sandesha2Constants.MessageTypes.APPLICATION) {
				Sequence sequence = (Sequence) rmMsg
						.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
				
				//TODO support WSRM 1.1 spce here ( there is no last message concept)
				if (sequence.getLastMessage() != null) {
					TerminateManager.cleanReceivingSideAfterInvocation(configurationContext, sequencePropertyKey, sequenceId, storageManager);
					// exit from current iteration. (since an entry
					// was removed)
					if(log.isDebugEnabled()) log.debug("Exit: InvokerWorker::run Last message return");					
					return;
				}
			}
			
			if(!ignoreNextMsg){
				// updating the next msg to invoke

				String s = invokerBean.getSequenceID();
				NextMsgBean nextMsgBean = nextMsgMgr.retrieve(sequenceId);
				long nextMsgNo = nextMsgBean.getNextMsgNoToProcess();
				
				if (!(messageNo==nextMsgNo)) {
					String message = "Operated message number is different from the Next Message Number to invoke";
					throw new SandeshaException (message);
				}
				
				if (invoked) {
					nextMsgNo++;
					nextMsgBean.setNextMsgNoToProcess(nextMsgNo);
					nextMsgMgr.update(nextMsgBean);
				}				
			}
		} catch (SandeshaStorageException e) {
			transaction.rollback();
		} catch (SandeshaException e) {
			log.error(e.toString(), e);
		} catch (Exception e) {
			log.error(e.toString(), e);
		} finally {
			if (transaction!=null && transaction.isActive())
				transaction.commit();
			
			if (workId !=null && lock!=null) {
				lock.removeWork(workId);
			}
		}
		
		if(log.isDebugEnabled()) log.debug("Exit: InvokerWorker::run");
	}

	private void makeMessageReadyForReinjection(MessageContext messageContext) {
		messageContext.setProperty(AddressingConstants.WS_ADDRESSING_VERSION, null);
		messageContext.getOptions().setMessageId(null);
		messageContext.getOptions().setTo(null);
		messageContext.getOptions().setAction(null);
		messageContext.setProperty(Sandesha2Constants.REINJECTED_MESSAGE, Sandesha2Constants.VALUE_TRUE);
	}

	private void handleFault(MessageContext inMsgContext, Exception e) throws Exception {
		// msgContext.setProperty(MessageContext.TRANSPORT_OUT, out);
		AxisEngine engine = new AxisEngine(inMsgContext.getConfigurationContext());
		MessageContext faultContext = engine.createFaultMessageContext(inMsgContext, e);
		engine.sendFault(faultContext);
	}
	
}
