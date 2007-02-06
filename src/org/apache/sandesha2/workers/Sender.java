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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;

/**
 * This is responsible for sending and re-sending messages of Sandesha2. This
 * represent a thread that keep running all the time. This keep looking at the
 * Sender table to find out any entries that should be sent.
 */

public class Sender extends SandeshaThread {

	private static final Log log = LogFactory.getLog(Sender.class);
	    
	public Sender () {
		super(Sandesha2Constants.SENDER_SLEEP_TIME);
	}

	protected boolean internalRun() {
		if (log.isDebugEnabled()) log.debug("Enter: Sender::internalRun");

		Transaction transaction = null;

		try {
			transaction = storageManager.getTransaction();

			SenderBeanMgr mgr = storageManager.getSenderBeanMgr();
			SenderBean senderBean = mgr.getNextMsgToSend();
			
			if (senderBean == null) {
				// As there was no work to do, we sleep for a while on the next loop.
				if (log.isDebugEnabled()) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.senderBeanNotFound);
					log.debug("Exit: Sender::internalRun, " + message + ", sleeping");
				}
				return true;
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
