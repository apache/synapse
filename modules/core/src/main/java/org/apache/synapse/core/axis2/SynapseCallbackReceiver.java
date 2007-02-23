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

package org.apache.synapse.core.axis2;

import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.client.async.AsyncResult;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class SynapseCallbackReceiver implements MessageReceiver {

    private static final Log log = LogFactory.getLog(SynapseCallbackReceiver.class);

    private Map callbackStore;  // this will be made thread safe within the constructor

    public SynapseCallbackReceiver() {
        callbackStore = Collections.synchronizedMap(new HashMap());
    }

    public void addCallback(String MsgID, Callback callback) {
        callbackStore.put(MsgID, callback);
    }

    public void receive(MessageContext messageCtx) throws AxisFault {

        if (messageCtx.getOptions() != null && messageCtx.getOptions().getRelatesTo() != null) {
            String messageID  = messageCtx.getOptions().getRelatesTo().getValue();
            Callback callback = (Callback) callbackStore.get(messageID);

            if (callback != null) {
                callbackStore.remove(messageID);
                callback.onComplete(new AsyncResult(messageCtx));
            } else {
                // TODO invoke a generic synapse error handler for this message
                log.warn("Synapse received a response for the request with message Id : " +
                    messageID + " But a callback has not been registered to process this response");
            }

        } else {
            // TODO invoke a generic synapse error handler for this message
            log.warn("Synapse received a response message without a message Id");
        }
    }
}
