package org.apache.synapse.core.axis2;

public class SynapseCallbackStoreView implements SynapseCallbackStoreViewMBean {

    private SynapseCallbackReceiver receiver;

    public SynapseCallbackStoreView(SynapseCallbackReceiver receiver) {
        this.receiver = receiver;
    }

    public int getCallbackCount() {
        return receiver.getCallbackCount();
    }

    public String[] getPendingCallbacks() {
        return receiver.getPendingCallbacks();
    }
}
