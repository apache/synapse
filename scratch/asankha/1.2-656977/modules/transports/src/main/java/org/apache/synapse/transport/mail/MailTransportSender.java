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

import org.apache.synapse.format.MessageFormatterEx;
import org.apache.synapse.format.MessageFormatterExAdapter;
import org.apache.synapse.transport.base.AbstractTransportSender;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.base.BaseUtils;
import org.apache.synapse.transport.base.ManagementSupport;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axiom.om.OMOutputFormat;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.DataHandler;
import javax.activation.MailcapCommandMap;
import javax.activation.CommandMap;

import java.util.*;
import java.io.IOException;

/**
 * The mail transport sender sends mail using an SMTP server configuration defined
 * in the axis2.xml's transport sender definition
 */
public class MailTransportSender extends AbstractTransportSender
    implements ManagementSupport {

    private String smtpUsername = null;
    private String smtpPassword = null;
    /** Default from address for outgoing messages */
    private InternetAddress smtpFromAddress = null;
    /** A set of custom Bcc address for all outgoing messages */
    private InternetAddress[] smtpBccAddresses = null;
    /** Default mail format */
    private String defaultMailFormat = "Text";
    /** The default Session which can be safely shared */
    private Session session = null;

    /**
     * The public constructor
     */
    public MailTransportSender() {
        log = LogFactory.getLog(MailTransportSender.class);
    }

    /**
     * Initialize the Mail sender and be ready to send messages
     * @param cfgCtx the axis2 configuration context
     * @param transportOut the transport-out description
     * @throws org.apache.axis2.AxisFault on error
     */
    public void init(ConfigurationContext cfgCtx, TransportOutDescription transportOut) throws AxisFault {
        setTransportName(MailConstants.TRANSPORT_NAME);
        super.init(cfgCtx, transportOut);

        // initialize SMTP session
        Properties props = new Properties();
        List<Parameter> params = transportOut.getParameters();
        for (Parameter p : params) {
            props.put(p.getName(), p.getValue());
        }

        if (props.containsKey(MailConstants.MAIL_SMTP_FROM)) {
            try {
                smtpFromAddress = new InternetAddress(
                    (String) props.get(MailConstants.MAIL_SMTP_FROM));
            } catch (AddressException e) {
                handleException("Invalid default 'From' address : " +
                    props.get(MailConstants.MAIL_SMTP_FROM), e);
            }
        }

        if (props.containsKey(MailConstants.MAIL_SMTP_BCC)) {
            try {
                smtpBccAddresses = InternetAddress.parse(
                    (String) props.get(MailConstants.MAIL_SMTP_BCC));
            } catch (AddressException e) {
                handleException("Invalid default 'Bcc' address : " +
                    props.get(MailConstants.MAIL_SMTP_BCC), e);
            }
        }

        if (props.containsKey(MailConstants.TRANSPORT_MAIL_FORMAT)) {
            defaultMailFormat = (String) props.get(MailConstants.TRANSPORT_MAIL_FORMAT);
        }

        smtpUsername = (String) props.get(MailConstants.MAIL_SMTP_USERNAME);
        smtpPassword = (String) props.get(MailConstants.MAIL_SMTP_PASSWORD);

        if (smtpUsername != null && smtpPassword != null) {
            session = Session.getInstance(props, new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);    
                }
            });
        } else {
            session = Session.getInstance(props, null);
        }

        // add handlers for main MIME types
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("application/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("application/soap+xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mc);
        
        session.setDebug(log.isTraceEnabled());
    }

    /**
     * Send the given message over the Mail transport
     *
     * @param msgCtx the axis2 message context
     * @throws AxisFault on error
     */
    public void sendMessage(MessageContext msgCtx, String targetAddress,
        OutTransportInfo outTransportInfo) throws AxisFault {

        MailOutTransportInfo mailOutInfo = null;

        if (targetAddress != null) {
            if (targetAddress.startsWith(MailConstants.TRANSPORT_NAME)) {
                targetAddress = targetAddress.substring(MailConstants.TRANSPORT_NAME.length()+1);
            }

            if (msgCtx.getReplyTo() != null &&
                !AddressingConstants.Final.WSA_NONE_URI.equals(msgCtx.getReplyTo().getAddress()) &&
                !AddressingConstants.Final.WSA_ANONYMOUS_URL.equals(msgCtx.getReplyTo().getAddress())) {
                
                String replyTo = msgCtx.getReplyTo().getAddress();
                if (replyTo.startsWith(MailConstants.TRANSPORT_NAME)) {
                    replyTo = replyTo.substring(MailConstants.TRANSPORT_NAME.length()+1);
                }
                try {
                    mailOutInfo = new MailOutTransportInfo(new InternetAddress(replyTo));
                } catch (AddressException e) {
                    handleException("Invalid reply address/es : " + replyTo, e);
                }
            } else {
                mailOutInfo = new MailOutTransportInfo(smtpFromAddress);
            }

            try {
                mailOutInfo.setTargetAddresses(InternetAddress.parse(targetAddress));
            } catch (AddressException e) {
                handleException("Invalid target address/es : " + targetAddress, e);
            }
        } else if (outTransportInfo != null && outTransportInfo instanceof MailOutTransportInfo) {
            mailOutInfo = (MailOutTransportInfo) outTransportInfo;
        }

        if (mailOutInfo != null) {
            try {
                sendMail(mailOutInfo, msgCtx);
            } catch (MessagingException e) {
                handleException("Error generating mail message", e);
            } catch (IOException e) {
                handleException("Error generating mail message", e);
            }
        } else {
            handleException("Unable to determine out transport information to send message");
        }
    }

    /**
     * Populate email with a SOAP formatted message
     * @param outInfo the out transport information holder
     * @param msgContext the message context that holds the message to be written
     * @throws AxisFault on error
     */
    private void sendMail(MailOutTransportInfo outInfo, MessageContext msgContext)
        throws AxisFault, MessagingException, IOException {

        OMOutputFormat format = BaseUtils.getOMOutputFormat(msgContext);
        MessageFormatter messageFormatter = BaseUtils.getMessageFormatter(msgContext);

        WSMimeMessage message = new WSMimeMessage(session);
        Map trpHeaders = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);

        // set From address - first check if this is a reply, then use from address from the
        // transport out, else if any custom transport headers set on this message, or default
        // to the transport senders default From address        
        if (outInfo.getTargetAddresses() != null && outInfo.getFromAddress() != null) {
            message.setFrom(outInfo.getFromAddress());
            message.setReplyTo((new Address []{outInfo.getFromAddress()}));
        } else if (trpHeaders != null && trpHeaders.containsKey(MailConstants.MAIL_HEADER_FROM)) {
            message.setFrom(
                new InternetAddress((String) trpHeaders.get(MailConstants.MAIL_HEADER_FROM)));
            message.setReplyTo(InternetAddress.parse(
                (String) trpHeaders.get(MailConstants.MAIL_HEADER_FROM)));
        } else {
            if (smtpFromAddress != null) {
                message.setFrom(smtpFromAddress);
                message.setReplyTo(new Address[] {smtpFromAddress});
            } else {
                handleException("From address for outgoing message cannot be determined");
            }
        }

        // set To address/es to any custom transport header set on the message, else use the reply
        // address from the out transport information
        if (trpHeaders != null && trpHeaders.containsKey(MailConstants.MAIL_HEADER_TO)) {
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse((String) trpHeaders.get(MailConstants.MAIL_HEADER_TO)));
        } else if (outInfo.getTargetAddresses() != null) {
            message.setRecipients(Message.RecipientType.TO, outInfo.getTargetAddresses());
        } else {
            handleException("To address for outgoing message cannot be determined");
        }

        // set Cc address/es to any custom transport header set on the message, else use the
        // Cc list from original request message
        if (trpHeaders != null && trpHeaders.containsKey(MailConstants.MAIL_HEADER_CC)) {
            message.setRecipients(Message.RecipientType.CC,
                InternetAddress.parse((String) trpHeaders.get(MailConstants.MAIL_HEADER_CC)));
        } else if (outInfo.getTargetAddresses() != null) {
            message.setRecipients(Message.RecipientType.CC, outInfo.getCcAddresses());
        }

        // set Bcc address/es to any custom addresses set at the transport sender level + any
        // custom transport header
        InternetAddress[] trpBccArr = null;
        if (trpHeaders != null && trpHeaders.containsKey(MailConstants.MAIL_HEADER_BCC)) {
            trpBccArr = InternetAddress.parse((String) trpHeaders.get(MailConstants.MAIL_HEADER_BCC));
        }

        InternetAddress[] mergedBcc = new InternetAddress[
            (trpBccArr != null ? trpBccArr.length : 0) +
            (smtpBccAddresses != null ? smtpBccAddresses.length : 0)];
        if (trpBccArr != null) {
            System.arraycopy(trpBccArr, 0, mergedBcc, 0, trpBccArr.length);
        }
        if (smtpBccAddresses != null) {
            System.arraycopy(smtpBccAddresses, 0, mergedBcc, mergedBcc.length, smtpBccAddresses.length);
        }
        if (mergedBcc != null) {
            message.setRecipients(Message.RecipientType.BCC, mergedBcc);
        }

        // set subject
        if (trpHeaders != null && trpHeaders.containsKey(MailConstants.MAIL_HEADER_SUBJECT)) {
            message.setSubject((String) trpHeaders.get(MailConstants.MAIL_HEADER_SUBJECT));
        } else if (outInfo.getSubject() != null) {
            message.setSubject(outInfo.getSubject());
        } else {
            message.setSubject(BaseConstants.SOAPACTION + ": " + msgContext.getSoapAction());
        }

        // if a custom message id is set, use it
        if (msgContext.getMessageID() != null) {
            message.setHeader(MailConstants.MAIL_HEADER_MESSAGE_ID, msgContext.getMessageID());
            message.setHeader(MailConstants.MAIL_HEADER_X_MESSAGE_ID, msgContext.getMessageID());
        }

        // if this is a reply, set reference to original message
        if (outInfo.getRequestMessageID() != null) {
            message.setHeader(MailConstants.MAIL_HEADER_IN_REPLY_TO, outInfo.getRequestMessageID());
            message.setHeader(MailConstants.MAIL_HEADER_REFERENCES, outInfo.getRequestMessageID());

        } else {
            if (trpHeaders != null &&
                trpHeaders.containsKey(MailConstants.MAIL_HEADER_IN_REPLY_TO)) {
                message.setHeader(MailConstants.MAIL_HEADER_IN_REPLY_TO,
                    (String) trpHeaders.get(MailConstants.MAIL_HEADER_IN_REPLY_TO));
            }
            if (trpHeaders != null && trpHeaders.containsKey(MailConstants.MAIL_HEADER_REFERENCES)) {
                message.setHeader(MailConstants.MAIL_HEADER_REFERENCES,
                    (String) trpHeaders.get(MailConstants.MAIL_HEADER_REFERENCES));
            }
        }

        // set Date
        message.setSentDate(new Date());

        // set SOAPAction header
        message.setHeader(BaseConstants.SOAPACTION, msgContext.getSoapAction());

        // write body
        MessageFormatterEx messageFormatterEx;
        if (messageFormatter instanceof MessageFormatterEx) {
            messageFormatterEx = (MessageFormatterEx)messageFormatter;
        } else {
            messageFormatterEx = new MessageFormatterExAdapter(messageFormatter);
        }
        
        DataHandler dataHandler = new DataHandler(messageFormatterEx.getDataSource(msgContext, format, msgContext.getSoapAction()));
        
        MimeMultipart mimeMultiPart = null;

        String mFormat = (String) msgContext.getProperty(MailConstants.TRANSPORT_MAIL_FORMAT);
        if (mFormat == null) {
            mFormat = defaultMailFormat;
        }

        if (MailConstants.TRANSPORT_FORMAT_MP.equals(mFormat)) {
            mimeMultiPart = new MimeMultipart();
            MimeBodyPart mimeBodyPart1 = new MimeBodyPart();
            mimeBodyPart1.setContent("Web Service Message Attached","text/plain");
            MimeBodyPart mimeBodyPart2 = new MimeBodyPart();
            mimeBodyPart2.setDataHandler(dataHandler);
            mimeBodyPart2.setHeader(BaseConstants.SOAPACTION, msgContext.getSoapAction());
            mimeMultiPart.addBodyPart(mimeBodyPart1);
            mimeMultiPart.addBodyPart(mimeBodyPart2);

        } else {
            message.setHeader(BaseConstants.SOAPACTION, msgContext.getSoapAction());
        }

        try {
            if (mimeMultiPart == null) {
                message.setDataHandler(dataHandler);
            } else {
                message.setContent(mimeMultiPart);
            }
            Transport.send(message);

            // update metrics
            metrics.incrementMessagesSent();
            if (mimeMultiPart != null) {
                for (int i=0; i<mimeMultiPart.getCount(); i++) {
                    MimeBodyPart mbp = (MimeBodyPart) mimeMultiPart.getBodyPart(i);
                    int size = mbp.getSize();
                    if (size != -1) {
                        metrics.incrementBytesSent(size);
                    }
                }
            } else {
                int size = message.getSize();
                if (size != -1) {
                    metrics.incrementBytesSent(size);
                }
            }

        } catch (MessagingException e) {
            handleException("Error creating mail message or sending it to the configured server", e);
            metrics.incrementFaultsSending();
            
        }
    }
}
