package org.apache.axis2.transport.base.threads;

public interface WorkerPool {
    public void execute(Runnable task);
}
