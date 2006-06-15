/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *  
 */

package org.apache.sandesha2.workers;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.NextMsgBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.InvokerBean;
import org.apache.sandesha2.storage.beans.NextMsgBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.wsrm.Sequence;

/**
 * This is used when InOrder invocation is required. This is a seperated Thread that keep running
 * all the time. At each iteration it checks the InvokerTable to find weather there are any messages to
 * me invoked.
 * 
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 */

public class InOrderInvoker extends Thread {
	
	private boolean runInvoker = false;
	private ArrayList workingSequences = new ArrayList();
	private ConfigurationContext context = null;
	private static final Log log = LogFactory.getLog(InOrderInvoker.class);
	
	public synchronized void stopInvokerForTheSequence(String sequenceID) {
		workingSequences.remove(sequenceID);
		if (workingSequences.size()==0) {
			runInvoker = false;
		}
	}
	
	public synchronized void stopInvoking () {
		runInvoker = false;
	}

	public synchronized boolean isInvokerStarted() {
		return runInvoker;
	}

	public void setConfugurationContext(ConfigurationContext context) {
		this.context = context;
	}

	public synchronized void runInvokerForTheSequence(ConfigurationContext context, String sequenceID) {
		
		if (!workingSequences.contains(sequenceID))
			workingSequences.add(sequenceID);

		if (!isInvokerStarted()) {
			this.context = context;
			runInvoker = true;     //so that isSenderStarted()=true.
			super.start();
		}
	}

	public void run() {

		while (isInvokerStarted()) {

			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				log.debug("Invoker was Inturrepted....");
				log.debug(ex.getMessage());
			}

			Transaction transaction = null;
			boolean rolebacked = false;
			
			try {
				StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context,context.getAxisConfiguration());
				NextMsgBeanMgr nextMsgMgr = storageManager.getNextMsgBeanMgr();

				InvokerBeanMgr storageMapMgr = storageManager.getStorageMapBeanMgr();

				SequencePropertyBeanMgr sequencePropMgr = storageManager
						.getSequencePropretyBeanMgr();

				transaction = storageManager.getTransaction();
				
				//Getting the incomingSequenceIdList
				SequencePropertyBean allSequencesBean = (SequencePropertyBean) sequencePropMgr
						.retrieve(
								Sandesha2Constants.SequenceProperties.ALL_SEQUENCES,
								Sandesha2Constants.SequenceProperties.INCOMING_SEQUENCE_LIST);
				
				if (allSequencesBean == null) {
					continue;
				}
				ArrayList allSequencesList = SandeshaUtil.getArrayListFromString (allSequencesBean.getValue());
				
				Iterator allSequencesItr = allSequencesList.iterator();
				
				currentIteration: while (allSequencesItr.hasNext()) {
					String sequenceId = (String) allSequencesItr.next();
					
					//commiting the old transaction
					transaction.commit();
					
					//starting a new transaction for the new iteration.
					transaction = storageManager.getTransaction();
					
					NextMsgBean nextMsgBean = nextMsgMgr.retrieve(sequenceId);
					if (nextMsgBean == null) {
						String message = "Next message not set correctly. Removing invalid entry.";
						log.debug(message);
						allSequencesItr.remove();
						
						//cleaning the invalid data of the all sequences.
						allSequencesBean.setValue(allSequencesList.toString());
						sequencePropMgr.update(allSequencesBean);	
						continue;
					}

					long nextMsgno = nextMsgBean.getNextMsgNoToProcess();
					if (nextMsgno <= 0) { 
						String message = "Invalid message number as the Next Message Number.";
						throw new SandeshaException(message);
					}

					Iterator stMapIt = storageMapMgr.find(
							new InvokerBean(null, nextMsgno, sequenceId))
							.iterator();
					
					boolean invoked = false;
					
					while (stMapIt.hasNext()) {

						InvokerBean stMapBean = (InvokerBean) stMapIt.next();
						String key = stMapBean.getMessageContextRefKey();

						MessageContext msgToInvoke = storageManager.retrieveMessageContext(key,context);
						RMMsgContext rmMsg = MsgInitializer.initializeMessage(msgToInvoke);

						//have to commit the transaction before invoking. This may get changed when WS-AT is available.
						transaction.commit();
						
						try {
							//Invoking the message.														
							msgToInvoke.setProperty(Sandesha2Constants.WITHIN_TRANSACTION,Sandesha2Constants.VALUE_TRUE);
		
							boolean postFailureInvocation = false;
							
							//StorageManagers should st following property to true, to indicate that the message received comes after a failure.
							String postFaulureProperty = (String) msgToInvoke.getProperty(Sandesha2Constants.POST_FAILURE_MESSAGE);
							if (postFaulureProperty!=null && Sandesha2Constants.VALUE_TRUE.equals(postFaulureProperty))
								postFailureInvocation = true;  
							
							AxisEngine engine = new AxisEngine (context);
							if (postFailureInvocation) {
								makeMessageReadyForReinjection (msgToInvoke);
								engine.receive(msgToInvoke);
							} else {
								engine.resume(msgToInvoke);
							}
							
							invoked = true;

						} catch (Exception e) {
							throw new SandeshaException(e);
						} finally {
							transaction = storageManager.getTransaction();
						}
						
						//Service will be invoked only once. I.e. even if an exception get thrown in invocation
						//the service will not be invoked again. 
						storageMapMgr.delete(key);
						
						//removing the corresponding message context as well.
						MessageContext msgCtx = storageManager.retrieveMessageContext(key,context);
						if (msgCtx!=null) {
							storageManager.removeMessageContext(key);
						}
						
						
						//undating the next msg to invoke

						if (rmMsg.getMessageType() == Sandesha2Constants.MessageTypes.APPLICATION) {
							Sequence sequence = (Sequence) rmMsg
									.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
							if (sequence.getLastMessage() != null) {
								TerminateManager.cleanReceivingSideAfterInvocation(context, sequenceId,storageManager);
								//exit from current iteration. (since an entry was removed)
								break currentIteration;
							}
						}
					}

					if (invoked) {
						nextMsgno++;
						nextMsgBean.setNextMsgNoToProcess(nextMsgno);
						nextMsgMgr.update(nextMsgBean);
					}	
				}
				
			} catch (Exception e) {				
				if (transaction!=null) {
					try {
						transaction.rollback();
						rolebacked = true;
					} catch (Exception e1) {
						String message = "Exception thrown when trying to roleback the transaction.";
						log.debug(message,e1);
					}
				}
				String message = "Sandesha2 got an exception when trying to invoke the message";
				log.debug(message,e);
			} finally { 
				if (!rolebacked && transaction!=null) {
					try {
						transaction.commit();
					} catch (Exception e) {
						String message = "Exception thrown when trying to commit the transaction.";
						log.debug(message,e);
					}
				}
			}
		}
	}
	
	private void makeMessageReadyForReinjection (MessageContext messageContext) {
		messageContext.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,null);
		messageContext.getOptions().setMessageId(null);
		messageContext.getOptions().setTo(null);
		messageContext.getOptions().setAction(null);
		messageContext.setProperty(Sandesha2Constants.REINJECTED_MESSAGE,Sandesha2Constants.VALUE_TRUE);
	}
}