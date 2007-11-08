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

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WorkerLock {

  static final Log log = LogFactory.getLog(WorkerLock.class);
  private HashMap locks = new HashMap();
	
  public WorkerLock () {
  }
	
  public synchronized boolean addWork (String work, Object owner) {
	if(log.isDebugEnabled()) log.debug("Enter: WorkerLock::addWork " + work + ", " + owner);
    if(locks.containsKey(work)){
    	if(log.isDebugEnabled()) log.debug("Exit: WorkerLock::addWork " + false);
    	return false;
    }
    locks.put(work, owner);
    if(log.isDebugEnabled()) log.debug("Exit: WorkerLock::addWork " + true);
	return true;
  }
	
	public synchronized void removeWork (String work) {
		if(log.isDebugEnabled()) log.debug("Enter: WorkerLock::removeWork " + work);
		locks.remove(work);
		if(log.isDebugEnabled()) log.debug("Exit: WorkerLock::removeWork");
	}
	
	public synchronized boolean isWorkPresent (String work) {
	  if(log.isDebugEnabled()) log.debug("Enter: WorkerLock::isWorkPresent " + work);
	  boolean value = locks.containsKey(work);
	  if(log.isDebugEnabled()) log.debug("Exit: WorkerLock::isWorkPresent " + value);
	  return value;
	}
	
	 public synchronized boolean ownsLock(String work, Object owner) {
		if(log.isDebugEnabled()) log.debug("Enter: WorkerLock::ownsLock " + work + " ," + owner);
	    Object realOwner = locks.get(work);
	    if(log.isDebugEnabled()) log.debug("Exit: WorkerLock::ownsLock " + Boolean.valueOf(realOwner == owner));
	    return realOwner == owner;
	  }

}