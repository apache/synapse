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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.util.threadpool.ThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.Constants;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

/**
 * <p> This is the Axis2 implementation of the MessageContext
 */
public class Axis2SynapseEnvironment implements SynapseEnvironment {

    private static final Log log = LogFactory.getLog(Axis2SynapseEnvironment.class);

    private ConfigurationContext cfgCtx = null;
    private ThreadFactory threadFactory = null;

    private SynapseConfiguration synapseConfig;

    public Axis2SynapseEnvironment() {}

    public Axis2SynapseEnvironment(ConfigurationContext cfgCtx,
                                   SynapseConfiguration synapseConfig) {
        this.cfgCtx = cfgCtx;
        this.synapseConfig = synapseConfig;
        threadFactory = cfgCtx.getThreadPool();
    }

    public void injectMessage(final MessageContext synCtx) {
        synCtx.setEnvironment(this);
        /*threadFactory.execute(new Runnable() {
            public void run() {
                synCtx.getConfiguration().getMainMediator().mediate(synCtx);
            }
        });*/

        // if the outSequence property is present use that for the message mediation
        // if not use the main mediator to mediate the outgoing message
        if (synCtx.getProperty(Constants.OUT_SEQUENCE) != null) {
            Mediator mediator = synCtx.getConfiguration().getNamedSequence(
                    (String)synCtx.getProperty(Constants.OUT_SEQUENCE));
            // check weather the sequence specified with the property outSequence is availabel
            if(mediator != null) {
                log.debug("Using the outSequence " + synCtx.getProperty(Constants.OUT_SEQUENCE)
                        + " for the out message mediation");
                mediator.mediate(synCtx);
            } else {
                log.error("Sequence named " + synCtx.getProperty(Constants.OUT_SEQUENCE)
                        + " doesn't exists in synapse");
            }
        } else {
            synCtx.getConfiguration().getMainMediator().mediate(synCtx);
        }
    }

    public void send(MessageContext synCtx) {
        if (synCtx.isResponse())
            Axis2Sender.sendBack(synCtx);
        else
            Axis2Sender.sendOn(synCtx);
    }

    public MessageContext createMessageContext() {
        org.apache.axis2.context.MessageContext axis2MC
                = new org.apache.axis2.context.MessageContext();
        MessageContext mc = new Axis2MessageContext(axis2MC, synapseConfig, this);
        return mc;
    }

}
