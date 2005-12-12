package org.apache.synapse.mediators.builtin.axis2;

import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseEnvironmentTest;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseMessageTest;
import org.apache.synapse.processors.builtin.axis2.AddressingInProcessor;

import junit.framework.TestCase;

public class AddressingInProcessorTest extends TestCase {

    public void testAddressingInProcessor() throws Exception {
    	
    	SynapseEnvironment se = SynapseEnvironmentTest.createAxis2SynapseEnvironment();
    	SynapseMessage smNoAdd = SynapseMessageTest.createSampleSOAP11MessageWithoutAddressing();
    	AddressingInProcessor aip = new AddressingInProcessor();
    	
    	aip.process(se,smNoAdd);
    	assertTrue("to should be null if there is no addressing header", smNoAdd.getTo()==null);
    	
    	SynapseMessage smAdd = SynapseMessageTest.createSampleSOAP11MessageWithAddressing();
    	assertTrue("to should be the incoming addressing header", smAdd.getTo().getAddress().equals(SynapseMessageTest.URN_SAMPLE_TO_ADDRESS));
    	
    }
}