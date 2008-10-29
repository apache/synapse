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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.util.LoggingControl;

public class WorkerLock {

  static final Log log = LogFactory.getLog(WorkerLock.class);
  private ConcurrentHashMap locks = new ConcurrentHashMap();
  
  public WorkerLock () {
  }

  
  	private static class Holder {
		CountDownLatch latch = new CountDownLatch(1);

		Object value;

		public Holder(Object newValue) {
			value = newValue;
		}

		public void awaitRelease() throws InterruptedException {
			latch.await();
		}

		public void release() {
			latch.countDown();
		}

		public Object getValue() {
			return value;
		}
	}

	public boolean addWork(String work, Object owner) {
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Enter: WorkerLock::addWork " + work + ", " + owner);
    	Holder h = new Holder(owner);
		Object prev = locks.putIfAbsent(work, h);
		boolean result = (prev == null);
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Exit: WorkerLock::addWork " + result);
		return result;
	}

	public void awaitRemoval(String work) throws InterruptedException {
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Enter: WorkerLock::awaitRemoval " + work);
		Holder h = (Holder) locks.get(work);
		if (h != null) {
			h.awaitRelease();
		}
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Exit: WorkerLock::awaitRemoval");
	}

	public void removeWork(String work) {
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Enter: WorkerLock::removeWork " + work);
		Holder h = (Holder) locks.remove(work);
		if (h != null){
			h.release();
		}
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Exit: WorkerLock::removeWork");
	}

	public boolean isWorkPresent(String work) {

		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Enter: WorkerLock::isWorkPresent " + work);
		boolean value = locks.containsKey(work);
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Exit: WorkerLock::isWorkPresent " + value);
		return value;
	}

	public boolean ownsLock(String work, Object owner) {
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Enter: WorkerLock::ownsLock " + work + " ," + owner);
		Holder h = (Holder) locks.get(work);
		Object realOwner = (h != null ? h.getValue() : null);
		if (LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled())
			log.debug("Exit: WorkerLock::ownsLock " + Boolean.valueOf(realOwner == owner));
		return realOwner == owner;
	}
}
