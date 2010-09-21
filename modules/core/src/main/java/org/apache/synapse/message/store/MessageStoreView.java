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

package org.apache.synapse.message.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class MessageStoreView implements MessageStoreViewMBean{

    private String messageStoreName;

    private MessageStore messageStore;

    private static final Log log = LogFactory.getLog(MessageStoreView.class);

    public MessageStoreView(String name , MessageStore messageStore){
        this.messageStoreName = name;
        this.messageStore = messageStore;
    }

    public void resendAll() {
        List<StorableMessage> list = messageStore.getAllMessages();

        for(int i = 0; i < list.size(); i++) {
            StorableMessage m = list.get(i);
            // wait till the endpoint is ready
            while(!m.getEndpoint().readyToSend());
            //resend
            m.getEndpoint().send(m.getMessageContext());
        }

        log.info("All Messages in Message Store " +messageStoreName+ " were resent");
    }

    public void deleteAll() {
        messageStore.unstoreAll();
        log.info("All messages in Message Store" + messageStoreName + " were deleted");
    }

    public List<String> getMessageIds() {

        List<String> returnList = new ArrayList<String>();
        List<StorableMessage> list = messageStore.getAllMessages();

        for(StorableMessage m : list) {
            returnList.add(m.getMessageContext().getMessageID());
        }
        return returnList;
    }

    public boolean resend(String messageID) {

        StorableMessage m = messageStore.getMessage(messageID);

        if (m != null) {
            if (m.getEndpoint().readyToSend()) {
                m.getEndpoint().send(m.getMessageContext());
                log.info("Message with ID " + messageID + " resent via the Endpoint" +
                        m.getEndpoint().getName());
                return true;
            } else {
                log.info("Message with ID " + messageID +" unable resent via the Endpoint" +
                        m.getEndpoint().getName());
            }
        }

        return false;
    }

    public void delete(String messageID) {
        if(messageID != null) {
            StorableMessage m =messageStore.unstore(messageID);
            if (m != null){
                log.info("Message with ID :" + messageID + " removed from the MessageStore");
            }
        }
    }

    public String getEnvelope(String messageID) {
        if (messageID != null) {
            StorableMessage m = messageStore.getMessage(messageID);

            if (m != null) {
                return m.getMessageContext().getEnvelope().toString();
            }
        }
        return null;
    }

    public int getSize() {
        return messageStore.getSize();
    }
}
