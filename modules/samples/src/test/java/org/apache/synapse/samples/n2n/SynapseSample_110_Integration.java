package org.apache.synapse.samples.n2n;

import org.apache.synapse.Constants;

/**
 *
 */
public class SynapseSample_110_Integration extends AbstractAutomationTestCase {

    protected void setUp() throws Exception {
        System.setProperty(Constants.SYNAPSE_XML, SAMPLE_CONFIG_ROOT_PATH + "synapse_sample_110.xml");
//        System.setProperty("addurl", SYNAPSE_BASE_URL + "soap/StockQuoteProxy");
//        System.setProperty("symbol", "IBM");
        // todo : setup the JMS
        super.setUp();
    }

    public void testSample() throws Exception {
//        String resultString = getStringResultOfTest(StockQuoteClient.executeTestClient());
//        assertXpathExists("ns:getQuoteResponse", resultString);
//        assertXpathExists("ns:getQuoteResponse/ns:return", resultString);
    }
}
