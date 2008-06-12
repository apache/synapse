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

package org.apache.synapse.transport.mail;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.AbstractTransportTest;
import org.apache.synapse.transport.base.BaseConstants;

public class MailEchoRawXMLTest extends AbstractTransportTest {

    private static final Log log = LogFactory.getLog(MailEchoRawXMLTest.class);

    private static final String KOREAN_TEXT = "\uc5ec\ubcf4\uc138\uc694 \uc138\uacc4!";
    private static final String KOREAN_CHARSET = "ISO-2022-KR";

    private Properties props = new Properties();
    private String username = "synapse.test.0";
    private String password = "mailpassword";

    public MailEchoRawXMLTest() {
        //Logger.getLogger("org.apache.synapse.transport.mail").setLevel(Level.TRACE);
        server = new UtilsMailServer();

        props.put("mail.smtp.class", TestTransport.class.getName());
        
        props.put("mail.pop3.host", "pop.gmail.com");
        props.put("mail.pop3.port", "995");
        props.put("mail.pop3.user", "synapse.test.0");

        props.put("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.pop3.socketFactory.fallback", "false");
        props.put("mail.pop3.socketFactory.port", "995");

        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.user", "synapse.test.1");
        props.put("mail.smtp.password", "mailpassword");
        props.put("mail.smtp.auth", "true");
    }

    private void assertPOXEchoResponse(String textValue, Object reply) throws Exception {
        if (reply != null && reply instanceof String) {
            log.debug("Result Body : " + reply);
            XMLStreamReader reader = StAXUtils.createXMLStreamReader(new StringReader((String) reply));
            OMElement res = new StAXOMBuilder(reader).getDocumentElement();
            assertEchoResponse(textValue, res);
        } else {
            fail("Did not receive the reply mail");
        }
    }
    
    private void assertSOAPEchoResponse(String textValue, Object reply) throws Exception {
        if (reply != null && reply instanceof String) {
            log.debug("Result Body : " + reply);
            XMLStreamReader reader = StAXUtils.createXMLStreamReader(new StringReader((String) reply));
            assertSOAPEchoResponse(textValue, reader);
        } else {
            fail("Did not receive the reply mail");
        }
    }

    public void testRoundTripPOX() throws Exception {

        String msgId = UUIDGenerator.getUUID();

        Session session = Session.getInstance(props, new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("synapse.test.1", "mailpassword");    
            }
        });
        session.setDebug(log.isTraceEnabled());
        
        WSMimeMessage msg = new WSMimeMessage(session);
        msg.setFrom(new InternetAddress("synapse.test.0"));
        msg.setReplyTo(InternetAddress.parse("synapse.test.0"));
        InternetAddress[] address = {new InternetAddress("synapse.test.6")};
        msg.setRecipients(Message.RecipientType.TO, address);
        msg.setSubject("POX Roundtrip");
        msg.setHeader(BaseConstants.SOAPACTION, Constants.AXIS2_NAMESPACE_URI + "/echoOMElement");
        msg.setSentDate(new Date());
        msg.setHeader(MailConstants.MAIL_HEADER_MESSAGE_ID, msgId);
        msg.setHeader(MailConstants.MAIL_HEADER_X_MESSAGE_ID, msgId);
        msg.setText(createPayload().toString());
        Transport.send(msg);

        assertPOXEchoResponse("omTextValue", waitForReply(msgId));
    }

    public void testRoundTripMultiPart() throws Exception {

        String msgId = UUIDGenerator.getUUID();
        Options options = new Options();
        options.setTo(new EndpointReference("mailto:synapse.test.6"));
        options.setReplyTo(new EndpointReference("mailto:synapse.test.0"));
        options.setAction(Constants.AXIS2_NAMESPACE_URI + "/echoOMElement");
        options.setMessageId(msgId);

        options.setProperty(MailConstants.TRANSPORT_MAIL_FORMAT, MailConstants.TRANSPORT_FORMAT_MP);

        ServiceClient sender = new ServiceClient(getClientCfgCtx(), null);
        sender.setOptions(options);
        sender.fireAndForget(createPayload());

        assertSOAPEchoResponse("omTextValue", waitForReply(msgId));
    }

    public void testRoundTripMultiPartKorean() throws Exception {

        String msgId = UUIDGenerator.getUUID();
        Options options = new Options();
        options.setTo(new EndpointReference("mailto:synapse.test.6"));
        options.setReplyTo(new EndpointReference("mailto:synapse.test.0"));
        options.setAction(Constants.AXIS2_NAMESPACE_URI + "/echoOMElement");
        options.setMessageId(msgId);

        options.setProperty(MailConstants.TRANSPORT_MAIL_FORMAT, MailConstants.TRANSPORT_FORMAT_MP);

        ServiceClient sender = new ServiceClient(getClientCfgCtx(), null);
        sender.setOptions(options);
        sender.fireAndForget(createPayload(KOREAN_TEXT));

        assertSOAPEchoResponse(KOREAN_TEXT, waitForReply(msgId));
    }

    public void testRoundTripPOPDefaultCharsetSOAP12() throws Exception {

        String msgId = UUIDGenerator.getUUID();
        Options options = new Options();
        options.setTo(new EndpointReference("mailto:synapse.test.6"));
        options.setReplyTo(new EndpointReference("mailto:synapse.test.0"));
        options.setAction(Constants.AXIS2_NAMESPACE_URI + "/echoOMElement");
        options.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
        options.setMessageId(msgId);

        ServiceClient sender = new ServiceClient(getClientCfgCtx(), null);
        sender.setOptions(options);
        sender.fireAndForget(createPayload());

        assertSOAPEchoResponse("omTextValue", waitForReply(msgId));
    }

    public void testRoundTripIMAPUTF8Charset() throws Exception {

        String msgId = UUIDGenerator.getUUID();
        Options options = new Options();
        options.setTo(new EndpointReference("mailto:synapse.test.7"));
        options.setReplyTo(new EndpointReference("mailto:synapse.test.0"));
        options.setAction(Constants.AXIS2_NAMESPACE_URI + "/echoOMElement");
        options.setMessageId(msgId);

        ServiceClient sender = new ServiceClient(getClientCfgCtx(), null);
        sender.setOptions(options);
        sender.fireAndForget(createPayload(KOREAN_TEXT));

        assertSOAPEchoResponse(KOREAN_TEXT, waitForReply(msgId));
    }

    public void testRoundTripIMAPKoreanCharset() throws Exception {

        String msgId = UUIDGenerator.getUUID();
        Options options = new Options();
        options.setTo(new EndpointReference("mailto:synapse.test.7"));
        options.setReplyTo(new EndpointReference("mailto:synapse.test.0"));
        options.setAction(Constants.AXIS2_NAMESPACE_URI + "/echoOMElement");
        options.setMessageId(msgId);
        options.setProperty(Constants.Configuration. CHARACTER_SET_ENCODING, KOREAN_CHARSET);

        ServiceClient sender = new ServiceClient(getClientCfgCtx(), null);
        sender.setOptions(options);
        sender.fireAndForget(createPayload(KOREAN_TEXT));

        assertSOAPEchoResponse(KOREAN_TEXT, waitForReply(msgId));
    }

    private Object getMessage(String requestMsgId) {
        Session session = Session.getInstance(props, null);
        session.setDebug(log.isTraceEnabled());
        Store store = null;

        try {
            store = session.getStore("test-store");
            store.connect(username, password);
            Folder folder = store.getFolder(MailConstants.DEFAULT_FOLDER);
            folder.open(Folder.READ_WRITE);
            Message[] msgs = folder.getMessages();
            log.debug(msgs.length + " replies in reply mailbox");
            for (Message m:msgs) {
                String[] inReplyTo = m.getHeader(MailConstants.MAIL_HEADER_IN_REPLY_TO);
                log.debug("Got reply to : " + Arrays.toString(inReplyTo));
                if (inReplyTo != null && inReplyTo.length > 0) {
                    for (int j=0; j<inReplyTo.length; j++) {
                        if (requestMsgId.equals(inReplyTo[j])) {
                            m.setFlag(Flags.Flag.DELETED, true);
                            return m.getContent();
                        }
                    }
                }
                m.setFlag(Flags.Flag.DELETED, true);
            }
            folder.close(true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {    
            if (store != null) {
                try {
                    store.close();
                } catch (MessagingException ignore) {}
                store = null;
            }
        }
        return null;
    }
    
    private Object waitForReply(String msgId) throws Exception {
        Thread.yield();
        Thread.sleep(100);
        
        Object reply = null;
        boolean replyNotFound = true;
        int retryCount = 50;
        while (replyNotFound) {
            log.debug("Checking for response ... with MessageID : " + msgId);
            reply = getMessage(msgId);
            if (reply != null) {
                replyNotFound = false;
            } else {
                if (retryCount-- > 0) {
                    Thread.sleep(100);
                } else {
                    break;
                }
            }
        }
        return reply;
    }

    /**
     * Create a axis2 configuration context that 'knows' about the Mail transport
     * @return
     * @throws Exception
     */
    public ConfigurationContext getClientCfgCtx() throws Exception {

        AxisConfiguration axisCfg = new AxisConfiguration();
        TransportOutDescription trpOutDesc = new TransportOutDescription("mailto");

        trpOutDesc.addParameter(new Parameter("mail.smtp.class", TestTransport.class.getName()));
        trpOutDesc.addParameter(new Parameter("mail.smtp.host", "smtp.gmail.com"));
        trpOutDesc.addParameter(new Parameter("mail.smtp.port", "587"));
        trpOutDesc.addParameter(new Parameter("mail.smtp.starttls.enable", "true"));
        trpOutDesc.addParameter(new Parameter("mail.smtp.user", "synapse.test.1"));
        trpOutDesc.addParameter(new Parameter("mail.smtp.password", "mailpassword"));
        trpOutDesc.addParameter(new Parameter("mail.smtp.auth", "true"));

        MailTransportSender trpSender = new MailTransportSender();
        trpOutDesc.setSender(trpSender);

        axisCfg.addTransportOut(trpOutDesc);
        ConfigurationContext cfgCtx = new ConfigurationContext(axisCfg);

        trpSender.init(cfgCtx, trpOutDesc);
        return cfgCtx;
    }
}
