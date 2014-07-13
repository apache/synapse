package org.apache.synapse.core.axis2;

/**
 * JMX MBean interface for monitoring the Synapse callback store.
 */
public interface SynapseCallbackStoreViewMBean {

    /**
     * Get the number of pending callbacks in Synapse callback store
     *
     * @return An integer
     */
    public int getCallbackCount();

    /**
     * Get the IDs (message IDs) of the pending callbacks in Synapse callback store
     *
     * @return An array of strings
     */
    public String[] getPendingCallbacks();

}
