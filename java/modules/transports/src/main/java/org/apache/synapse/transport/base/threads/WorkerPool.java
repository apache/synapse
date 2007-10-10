package org.apache.synapse.transport.base.threads;

public interface WorkerPool {
    public void execute(Runnable task);
}
