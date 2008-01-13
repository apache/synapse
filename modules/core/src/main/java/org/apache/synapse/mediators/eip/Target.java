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

package org.apache.synapse.mediators.eip;

import org.apache.synapse.MessageContext;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.axis2.addressing.EndpointReference;

/**
 * A bean class that holds the target (i.e. sequence or endpoint) information for a message
 * as used by common EIP mediators
 */
public class Target {

    /** An optional To address to be set on the message when handing over to the target */
    private String toAddress = null;

    /** An optional Action to be set on the message when handing over to the target */
    private String soapAction = null;

    /** The inlined target sequence definition */
    private SequenceMediator sequence = null;

    /** The target sequence reference key */
    private String sequenceRef = null;

    /** The inlined target endpoint definition */
    private Endpoint endpoint = null;

    /** The target endpoint reference key */
    private String endpointRef = null;

    /**
     * process the message through this target (may be to mediate
     * using the target sequence, send message to the target endpoint or both)
     *
     * @param synCtx - MessageContext to be mediated
     */
    public void mediate(MessageContext synCtx) {

        if (soapAction != null) {
            synCtx.setSoapAction(soapAction);
        }

        if (toAddress != null) {
            if (synCtx.getTo() != null) {
                synCtx.getTo().setAddress(toAddress);
            } else {
                synCtx.setTo(new EndpointReference(toAddress));
            }
        }

        // since we are injecting the new messages asynchronously, we cannot process a message
        // through a sequence and then again with an endpoint
        if (sequence != null) {
            synCtx.getEnvironment().injectAsync(synCtx, sequence);
        } else if (sequenceRef != null) {
            SequenceMediator refSequence = (SequenceMediator) synCtx.getSequence(sequenceRef);
            if (refSequence != null) {
                synCtx.getEnvironment().injectAsync(synCtx, refSequence);
            }
        } else if (endpoint != null) {
            endpoint.send(synCtx);
        } else if (endpointRef != null) {
            Endpoint epr = synCtx.getConfiguration().getEndpoint(endpointRef);
            if (epr != null) {
                epr.send(synCtx);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //                        Getters and Setters                                        //
    ///////////////////////////////////////////////////////////////////////////////////////

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getSoapAction() {
        return soapAction;
    }

    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    public SequenceMediator getSequence() {
        return sequence;
    }

    public void setSequence(SequenceMediator sequence) {
        this.sequence = sequence;
    }

    public String getSequenceRef() {
        return sequenceRef;
    }

    public void setSequenceRef(String sequenceRef) {
        this.sequenceRef = sequenceRef;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpointRef() {
        return endpointRef;
    }

    public void setEndpointRef(String endpointRef) {
        this.endpointRef = endpointRef;
    }
}