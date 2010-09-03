package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Startup;

/**
 *
 */
public class StartupSerializationTest extends AbstractTestCase {

    public void testStartupSerializationSenarioOne() throws Exception {
        String inputXml = "<task class=\"org.apache.synapse.util.TestTask\" group=\"org\" " +
                "name=\"TestTask\" xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\">" +
                "<property name=\"name\" value=\"foo\"/>" +
                "<trigger interval=\"5\"/></task>";
        OMElement inputOM = createOMElement(inputXml);
        Startup startup = StartupFinder.getInstance().getStartup(inputOM);
        OMElement resultOM = StartupFinder.getInstance().serializeStartup(null, startup);
        assertTrue(compare(resultOM, inputOM));
    }

    public void testStartupSerializationSenarioTwo() throws Exception {
        String inputXml = "<task class=\"org.apache.synapse.util.TestTask\" group=\"org\" " +
                "name=\"TestTask\" xmlns=\"http://synapse.apache.org/ns/2010/04/configuration\">" +
                "<description>Test description</description>" +
                "<property name=\"name\" value=\"foo\"/>" +
                "<trigger interval=\"5\"/></task>";
        OMElement inputOM = createOMElement(inputXml);
        Startup startup = StartupFinder.getInstance().getStartup(inputOM);
        OMElement resultOM = StartupFinder.getInstance().serializeStartup(null, startup);
        assertTrue(compare(resultOM, inputOM));
    }
}
