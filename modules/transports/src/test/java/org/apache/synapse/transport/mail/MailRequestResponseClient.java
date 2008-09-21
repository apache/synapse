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

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.ContentType;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.client.RequestResponseTestClient;
import org.apache.synapse.transport.testkit.message.IncomingMessage;

public class MailRequestResponseClient extends MailClient implements RequestResponseTestClient<byte[],byte[]> {
    private static final Log log = LogFactory.getLog(MailRequestResponseClient.class);
    
    private Store store;
    
    public MailRequestResponseClient(MessageLayout layout) {
        super(layout);
    }
    
    @SuppressWarnings("unused")
    private void setUp(MailTestEnvironment env, MailChannel channel) throws MessagingException {
        Session session = channel.getReplySession();
        session.setDebug(log.isTraceEnabled());
        store = session.getStore(env.getProtocol());
        MailTestEnvironment.Account sender = channel.getSender();
        store.connect(sender.getLogin(), sender.getPassword());
    }
    
    @SuppressWarnings("unused")
    private void tearDown() throws MessagingException {
        store.close();
        store = null;
    }
    
    public IncomingMessage<byte[]> sendMessage(ClientOptions options, ContentType contentType, byte[] message) throws Exception {
        String msgId = sendMessage(contentType, message);
        Message reply = waitForReply(msgId);
        Assert.assertNotNull("No response received", reply);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reply.getDataHandler().writeTo(baos);
        return new IncomingMessage<byte[]>(new ContentType(reply.getContentType()), baos.toByteArray());
    }
    
    private Message waitForReply(String msgId) throws Exception {
        Thread.yield();
        Thread.sleep(100);
        
        Message reply = null;
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

    private Message getMessage(String requestMsgId) {
        try {
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
                            return m;
                        }
                    }
                }
                m.setFlag(Flags.Flag.DELETED, true);
            }
            folder.close(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
