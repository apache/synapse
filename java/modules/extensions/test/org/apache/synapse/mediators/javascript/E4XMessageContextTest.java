package org.apache.synapse.mediators.javascript;

import java.io.ByteArrayInputStream;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPProcessingException;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class E4XMessageContextTest extends TestCase {

    E4XMessageContext e4xMC;

    static final String SQ_REQUEST = "<x:getQuote xmlns:x=\"urn:xmethods-delayed-quotes\">  <Symbol>IBM</Symbol>  </x:getQuote>";

    public void testGetEnvelopeXML() throws AxisFault, SOAPProcessingException {
        Scriptable xml = e4xMC.getEnvelopeXML();
        assertNotNull(xml);
    }

    public void testGetPayloadXML() throws AxisFault, SOAPProcessingException {
        Scriptable xml = e4xMC.getPayloadXML();
        assertNotNull(xml);
    }

    public void testEnvelopeType() throws AxisFault, SOAPProcessingException {
        JavaScriptMediator mediator = new JavaScriptMediator();
        mediator.setScript("function mediate(mc) { return 'xml' == (typeof mc.getEnvelopeXML());}");
        assertTrue(mediator.mediate(e4xMC));
    }

    public void testPayloadType() throws AxisFault, SOAPProcessingException {
        JavaScriptMediator mediator = new JavaScriptMediator();
        mediator.setScript("function mediate(mc) { return 'xml' == (typeof mc.getPayloadXML());}");
        assertTrue(mediator.mediate(e4xMC));
    }

    public void testPayloadContents() throws AxisFault, SOAPProcessingException {
        JavaScriptMediator mediator = new JavaScriptMediator();
        mediator.setScript("function mediate(mc) { return 'IBM' == mc.getPayloadXML().Symbol;}");
        assertTrue(mediator.mediate(e4xMC));
    }

    public void testUpdatePayload() throws AxisFault, SOAPProcessingException {
        JavaScriptMediator mediator = new JavaScriptMediator();
        String script = "function mediate(mc) {" 
            + " var xml = mc.getPayloadXML();" 
            + " xml.Symbol = 'WSO2';" 
            + " mc.setPayloadXML(xml);"
            + "return true;}";
        mediator.setScript(script);

        assertTrue(mediator.mediate(e4xMC));

        SOAPEnvelope env = e4xMC.getEnvelope();
        OMElement omEl = env.getBody().getFirstElement().getFirstElement();
        assertEquals("<Symbol>WSO2</Symbol>", omEl.toString());
    }

    // ----- end of tests -----

    public void setUp() throws AxisFault, SOAPProcessingException, XMLStreamException {
        MessageContext mc = createMockMessageContext();
        Context cx = Context.enter();
        ScriptableObject scope;
        try {
            scope = cx.initStandardObjects(null, true);
        } finally {
            Context.exit();
        }
        this.e4xMC = new E4XMessageContext(mc, scope);
        this.e4xMC.setEnvelope(OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope());

        byte[] xmlBytes = SQ_REQUEST.getBytes();
        StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(xmlBytes));
        OMElement omElement = builder.getDocumentElement();
        this.e4xMC.getEnvelope().getBody().setFirstChild(omElement);
    }

    protected MessageContext createMockMessageContext() {
        MessageContext mc = new MessageContext() {

            public SynapseConfiguration getConfiguration() {

                return null;
            }

            public void setConfiguration(SynapseConfiguration cfg) {

            }

            public SynapseEnvironment getEnvironment() {

                return null;
            }

            public void setEnvironment(SynapseEnvironment se) {

            }

            public Object getProperty(String key) {

                return null;
            }

            public void setProperty(String key, Object value) {

            }

            public Set getPropertyKeySet() {

                return null;
            }

            SOAPEnvelope env;

            public SOAPEnvelope getEnvelope() {

                return env;
            }

            public void setEnvelope(SOAPEnvelope envelope) throws AxisFault {
                this.env = envelope;

            }

            public EndpointReference getFaultTo() {

                return null;
            }

            public void setFaultTo(EndpointReference reference) {

            }

            public EndpointReference getFrom() {

                return null;
            }

            public void setFrom(EndpointReference reference) {

            }

            public String getMessageID() {

                return null;
            }

            public void setMessageID(String string) {

            }

            public RelatesTo getRelatesTo() {

                return null;
            }

            public void setRelatesTo(RelatesTo[] reference) {

            }

            public EndpointReference getReplyTo() {

                return null;
            }

            public void setReplyTo(EndpointReference reference) {

            }

            public EndpointReference getTo() {

                return null;
            }

            public void setTo(EndpointReference reference) {

            }

            public void setWSAAction(String actionURI) {

            }

            public String getWSAAction() {

                return null;
            }

            public String getSoapAction() {

                return null;
            }

            public void setSoapAction(String string) {

            }

            public void setMessageId(String messageID) {

            }

            public String getMessageId() {

                return null;
            }

            public boolean isDoingMTOM() {

                return false;
            }

            public void setDoingMTOM(boolean b) {

            }

            public boolean isDoingREST() {

                return false;
            }

            public void setDoingREST(boolean b) {

            }

            public boolean isSOAP11() {

                return false;
            }

            public void setResponse(boolean b) {

            }

            public boolean isResponse() {

                return false;
            }

            public void setFaultResponse(boolean b) {

            }

            public boolean isFaultResponse() {

                return false;
            }
        };
        return mc;
    }

}
