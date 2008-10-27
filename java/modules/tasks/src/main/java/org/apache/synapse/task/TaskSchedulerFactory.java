/**
 * To change this template use File | Settings | File Templates.
 */
package org.apache.synapse.task;


import java.util.HashMap;
import java.util.Map;

public class TaskSchedulerFactory {

    private static TaskSchedulerFactory ourInstance = new TaskSchedulerFactory();

    private static final Map<String, TaskScheduler> MAP = new HashMap<String, TaskScheduler>();

    public static TaskSchedulerFactory getInstance() {
        return ourInstance;
    }

    private TaskSchedulerFactory() {
    }

    public TaskScheduler getTaskScheduler(String name) {

        if (name == null || "".equals(name)) {
            throw new SynapseTaskException("Name cannot be found.");
        }

        TaskScheduler taskScheduler = MAP.get(name);
        if (taskScheduler == null) {
            taskScheduler = new TaskScheduler(name);
            MAP.put(name, taskScheduler);
        }

        return taskScheduler;
    }
}
