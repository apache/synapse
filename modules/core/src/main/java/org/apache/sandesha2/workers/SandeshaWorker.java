package org.apache.sandesha2.workers;

public class SandeshaWorker {
	WorkerLock lock = null;
	String workId = null;
	
	public WorkerLock getLock() {
		return lock;
	}
	public void setLock(WorkerLock lock) {
		this.lock = lock;
	}
	public String getWorkId() {
		return workId;
	}
	public void setWorkId(String workId) {
		this.workId = workId;
	}
	

	
	
}
