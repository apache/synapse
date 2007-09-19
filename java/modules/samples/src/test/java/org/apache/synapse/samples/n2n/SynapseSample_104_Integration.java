package org.apache.synapse.samples.n2n;

import org.apache.synapse.SynapseConstants;
import samples.userguide.StockQuoteClient;

/**
 *
 */
public class SynapseSample_104_Integration extends AbstractAutomationTestCase {

    protected void setUp() throws Exception {
        System.setProperty(SynapseConstants.SYNAPSE_XML, SAMPLE_CONFIG_ROOT_PATH + "synapse_sample_104.xml");
        System.setProperty("addurl", SYNAPSE_BASE_URL + "soap/StockQuoteProxy");
        System.setProperty("symbol", "IBM");
        System.setProperty("mode", "quote");
        System.setProperty("wsrm", "true");
        super.setUp();
    }

    public void testSample() throws Exception {
        String resultString = getStringResultOfTest(StockQuoteClient.executeTestClient());
        assertXpathExists("ns:getQuoteResponse", resultString);
        assertXpathExists("ns:getQuoteResponse/ns:return", resultString);
    }
}
