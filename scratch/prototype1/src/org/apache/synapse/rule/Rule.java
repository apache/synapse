package org.apache.synapse.rule;

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.mediator.Mediator;

import java.util.List;


public class Rule {

    private Mediator[] mediators;
    private AxisConfiguration axisConfig;
    private List qosModules;
    private String name;

    public Mediator[] getMediators() {
        return mediators;
    }

    public void setMediators(Mediator[] mediators) {

        this.mediators = mediators;
    }

    public AxisConfiguration getAxisConfig() {
        return axisConfig;
    }

    public void setAxisConfig(AxisConfiguration axisConfig) {
        this.axisConfig = axisConfig;
    }

    public List getQosModules() {
        return qosModules;
    }

    public void setQosModules(List qosModules) {
        this.qosModules = qosModules;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
