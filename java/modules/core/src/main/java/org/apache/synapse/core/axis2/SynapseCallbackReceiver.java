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

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class SynapseCallbackReceiver implements MessageReceiver {

    private Map callbackStore;  // this will be made thread safe

    public SynapseCallbackReceiver() {
        callbackStore = Collections.synchronizedMap(new HashMap());
    }

    public void addCallback(String MsgID, Callback callback) {
        callbackStore.put(MsgID, callback);
    }

    public void receive(MessageContext messageCtx) throws AxisFault {

        RelatesTo relatesTO = messageCtx.getOptions().getRelatesTo();
        String messageID    = relatesTO.getValue();
        Callback callback   = (Callback) callbackStore.get(messageID);

        if (callback != null) {
            callbackStore.remove(messageID);
            callback.onComplete(new AsyncResult(messageCtx));
        }
    }
}
