package org.apache.synapse.transport.base.threads;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.emory.mathcs.backport.java.util.concurrent.*;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class to support the backport util.concurrent in JDK 1.4 and the
 * native concurrent package in JDK 1.5 or later
 */
public class BackportWorkerPool implements WorkerPool {

    private static final Log log = LogFactory.getLog(BackportWorkerPool.class);

    java.util.concurrent.Executor nativeExecutor = null;
    Executor executor = null;

    public BackportWorkerPool(int core, int max, int keepAlive,
        int queueLength, String threadGroupName, String threadGroupId) {

        if (log.isDebugEnabled()) {
            log.debug("Using backport of the util.concurrent package..");
        }
        executor = new ThreadPoolExecutor(
            core, max, keepAlive,
            TimeUnit.SECONDS,
            queueLength == -1 ?
                new LinkedBlockingQueue() :
                new LinkedBlockingQueue(queueLength),
            new BackportWorkerPool.BackportThreadFactory(new ThreadGroup(threadGroupName), threadGroupId));
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }

    /**
     * This is a simple ThreadFactory implementation using java.util.concurrent
     * Creates threads with the given name prefix
     */
    public class BackportThreadFactory implements
        ThreadFactory {

        final ThreadGroup group;
        final AtomicInteger count;
        final String namePrefix;

        public BackportThreadFactory(final ThreadGroup group, final String namePrefix) {
            super();
            this.count = new AtomicInteger(1);
            this.group = group;
            this.namePrefix = namePrefix;
        }

        public Thread newThread(final Runnable runnable) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(this.namePrefix);
            buffer.append('-');
            buffer.append(this.count.getAndIncrement());
            Thread t = new Thread(group, runnable, buffer.toString(), 0);
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }

    }
}
