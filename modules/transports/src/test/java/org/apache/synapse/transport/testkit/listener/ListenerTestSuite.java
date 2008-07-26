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
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
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
import org.apache.commons.io.IOUtils;
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

    public void addSOAP11Test(Channel<?> channel, XMLMessageSender sender, ContentTypeMode contentTypeMode, MessageTestData data) {
        addTest(new SOAPTestCase(channel, sender, "SOAP11", contentTypeMode, SOAP11Constants.SOAP_11_CONTENT_TYPE, data) {
            @Override
            protected SOAPFactory getOMFactory() {
                return OMAbstractFactory.getSOAP11Factory();
            }
        });
    }
    
    public void addSOAP12Test(Channel<?> channel, XMLMessageSender sender, ContentTypeMode contentTypeMode, MessageTestData data) {
        addTest(new SOAPTestCase(channel, sender, "SOAP12", contentTypeMode, SOAP12Constants.SOAP_12_CONTENT_TYPE, data) {
            @Override
            protected SOAPFactory getOMFactory() {
                return OMAbstractFactory.getSOAP12Factory();
            }
        });
    }
    
    public void addSOAPTests(Channel<?> channel, XMLMessageSender sender, ContentTypeMode contentTypeMode) {
        for (MessageTestData data : messageTestData) {
            addSOAP11Test(channel, sender, contentTypeMode, data);
            addSOAP12Test(channel, sender, contentTypeMode, data);
        }
    }
    
    public void addPOXTest(Channel<?> channel, XMLMessageSender sender, ContentTypeMode contentTypeMode, MessageTestData data) {
        addTest(new XMLMessageTestCase(channel, sender, "POX", contentTypeMode, "application/xml", data) {
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
    
    public void addPOXTests(Channel<?> channel, XMLMessageSender sender, ContentTypeMode contentTypeMode) {
        for (MessageTestData data : messageTestData) {
            addPOXTest(channel, sender, contentTypeMode, data);
        }
    }
    
    // TODO: this test actually only makes sense if the transport supports a Content-Type header
    public void addSwATests(Channel<?> channel, BinaryPayloadSender sender) {
        addTest(new ListenerTestCase<BinaryPayloadSender>(channel, sender, "SOAPWithAttachments", ContentTypeMode.TRANSPORT, null) {
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
            protected void sendMessage(BinaryPayloadSender sender, String endpointReference, String contentType) throws Exception {
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
                sender.sendMessage(getChannel(), endpointReference, outputFormat.getContentTypeForSwA(SOAP12Constants.SOAP_12_CONTENT_TYPE), baos.toByteArray());
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
    
    public void addTextPlainTest(Channel<?> channel, BinaryPayloadSender sender, ContentTypeMode contentTypeMode, final MessageTestData data) {
        addTest(new ListenerTestCase<BinaryPayloadSender>(channel, sender, "TextPlain", contentTypeMode, "text/plain; charset=\"" + data.getCharset() + "\"") {
            @Override
            protected void buildName(NameBuilder name) {
                super.buildName(name);
                data.buildName(name);
            }
            
            @Override
            protected void sendMessage(BinaryPayloadSender sender, String endpointReference, String contentType) throws Exception {
                sender.sendMessage(getChannel(), endpointReference, contentType, data.getText().getBytes(data.getCharset()));
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
    
    public void addTextPlainTests(Channel<?> channel, BinaryPayloadSender sender, ContentTypeMode contentTypeMode) {
        for (MessageTestData data : messageTestData) {
            addTextPlainTest(channel, sender, contentTypeMode, data);
        }
    }
    
    public void addBinaryTest(Channel<?> channel, BinaryPayloadSender sender, ContentTypeMode contentTypeMode) {
        addTest(new ListenerTestCase<BinaryPayloadSender>(channel, sender, "Binary", contentTypeMode, "application/octet-stream") {
            private byte[] content;
            
            @Override
            protected void setUp() throws Exception {
                super.setUp();
                content = new byte[8192];
                random.nextBytes(content);
            }

            @Override
            protected void sendMessage(BinaryPayloadSender sender, String endpointReference, String contentType) throws Exception {
                sender.sendMessage(getChannel(), endpointReference, contentType, content);
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

    public void addRESTTests(Channel<?> channel, RESTSender sender) {
        addTest(new ListenerTestCase<RESTSender>(channel, sender, "REST", ContentTypeMode.TRANSPORT, null) {
            @Override
            protected void sendMessage(RESTSender sender, String endpointReference, String contentType) throws Exception {
                sender.sendMessage(endpointReference);
            }
        
            @Override
            protected void checkMessageData(MessageData messageData) throws Exception {
                // TODO
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
                    ListenerTestCase<?> listenerTest = (ListenerTestCase<?>)test;
                    Channel<?> channel = listenerTest.getChannel();
                    ListenerTestServer server;
                    try {
                        server = new ListenerTestServer(channel);
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
                            listenerTest = (ListenerTestCase<?>)test;
                            if (listenerTest.getChannel() == channel) {
                                it.remove();
                                listenerTest.setServer(server);
                                runTest(test, result);
                            }
                        }
                    }
                    try {
                        server.stop();
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
