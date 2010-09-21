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

import org.apache.synapse.MessageContext;
import org.apache.synapse.endpoints.Endpoint;


/**
 * The Wrapper for the SynapseMessageContext to be stored in the Message store.
 * This will contain the additional information needed to be stored with the
 * synpase message.
 */
public class StorableMessage {

    /**
     * Endpoint where message must be delivered to
     */
    private Endpoint endpoint;

    /**
     * The MessageContext to be saved
     */
    private MessageContext messageContext;


    public StorableMessage(Endpoint endpoint , MessageContext messageContext) {
        this.endpoint = endpoint;
        this.messageContext = messageContext;
    }

    /**
     * Get the Endpoint that the Message to be delivered
     *
     * @return the Endpoint to which message should be sent
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Get the Synpase Message Context
     *
     * @return MessageContext
     */
    public MessageContext getMessageContext() {
        return messageContext;
    }

}