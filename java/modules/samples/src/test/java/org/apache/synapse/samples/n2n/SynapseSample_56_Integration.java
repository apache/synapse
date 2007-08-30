package org.apache.synapse.samples.n2n;

import org.apache.synapse.Constants;
import samples.userguide.StockQuoteClient;

/**
 *
 */
public class SynapseSample_56_Integration extends AbstractAutomationTestCase {

    protected void setUp() throws Exception {
        System.setProperty(Constants.SYNAPSE_XML, SAMPLE_CONFIG_ROOT_PATH + "synapse_sample_56.xml");
        System.setProperty("addurl", SYNAPSE_BASE_URL);
        System.setProperty("symbol", "IBM");
        System.setProperty("mode", "quote");
        super.setUp();
    }

    public void testSample() throws Exception {
        String resultString = getStringResultOfTest(StockQuoteClient.executeTestClient());
        assertXpathExists("ns:getQuoteResponse", resultString);
        assertXpathExists("ns:getQuoteResponse/ns:return", resultString);
    }
}
