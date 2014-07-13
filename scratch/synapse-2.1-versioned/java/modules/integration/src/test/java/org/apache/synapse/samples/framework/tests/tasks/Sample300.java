package org.apache.synapse.samples.framework.tests.tasks;

import org.apache.synapse.samples.framework.SynapseTestCase;

public class Sample300 extends SynapseTestCase {

    public Sample300() {
        super(300);
    }

    public void testScheduledTask() throws Exception {
        log.info("Waiting 10 seconds for the task to run...");
        Thread.sleep(10000);
        int messageCount = getAxis2Server().getMessageCount("SimpleStockQuoteService", "getQuote");
        log.info("Task sent " + messageCount + " messages.");
        assertTrue(messageCount >= 2);
    }
}
