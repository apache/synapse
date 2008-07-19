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
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.MIMEOutputUtils;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axiom.soap.SOAP12Constants;
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
    public static abstract class TestStrategy {
        private final String name;
        
        public TestStrategy() {
            name = null;
        }
        
        public TestStrategy(String name) {
            this.name = name;
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
        
        public String getTestName(String baseName) {
            if (name == null) {
                return "test" + baseName;
            } else {
                return "test" + name + baseName;
            }
        }
    }
    
    private static final String testString = "\u00e0 peine arriv\u00e9s nous entr\u00e2mes dans sa chambre";
    
    private static final Random random = new Random();

    private static MessageData runTest(TestStrategy strategy, String contentType, byte[] content) throws Exception {
        UtilsTransportServer server = new UtilsTransportServer();
        
        TransportInDescription trpInDesc = strategy.createTransportInDescription();
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
        List<Parameter> parameters = strategy.getServiceParameters(contentType);
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                service.addParameter(parameter);
            }
        }
        axisConfiguration.addService(service);
        
        // Run the test.
        strategy.beforeStartup();
        server.start();
        try {
            EndpointReference[] endpointReferences
                = trpInDesc.getReceiver().getEPRsForService(service.getName(), "localhost");
            strategy.sendMessage(endpointReferences != null && endpointReferences.length > 0
                                    ? endpointReferences[0].getAddress() : null,
                                            contentType, content);
            MessageData messageData = messageReceiver.waitForMessage(8, TimeUnit.SECONDS);
            if (messageData == null) {
                fail("Failed to get message");
            }
            return messageData;
        }
        finally {
            server.stop();
            Thread.sleep(100); // TODO: this is required for the NIO transport; check whether this is a bug
        }
    }
    
    private static void testSOAP11(TestStrategy strategy, String text, String charset) throws Exception {
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
        SOAPEnvelope envelope = runTest(strategy, "text/xml; charset=\"" + charset + "\"", baos.toByteArray()).getEnvelope();
        OMElement element = envelope.getBody().getFirstElement();
        assertEquals(orgElement.getQName(), element.getQName());
        assertEquals(text, element.getText());
    }
    
    public static void addSOAP11Tests(final TestStrategy strategy, TestSuite suite) {
        suite.addTest(new TestCase(strategy.getTestName("SOAP11ASCII")) {
            @Override
            protected void runTest() throws Throwable {
                testSOAP11(strategy, "test string", "us-ascii");
            }
        });
        suite.addTest(new TestCase(strategy.getTestName("SOAP11UTF8")) {
            @Override
            protected void runTest() throws Throwable {
                testSOAP11(strategy, testString, "UTF-8");
            }
        });
        suite.addTest(new TestCase(strategy.getTestName("SOAP11Latin1")) {
            @Override
            protected void runTest() throws Throwable {
                testSOAP11(strategy, testString, "ISO-8859-1");
            }
        });
    }
    
    // TODO: this test actually only makes sense if the transport supports a Content-Type header
    public static void addSwATests(final TestStrategy strategy, TestSuite suite) {
        suite.addTest(new TestCase(strategy.getTestName("SOAPWithAttachments")) {
            @Override
            protected void runTest() throws Throwable {
                SOAPFactory factory = OMAbstractFactory.getSOAP12Factory();
                SOAPEnvelope orgEnvelope = factory.createSOAPEnvelope();
                SOAPBody orgBody = factory.createSOAPBody();
                OMElement orgElement = factory.createOMElement(new QName("root"));
                orgBody.addChild(orgElement);
                orgEnvelope.addChild(orgBody);
                OMOutputFormat outputFormat = new OMOutputFormat();
                outputFormat.setCharSetEncoding("UTF-8");
                outputFormat.setIgnoreXMLDeclaration(true);
                StringWriter writer = new StringWriter();
                orgEnvelope.serializeAndConsume(writer);
                byte[] attachmentContent = new byte[8192];
                random.nextBytes(attachmentContent);
                String contentID = UUIDGenerator.getUUID();
                Attachments orgAttachments = new Attachments();
                orgAttachments.addDataHandler(contentID, new DataHandler(new ByteArrayDataSource(attachmentContent, "application/octet-stream")));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                MIMEOutputUtils.writeSOAPWithAttachmentsMessage(writer, baos, orgAttachments, outputFormat);
                MessageData messageData = TransportListenerTestTemplate.runTest(strategy, outputFormat.getContentTypeForSwA(SOAP12Constants.SOAP_12_CONTENT_TYPE), baos.toByteArray());
                Attachments attachments = messageData.getAttachments();
                DataHandler dataHandler = attachments.getDataHandler(contentID);
                assertNotNull(dataHandler);
                baos.reset();
                dataHandler.writeTo(baos);
                assertTrue(Arrays.equals(attachmentContent, baos.toByteArray()));
            }
        });
    }
    
    private static void testTextPlain(TestStrategy strategy, String text, String charset) throws Exception {
        SOAPEnvelope envelope = runTest(strategy, "text/plain; charset=" + charset, text.getBytes(charset)).getEnvelope();
        OMElement wrapper = envelope.getBody().getFirstElement();
        assertEquals(BaseConstants.DEFAULT_TEXT_WRAPPER, wrapper.getQName());
        assertEquals(text, wrapper.getText());
    }
    
    public static void addTextPlainTests(final TestStrategy strategy, TestSuite suite) {
        suite.addTest(new TestCase(strategy.getTestName("TextPlainASCII")) {
            @Override
            public void runTest() throws Exception {
                testTextPlain(strategy, "test string", "us-ascii");
            }
        });
        suite.addTest(new TestCase(strategy.getTestName("TextPlainUTF8")) {
            @Override
            public void runTest() throws Exception {
                testTextPlain(strategy, testString, "UTF-8");
            }
        });
        suite.addTest(new TestCase(strategy.getTestName("TextPlainLatin1")) {
            @Override
            public void runTest() throws Exception {
                testTextPlain(strategy, testString, "ISO-8859-1");
            }
        });
    }
    
    public static void addBinaryTest(final TestStrategy strategy, TestSuite suite) {
        suite.addTest(new TestCase(strategy.getTestName("Binary")) {
            @Override
            public void runTest() throws Exception {
                byte[] content = new byte[8192];
                random.nextBytes(content);
                SOAPEnvelope envelope = TransportListenerTestTemplate.runTest(strategy, "application/octet-stream", content).getEnvelope();
                OMElement wrapper = envelope.getBody().getFirstElement();
                assertEquals(BaseConstants.DEFAULT_BINARY_WRAPPER, wrapper.getQName());
                OMNode child = wrapper.getFirstOMChild();
                assertTrue(child instanceof OMText);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ((DataHandler)((OMText)child).getDataHandler()).writeTo(baos);
                assertTrue(Arrays.equals(content, baos.toByteArray()));
            }
        });
    }
}
