package org.apache.synapse.transport.base.threads;

/**
 * Dynamically select util.concurrent implemenation
 */
public class WorkerPoolFactory {

    public static WorkerPool getWorkerPool(int core, int max, int keepAlive,
        int queueLength, String threadGroupName, String threadGroupId) {
        try {
            Class.forName("java.util.concurrent.ThreadPoolExecutor");
            return new NativeWorkerPool(
                core, max, keepAlive, queueLength, threadGroupName, threadGroupId);
        } catch (ClassNotFoundException e) {
            return new BackportWorkerPool(
                core, max, keepAlive, queueLength, threadGroupName, threadGroupId);
        }
    }
}
