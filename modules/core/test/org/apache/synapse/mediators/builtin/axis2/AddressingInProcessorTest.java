package org.apache.synapse.mediators.builtin.axis2;

import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.TestSynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.TestSynapseMessage;


import junit.framework.TestCase;

public class AddressingInProcessorTest extends TestCase {

    public void testAddressingInProcessor() throws Exception {

        SynapseEnvironment se = TestSynapseEnvironment.createAxis2SynapseEnvironment();
        SynapseMessage smNoAdd = TestSynapseMessage.createSampleSOAP11MessageWithoutAddressing("target/synapse-repository");

        AddressingInMediator aip = new AddressingInMediator();

        aip.mediate(smNoAdd);
        assertTrue("to should be null if there is no addressing header", smNoAdd.getTo() == null);

        SynapseMessage smAdd = TestSynapseMessage.createSampleSOAP11MessageWithAddressing("target/synapse-repository");
        aip.mediate(smAdd);
        assertTrue("to should be the incoming addressing header",
                smAdd.getTo().getAddress().equals(TestSynapseMessage.URN_SAMPLE_TO_ADDRESS));

    }
}