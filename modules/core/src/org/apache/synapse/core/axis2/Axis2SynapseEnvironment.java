/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.core.axis2;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.util.threadpool.ThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
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

    public Axis2SynapseEnvironment(ConfigurationContext cfgCtx, SynapseConfiguration synapseConfig) {
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
        synCtx.getConfiguration().getMainMediator().mediate(synCtx);
    }

    public void send(MessageContext synCtx) {
        if (synCtx.isResponse())
            Axis2Sender.sendBack(synCtx);
        else
            Axis2Sender.sendOn(synCtx);
    }

    public MessageContext createMessageContext() {
        org.apache.axis2.context.MessageContext axis2MC = new org.apache.axis2.context.MessageContext();
        MessageContext mc = new Axis2MessageContext(axis2MC, synapseConfig, this);
        return mc;
    }

}
