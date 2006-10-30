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
import java.util.Random;

import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.util.threadpool.ThreadFactory;
import org.apache.axis2.util.threadpool.ThreadPool;
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

public class Invoker extends Thread {

	private boolean runInvoker = false;
	private ArrayList workingSequences = new ArrayList();
	private ConfigurationContext context = null;
	private static final Log log = LogFactory.getLog(Invoker.class);
	private boolean hasStopped = false;
	
	private transient ThreadFactory threadPool;
	public int INVOKER_THREADPOOL_SIZE = 5;

	private WorkerLock lock = null;
	
	public Invoker() {
		threadPool = new ThreadPool(INVOKER_THREADPOOL_SIZE,
				INVOKER_THREADPOOL_SIZE);
		lock = new WorkerLock ();
	}

	public synchronized void stopInvokerForTheSequence(String sequenceID) {
		if (log.isDebugEnabled())
			log.debug("Enter: InOrderInvoker::stopInvokerForTheSequence, "
					+ sequenceID);

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

	public synchronized void runInvokerForTheSequence(
			ConfigurationContext context, String sequenceID) {
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
			log
					.debug("Exit: InOrderInvoker::hasStoppedInvoking, "
							+ hasStopped);
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
		
		// If this invoker is working for several sequences, we use round-robin to
		// try and give them all a chance to invoke messages.
		int nextIndex = 0;

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
				StorageManager storageManager = SandeshaUtil
						.getSandeshaStorageManager(context, context
								.getAxisConfiguration());
				NextMsgBeanMgr nextMsgMgr = storageManager.getNextMsgBeanMgr();

				InvokerBeanMgr storageMapMgr = storageManager
						.getStorageMapBeanMgr();

				SequencePropertyBeanMgr sequencePropMgr = storageManager
						.getSequencePropertyBeanMgr();

				transaction = storageManager.getTransaction();

				// Getting the incomingSequenceIdList
				SequencePropertyBean allSequencesBean = sequencePropMgr
						.retrieve(
								Sandesha2Constants.SequenceProperties.ALL_SEQUENCES,
								Sandesha2Constants.SequenceProperties.INCOMING_SEQUENCE_LIST);

				if (allSequencesBean == null) {
					if (log.isDebugEnabled())
						log.debug("AllSequencesBean not found");
					continue;
				}
				
				// Pick a sequence using a round-robin approach
				ArrayList allSequencesList = SandeshaUtil
						.getArrayListFromString(allSequencesBean.getValue());
				int size = allSequencesList.size();
				log.debug("Choosing one from " + size + " sequences");
				if(nextIndex >= size) {
					nextIndex = 0;
					if (size == 0) continue;
				}
				String sequenceId = (String) allSequencesList.get(nextIndex++);
				log.debug("Chose sequence " + sequenceId);

				NextMsgBean nextMsgBean = nextMsgMgr.retrieve(sequenceId);
				if (nextMsgBean == null) {
					String message = "Next message not set correctly. Removing invalid entry.";
					log.debug(message);
	
					allSequencesList.remove(size);
					
					// cleaning the invalid data of the all sequences.
					allSequencesBean.setValue(allSequencesList.toString());
					sequencePropMgr.update(allSequencesBean);
					continue;
				}

				long nextMsgno = nextMsgBean.getNextMsgNoToProcess();
				if (nextMsgno <= 0) {
					if (log.isDebugEnabled())
						log.debug("Invalid Next Message Number " + nextMsgno);
					String message = SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.invalidMsgNumber, Long
									.toString(nextMsgno));
					throw new SandeshaException(message);
				}

				Iterator stMapIt = storageMapMgr.find(
						new InvokerBean(null, nextMsgno, sequenceId))
						.iterator();

				
				//TODO correct the locking mechanism to have one lock per sequence.
				
				if (stMapIt.hasNext()) { //the next Msg entry is present.

					String workId = sequenceId + "::" + nextMsgno; //creating a workId to uniquely identify the
																   //piece of work that will be assigned to the Worker.
										
					//check weather the bean is already assigned to a worker.
					if (lock.isWorkPresent(workId)) {
						String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.workAlreadyAssigned, workId);
						log.debug(message);
						continue;
					}
					
					InvokerBean bean = (InvokerBean) stMapIt.next();
					String messageContextKey = bean.getMessageContextRefKey();
					
					transaction.commit();

					// start a new worker thread and let it do the invocation.
					InvokerWorker worker = new InvokerWorker(context,messageContextKey);
					
					worker.setLock(lock);
					worker.setWorkId(workId);
					
					threadPool.execute(worker);
					
					//adding the workId to the lock after assigning it to a thread makes sure 
					//that all the workIds in the Lock are handled by threads.
					lock.addWork(workId);
				}

			} catch (Exception e) {
				if (transaction != null) {
					try {
						transaction.rollback();
						rolebacked = true;
					} catch (Exception e1) {
						String message = SandeshaMessageHelper.getMessage(
								SandeshaMessageKeys.rollbackError, e1
										.toString());
						log.debug(message, e1);
					}
				}
				String message = SandeshaMessageHelper
						.getMessage(SandeshaMessageKeys.invokeMsgError);
				log.debug(message, e);
			} finally {
				if (!rolebacked && transaction != null) {
					try {
						transaction.commit();
					} catch (Exception e) {
						String message = SandeshaMessageHelper.getMessage(
								SandeshaMessageKeys.commitError, e.toString());
						log.debug(message, e);
					}
				}
			}
		}
		if (log.isDebugEnabled())
			log.debug("Exit: InOrderInvoker::internalRun");
	}

}
