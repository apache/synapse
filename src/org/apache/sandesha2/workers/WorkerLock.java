package org.apache.sandesha2.workers;

import java.util.ArrayList;

public class WorkerLock {

	public ArrayList workList = null;
	
	public WorkerLock () {
		workList = new ArrayList ();
	}
	
	public synchronized void addWork (String work) {
		workList.add(work);
	}
	
	public synchronized void removeWork (String work) {
		workList.remove(work);
	}
	
	public synchronized boolean isWorkPresent (String work) {
		return workList.contains(work);
	}

}
