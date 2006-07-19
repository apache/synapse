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

import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
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
 * This is used when InOrder invocation is required. This is a seperated Thread
 * that keep running all the time. At each iteration it checks the InvokerTable
 * to find weather there are any messages to me invoked.
 */

public class InOrderInvoker extends Thread {

	private boolean runInvoker = false;

	private ArrayList workingSequences = new ArrayList();

	private ConfigurationContext context = null;

	private static final Log log = LogFactory.getLog(InOrderInvoker.class);

	private boolean hasStopped = false;

	public synchronized void stopInvokerForTheSequence(String sequenceID) {
		if (log.isDebugEnabled())
			log.debug("Enter: InOrderInvoker::stopInvokerForTheSequence, " + sequenceID);

		workingSequences.remove(sequenceID);
		if (workingSequences.size() == 0) {
			runInvoker = false;
		}

		if (log.isDebugEnabled())
			log.debug("Exit: InOrderInvoker::stopInvokerForTheSequence");
	}

	public synchronized void stopInvoking() {
		if (log.isDebugEnabled())
			log.debug("Enter: InOrderInvoker::stopInvoking");

		if (isInvokerStarted()) {
			// the invoker is started so stop it
			runInvoker = false;
			// wait for it to finish
			while (!hasStoppedInvoking()) {
				try {
					wait(Sandesha2Constants.INVOKER_SLEEP_TIME);
				} catch (InterruptedException e1) {
					log.debug(e1.getMessage());
				}
			}
		}

		if (log.isDebugEnabled())
			log.debug("Exit: InOrderInvoker::stopInvoking");
	}

	public synchronized boolean isInvokerStarted() {
		if (log.isDebugEnabled()) {
			log.debug("Enter: InOrderInvoker::isInvokerStarted");
			log.debug("Exit: InOrderInvoker::isInvokerStarted, " + runInvoker);
		}
		return runInvoker;
	}

	public synchronized void runInvokerForTheSequence(ConfigurationContext context, String sequenceID) {
		if (log.isDebugEnabled())
			log.debug("Enter: InOrderInvoker::runInvokerForTheSequence");

		if (!workingSequences.contains(sequenceID))
			workingSequences.add(sequenceID);

		if (!isInvokerStarted()) {
			this.context = context;
			runInvoker = true; // so that isSenderStarted()=true.
			super.start();
		}
		if (log.isDebugEnabled())
			log.debug("Exit: InOrderInvoker::runInvokerForTheSequence");
	}

	private synchronized boolean hasStoppedInvoking() {
		if (log.isDebugEnabled()) {
			log.debug("Enter: InOrderInvoker::hasStoppedInvoking");
			log.debug("Exit: InOrderInvoker::hasStoppedInvoking, " + hasStopped);
		}
		return hasStopped;
	}

	public void run() {
		if (log.isDebugEnabled())
			log.debug("Enter: InOrderInvoker::run");

		try {
			internalRun();
		} finally {
			// flag that we have exited the run loop and notify any waiting
			// threads
			synchronized (this) {
				hasStopped = true;
				notify();
			}
		}

		if (log.isDebugEnabled())
			log.debug("Exit: InOrderInvoker::run");
	}

	private void internalRun() {
		if (log.isDebugEnabled())
			log.debug("Enter: InOrderInvoker::internalRun");

		while (isInvokerStarted()) {

			try {
				Thread.sleep(Sandesha2Constants.INVOKER_SLEEP_TIME);
			} catch (InterruptedException ex) {
				log.debug("Invoker was Inturrepted....");
				log.debug(ex.getMessage());
			}

			Transaction transaction = null;
			boolean rolebacked = false;

			try {
				StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context, context
						.getAxisConfiguration());
				NextMsgBeanMgr nextMsgMgr = storageManager.getNextMsgBeanMgr();

				InvokerBeanMgr storageMapMgr = storageManager.getStorageMapBeanMgr();

				SequencePropertyBeanMgr sequencePropMgr = storageManager.getSequencePropertyBeanMgr();

				transaction = storageManager.getTransaction();

				// Getting the incomingSequenceIdList
				SequencePropertyBean allSequencesBean = sequencePropMgr.retrieve(
						Sandesha2Constants.SequenceProperties.ALL_SEQUENCES,
						Sandesha2Constants.SequenceProperties.INCOMING_SEQUENCE_LIST);

				if (allSequencesBean == null) {
					if (log.isDebugEnabled())
						log.debug("AllSequencesBean not found");
					continue;
				}
				ArrayList allSequencesList = SandeshaUtil.getArrayListFromString(allSequencesBean.getValue());

				Iterator allSequencesItr = allSequencesList.iterator();

				currentIteration: while (allSequencesItr.hasNext()) {
					String sequenceId = (String) allSequencesItr.next();

					// commiting the old transaction
					transaction.commit();

					// starting a new transaction for the new iteration.
					transaction = storageManager.getTransaction();

					NextMsgBean nextMsgBean = nextMsgMgr.retrieve(sequenceId);
					if (nextMsgBean == null) {
						String message = "Next message not set correctly. Removing invalid entry.";
						log.debug(message);
						allSequencesItr.remove();

						// cleaning the invalid data of the all sequences.
						allSequencesBean.setValue(allSequencesList.toString());
						sequencePropMgr.update(allSequencesBean);
						continue;
					}

					long nextMsgno = nextMsgBean.getNextMsgNoToProcess();
					if (nextMsgno <= 0) {
						if (log.isDebugEnabled())
							log.debug("Invalid Next Message Number " + nextMsgno);
						String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.invalidMsgNumber, Long
								.toString(nextMsgno));
						throw new SandeshaException(message);
					}

					Iterator stMapIt = storageMapMgr.find(new InvokerBean(null, nextMsgno, sequenceId)).iterator();

					boolean invoked = false;

					while (stMapIt.hasNext()) {

						InvokerBean stMapBean = (InvokerBean) stMapIt.next();
						String key = stMapBean.getMessageContextRefKey();

						MessageContext msgToInvoke = storageManager.retrieveMessageContext(key, context);
						RMMsgContext rmMsg = MsgInitializer.initializeMessage(msgToInvoke);

						// have to commit the transaction before invoking. This
						// may get changed when WS-AT is available.
						transaction.commit();

						try {
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

							AxisEngine engine = new AxisEngine(context);
							if (postFailureInvocation) {
								makeMessageReadyForReinjection(msgToInvoke);
								if (log.isDebugEnabled())
									log.debug("Receiving message, key=" + key + ", msgCtx="
											+ msgToInvoke.getEnvelope().getHeader());
								engine.receive(msgToInvoke);
							} else {
								if (log.isDebugEnabled())
									log.debug("Resuming message, key=" + key + ", msgCtx="
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
						} finally {
							transaction = storageManager.getTransaction();
						}

						// Service will be invoked only once. I.e. even if an
						// exception get thrown in invocation
						// the service will not be invoked again.
						storageMapMgr.delete(key);

						// removing the corresponding message context as well.
						MessageContext msgCtx = storageManager.retrieveMessageContext(key, context);
						if (msgCtx != null) {
							storageManager.removeMessageContext(key);
						}

						// undating the next msg to invoke

						if (rmMsg.getMessageType() == Sandesha2Constants.MessageTypes.APPLICATION) {
							Sequence sequence = (Sequence) rmMsg
									.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
							if (sequence.getLastMessage() != null) {
								TerminateManager.cleanReceivingSideAfterInvocation(context, sequenceId, storageManager);
								// exit from current iteration. (since an entry
								// was removed)
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
				if (transaction != null) {
					try {
						transaction.rollback();
						rolebacked = true;
					} catch (Exception e1) {
						String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.rollbackError, e1
								.toString());
						log.debug(message, e1);
					}
				}
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.invokeMsgError);
				log.debug(message, e);
			} finally {
				if (!rolebacked && transaction != null) {
					try {
						transaction.commit();
					} catch (Exception e) {
						String message = SandeshaMessageHelper
								.getMessage(SandeshaMessageKeys.commitError, e.toString());
						log.debug(message, e);
					}
				}
			}
		}
		if (log.isDebugEnabled())
			log.debug("Exit: InOrderInvoker::internalRun");
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
