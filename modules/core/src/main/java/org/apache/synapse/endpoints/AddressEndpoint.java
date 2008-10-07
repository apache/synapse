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

package org.apache.synapse.endpoints;

import org.apache.axis2.clustering.ClusterManager;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.EndpointDefinition;

/**
 * This class represents an actual endpoint to send the message. It is responsible for sending the
 * message, performing retries if a failure occurred and informing the parent endpoint if a failure
 * couldn't be recovered.
 */
public class AddressEndpoint extends AbstractEndpoint {

    public void onFault(MessageContext synCtx) {

        // is this really a fault or a timeout/connection close etc?
        if (isTimeout(synCtx)) {
            getContext().onTimeout();
        } else if (isSuspendFault(synCtx)) {
            getContext().onFault();
        }

        // this should be an ignored error if we get here
        synCtx.setProperty(SynapseConstants.ERROR_CODE, null);
        synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, null);
        synCtx.setProperty(SynapseConstants.ERROR_DETAIL, null);
        synCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, null);
        super.onFault(synCtx);
    }

    public void onSuccess() {
        getContext().onSuccess();
    }
}
