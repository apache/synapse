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

    public <C extends AsyncChannel<?>> void addSOAP11Test(C channel, AsyncMessageSender<? super C,XMLMessage> sender, ContentTypeMode contentTypeMode, MessageTestData data) {
        addTest(new XMLAsyncMessageTestCase<C>(channel, sender, XMLMessageType.SOAP11, "SOAP11", contentTypeMode, SOAP11Constants.SOAP_11_CONTENT_TYPE, data));
    }
    
    public <C extends AsyncChannel<?>> void addSOAP12Test(C channel, AsyncMessageSender<? super C,XMLMessage> sender, ContentTypeMode contentTypeMode, MessageTestData data) {
        addTest(new XMLAsyncMessageTestCase<C>(channel, sender, XMLMessageType.SOAP12, "SOAP12", contentTypeMode, SOAP12Constants.SOAP_12_CONTENT_TYPE, data));
    }
    
    public <C extends AsyncChannel<?>> void addSOAPTests(C channel, AsyncMessageSender<? super C,XMLMessage> sender, ContentTypeMode contentTypeMode) {
        for (MessageTestData data : messageTestData) {
            addSOAP11Test(channel, sender, contentTypeMode, data);
            addSOAP12Test(channel, sender, contentTypeMode, data);
        }
    }
    
    public <C extends AsyncChannel<?>> void addPOXTest(C channel, AsyncMessageSender<? super C,XMLMessage> sender, ContentTypeMode contentTypeMode, MessageTestData data) {
        addTest(new XMLAsyncMessageTestCase<C>(channel, sender, XMLMessageType.POX, "POX", contentTypeMode, "application/xml", data));
    }
    
    public <C extends AsyncChannel<?>> void addPOXTests(C channel, AsyncMessageSender<? super C,XMLMessage> sender, ContentTypeMode contentTypeMode) {
        for (MessageTestData data : messageTestData) {
            addPOXTest(channel, sender, contentTypeMode, data);
        }
    }
    
    public <C extends RequestResponseChannel<?>> void addPOXTest(C channel, XMLRequestResponseMessageSender<? super C> sender, ContentTypeMode contentTypeMode, MessageTestData data) {
        addTest(new XMLRequestResponseMessageTestCase<C>(channel, sender, "POXEcho", contentTypeMode, "application/xml", XMLMessageType.POX, data));
    }
    
    public <C extends RequestResponseChannel<?>> void addPOXTests(C channel, XMLRequestResponseMessageSender<? super C> sender, ContentTypeMode contentTypeMode) {
        for (MessageTestData data : messageTestData) {
            addPOXTest(channel, sender, contentTypeMode, data);
        }
    }
    
    // TODO: this test actually only makes sense if the transport supports a Content-Type header
    public <C extends AsyncChannel<?>> void addSwATests(C channel, AsyncMessageSender<? super C,ByteArrayMessage> sender) {
        addTest(new AsyncMessageTestCase<C,ByteArrayMessage>(channel, sender, "SOAPWithAttachments", ContentTypeMode.TRANSPORT, null, null) {
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
            protected ByteArrayMessage prepareMessage() throws Exception {
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
                return new ByteArrayMessage(outputFormat.getContentTypeForSwA(SOAP12Constants.SOAP_12_CONTENT_TYPE), baos.toByteArray());
            }

            @Override
            protected void checkMessageData(ByteArrayMessage message, MessageData messageData) throws Exception {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Attachments attachments = messageData.getAttachments();
                DataHandler dataHandler = attachments.getDataHandler(contentID);
                assertNotNull(dataHandler);
                dataHandler.writeTo(baos);
                assertTrue(Arrays.equals(attachmentContent, baos.toByteArray()));
            }
        });
    }
    
    public <C extends AsyncChannel<?>> void addTextPlainTest(C channel, AsyncMessageSender<? super C,ByteArrayMessage> sender, ContentTypeMode contentTypeMode, final MessageTestData data) {
        addTest(new AsyncMessageTestCase<C,ByteArrayMessage>(channel, sender, "TextPlain", contentTypeMode, "text/plain; charset=\"" + data.getCharset() + "\"", data.getCharset()) {
            @Override
            protected void buildName(NameBuilder name) {
                super.buildName(name);
                data.buildName(name);
            }
            
            @Override
            protected ByteArrayMessage prepareMessage() throws Exception {
                return new ByteArrayMessage(contentType, data.getText().getBytes(data.getCharset()));
            }

            @Override
            protected void checkMessageData(ByteArrayMessage message, MessageData messageData) throws Exception {
                SOAPEnvelope envelope = messageData.getEnvelope();
                OMElement wrapper = envelope.getBody().getFirstElement();
                assertEquals(BaseConstants.DEFAULT_TEXT_WRAPPER, wrapper.getQName());
                assertEquals(data.getText(), wrapper.getText());
            }
        });
    }
    
    public <C extends AsyncChannel<?>> void addTextPlainTests(C channel, AsyncMessageSender<? super C,ByteArrayMessage> sender, ContentTypeMode contentTypeMode) {
        for (MessageTestData data : messageTestData) {
            addTextPlainTest(channel, sender, contentTypeMode, data);
        }
    }
    
    public <C extends AsyncChannel<?>> void addBinaryTest(C channel, AsyncMessageSender<? super C,ByteArrayMessage> sender, ContentTypeMode contentTypeMode) {
        addTest(new AsyncMessageTestCase<C,ByteArrayMessage>(channel, sender, "Binary", contentTypeMode, "application/octet-stream", null) {
            @Override
            protected ByteArrayMessage prepareMessage() throws Exception {
                byte[] content = new byte[8192];
                random.nextBytes(content);
                return new ByteArrayMessage("application/octet-stream", content);
            }

            @Override
            protected void checkMessageData(ByteArrayMessage message, MessageData messageData) throws Exception {
                SOAPEnvelope envelope = messageData.getEnvelope();
                OMElement wrapper = envelope.getBody().getFirstElement();
                assertEquals(BaseConstants.DEFAULT_BINARY_WRAPPER, wrapper.getQName());
                OMNode child = wrapper.getFirstOMChild();
                assertTrue(child instanceof OMText);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ((DataHandler)((OMText)child).getDataHandler()).writeTo(baos);
                assertTrue(Arrays.equals(message.getContent(), baos.toByteArray()));
            }
        });
    }

    public <C extends AsyncChannel<?>> void addRESTTests(C channel, AsyncMessageSender<? super C,RESTMessage> sender) {
        addTest(new AsyncMessageTestCase<C,RESTMessage>(channel, sender, "REST", ContentTypeMode.TRANSPORT, null, null) {
            @Override
            protected RESTMessage prepareMessage() throws Exception {
                return new RESTMessage();
            }

            @Override
            protected void checkMessageData(RESTMessage message, MessageData messageData) throws Exception {
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
            for (Enumeration<?> e = tests(); e.hasMoreElements(); ) {
                tests.add((Test)e.nextElement());
            }
            while (!tests.isEmpty()) {
                if (result.shouldStop()) {
                    return;
                }
                Test test = tests.removeFirst();
                if (test instanceof AsyncMessageTestCase) {
                    AsyncMessageTestCase<?,?> listenerTest = (AsyncMessageTestCase<?,?>)test;
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
                        if (test instanceof AsyncMessageTestCase) {
                            listenerTest = (AsyncMessageTestCase<?,?>)test;
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
