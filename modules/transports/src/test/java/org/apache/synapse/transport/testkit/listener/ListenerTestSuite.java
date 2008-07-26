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

package org.apache.synapse.transport.testkit.listener;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestResult;
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
import org.apache.synapse.transport.base.BaseConstants;

public class ListenerTestSuite extends TestSuite {
    public static final String testString = "\u00e0 peine arriv\u00e9s nous entr\u00e2mes dans sa chambre";
    
    public static final MessageTestData ASCII_TEST_DATA = new MessageTestData("ASCII", "test string", "us-ascii");
    public static final MessageTestData UTF8_TEST_DATA = new MessageTestData("UTF8", testString, "UTF-8");
    public static final MessageTestData LATIN1_TEST_DATA = new MessageTestData("Latin1", testString, "ISO-8859-1");
    
    private static final MessageTestData[] messageTestData = new MessageTestData[] {
        ASCII_TEST_DATA,
        UTF8_TEST_DATA,
        LATIN1_TEST_DATA,
    };
    
    private static final Random random = new Random();
    
    private final boolean reuseServer;
    
    public ListenerTestSuite(boolean reuseServer) {
        this.reuseServer = reuseServer;
    }
    
    public ListenerTestSuite() {
        this(true);
    }

    public void addSOAP11Test(ListenerTestSetup strategy, XMLMessageSender sender, ContentTypeMode contentTypeMode, MessageTestData data) {
        addTest(new SOAPTestCase(strategy, sender, "SOAP11" + data.getName(), contentTypeMode, SOAP11Constants.SOAP_11_CONTENT_TYPE, data) {
            @Override
            protected SOAPFactory getOMFactory() {
                return OMAbstractFactory.getSOAP11Factory();
            }
        });
    }
    
    public void addSOAP12Test(ListenerTestSetup strategy, XMLMessageSender sender, ContentTypeMode contentTypeMode, MessageTestData data) {
        addTest(new SOAPTestCase(strategy, sender, "SOAP12" + data.getName(), contentTypeMode, SOAP12Constants.SOAP_12_CONTENT_TYPE, data) {
            @Override
            protected SOAPFactory getOMFactory() {
                return OMAbstractFactory.getSOAP12Factory();
            }
        });
    }
    
    public void addSOAPTests(ListenerTestSetup strategy, XMLMessageSender sender, ContentTypeMode contentTypeMode) {
        for (MessageTestData data : messageTestData) {
            addSOAP11Test(strategy, sender, contentTypeMode, data);
            addSOAP12Test(strategy, sender, contentTypeMode, data);
        }
    }
    
    public void addPOXTest(ListenerTestSetup strategy, XMLMessageSender sender, ContentTypeMode contentTypeMode, MessageTestData data) {
        addTest(new XMLMessageTestCase(strategy, sender, "POX" + data.getName(), contentTypeMode, "application/xml", data) {
            @Override
            protected OMFactory getOMFactory() {
                return OMAbstractFactory.getOMFactory();
            }

            @Override
            protected OMElement getMessage(OMElement payload) {
                return payload;
            }
        });
    }
    
    public void addPOXTests(ListenerTestSetup strategy, XMLMessageSender sender, ContentTypeMode contentTypeMode) {
        for (MessageTestData data : messageTestData) {
            addPOXTest(strategy, sender, contentTypeMode, data);
        }
    }
    
    // TODO: this test actually only makes sense if the transport supports a Content-Type header
    public void addSwATests(final ListenerTestSetup setup, final MessageSender sender) {
        addTest(new ListenerTestCase(setup, "SOAPWithAttachments", ContentTypeMode.TRANSPORT, null) {
            private byte[] attachmentContent;
            private String contentID;
            
            @Override
            protected void setUp() throws Exception {
                super.setUp();
                attachmentContent = new byte[8192];
                random.nextBytes(attachmentContent);
                contentID = UUIDGenerator.getUUID();
            }

            @Override
            protected void sendMessage(String endpointReference, String contentType) throws Exception {
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
                sender.sendMessage(setup, endpointReference, outputFormat.getContentTypeForSwA(SOAP12Constants.SOAP_12_CONTENT_TYPE), baos.toByteArray());
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
    
    public void addTextPlainTest(ListenerTestSetup strategy, final MessageSender sender, ContentTypeMode contentTypeMode, final MessageTestData data) {
        addTest(new ListenerTestCase(strategy, "TextPlain" + data.getName(), contentTypeMode, "text/plain; charset=\"" + data.getCharset() + "\"") {
            @Override
            protected void sendMessage(String endpointReference, String contentType) throws Exception {
                sender.sendMessage(getSetup(), endpointReference, contentType, data.getText().getBytes(data.getCharset()));
            }
            
            @Override
            protected void checkMessageData(MessageData messageData) throws Exception {
                SOAPEnvelope envelope = messageData.getEnvelope();
                OMElement wrapper = envelope.getBody().getFirstElement();
                assertEquals(BaseConstants.DEFAULT_TEXT_WRAPPER, wrapper.getQName());
                assertEquals(data.getText(), wrapper.getText());
            }
        });
    }
    
    public void addTextPlainTests(ListenerTestSetup setup, MessageSender sender, ContentTypeMode contentTypeMode) {
        for (MessageTestData data : messageTestData) {
            addTextPlainTest(setup, sender, contentTypeMode, data);
        }
    }
    
    public void addBinaryTest(final ListenerTestSetup setup, final MessageSender sender, ContentTypeMode contentTypeMode) {
        addTest(new ListenerTestCase(setup, "Binary", contentTypeMode, "application/octet-stream") {
            private byte[] content;
            
            @Override
            protected void setUp() throws Exception {
                super.setUp();
                content = new byte[8192];
                random.nextBytes(content);
            }

            @Override
            protected void sendMessage(String endpointReference, String contentType) throws Exception {
                sender.sendMessage(setup, endpointReference, contentType, content);
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

    @Override
    public void run(TestResult result) {
        if (!reuseServer) {
            super.run(result);
        } else {
            LinkedList<Test> tests = new LinkedList<Test>();
            for (Enumeration e = tests(); e.hasMoreElements(); ) {
                tests.add((Test)e.nextElement());
            }
            while (!tests.isEmpty()) {
                if (result.shouldStop()) {
                    return;
                }
                Test test = tests.removeFirst();
                if (test instanceof ListenerTestCase) {
                    ListenerTestCase listenerTest = (ListenerTestCase)test;
                    ListenerTestSetup setup = listenerTest.getSetup();
                    ListenerTestServer server;
                    try {
                        server = new ListenerTestServer(setup);
                        setup.beforeStartup();
                        server.start();
                    } catch (Throwable t) {
                        result.addError(this, t);
                        return;
                    }
                    listenerTest.setServer(server);
                    runTest(test, result);
                    for (Iterator<Test> it = tests.iterator(); it.hasNext(); ) {
                        if (result.shouldStop()) {
                            return;
                        }
                        test = it.next();
                        if (test instanceof ListenerTestCase) {
                            listenerTest = (ListenerTestCase)test;
                            if (listenerTest.getSetup() == setup) {
                                it.remove();
                                listenerTest.setServer(server);
                                runTest(test, result);
                            }
                        }
                    }
                    try {
                        server.stop();
                        Thread.sleep(100); // TODO: this is required for the NIO transport; check whether this is a bug
                    } catch (Throwable t) {
                        result.addError(this, t);
                        return;
                    }
                } else {
                    runTest(test, result);
                }
            }
        }
    }
}
