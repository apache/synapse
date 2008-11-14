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

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.util.threadpool.ThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.util.SandeshaUtil;

/**
 * Aggregates pause and stop logic between sender and invoker threads.
 */
public abstract class SandeshaThread extends Thread{

	private static final Log log = LogFactory.getLog(SandeshaThread.class);

	private boolean runThread = false;
	private boolean hasStoppedRunning = false;
	private boolean hasPausedRunning = false;
	private boolean pauseRequired = false;
	private boolean stopRequested = false;
	
	private int sleepTime;
    private WorkerLock lock = null;

	private ArrayList<SequenceEntry> workingSequences = new ArrayList<SequenceEntry>();
	
	protected transient ThreadFactory threadPool;
	protected ConfigurationContext context = null;
	protected StorageManager storageManager = null;
	private boolean reRunThread;

	
	public SandeshaThread(int sleepTime) {
		this.sleepTime = sleepTime;
		this.setDaemon(true);
  	    lock = new WorkerLock ();
	}
	
	public final WorkerLock getWorkerLock() {
		return lock;
	}
	
	public synchronized void stopThreadForSequence(String sequenceID, boolean rmSource){
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaThread::stopThreadForSequence, " + sequenceID);
		
		// We do not actually stop the thread here, as the workers are smart enough
		// to sleep when there is no work to do. If we were to exit the thread then
		// we wouldn't be able to start back up when the thread gets some more work
		// to do.
		workingSequences.remove(new SequenceEntry(sequenceID, rmSource));
		
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaThread::stopThreadForSequence");		
	}
	
	/**
	 * Waits for the invoking thread to pause
	 */
	public synchronized void blockForPause(){
		while(pauseRequired){
			//someone else is requesting a pause - wait for them to finish
			try{
				wait(sleepTime);
			}catch(InterruptedException e){
				//ignore
			}
		}
				
		//we can now request a pause - the next pause will be our
		pauseRequired = true;
				
		if(hasStoppedRunning() || !isThreadStarted()){
			throw new IllegalStateException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotPauseThread));
		}
		while(!hasPausedRunning){
			//wait for our pause to come around
			try{
				wait(sleepTime);
			}catch(InterruptedException e){
				//ignore
			}
			
		}
		//the sandesha thread is now paused
	}
	
	public synchronized void finishPause(){
		//indicate that the current pause is no longer required.
		pauseRequired = false;
		notifyAll();
	}
	
	public synchronized void stopRunning() {
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaThread::stopRunning, " + this);

		//NOTE: we do not take acount of pausing when stopping.
		//The call to stop will wait until the invoker has exited the loop
		if (isThreadStarted()) {
			// the invoker is started so stop it
			runThread = false;
			stopRequested = true;
			// wait for it to finish
			while (!hasStoppedRunning()) {
				try {
					wait(sleepTime);
				} catch (InterruptedException e1) {
					//ignore
				}
			}
		}
		
    // In a unit test, tracing 'this' once the thread was stopped caused
    // an exception, so we just trace exit.
    if (log.isDebugEnabled())
      log.debug("Exit: SandeshaThread::stopRunning");
	}
	
	public synchronized boolean isThreadStarted() {

		if (!runThread && log.isDebugEnabled())
			log.debug("SandeshaThread not started");	

		return runThread;
	}
	

	/**
	 * Ensure that the worker thread is aware of the given sequence. As source sequences
	 * do not have a proper sequence id at the time they are bootstrapped, the caller
	 * must pass in the internal sequence id when rmSource is true.
	 */
	public synchronized void runThreadForSequence(ConfigurationContext context, String sequenceID, boolean rmSource){
		if(log.isDebugEnabled()) 
			log.debug("Entry: SandeshaThread::runThreadForSequence, " + this + ", " + sequenceID + ", " + rmSource);

		SequenceEntry entry = new SequenceEntry(sequenceID, rmSource);
		if (!workingSequences.contains(entry)) workingSequences.add(entry);
		
		if (!isThreadStarted() && !stopRequested) {
			if(log.isDebugEnabled()) log.debug("Starting thread");

			this.context = context;
			
			// Get the axis2 thread pool
			threadPool = context.getThreadPool();
			
			runThread = true; // so that isStarted()=true.
			
			super.start();
			
			// Set the SandeshaThread to have the same context classloader as the application
			try{
				AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					public Object run() throws Exception {
						SandeshaThread.this.setContextClassLoader(Thread.currentThread().getContextClassLoader());
						return null;
					}
				});				
			}
			catch(Exception e){
				log.error(e);
				throw new RuntimeException(e);
			}

			
		} else if (!stopRequested){
			if(log.isDebugEnabled()) log.debug("Waking thread");
			wakeThread();
		} else if (stopRequested) {
			if(log.isDebugEnabled()) log.debug("Can't start thread as it has been stopped");
		}

		if(log.isDebugEnabled()) log.debug("Exit: SandeshaThread::runThreadForSequence");
	}
	
	/**
	 * 
	 * @return a List of SequenceEntry instances
	 */
	public synchronized ArrayList<SequenceEntry> getSequences() {
		// Need to copy the list for thread safety
		ArrayList<SequenceEntry> result = new ArrayList<SequenceEntry>();
		result.addAll(workingSequences);
		return result;
	}

	protected synchronized boolean hasStoppedRunning() {
		return hasStoppedRunning;
	}
	
	protected synchronized void doPauseIfNeeded(){
		//see if we need to pause
			
			while(pauseRequired){
				if(!hasPausedRunning){
					//let the requester of this pause know we are now pausing
				  hasPausedRunning = true;
				  notifyAll();						
				}
				//now we pause
			  try{
			  	wait(sleepTime);
			  }catch(InterruptedException e){
			  	//ignore
			  }
			}//end while
			//the request to pause has finished so we are no longer pausing
			hasPausedRunning = false;
	}

	/**
	 * Wake the current thread as there is work to be done.
	 * Also flag that if we miss a notify, then there is 
	 * work to be done.  Implementing threads should check this value
	 * before waiting
	 *
	 */
	public synchronized void wakeThread() {
		reRunThread = true;
		
		if (!hasPausedRunning)
			notifyAll();
	}
	
	/**
	 * Indicate that the main loop has been run	 
	 */
	public synchronized void setRanMainLoop() {
		reRunThread = false;
	}
	
	/**
	 * Test to check if a notify has been called when not waiting
	 * 
	 * @return
	 */
	protected synchronized boolean runMainLoop () {
		return reRunThread;
	}
	
	/**
	 * The main work loop, to be implemented by any child class. If the child wants
	 * to sleep before the next loop then they should return true.
	 */
	protected abstract boolean internalRun();
	
	public void run() {
		try {
			boolean sleep = false;

			while (isThreadStarted()) {
				try {
					synchronized (this) {		
						if(sleep && !runMainLoop()) wait(sleepTime);
						// Indicate that we are running the main loop
						setRanMainLoop();
					}
				} catch (InterruptedException e1) {
					log.debug("SandeshaThread was interupted...");
					log.debug(e1.getMessage());
					log.debug("End printing Interrupt...");
				}

				//pause if we have to
				doPauseIfNeeded();

				// Ensure we have context and a storage manager
				if (context == null) {
					String message = SandeshaMessageHelper
							.getMessage(SandeshaMessageKeys.configContextNotSet);
					message = SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.cannotCointinueSender, message);
					log.debug(message);
					throw new RuntimeException(message);
				}

				if(storageManager == null) {
					try {
						storageManager = SandeshaUtil.getSandeshaStorageManager(context, context.getAxisConfiguration());
					} catch (SandeshaException e2) {
						String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotCointinueSender, e2.toString());
						log.debug(message);
						throw new RuntimeException(message);
					}
				}

				// Call into the real function
				sleep = internalRun();
			}
		} finally {
			// flag that we have exited the run loop and notify any waiting
			// threads
			synchronized (this) {
				if(log.isDebugEnabled()) log.debug("SandeshaThread really stopping " + this);
				hasStoppedRunning = true;
				notifyAll();
			}
		}
	}
}
