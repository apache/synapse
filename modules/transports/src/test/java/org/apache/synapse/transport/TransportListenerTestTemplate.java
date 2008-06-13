/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.transport;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMText;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOnlyAxisOperation;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.DispatchPhase;
import org.apache.synapse.transport.base.BaseConstants;

/**
 * Base class for standard transport listener tests.
 * This test case verifies that the transport listener is able to process various
 * types of messages and to hand them over to the Axis2 engine. Subclasses only
 * need to provide
 * <ul>
 *   <li>the {@link TransportInDescription} object for the transport;</li>
 *   <li>the parameters necessary to configure a service to receive messages of
 *       a given content type through the transport;</li>
 *   <li>the logic to send a message with a given content type to the listener.</li>
 * </ul>
 * The test sets up a server environment and intercepts the received messages by
 * configuring a service with a custom message receiver.
 */
public abstract class TransportListenerTestTemplate extends TestCase {
    private static final String testString = "\u00e0 peine arriv\u00e9s nous entr\u00e2mes dans sa chambre";
    
    private SOAPEnvelope runTest(String contentType, byte[] content) throws Exception {
        UtilsTransportServer server = new UtilsTransportServer();
        
        TransportInDescription trpInDesc = createTransportInDescription();
        server.addTransport(trpInDesc);
        
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        
        // Add a DefaultOperationDispatcher to the InFlow phase. This is necessary because
        // we want to receive all messages through the same operation.
        DispatchPhase dispatchPhase = null;
        for (Object phase : axisConfiguration.getInFlowPhases()) {
            if (phase instanceof DispatchPhase) {
                dispatchPhase = (DispatchPhase)phase;
                break;
            }
        }
        DefaultOperationDispatcher dispatcher = new DefaultOperationDispatcher();
        dispatcher.initDispatcher();
        dispatchPhase.addHandler(dispatcher);
        
        // Set up a test service with a default operation backed by a mock message
        // receiver. The service is configured using the parameters specified by the
        // implementation.
        AxisService service = new AxisService("TestService");
        AxisOperation operation = new InOnlyAxisOperation(DefaultOperationDispatcher.DEFAULT_OPERATION_NAME);
        MockMessageReceiver messageReceiver = new MockMessageReceiver();
        operation.setMessageReceiver(messageReceiver);
        service.addOperation(operation);
        List<Parameter> parameters = getServiceParameters(contentType);
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                service.addParameter(parameter);
            }
        }
        axisConfiguration.addService(service);
        
        // Run the test.
        beforeStartup();
        server.start();
        Thread.sleep(100); // TODO: this is required for the NIO transport; check whether this is a bug
        try {
            EndpointReference[] endpointReferences
                = trpInDesc.getReceiver().getEPRsForService(service.getName(), "localhost");
            sendMessage(endpointReferences != null && endpointReferences.length > 0
                                ? endpointReferences[0].getAddress() : null,
                        contentType, content);
            SOAPEnvelope envelope = messageReceiver.waitForMessage(8, TimeUnit.SECONDS);
            if (envelope == null) {
                fail("Failed to get message");
            }
            return envelope;
        }
        finally {
            server.stop();
            Thread.sleep(100); // TODO: this is required for the NIO transport; check whether this is a bug
        }
    }
    
    private void testSOAP11(String text, String charset) throws Exception {
        SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
        SOAPEnvelope orgEnvelope = factory.createSOAPEnvelope();
        SOAPBody orgBody = factory.createSOAPBody();
        OMElement orgElement = factory.createOMElement(new QName("root"));
        orgElement.setText(text);
        orgBody.addChild(orgElement);
        orgEnvelope.addChild(orgBody);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OMOutputFormat outputFormat = new OMOutputFormat();
        outputFormat.setCharSetEncoding(charset);
        outputFormat.setIgnoreXMLDeclaration(true);
        orgEnvelope.serializeAndConsume(baos, outputFormat);
        SOAPEnvelope envelope = runTest("text/xml; charset=\"" + charset + "\"", baos.toByteArray());
        OMElement element = envelope.getBody().getFirstElement();
        assertEquals(orgElement.getQName(), element.getQName());
        assertEquals(text, element.getText());
    }
    
    public void testSOAP11ASCII() throws Exception {
        testSOAP11("test string", "us-ascii");
    }
    
    public void testSOAP11UTF8() throws Exception {
        testSOAP11(testString, "UTF-8");
    }
    
    public void testSOAP11Latin1() throws Exception {
        testSOAP11(testString, "ISO-8859-1");
    }
    
    private void testTextPlain(String text, String charset) throws Exception {
        SOAPEnvelope envelope = runTest("text/plain; charset=" + charset, text.getBytes(charset));
        OMElement wrapper = envelope.getBody().getFirstElement();
        assertEquals(BaseConstants.DEFAULT_TEXT_WRAPPER, wrapper.getQName());
        assertEquals(text, wrapper.getText());
    }
    
    public void testTextPlainASCII() throws Exception {
        testTextPlain("test string", "us-ascii");
    }
    
    public void testTextPlainUTF8() throws Exception {
        testTextPlain(testString, "UTF-8");
    }
    
    public void testTextPlainLatin1() throws Exception {
        testTextPlain(testString, "ISO-8859-1");
    }
    
    public void testBinary() throws Exception {
        Random random = new Random();
        byte[] content = new byte[8192];
        random.nextBytes(content);
        SOAPEnvelope envelope = runTest("application/octet-stream", content);
        OMElement wrapper = envelope.getBody().getFirstElement();
        assertEquals(BaseConstants.DEFAULT_BINARY_WRAPPER, wrapper.getQName());
        OMNode child = wrapper.getFirstOMChild();
        assertTrue(child instanceof OMText);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ((DataHandler)((OMText)child).getDataHandler()).writeTo(baos);
        assertTrue(Arrays.equals(content, baos.toByteArray()));
    }
    
    /**
     * Create a TransportInDescription for the transport under test.
     * 
     * @return the transport description
     * @throws Exception
     */
    protected abstract TransportInDescription createTransportInDescription() throws Exception;
    
    /**
     * Carry out initialization before server startup. This method is called
     * immediately before the test server is started and can be used by subclasses
     * to set up the test environment.
     * 
     * @throws Exception
     */
    protected void beforeStartup() throws Exception {
    }
    
    /**
     * Get the parameters necessary to configure a service to receive messages of
     * a given content type through the transport under test.
     * 
     * @param contentType the content type
     * @return a list of service parameters
     * @throws Exception
     */
    protected List<Parameter> getServiceParameters(String contentType) throws Exception {
        return null;
    }
    
    /**
     * Send a message to the transport listener. It is not recommended to use the
     * corresponding transport sender to achieve this. Instead the implementation
     * should use protocol specific libraries or APIs.
     * 
     * @param endpointReference the endpoint reference of the service
     * @param contentType the content type of the message
     * @param content the content of the message
     * @throws Exception
     */
    protected abstract void sendMessage(String endpointReference, String contentType, byte[] content) throws Exception;
}
