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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;

/**
 * This is responsible for sending and re-sending messages of Sandesha2. This
 * represent a thread that keep running all the time. This keep looking at the
 * Sender table to find out any entries that should be sent.
 */

public class Sender extends SandeshaThread {

	private static final Log log = LogFactory.getLog(Sender.class);

	// If this sender is working for several sequences, we use round-robin to
	// try and give them all a chance to invoke messages.
	int nextIndex = 0;
	boolean processedMessage = false;
	
	public Sender () {
		super(Sandesha2Constants.SENDER_SLEEP_TIME);
	}

	protected boolean internalRun() {
		if (log.isDebugEnabled()) log.debug("Enter: Sender::internalRun");

		Transaction transaction = null;
		boolean sleep = false;

		try {
			// Pick a sequence using a round-robin approach
			ArrayList allSequencesList = getSequences();
			int size = allSequencesList.size();
			log.debug("Choosing one from " + size + " sequences");
			if(nextIndex >= size) {
				nextIndex = 0;

				// We just looped over the set of sequences. If we didn't process any
				// messages on this loop then we sleep before the next one
				if(size == 0 || !processedMessage) {
					sleep = true;
				}
				processedMessage = false;
				
				if (log.isDebugEnabled()) log.debug("Exit: Sender::internalRun, looped over all sequences, sleep " + sleep);
				return sleep;
			}

			SequenceEntry entry = (SequenceEntry) allSequencesList.get(nextIndex++);
			String sequenceId = entry.getSequenceId();
			log.debug("Chose sequence " + sequenceId);

			transaction = storageManager.getTransaction();

			// Check that the sequence is still valid
			boolean found = false;
			if(entry.isRmSource()) {
				RMSBean matcher = new RMSBean();
				matcher.setInternalSequenceID(sequenceId);
				matcher.setTerminated(false);
				RMSBean rms = storageManager.getRMSBeanMgr().findUnique(matcher);
				if(rms != null) {
					sequenceId = rms.getSequenceID();
					found = true;
				}
			} else {
				RMDBean matcher = new RMDBean();
				matcher.setSequenceID(sequenceId);
				matcher.setTerminated(false);
				RMDBean rmd = storageManager.getRMDBeanMgr().findUnique(matcher);
				if(rmd != null) found = true;
			}
			if (!found) {
				stopThreadForSequence(sequenceId, entry.isRmSource());
				if (log.isDebugEnabled()) log.debug("Exit: Sender::internalRun, sequence has ended");
				return false;
			}
			
			SenderBeanMgr mgr = storageManager.getSenderBeanMgr();
			SenderBean senderBean = mgr.getNextMsgToSend(sequenceId);
			
			if (senderBean == null) {
				if (log.isDebugEnabled()) log.debug("Exit: Sender::internalRun, no message for this sequence");
				return false; // Move on to the next sequence in the list
			}

			// work Id is used to define the piece of work that will be
			// assigned to the Worker thread,
			// to handle this Sender bean.
			
			//workId contains a timeTiSend part to cater for retransmissions.
			//This will cause retransmissions to be treated as new work.
			String workId = senderBean.getMessageID() + senderBean.getTimeToSend();

			// check weather the bean is already assigned to a worker.
			if (getWorkerLock().isWorkPresent(workId)) {
				// As there is already a worker running we are probably looping
				// too fast, so sleep on the next loop.
				if (log.isDebugEnabled()) {
					String message = SandeshaMessageHelper.getMessage(
									SandeshaMessageKeys.workAlreadyAssigned,
									workId);
					log.debug("Exit: Sender::internalRun, " + message + ", sleeping");
				}
				return true;
			}

			if(transaction != null) {
				transaction.commit();
				transaction = null;
			}

			// start a worker which will work on this messages.
			SenderWorker worker = new SenderWorker(context, senderBean);
			worker.setLock(getWorkerLock());
			worker.setWorkId(workId);
			threadPool.execute(worker);

			// adding the workId to the lock after assigning it to a thread
			// makes sure
			// that all the workIds in the Lock are handled by threads.
			getWorkerLock().addWork(workId);

			// If we got to here then we found work to do on the sequence, so we should
			// remember not to sleep at the end of the list of sequences.
			processedMessage = true;

		} catch (Exception e) {

			// TODO : when this is the client side throw the exception to
			// the client when necessary.

			
			//TODO rollback only if a SandeshaStorageException.
			//This allows the other Exceptions to be used within the Normal flow.
			
			if (transaction != null) {
				try {
					transaction.rollback();
					transaction = null;
				} catch (Exception e1) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.rollbackError, e1
							.toString());
					log.debug(message, e1);
				}
			}

			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.sendMsgError, e.toString());

			log.debug(message, e);
		} finally {
			if (transaction != null) {
				try {
					transaction.commit();
					transaction = null;
				} catch (Exception e) {
					String message = SandeshaMessageHelper
							.getMessage(SandeshaMessageKeys.commitError, e.toString());
					log.debug(message, e);
				}
			}
		}
		if (log.isDebugEnabled()) log.debug("Exit: Sender::internalRun, not sleeping");
		return false;
	}

}
