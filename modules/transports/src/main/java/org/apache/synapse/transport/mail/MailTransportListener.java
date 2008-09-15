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

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.synapse.transport.base.AbstractPollingTransportListener;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.base.BaseUtils;
import org.apache.synapse.transport.base.ManagementSupport;
import org.apache.synapse.transport.base.ParamUtils;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * This mail transport lister implementation uses the base transport framework and is a polling
 * transport. i.e. a service can register itself with custom a custom mail configuration (i.e.
 * pop3 or imap) and specify its polling duration, and what action to be taken after processing
 * messages. The transport always deletes processed mails from the folder they were fetched from
 * and can be configured to be optionally moved to a different folder, if the server supports it
 * (e.g. with imap). When checking for new mail, the transport ignores messages already flaged as
 * SEEN and DELETED
 */
public class MailTransportListener extends AbstractPollingTransportListener<PollTableEntry>
    implements ManagementSupport {

    public static final String DELETE = "DELETE";
    public static final String MOVE = "MOVE";

    /**
     * Initializes the Mail transport
     *
     * @param cfgCtx    the Axsi2 configuration context
     * @param trpInDesc the POP3 transport in description from the axis2.xml
     * @throws AxisFault on error
     */
    @Override
    public void init(ConfigurationContext cfgCtx, TransportInDescription trpInDesc)
        throws AxisFault {
        super.init(cfgCtx, trpInDesc);
    }

    @Override
    protected void poll(PollTableEntry entry) {
        checkMail(entry, entry.getEmailAddress());
    }

    /**
     * Check mail for a particular service that has registered with the mail transport
     *
     * @param entry        the poll table entry that stores service specific informaiton
     * @param emailAddress the email address checked
     */
    private void checkMail(final PollTableEntry entry, InternetAddress emailAddress) {

        if (log.isDebugEnabled()) {
            log.debug("Checking mail for account : " + emailAddress);
        }

        boolean connected = false;
        int retryCount = 0;
        int maxRetryCount = entry.getMaxRetryCount();
        long reconnectionTimeout = entry.getReconnectTimeout();
        Store store = null;

        while (!connected) {
            try {
                retryCount++;
                if (log.isDebugEnabled()) {
                    log.debug("Attempting to connect to POP3/IMAP server for : " +
                        entry.getEmailAddress() + " using " + entry.getProperties());
                }

                Session session = Session.getInstance(entry.getProperties(), null);
                session.setDebug(log.isTraceEnabled());
                store = session.getStore(entry.getProtocol());

                if (entry.getUserName() != null && entry.getPassword() != null) {
                    store.connect(entry.getUserName(), entry.getPassword());
                } else {
                    handleException("Unable to locate username and password for mail login", null);
                }

                // were we able to connect?
                connected = store.isConnected();

            } catch (Exception e) {
                log.error("Error connecting to mail server for address : " + emailAddress, e);
                if (maxRetryCount <= retryCount) {
                    processFailure("Error connecting to mail server for address : " +
                        emailAddress + " :: " + e.getMessage(), e, entry);
                    return;
                }
            }

            if (!connected) {
                try {
                    log.warn("Connection to mail server for account : " + entry.getEmailAddress() +
                        " failed. Retrying in : " + reconnectionTimeout / 1000 + " seconds");
                    Thread.sleep(reconnectionTimeout);
                } catch (InterruptedException ignore) {
                }
            }
        }

        if (connected) {
            Folder folder = null;
            try {

                if (entry.getFolder() != null) {
                    folder = store.getFolder(entry.getFolder());
                } else {
                    folder = store.getFolder(MailConstants.DEFAULT_FOLDER);
                }
                if (folder == null) {
                    folder = store.getDefaultFolder();
                }

                if (folder == null) {
                    processFailure("Unable to access mail folder", null, entry);

                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Connecting to folder : " + folder.getName() +
                            " of email account : " + emailAddress);
                    }

                    folder.open(Folder.READ_WRITE);
                    int total = folder.getMessageCount();
                    Message[] messages = folder.getMessages();

                    if (log.isDebugEnabled()) {
                        log.debug(messages.length + " messgaes in folder : " + folder);
                    }

                    for (int i = 0; i < total; i++) {

                        if (messages[i].isSet(Flags.Flag.SEEN)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Skipping message # : " + i + " : " +
                                    messages[i].getSubject() + " - already marked SEEN");
                            }
                        } else if (messages[i].isSet(Flags.Flag.DELETED)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Skipping message # : " + i + " : " +
                                    messages[i].getSubject() + " - already marked DELETED");
                            }

                        } else {
                            entry.setLastPollState(PollTableEntry.NONE);
                            try {
                                processMail(messages[i], entry);
                                entry.setLastPollState(PollTableEntry.SUCCSESSFUL);
                                metrics.incrementMessagesReceived();
                            } catch (Exception e) {
                                entry.setLastPollState(PollTableEntry.FAILED);
                                metrics.incrementFaultsReceiving();
                            }

                            moveOrDeleteAfterProcessing(entry, store, folder, messages[i]);
                        }
                    }
                }

            } catch (MessagingException me) {
                processFailure("Error checking mail for account : " +
                    emailAddress + " :: " + me.getMessage(), me, entry);

            } finally {

                try {
                    folder.close(true /** expunge messages flagged as DELETED*/);
                } catch (MessagingException e) {
                    processFailure("Error closing mail folder : " +
                        folder + " for account : " + emailAddress, e, entry);
                }

                if (store != null) {
                    try {
                        store.close();
                    } catch (MessagingException e) {
                        log.warn("Error closing mail store for account : " +
                            emailAddress + " :: " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * Process a mail message through Axis2
     *
     * @param message the email message
     * @param entry   the poll table entry
     * @throws MessagingException on error
     * @throws IOException        on error
     */
    private void processMail(Message message, PollTableEntry entry)
        throws MessagingException, IOException {

        if (message instanceof MimeMessage) {
            MimeMessage mimeMessage = (MimeMessage) message;
            if (mimeMessage.getContent() instanceof Multipart) {
                Multipart mp = (Multipart) mimeMessage.getContent();
                for (int i=0; i<mp.getCount(); i++) {
                    MimeBodyPart mbp = (MimeBodyPart) mp.getBodyPart(i);
                    int size = mbp.getSize();
                    if (size != -1) {
                        metrics.incrementBytesReceived(size);
                    }
                }
            } else {
                int size = mimeMessage.getSize();
                if (size != -1) {
                    metrics.incrementBytesReceived(size);
                }
            }
        }

        // populate transport headers using the mail headers
        Map trpHeaders = new HashMap();
        try {
            Enumeration e = message.getAllHeaders();
            while (e.hasMoreElements()) {
                Header h = (Header) e.nextElement();
                if (entry.retainHeader(h.getName())) {
                    trpHeaders.put(h.getName(), h.getValue());
                }
            }
        } catch (MessagingException ignore) {}

        // figure out content type of primary request. If the content type is specified, use it
        String contentType = entry.getContentType();
        if (BaseUtils.isBlank(contentType)) {

            Object content = message.getContent();
            if (content instanceof Multipart) {
                contentType = message.getContentType();
            } else if (content instanceof String) {
                contentType = message.getContentType();
            } else if (content instanceof InputStream) {
                contentType = MailConstants.APPLICATION_BINARY;
            }
        }

        // if the content type was not found, we have an error
        if (contentType == null) {
            processFailure("Unable to determine Content-type for message : " +
                message.getMessageNumber() + " :: " + message.getSubject(), null, entry);
            return;
        } else if (log.isDebugEnabled()) {
            log.debug("Processing message as Content-Type : " + contentType);
        }

        org.apache.axis2.context.MessageContext msgContext = createMessageContext();
        // set to bypass dispatching if we know the service - we already should!
        AxisService service = cfgCtx.getAxisConfiguration().getService(entry.getServiceName());
        msgContext.setAxisService(service);

        // find the operation for the message, or default to one
        Parameter operationParam = service.getParameter(BaseConstants.OPERATION_PARAM);
        QName operationQName = (
            operationParam != null ?
                BaseUtils.getQNameFromString(operationParam.getValue()) :
                BaseConstants.DEFAULT_OPERATION);

        AxisOperation operation = service.getOperation(operationQName);
        if (operation != null) {
            msgContext.setAxisOperation(operation);
            msgContext.setSoapAction("urn:" + operation.getName().getLocalPart());
        }

        InternetAddress[] fromAddress = (InternetAddress[]) message.getReplyTo();
        if (fromAddress == null) {
            fromAddress = (InternetAddress[]) message.getFrom();
        }

        MailOutTransportInfo outInfo = new MailOutTransportInfo(fromAddress[0]);

        // determine reply address
        if (message.getReplyTo() != null) {
            outInfo.setTargetAddresses((InternetAddress[]) message.getReplyTo());
        } else if (message.getFrom() != null) {
            outInfo.setTargetAddresses((InternetAddress[]) message.getFrom());
        } else {
            // does the service specify a default reply address ?
            Parameter param = service.getParameter(MailConstants.TRANSPORT_MAIL_REPLY_ADDRESS);
            if (param != null && param.getValue() != null) {
                outInfo.setTargetAddresses(InternetAddress.parse((String) param.getValue()));
            }
        }

        // save CC addresses
        if (message.getRecipients(Message.RecipientType.CC) != null) {
            outInfo.setCcAddresses(
                (InternetAddress[]) message.getRecipients(Message.RecipientType.CC));
        }

        // determine and subject for the reply message
        if (message.getSubject() != null) {
            outInfo.setSubject("Re: " + message.getSubject());
        }

        // save original message ID if one exists, so that replies can be correlated
        if (message.getHeader(MailConstants.MAIL_HEADER_X_MESSAGE_ID) != null) {
            outInfo.setRequestMessageID(message.getHeader(MailConstants.MAIL_HEADER_X_MESSAGE_ID)[0]);
        } else if (message instanceof MimeMessage && ((MimeMessage) message).getMessageID() != null) {
            outInfo.setRequestMessageID(((MimeMessage) message).getMessageID());
        }

        // save out transport information
        msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, outInfo);

        // set message context From
        if (outInfo.getFromAddress() != null) {
            msgContext.setFrom(
                new EndpointReference(MailConstants.TRANSPORT_PREFIX +
                    outInfo.getFromAddress().getAddress()));
        }

        // save original mail message id message context MessageID
        msgContext.setMessageID(outInfo.getRequestMessageID());

        // set the message payload to the message context
        MailUtils.getInstace().setSOAPEnvelope(message, msgContext, contentType);

        String soapAction = (String) trpHeaders.get(BaseConstants.SOAPACTION);
        if (soapAction == null && message.getSubject() != null &&
            message.getSubject().startsWith(BaseConstants.SOAPACTION)) {
            soapAction = message.getSubject().substring(BaseConstants.SOAPACTION.length());
            if (soapAction.startsWith(":")) {
                soapAction = soapAction.substring(1).trim();
            }
        }

        handleIncomingMessage(
            msgContext,
            trpHeaders,
            soapAction,
            contentType
        );

        if (log.isDebugEnabled()) {
            log.debug("Processed message : " + message.getMessageNumber() +
                " :: " + message.getSubject());
        }
    }

    /**
     * Take specified action to either move or delete the processed email
     *
     * @param entry   the PollTableEntry for the email that has been processed
     * @param store   the mail store
     * @param folder  mail folder
     * @param message the email message to be moved or deleted
     */
    private void moveOrDeleteAfterProcessing(final PollTableEntry entry, Store store,
        Folder folder, Message message) {

        String moveToFolder = null;
        try {
            switch (entry.getLastPollState()) {
                case PollTableEntry.SUCCSESSFUL:
                    if (entry.getActionAfterProcess() == PollTableEntry.MOVE) {
                        moveToFolder = entry.getMoveAfterProcess();
                    }
                    break;

                case PollTableEntry.FAILED:
                    if (entry.getActionAfterFailure() == PollTableEntry.MOVE) {
                        moveToFolder = entry.getMoveAfterFailure();
                    }
                    break;
                case PollTableEntry.NONE:
                    return;
            }

            if (moveToFolder != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Moving processed email to folder :" + moveToFolder);
                }
                Folder dFolder = store.getFolder(moveToFolder);
                if (!dFolder.exists()) {
                    dFolder.create(Folder.HOLDS_MESSAGES);
                }
                folder.copyMessages(new Message[]{message}, dFolder);
            }

            if (log.isDebugEnabled()) {
                log.debug("Deleting email :" + message.getMessageNumber());
            }

            message.setFlag(Flags.Flag.DELETED, true);

        } catch (MessagingException e) {
            log.error("Error deleting or resolving folder to move after processing : "
                + moveToFolder, e);
        }
    }

    @Override
    protected PollTableEntry createPollTableEntry(AxisService service) {

        PollTableEntry entry = new PollTableEntry();
        try {
            entry.setEmailAddress(
                ParamUtils.getRequiredParam(service, MailConstants.TRANSPORT_MAIL_ADDRESS));

            List<Parameter> params = service.getParameters();
            for (Parameter p : params) {
                if (p.getName().startsWith("mail.")) {
                    entry.addProperty(p.getName(), (String) p.getValue());
                }

                if (MailConstants.MAIL_POP3_USERNAME.equals(p.getName()) ||
                    MailConstants.MAIL_IMAP_USERNAME.equals(p.getName())) {
                    entry.setUserName((String) p.getValue());
                }
                if (MailConstants.MAIL_POP3_PASSWORD.equals(p.getName()) ||
                    MailConstants.MAIL_IMAP_PASSWORD.equals(p.getName())) {
                    entry.setPassword((String) p.getValue());
                }
                if (MailConstants.TRANSPORT_MAIL_PROTOCOL.equals(p.getName())) {
                    entry.setProtocol((String) p.getValue());
                }
            }

            entry.setContentType(
                ParamUtils.getOptionalParam(service, MailConstants.TRANSPORT_MAIL_CONTENT_TYPE));
            entry.setReplyAddress(
                ParamUtils.getOptionalParam(service, MailConstants.TRANSPORT_MAIL_REPLY_ADDRESS));

            entry.addPreserveHeaders(
                ParamUtils.getOptionalParam(service, MailConstants.TRANSPORT_MAIL_PRESERVE_HEADERS));
            entry.addRemoveHeaders(
                ParamUtils.getOptionalParam(service, MailConstants.TRANSPORT_MAIL_REMOVE_HEADERS));

            String option = ParamUtils.getOptionalParam(
                service, MailConstants.TRANSPORT_MAIL_ACTION_AFTER_PROCESS);
            entry.setActionAfterProcess(
                MOVE.equals(option) ? PollTableEntry.MOVE : PollTableEntry.DELETE);
            option = ParamUtils.getOptionalParam(
                service, MailConstants.TRANSPORT_MAIL_ACTION_AFTER_FAILURE);
            entry.setActionAfterFailure(
                MOVE.equals(option) ? PollTableEntry.MOVE : PollTableEntry.DELETE);

            String moveFolderAfterProcess = ParamUtils.getOptionalParam(
                service, MailConstants.TRANSPORT_MAIL_MOVE_AFTER_PROCESS);
            entry.setMoveAfterProcess(moveFolderAfterProcess);
            String modeFolderAfterFailure = ParamUtils.getOptionalParam(
                service, MailConstants.TRANSPORT_MAIL_MOVE_AFTER_FAILURE);
            entry.setMoveAfterFailure(modeFolderAfterFailure);

            String strMaxRetryCount = ParamUtils.getOptionalParam(
                service, MailConstants.MAX_RETRY_COUNT);
            if (strMaxRetryCount != null)
                entry.setMaxRetryCount(Integer.parseInt(strMaxRetryCount));

            String strReconnectTimeout = ParamUtils.getOptionalParam(
                service, MailConstants.RECONNECT_TIMEOUT);
            if (strReconnectTimeout != null)
                entry.setReconnectTimeout(Integer.parseInt(strReconnectTimeout) * 1000);

            return entry;
            
        } catch (AxisFault axisFault) {
            String msg = "Error configuring the Mail transport for Service : " +
                service.getName() + " :: " + axisFault.getMessage();
            log.warn(msg);
            return null;
        } catch (AddressException e) {
            String msg = "Error configuring the Mail transport for Service : " +
                " Invalid email address specified by '" + MailConstants.TRANSPORT_MAIL_ADDRESS +
                "'parameter for service : " + service.getName() + " :: " + e.getMessage();
            log.warn(msg);
            return null;
        }
    }
}
