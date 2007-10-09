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

package org.apache.synapse.mediators;

import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;

/**
 * This class will be used as the executer for the injectAsync method for the
 * sequence mediation
 */
public class MediatorWorker implements Runnable {

    /** Mediator to be executed */
    private Mediator seq = null;

    /** MessageContext to be mediated using the mediator */
    private MessageContext synCtx = null;

    /**
     * Constructor of the MediatorWorker which sets the sequence and the message context
     *
     * @param seq    - Sequence Mediator to be set
     * @param synCtx - Synapse MessageContext to be set
     */
    public MediatorWorker(Mediator seq, MessageContext synCtx) {
        this.seq = seq;
        this.synCtx = synCtx;
    }

    /**
     * Constructor od the MediatorWorker which sets the provided message context and the
     * main sequence as the sequence for mediation
     *
     * @param synCtx - Synapse MessageContext to be set
     */
    public MediatorWorker(MessageContext synCtx) {
        this.synCtx = synCtx;
        seq = synCtx.getMainSequence();
    }

    /**
     * Execution method of the thread. This will just call the mediation of the specified
     * Synapse MessageContext using the specified Sequence Mediator
     */
    public void run() {
        seq.mediate(synCtx);
    }
}
