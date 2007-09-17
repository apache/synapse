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

import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.base.SequenceMediator;

/**
 * This class will be a bean which carries the target information for most of the EIP mediators
 */
public class Target {

    /**
     * Holds the to address of the target endpoint
     */
    private String to = null;

    /**
     * Holds the soapAction of the target service
     */
    private String soapAction = null;

    /**
     * Holds the target mediation sequence as an annonymous sequence
     */
    private SequenceMediator sequence = null;

    /**
     * Holds the target mediation sequence as a sequence reference
     */
    private String sequenceRef = null;

    /**
     * Holds the target endpoint to which the message will be sent
     */
    private Endpoint endpoint = null;

    /**
     * Holds the reference to the target endpoint to which the message will be sent
     */
    private String endpointRef = null;

    /**
     * This method will be called by the EIP mediators to mediated the target (may be to mediate
     * using the target sequence, send message to the target endpoint or both)
     * 
     * @param synCtx - MessageContext to be mediated
     * @return boolean true if the sequence does not drop the message, false if it does
     */
    public boolean mediate(MessageContext synCtx) {

        if (sequence != null) {
            return sequence.mediate(synCtx);
        } else if (sequenceRef != null) {
            Mediator refSequence = synCtx.getConfiguration().getSequence(sequenceRef);
            if (refSequence != null) {
                return refSequence.mediate(synCtx);
            }
        } else if (endpoint != null) {
            endpoint.send(synCtx);
        } else if (endpointRef != null) {
            Endpoint epr = synCtx.getConfiguration().getEndpoint(endpointRef);
            if (epr != null) {
                epr.send(synCtx);
            }
        } else {
            synCtx.getEnvironment().injectMessage(synCtx);
        }

        return true;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
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