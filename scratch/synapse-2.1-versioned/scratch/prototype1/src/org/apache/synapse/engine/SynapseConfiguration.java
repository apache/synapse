package org.apache.synapse.engine;

import org.apache.axis2.om.OMElement;

/**
 *
 */
public class SynapseConfiguration {
    private OMElement incomingPreStageRuleSet;
    private OMElement incomingProcessingStageRuleSet;
    private OMElement incomingPostStageRuleSet;
    private OMElement outgoingPreStageRuleSet;
    private OMElement outgoingProcessingStageRuleSet;
    private OMElement outgoingPostStageRuleSet;

    public OMElement getIncomingPreStageRuleSet() {
        return incomingPreStageRuleSet;
    }

    public void setIncomingPreStageRuleSet(
            OMElement incomingPreStageRuleSet) {
        this.incomingPreStageRuleSet = incomingPreStageRuleSet;
    }

    public OMElement getIncomingProcessingStageRuleSet() {
        return incomingProcessingStageRuleSet;
    }

    public void setIncomingProcessingStageRuleSet(OMElement incomingProcessingStageRuleSet) {
        this.incomingProcessingStageRuleSet = incomingProcessingStageRuleSet;
    }

    public OMElement getIncomingPostStageRuleSet() {
        return incomingPostStageRuleSet;
    }

    public void setIncomingPostStageRuleSet(
            OMElement incomingPostStageRuleSet) {
        this.incomingPostStageRuleSet = incomingPostStageRuleSet;
    }

    public OMElement getOutgoingPreStageRuleSet() {
        return outgoingPreStageRuleSet;
    }

    public void setOutgoingPreStageRuleSet(
            OMElement outgoingPreStageRuleSet) {
        this.outgoingPreStageRuleSet = outgoingPreStageRuleSet;
    }

    public OMElement getOutgoingProcessingStageRuleSet() {
        return outgoingProcessingStageRuleSet;
    }

    public void setOutgoingProcessingStageRuleSet(OMElement outgoingProcessingStageRuleSet) {
        this.outgoingProcessingStageRuleSet = outgoingProcessingStageRuleSet;
    }

    public OMElement getOutgoingPostStageRuleSet() {
        return outgoingPostStageRuleSet;
    }

    public void setOutgoingPostStageRuleSet(
            OMElement outgoingPostStageRuleSet) {
        this.outgoingPostStageRuleSet = outgoingPostStageRuleSet;
    }
}
