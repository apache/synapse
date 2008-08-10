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

package org.apache.synapse.transport.testkit.message;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.synapse.transport.testkit.server.axis2.MockMessageReceiver;

/**
 * Class encapsulating a SOAP envelope and an attachment map.
 * This class is used by {@link MockMessageReceiver} because it is not safe to
 * keep a reference to the {@link org.apache.axis2.context.MessageContext} object.
 */
public class MessageData {
    private final SOAPEnvelope envelope;
    private final Attachments attachments;
    
    public MessageData(SOAPEnvelope envelope, Attachments attachments) {
        this.envelope = envelope;
        this.attachments = attachments;
    }

    public SOAPEnvelope getEnvelope() {
        return envelope;
    }

    public Attachments getAttachments() {
        return attachments;
    }
}