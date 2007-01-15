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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.util.threadpool.ThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Aggregates pause and stop logic between sender and invoker threads.
 */
public abstract class SandeshaThread extends Thread{

	private static final Log log = LogFactory.getLog(SandeshaThread.class);

	private boolean runThread = false;
	private boolean hasStoppedRunning = false;
	private boolean hasPausedRunning = false;
	private boolean pauseRequired = false;
	
	private int sleepTime;
  private WorkerLock lock = null;

	private ArrayList workingSequences = new ArrayList();
	
	protected transient ThreadFactory threadPool;
	protected ConfigurationContext context = null;
	private boolean reRunThread;

	public SandeshaThread(int sleepTime) {
		this.sleepTime = sleepTime;
  	lock = new WorkerLock ();
	}
	
	public final WorkerLock getWorkerLock() {
		return lock;
	}
	
	public synchronized void stopThreadForSequence(String sequenceID){
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaThread::stopThreadForSequence, " + sequenceID);
		
		workingSequences.remove(sequenceID);
		if (workingSequences.size() == 0) 
			runThread = false;		
		
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
		
	  //we can now request a pause - the next pause will be ours
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
			log.debug("Enter: SandeshaThread::stopRunning");

		//NOTE: we do not take acount of pausing when stopping.
		//The call to stop will wait until the invoker has exited the loop
		if (isThreadStarted()) {
			// the invoker is started so stop it
			runThread = false;
			// wait for it to finish
			while (!hasStoppedRunning()) {
				try {
					wait(sleepTime);
				} catch (InterruptedException e1) {
					//ignore
				}
			}
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaThread::stopRunning");
	}
	
	public synchronized boolean isThreadStarted() {

		if (!runThread && log.isDebugEnabled())
			log.debug("SandeshaThread not started");	

		return runThread;
	}
	

	public synchronized void runThreadForSequence(ConfigurationContext context, String sequenceID){
		if (!workingSequences.contains(sequenceID))
			workingSequences.add(sequenceID);
		if (!isThreadStarted()) {
			this.context = context;
			// Get the axis2 thread pool
			threadPool = context.getThreadPool();
			
			runThread = true; // so that isStarted()=true.
			super.start();
		}		
	}
	
	public synchronized ArrayList getSequences() {
		return workingSequences;
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
			notify();
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
	 * The main work loop, to be implemented by any child class.
	 */
	protected abstract void internalRun();
	
	public void run() {
		try {
			internalRun();
		} finally {
			// flag that we have exited the run loop and notify any waiting
			// threads
			synchronized (this) {
				hasStoppedRunning = true;
				notify();
			}
		}
	}
}
