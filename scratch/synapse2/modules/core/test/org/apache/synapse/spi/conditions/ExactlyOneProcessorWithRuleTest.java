package org.apache.synapse.spi.conditions;

import junit.framework.TestCase;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.Constants;
import org.apache.synapse.util.Axis2EnvSetup;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.axiom.om.OMElement;

/**
 * TestCase for SwitchProcessor
 */
public class ExactlyOneProcessorWithRuleTest extends TestCase {
    private MessageContext mc;
    private OMElement config;
    private SynapseEnvironment env;
    private String synapsexml1 =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<exactlyone>"+
                    "    <regex message-address=\"to\" pattern=\"http://xmethods..\\*\"/>\n" +
                    "    <xpath expr=\"//ns:text\" xmlns:ns=\"urn:text-body\">\n" +
                              "<engage-addressing-in/>"+
                    "    </xpath>"+
                    "</exactlyone>"+
                    "</synapse>";

    private String synapsexml2 =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<exactlyone>"+
                    "    <xpath expr=\"//ns:text_empty\" xmlns:ns=\"urn:text-body\"/>\n" +
                    "    <xpath expr=\"//ns:text_empty2\" xmlns:ns=\"urn:text-body\">\n" +
                              "<engage-addressing-in/>"+
                    "    </xpath>"+
                    "    <default> "+
                    "        <engage-addressing-in/>"+
                    "    </default> "+
                    "</exactlyone>"+
                    "</synapse>";



    public void testXathSwithProcessor() throws Exception {
            mc = Axis2EnvSetup
                    .axis2Deployment("target/synapse-repository");
            config = Axis2EnvSetup.getSynapseConfigElement(synapsexml1);
            env = new Axis2SynapseEnvironment(config,
                    Thread.currentThread().getContextClassLoader());
            SynapseMessage smc = new Axis2SynapseMessage(mc,env);
            env.injectMessage(smc);
            assertTrue(((Boolean) smc.getProperty(
                    Constants.MEDIATOR_RESPONSE_PROPERTY)).booleanValue());
        }

        public void testDefaultProcessor() throws Exception {
            mc = Axis2EnvSetup
                    .axis2Deployment("target/synapse-repository");
            config = Axis2EnvSetup.getSynapseConfigElement(synapsexml2);
            env = new Axis2SynapseEnvironment(config,
                    Thread.currentThread().getContextClassLoader());
            SynapseMessage smc = new Axis2SynapseMessage(mc,env);
            env.injectMessage(smc);
            assertTrue(((Boolean) smc.getProperty(
                    Constants.MEDIATOR_RESPONSE_PROPERTY)).booleanValue());
        }

}
