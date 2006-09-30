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

import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.util.threadpool.ThreadFactory;
import org.apache.axis2.util.threadpool.ThreadPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.SandeshaUtil;

/**
 * This is responsible for sending and re-sending messages of Sandesha2. This
 * represent a thread that keep running all the time. This keep looking at the
 * Sender table to find out any entries that should be sent.
 */

public class Sender extends Thread {

	private boolean runSender = false;
	private ArrayList workingSequences = new ArrayList();
	private ConfigurationContext context = null;
	private static final Log log = LogFactory.getLog(Sender.class);
	private boolean hasStopped = false;
	
    private transient ThreadFactory threadPool;
    public int SENDER_THREADPOOL_SIZE = 5;
    
    private WorkerLock lock = null;
    
    public Sender () {
    	threadPool = new ThreadPool (SENDER_THREADPOOL_SIZE,SENDER_THREADPOOL_SIZE);
    	lock = new WorkerLock ();
    }

	public synchronized void stopSenderForTheSequence(String sequenceID) {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::stopSenderForTheSequence, " + sequenceID);
		workingSequences.remove(sequenceID);
		if (workingSequences.size() == 0) {
			runSender = false;
		}
		if (log.isDebugEnabled())
			log.debug("Exit: Sender::stopSenderForTheSequence");
	}

	public synchronized void stopSending() {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::stopSending");

		if (isSenderStarted()) {
			// the sender is started so stop it
			runSender = false;
			// wait for it to finish
			while (!hasStoppedSending()) {
				try {
					wait(Sandesha2Constants.SENDER_SLEEP_TIME);
				} catch (InterruptedException e1) {
					log.debug(e1.getMessage());
				}
			}
		}

		if (log.isDebugEnabled())
			log.debug("Exit: Sender::stopSending");
	}

	private synchronized boolean hasStoppedSending() {
		if (log.isDebugEnabled()) {
			log.debug("Enter: Sender::hasStoppedSending");
			log.debug("Exit: Sender::hasStoppedSending, " + hasStopped);
		}
		return hasStopped;
	}

	public synchronized boolean isSenderStarted() {
		if (log.isDebugEnabled()) {
			log.debug("Enter: Sender::isSenderStarted");
			log.debug("Exit: Sender::isSenderStarted, " + runSender);
		}
		return runSender;
	}

	public void run() {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::run");

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
			log.debug("Exit: Sender::run");
	}

	private void internalRun() {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::internalRun");

		StorageManager storageManager = null;

		try {
			storageManager = SandeshaUtil.getSandeshaStorageManager(context, context.getAxisConfiguration());
		} catch (SandeshaException e2) {
			// TODO Auto-generated catch block
			log.debug(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotCointinueSender, e2.toString()), e2);
			e2.printStackTrace();
			return;
		}

		while (isSenderStarted()) {

			try {
				Thread.sleep(Sandesha2Constants.SENDER_SLEEP_TIME);
			} catch (InterruptedException e1) {
				// e1.printStackTrace();
				log.debug("Sender was interupted...");
				log.debug(e1.getMessage());
				log.debug("End printing Interrupt...");
			}

			Transaction transaction = null;
			boolean rolebacked = false;

			try {
				if (context == null) {
					String message = SandeshaMessageHelper
							.getMessage(SandeshaMessageKeys.configContextNotSet);
					message = SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.cannotCointinueSender, message);
					log.debug(message);
					throw new SandeshaException(message);
				}

				// TODO make sure this locks on reads.
				transaction = storageManager.getTransaction();

				SenderBeanMgr mgr = storageManager.getRetransmitterBeanMgr();
				SenderBean senderBean = mgr.getNextMsgToSend();
				if (senderBean == null) {
					if (log.isDebugEnabled()) {
						String message = SandeshaMessageHelper
								.getMessage(SandeshaMessageKeys.senderBeanNotFound);
						log.debug(message);
					}
					continue;
				}

				String messageId = senderBean.getMessageID();

				String toAddress = senderBean.getToAddress();
				if (toAddress != null) {
					boolean unsendableAddress = false;

					if (toAddress
							.equals(AddressingConstants.Submission.WSA_ANONYMOUS_URL))
						unsendableAddress = true;
					else if (toAddress
							.equals(AddressingConstants.Final.WSA_ANONYMOUS_URL))
						unsendableAddress = true;
					else if (toAddress
							.startsWith(Sandesha2Constants.WSRM_ANONYMOUS_URI_PREFIX))
						unsendableAddress = true;

					if (unsendableAddress) {
						if (log.isDebugEnabled()) {
							String message = SandeshaMessageHelper.getMessage(
									SandeshaMessageKeys.cannotSendToTheAddress,
									toAddress);
							log.debug(message);
						}
						continue;
					}
				}

				// work Id is used to define the piece of work that will be
				// assigned to the Worker thread,
				// to handle this Sender bean.
				String workId = messageId;

				// check weather the bean is already assigned to a worker.
				if (lock.isWorkPresent(workId)) {
					if (log.isDebugEnabled()) {
						String message = SandeshaMessageHelper
								.getMessage(
										SandeshaMessageKeys.workAlreadyAssigned,
										workId);
						log.debug(message);
					}
					continue;
				}

				transaction.commit();

				// start a worker which will work on this messages.
				SenderWorker worker = new SenderWorker(context, messageId);
				worker.setLock(lock);
				worker.setWorkId(messageId);
				threadPool.execute(worker);

				// adding the workId to the lock after assigning it to a thread
				// makes sure
				// that all the workIds in the Lock are handled by threads.
				lock.addWork(workId);

			} catch (Exception e) {

				// TODO : when this is the client side throw the exception to
				// the client when necessary.

				
				//TODO rollback only if a SandeshaStorageException.
				//This allows the other Exceptions to be used within the Normal flow.
				
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

				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.sendMsgError, e.toString());

				log.debug(message, e);
			} finally {
				if (transaction != null && !rolebacked) {
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
			log.debug("Exit: Sender::internalRun");
	}

	public synchronized void runSenderForTheSequence(ConfigurationContext context, String sequenceID) {
		if (log.isDebugEnabled())
			log.debug("Enter: Sender::runSenderForTheSequence, " + sequenceID);

		if (sequenceID != null && !workingSequences.contains(sequenceID))
			workingSequences.add(sequenceID);

		if (!isSenderStarted()) {
			this.context = context;
			runSender = true; // so that isSenderStarted()=true.
			super.start();
		}
		if (log.isDebugEnabled())
			log.debug("Exit: Sender::runSenderForTheSequence");
	}

}
