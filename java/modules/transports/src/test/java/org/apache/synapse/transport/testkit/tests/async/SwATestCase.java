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

package org.apache.synapse.transport.testkit.tests.async;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Random;

import javax.activation.DataHandler;
import javax.mail.internet.ContentType;
import javax.xml.namespace.QName;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.impl.MIMEOutputUtils;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.message.AxisMessage;
import org.apache.synapse.transport.testkit.message.ByteArrayMessage;
import org.apache.synapse.transport.testkit.name.Name;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;

@Name("AsyncSwA")
public class SwATestCase extends AsyncMessageTestCase<ByteArrayMessage,AxisMessage> {
    private static final Random random = new Random();
    
    private byte[] attachmentContent;
    private String contentID;
    
    public SwATestCase(AsyncChannel channel, AsyncTestClient<ByteArrayMessage> client, AsyncEndpointFactory<AxisMessage> endpointFactory, Object... resources) {
        super(channel, client, endpointFactory, ContentTypeMode.TRANSPORT, null, null, resources);
    }
    
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
        return new ByteArrayMessage(new ContentType(outputFormat.getContentTypeForSwA(SOAP12Constants.SOAP_12_CONTENT_TYPE)), baos.toByteArray());
    }

    @Override
    protected void checkMessageData(ByteArrayMessage message, AxisMessage messageData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Attachments attachments = messageData.getAttachments();
        DataHandler dataHandler = attachments.getDataHandler(contentID);
        assertNotNull(dataHandler);
        dataHandler.writeTo(baos);
        assertTrue(Arrays.equals(attachmentContent, baos.toByteArray()));
    }
}
