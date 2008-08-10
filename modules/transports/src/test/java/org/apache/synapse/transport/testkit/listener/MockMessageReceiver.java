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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.MessageReceiver;
//import org.apache.synapse.transport.base.event.TransportError;
//import org.apache.synapse.transport.base.event.TransportErrorListener;
import org.apache.synapse.transport.testkit.message.MessageData;

/**
 * A mock message receiver that puts the message data in a queue.
 */
public class MockMessageReceiver implements MessageReceiver /*, TransportErrorListener*/ {
    private interface Event {
        MessageData process() throws Throwable;
    }
    
    private final BlockingQueue<Event> queue = new LinkedBlockingQueue<Event>();
    
    public void receive(MessageContext messageCtx) throws AxisFault {
        SOAPEnvelope envelope = messageCtx.getEnvelope();
        envelope.build();
        Attachments attachments = messageCtx.getAttachmentMap();
        // Make sure that all attachments are read
        attachments.getAllContentIDs();
        final MessageData messageData = new MessageData(envelope, attachments);
        queue.add(new Event() {
            public MessageData process() throws Throwable {
                return messageData;
            }
        });
    }

//    public void error(final TransportError error) {
//        queue.add(new Event() {
//            public MessageData process() throws Throwable {
//                throw error.getException();
//            }
//        });
//    }
    
    public MessageData waitForMessage(long timeout, TimeUnit unit) throws Throwable {
        Event event = queue.poll(timeout, unit);
        return event == null ? null : event.process();
    }
}
