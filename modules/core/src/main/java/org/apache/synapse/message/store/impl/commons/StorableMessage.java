package org.apache.synapse.message.store.impl.commons;

import java.io.Serializable;

/**
 * This represents the final message that will be saved in the storage queue.
 */
public class StorableMessage implements Serializable {
    private static final int PRIORITY_UNSET = -999;

    private Axis2Message axis2message;

    private SynapseMessage synapseMessage;

    private int priority = PRIORITY_UNSET;

    public Axis2Message getAxis2message() {
        return axis2message;
    }

    public void setAxis2message(Axis2Message axis2message) {
        this.axis2message = axis2message;
    }

    public SynapseMessage getSynapseMessage() {
        return synapseMessage;
    }

    public void setSynapseMessage(SynapseMessage synapseMessage) {
        this.synapseMessage = synapseMessage;
    }

    public int getPriority(int defaultValue) {
        if (priority == PRIORITY_UNSET) {
            return defaultValue;
        }
        return priority;
    }

    /**
     * @Depricated
     * @return
     */
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
