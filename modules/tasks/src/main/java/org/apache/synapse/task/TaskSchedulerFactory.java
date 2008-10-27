/**
 * To change this template use File | Settings | File Templates.
 */
package org.apache.synapse.task;


import java.util.HashMap;
import java.util.Map;

/**
 * Factory method for retrieve / create a TaskScheduler
 */
public class TaskSchedulerFactory {

    private final static TaskSchedulerFactory SCHEDULER_FACTORY = new TaskSchedulerFactory();

    private final static Map<String, TaskScheduler> MAP = new HashMap<String, TaskScheduler>();

    public static TaskSchedulerFactory getInstance() {
        return SCHEDULER_FACTORY;
    }

    private TaskSchedulerFactory() {
    }

    /**
     * Returns a TaskScheduler whose name is match with given name.
     * There is an only one instance of TaskScheduler for a given name as Factory caches
     *
     * @param name Name of the TaskScheduler
     * @return TaskScheduler instance
     */
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
