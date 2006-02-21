package org.apache.synapse.mediators;

import org.apache.synapse.api.Mediator;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseException;
import org.apache.axis2.om.OMElement;
import junit.framework.TestCase;

/**
 * Simple resource handling mediator
 */
public class SimpleResourceAquireMediator implements Mediator, EnvironmentAware {

    private SynapseEnvironment se = null;
    public void setSynapseEnvironment(SynapseEnvironment se) {
        this.se = se;
    }

    public void setClassLoader(ClassLoader cl) {
        throw new SynapseException("no class loader available for <classmediator/>");
    }

    public boolean mediate(SynapseMessage smc) {
        OMElement resourcesElement = se.get("http://127.0.0.1:8090/axis2/services/npe/simple_resources");
        // test for resources being not null
        TestCase.assertNotNull(resourcesElement);
        return true;
    }
}
