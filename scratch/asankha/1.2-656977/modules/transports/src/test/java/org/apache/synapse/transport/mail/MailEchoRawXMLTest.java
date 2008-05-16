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

import org.apache.synapse.transport.AbstractTransportTest;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.mail.UtilsMailServer;
import org.apache.synapse.transport.mail.MailTransportSender;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.Constants;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.BasicConfigurator;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import javax.xml.stream.XMLStreamReader;
import javax.xml.namespace.QName;
import javax.activation.DataHandler;
import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import java.util.Properties;
import java.util.Arrays;
import java.util.Date;
import java.io.StringReader;

public class MailEchoRawXMLTest extends AbstractTransportTest {

    private static final Log log = LogFactory.getLog(MailEchoRawXMLTest.class);

    private static final String KOREAN_TEXT = "\uc5ec\ubcf4\uc138\uc694 \uc138\uacc4!";
    private static final String KOREAN_CHARSET = "ISO-2022-KR";
    private static final String POX_MESSAGE =
        "<my:echoOMElement xmlns:my=\"http://ws.apache.org/namespaces/axis2\">" +
        "<my:myValue>omTextValue</my:myValue></my:echoOMElement>";

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
        msg.setText(POX_MESSAGE);
        Transport.send(msg);

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

        if (reply != null && reply instanceof String) {
            log.debug("Result Body : " + reply);
            XMLStreamReader reader = StAXUtils.createXMLStreamReader(new StringReader((String) reply));
            OMElement res = new StAXOMBuilder(reader).getDocumentElement();
            if (res != null) {
                AXIOMXPath xpath = new AXIOMXPath("//my:myValue");
                xpath.addNamespace("my", "http://localhost/axis2/services/EchoXMLService");
                Object result = xpath.evaluate(res);
                if (result != null && result instanceof OMElement) {
                    assertEquals("omTextValue", ((OMElement) result).getText());
                }
            }
        } else {
            fail("Did not receive the reply mail");
        }
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

        if (reply != null && reply instanceof String) {
            log.debug("Result Body : " + reply);
            XMLStreamReader reader = StAXUtils.createXMLStreamReader(new StringReader((String) reply));
            SOAPEnvelope env = new StAXSOAPModelBuilder(reader).getSOAPEnvelope();
            if (env != null) {
                AXIOMXPath xpath = new AXIOMXPath("//my:myValue");
                xpath.addNamespace("my", "http://localhost/axis2/services/EchoXMLService");
                Object result = xpath.evaluate(env);
                if (result != null && result instanceof OMElement) {
                    assertEquals("omTextValue", ((OMElement) result).getText());
                }
            }
        } else {
            fail("Did not receive the reply mail");
        }
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
        sender.fireAndForget(createKoreanPayload());

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

        if (reply != null && reply instanceof String) {
            log.debug("Result Body : " + reply);
            XMLStreamReader reader = StAXUtils.createXMLStreamReader(new StringReader((String) reply));
            SOAPEnvelope env = new StAXSOAPModelBuilder(reader).getSOAPEnvelope();
            if (env != null) {
                AXIOMXPath xpath = new AXIOMXPath("//my:myValue");
                xpath.addNamespace("my", "http://localhost/axis2/services/EchoXMLService");
                Object result = xpath.evaluate(env);
                if (result != null && result instanceof OMElement) {
                    assertEquals("omTextValue", ((OMElement) result).getText());
                }
            }
        } else {
            fail("Did not receive the reply mail");
        }
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

        if (reply != null && reply instanceof String) {
            log.debug("Result Body : " + reply);
            XMLStreamReader reader = StAXUtils.createXMLStreamReader(new StringReader((String) reply));
            SOAPEnvelope env = new StAXSOAPModelBuilder(reader).getSOAPEnvelope();
            if (env != null) {
                AXIOMXPath xpath = new AXIOMXPath("//my:myValue");
                xpath.addNamespace("my", "http://localhost/axis2/services/EchoXMLService");
                Object result = xpath.evaluate(env);
                if (result != null && result instanceof OMElement) {
                    assertEquals("omTextValue", ((OMElement) result).getText());
                }
            }
        } else {
            fail("Did not receive the reply mail");
        }
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
        sender.fireAndForget(createKoreanPayload());

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

        if (reply != null && reply instanceof String) {
            log.debug("Result Body : " + reply);
            XMLStreamReader reader = StAXUtils.createXMLStreamReader(new StringReader((String) reply));
            SOAPEnvelope env = new StAXSOAPModelBuilder(reader).getSOAPEnvelope();
            if (env != null) {
                AXIOMXPath xpath = new AXIOMXPath("//my:myValue");
                xpath.addNamespace("my", "http://localhost/axis2/services/EchoXMLService");
                Object result = xpath.evaluate(env);
                if (result != null && result instanceof OMElement) {
                    assertEquals(KOREAN_TEXT, ((OMElement) result).getText());
                }
            }
        } else {
            fail("Did not receive the reply mail");
        }
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
        sender.fireAndForget(createKoreanPayload());

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

        if (reply != null && reply instanceof String) {
            log.debug("Result Body : " + reply);
            XMLStreamReader reader = StAXUtils.createXMLStreamReader(new StringReader((String) reply));
            SOAPEnvelope env = new StAXSOAPModelBuilder(reader).getSOAPEnvelope();
            if (env != null) {
                AXIOMXPath xpath = new AXIOMXPath("//my:myValue");
                xpath.addNamespace("my", "http://localhost/axis2/services/EchoXMLService");
                Object result = xpath.evaluate(env);
                if (result != null && result instanceof OMElement) {
                    assertEquals(KOREAN_TEXT, ((OMElement) result).getText());
                }
            }
        } else {
            fail("Did not receive the reply mail");
        }
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

    protected OMElement createKoreanPayload() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://localhost/axis2/services/EchoXMLService", "my");
        OMElement method = fac.createOMElement("echoOMElement", omNs);
        OMElement value = fac.createOMElement("myValue", omNs);
        value.addChild(fac.createOMText(value, KOREAN_TEXT));
        method.addChild(value);
        return method;
    }
}
