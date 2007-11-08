/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.workers;

import org.apache.axiom.om.impl.builder.StAXBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.context.ContextManager;
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

	static final Log log = LogFactory.getLog(InvokerWorker.class);
	static final WorkerLock lock = new WorkerLock();
	
	private ConfigurationContext configurationContext;
	private String  sequence;
	private long    messageNumber;
	private String  messageContextKey;
	private boolean ignoreNextMsg;
	private boolean pooledThread;
	
	public InvokerWorker (ConfigurationContext configurationContext, InvokerBean bean) {
		// All invoker workers need to use the same lock, so we point to the static one here.
		this.setLock(lock);
		
		this.configurationContext = configurationContext;
		initializeFromBean(bean);
	}
	
	public void forceOutOfOrder() {
		if(log.isDebugEnabled()) log.debug("Enter: InvokerWorker::forceOutOfOrder");
		ignoreNextMsg = true;
		if(log.isDebugEnabled()) log.debug("Exit: InvokerWorker::forceOutOfOrder");
	}

	public void setPooled() {
		if(log.isDebugEnabled()) log.debug("Enter: InvokerWorker::setPooled");
		pooledThread = true;
		if(log.isDebugEnabled()) log.debug("Exit: InvokerWorker::setPooled");
	}

	private void initializeFromBean(InvokerBean bean) {
		if(log.isDebugEnabled()) log.debug("Enter: InvokerWorker::initializeFromBean " + bean);
		
		this.sequence = bean.getSequenceID();
		this.messageNumber = bean.getMsgNo();
		this.messageContextKey = bean.getMessageContextRefKey();
		
		if(log.isDebugEnabled()) log.debug("Exit: InvokerWorker::initializeFromBean");
	}
		
	/**
	 * The run method invokes the message that this invoker has been primed with, but will
	 * also attempt to invoke subsequent messages. If the invoker worker is running on the
	 * application thread then we move on to a thread pool for the second message, but if
	 * we are already on a pooled thread then we just continue.
	 */
	public void run() {
		if(log.isDebugEnabled()) log.debug("Enter: InvokerWorker::run, message " + messageNumber + ", sequence " + sequence);
		
		// If we are not the holder of the correct lock, then we have to stop
		if(lock != null && !lock.ownsLock(workId, this)) {
			if (log.isDebugEnabled()) log.debug("Exit: InvokerWorker::run, another worker holds the lock");
			return;
		}
		
		Transaction tran = null;
		try {
			InvokerWorker nextWorker = null;
			Runnable nextRunnable = null;

			// Invoke the first message
			invokeMessage(null);

			// Look for the next message, so long as we are still processing normally
			while(!ignoreNextMsg) {
				InvokerBean finder = new InvokerBean();
				finder.setSequenceID(sequence);
				finder.setMsgNo(messageNumber + 1);

				StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
				tran = storageManager.getTransaction();

				InvokerBeanMgr mgr = storageManager.getInvokerBeanMgr();
				InvokerBean nextBean = mgr.findUnique(finder);

				if(nextBean != null) {
					if(pooledThread) {
						initializeFromBean(nextBean);
						final Transaction theTran = tran;
						Runnable work = new Runnable() {
							public void run() {
								invokeMessage(theTran);
							}
						};

						// Wrap the work with the correct context, if needed.
						ContextManager contextMgr = SandeshaUtil.getContextManager(configurationContext);
						if(contextMgr != null) {
							work = contextMgr.wrapWithContext(work, nextBean.getContext());
						}

						// Finally do the work
						work.run();

						tran = null;
					} else {
						nextWorker = new InvokerWorker(configurationContext, nextBean);
						nextWorker.setPooled();
						nextWorker.setWorkId(workId);

						// Wrap the invoker worker with the correct context, if needed.
						ContextManager contextMgr = SandeshaUtil.getContextManager(configurationContext);
						if(contextMgr != null) {
							nextRunnable = contextMgr.wrapWithContext(nextWorker, nextBean.getContext());
						} else {
							nextRunnable = nextWorker;
						}
					}
				}
		
				// Clean up the tran, in case we didn't pass it into the invoke method
				if(tran != null) tran.commit();
				tran = null;
						
				if(nextBean == null || nextWorker != null) {
					// We have run out of work, or the new worker has taken it on, so we can
					// break out of the loop
					break;
				}
			}
					
			if (workId !=null && lock!=null) {
				lock.removeWork(workId);
			}

			// If we created another worker, set it running now that we have released the lock
			if(nextWorker != null) {
				lock.addWork(workId, nextWorker);
				configurationContext.getThreadPool().execute(nextRunnable);
			}

		} catch(SandeshaException e) {
			log.debug("Exception within InvokerWorker", e);

			// Clean up the tran, if there is one left
			if(tran != null) {
				try {
					tran.rollback();
				} catch(SandeshaException e2) {
					log.debug("Exception rolling back tran", e2);
				}
			}
		} finally {
			// Release the lock
			if (workId !=null && lock!=null && lock.ownsLock(workId, this)) {
				lock.removeWork(workId);
			}
		}
				
		if(log.isDebugEnabled()) log.debug("Exit: InvokerWorker::run");
	}

	private void invokeMessage(Transaction tran) {
		if(log.isDebugEnabled()) log.debug("Enter: InvokerWorker::invokeMessage");

		Transaction transaction = null;
		MessageContext msgToInvoke = null;
		
		try {
			
			StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
			InvokerBeanMgr invokerBeanMgr = storageManager.getInvokerBeanMgr();
			
			//starting a transaction
			if(tran == null) {
				transaction = storageManager.getTransaction();
			} else {
				transaction = tran;
			}
			
			InvokerBean invokerBean = invokerBeanMgr.retrieve(messageContextKey);

			msgToInvoke = storageManager.retrieveMessageContext(messageContextKey, configurationContext);

			// ending the transaction before invocation.
			if(transaction != null) {
				transaction.commit();
				transaction = storageManager.getTransaction();
			}

			RMMsgContext rmMsg = MsgInitializer.initializeMessage(msgToInvoke);

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

		        InvocationResponse response = null;
				if (postFailureInvocation) {
					makeMessageReadyForReinjection(msgToInvoke);
					if (log.isDebugEnabled())
						log.debug("Receiving message, key=" + messageContextKey + ", msgCtx="
								+ msgToInvoke.getEnvelope().getHeader());
					response = AxisEngine.receive(msgToInvoke);
				} else {
					if (log.isDebugEnabled())
						log.debug("Resuming message, key=" + messageContextKey + ", msgCtx="
								+ msgToInvoke.getEnvelope().getHeader());
					msgToInvoke.setPaused(false);
					response = AxisEngine.resumeReceive(msgToInvoke);
				}
		        if(!InvocationResponse.SUSPEND.equals(response)) {
		            // Performance work - need to close the XMLStreamReader to prevent GC thrashing.
		            SOAPEnvelope env = msgToInvoke.getEnvelope();
		            if(env!=null){
		              StAXBuilder sb = (StAXBuilder)msgToInvoke.getEnvelope().getBuilder();
		              if(sb!=null){
		                sb.close();
		              }
		            }
		        }
		        
		        if (transaction != null && transaction.isActive())
		        	transaction.commit();

			} catch (Exception e) {
				if (log.isDebugEnabled())
					log.debug("Exception :", e);

				if (transaction != null && transaction.isActive())
					transaction.rollback();
				
				handleFault(rmMsg, e);
			}

			transaction = storageManager.getTransaction();
			 
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
					if(log.isDebugEnabled()) log.debug("Exit: InvokerWorker::invokeMessage Last message return");	
					if(transaction != null && transaction.isActive()) transaction.commit();
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
			if (transaction!=null && transaction.isActive()) {
				try {
					transaction.rollback();
				} catch (SandeshaStorageException e) {
					if (log.isWarnEnabled())
						log.warn("Caught exception rolling back transaction", e);
				}
			}
		}
		
		if(log.isDebugEnabled()) log.debug("Exit: InvokerWorker::invokeMessage");
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
				if (requestResponseTransport!=null)
					requestResponseTransport.signalFaultReady(fault);
				else
					AxisEngine.sendFault(faultContext);
				
			} else	
				AxisEngine.sendFault(faultContext);
			
		} catch (AxisFault e1) {
			if (log.isErrorEnabled())
				log.error("Unable to send fault message ", e1);
		}
	}
	
}
