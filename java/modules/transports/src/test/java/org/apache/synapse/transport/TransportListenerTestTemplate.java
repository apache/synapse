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
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.MIMEOutputUtils;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axiom.soap.SOAP11Constants;
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
    
    public static abstract class TransportListenerTestCase extends TestCase {
        protected final TestStrategy strategy;
        protected final String contentType;
        
        public TransportListenerTestCase(TestStrategy strategy, String baseName, String contentType) {
            super(strategy.getTestName(baseName));
            this.strategy = strategy;
            this.contentType = contentType;
        }

        @Override
        protected void runTest() throws Throwable {
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
            MessageData messageData;
            try {
                EndpointReference[] endpointReferences
                    = trpInDesc.getReceiver().getEPRsForService(service.getName(), "localhost");
                sendMessage(endpointReferences != null && endpointReferences.length > 0
                                        ? endpointReferences[0].getAddress() : null);
                messageData = messageReceiver.waitForMessage(8, TimeUnit.SECONDS);
                if (messageData == null) {
                    fail("Failed to get message");
                }
            }
            finally {
                server.stop();
                Thread.sleep(100); // TODO: this is required for the NIO transport; check whether this is a bug
            }
            checkMessageData(messageData);
        }
        
        protected abstract void sendMessage(String endpointReference) throws Exception;
        protected abstract void checkMessageData(MessageData messageData) throws Exception;
    }
    
    public static abstract class XMLMessageTestCase extends TransportListenerTestCase {
        private final String text;
        private final String charset;
        private OMElement orgElement;
        protected OMFactory factory;
        
        public XMLMessageTestCase(TestStrategy strategy, String baseName, String baseContentType, String text, String charset) {
            super(strategy, baseName, baseContentType + "; charset=\"" + charset + "\"");
            this.text = text;
            this.charset = charset;
        }
        
        @Override
        protected void setUp() throws Exception {
            factory = getOMFactory();
            orgElement = factory.createOMElement(new QName("root"));
            orgElement.setText(text);
        }
        
        @Override
        protected void checkMessageData(MessageData messageData) throws Exception {
            SOAPEnvelope envelope = messageData.getEnvelope();
            OMElement element = envelope.getBody().getFirstElement();
            assertEquals(orgElement.getQName(), element.getQName());
            assertEquals(text, element.getText());
        }

        @Override
        protected void sendMessage(String endpointReference) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OMOutputFormat outputFormat = new OMOutputFormat();
            outputFormat.setCharSetEncoding(charset);
            outputFormat.setIgnoreXMLDeclaration(true);
            getMessage(orgElement).serializeAndConsume(baos, outputFormat);
            strategy.sendMessage(endpointReference, contentType, baos.toByteArray());
        }
        
        protected abstract OMFactory getOMFactory();
        protected abstract OMElement getMessage(OMElement payload);
    }
    
    public static abstract class SOAPTestCase extends XMLMessageTestCase {
        public SOAPTestCase(TestStrategy strategy, String baseName, String baseContentType, String text, String charset) {
            super(strategy, baseName, baseContentType, text, charset);
        }

        @Override
        protected abstract SOAPFactory getOMFactory();

        @Override
        protected OMElement getMessage(OMElement payload) {
            SOAPEnvelope envelope = ((SOAPFactory)factory).createSOAPEnvelope();
            SOAPBody body = ((SOAPFactory)factory).createSOAPBody();
            body.addChild(payload);
            envelope.addChild(body);
            return envelope;
        }
    }
    
    public static class SOAP11TestCaseImpl extends SOAPTestCase {
        public SOAP11TestCaseImpl(TestStrategy strategy, String baseName, String text, String charset) {
            super(strategy, baseName, SOAP11Constants.SOAP_11_CONTENT_TYPE, text, charset);
        }

        @Override
        protected SOAPFactory getOMFactory() {
            return OMAbstractFactory.getSOAP11Factory();
        }
    }
    
    public static class SOAP12TestCaseImpl extends SOAPTestCase {
        public SOAP12TestCaseImpl(TestStrategy strategy, String baseName, String text, String charset) {
            super(strategy, baseName, SOAP12Constants.SOAP_12_CONTENT_TYPE, text, charset);
        }

        @Override
        protected SOAPFactory getOMFactory() {
            return OMAbstractFactory.getSOAP12Factory();
        }
    }
    
    public static class POXTestCaseImpl extends XMLMessageTestCase {
        public POXTestCaseImpl(TestStrategy strategy, String baseName, String text, String charset) {
            super(strategy, baseName, "application/xml", text, charset);
        }

        @Override
        protected OMFactory getOMFactory() {
            return OMAbstractFactory.getOMFactory();
        }

        @Override
        protected OMElement getMessage(OMElement payload) {
            return payload;
        }
    }
    
    public static class TextPlainTestCaseImpl extends TransportListenerTestCase {
        private final String text;
        private final String charset;

        public TextPlainTestCaseImpl(TestStrategy strategy, String baseName, String text, String charset) {
            super(strategy, baseName, "text/plain; charset=\"" + charset + "\"");
            this.text = text;
            this.charset = charset;
        }

        @Override
        protected void sendMessage(String endpointReference) throws Exception {
            strategy.sendMessage(endpointReference, contentType, text.getBytes(charset));
        }
        
        @Override
        protected void checkMessageData(MessageData messageData) throws Exception {
            SOAPEnvelope envelope = messageData.getEnvelope();
            OMElement wrapper = envelope.getBody().getFirstElement();
            assertEquals(BaseConstants.DEFAULT_TEXT_WRAPPER, wrapper.getQName());
            assertEquals(text, wrapper.getText());
        }
    }
    
    public static final String testString = "\u00e0 peine arriv\u00e9s nous entr\u00e2mes dans sa chambre";
    
    private static final Random random = new Random();

    public static void addSOAPTests(final TestStrategy strategy, TestSuite suite) {
        suite.addTest(new SOAP11TestCaseImpl(strategy, "SOAP11ASCII", "test string", "us-ascii"));
        suite.addTest(new SOAP11TestCaseImpl(strategy, "SOAP11UTF8", testString, "UTF-8"));
        suite.addTest(new SOAP11TestCaseImpl(strategy, "SOAP11Latin1", testString, "ISO-8859-1"));
        suite.addTest(new SOAP12TestCaseImpl(strategy, "SOAP12ASCII", "test string", "us-ascii"));
        suite.addTest(new SOAP12TestCaseImpl(strategy, "SOAP12UTF8", testString, "UTF-8"));
        suite.addTest(new SOAP12TestCaseImpl(strategy, "SOAP12Latin1", testString, "ISO-8859-1"));
    }
    
    public static void addPOXTests(final TestStrategy strategy, TestSuite suite) {
        suite.addTest(new POXTestCaseImpl(strategy, "POXASCII", "test string", "us-ascii"));
        suite.addTest(new POXTestCaseImpl(strategy, "POXUTF8", testString, "UTF-8"));
        suite.addTest(new POXTestCaseImpl(strategy, "POXLatin1", testString, "ISO-8859-1"));
    }
    
    // TODO: this test actually only makes sense if the transport supports a Content-Type header
    public static void addSwATests(TestStrategy strategy, TestSuite suite) {
        suite.addTest(new TransportListenerTestCase(strategy, "SOAPWithAttachments", null) {
            private byte[] attachmentContent;
            private String contentID;
            
            @Override
            protected void setUp() throws Exception {
                attachmentContent = new byte[8192];
                random.nextBytes(attachmentContent);
                contentID = UUIDGenerator.getUUID();
            }

            @Override
            protected void sendMessage(String endpointReference) throws Exception {
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
                Attachments orgAttachments = new Attachments();
                orgAttachments.addDataHandler(contentID, new DataHandler(new ByteArrayDataSource(attachmentContent, "application/octet-stream")));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                MIMEOutputUtils.writeSOAPWithAttachmentsMessage(writer, baos, orgAttachments, outputFormat);
                strategy.sendMessage(endpointReference, outputFormat.getContentTypeForSwA(SOAP12Constants.SOAP_12_CONTENT_TYPE), baos.toByteArray());
            }

            @Override
            protected void checkMessageData(MessageData messageData) throws Exception {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Attachments attachments = messageData.getAttachments();
                DataHandler dataHandler = attachments.getDataHandler(contentID);
                assertNotNull(dataHandler);
                dataHandler.writeTo(baos);
                assertTrue(Arrays.equals(attachmentContent, baos.toByteArray()));
            }
        });
    }
    
    public static void addTextPlainTests(final TestStrategy strategy, TestSuite suite) {
        suite.addTest(new TextPlainTestCaseImpl(strategy, "TextPlainASCII", "test string", "us-ascii"));
        suite.addTest(new TextPlainTestCaseImpl(strategy, "TextPlainUTF8", testString, "UTF-8"));
        suite.addTest(new TextPlainTestCaseImpl(strategy, "TextPlainLatin1", testString, "ISO-8859-1"));
    }
    
    public static void addBinaryTest(final TestStrategy strategy, TestSuite suite) {
        suite.addTest(new TransportListenerTestCase(strategy, "Binary", "application/octet-stream") {
            private byte[] content;
            
            @Override
            protected void setUp() throws Exception {
                content = new byte[8192];
                random.nextBytes(content);
            }

            @Override
            protected void sendMessage(String endpointReference) throws Exception {
                strategy.sendMessage(endpointReference, contentType, content);
            }
            
            @Override
            protected void checkMessageData(MessageData messageData) throws Exception {
                SOAPEnvelope envelope = messageData.getEnvelope();
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
