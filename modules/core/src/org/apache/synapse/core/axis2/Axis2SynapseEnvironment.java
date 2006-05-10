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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.core.SynapseEnvironment;

/**
 * <p> This is the Axis2 implementation of the SynapseMessageContext
 */
public class Axis2SynapseEnvironment implements SynapseEnvironment {

    private ClassLoader cl = null;
    private static final Log log = LogFactory.getLog(Axis2SynapseEnvironment.class);

    public Axis2SynapseEnvironment(ClassLoader cl) {
        super();
        this.cl = cl;
    }

    public void injectMessage(SynapseMessageContext synCtx) {
        synCtx.setSynapseEnvironment(this);
        synCtx.getConfiguration().getMainMediator().mediate(synCtx);
    }

    public void send(SynapseMessageContext synCtx) {
        if (synCtx.isResponse())
            Axis2Sender.sendBack(synCtx);
        else
            Axis2Sender.sendOn(synCtx);
    }

    public ClassLoader getClassLoader() {
        return cl;
    }

    public void setClassLoader(ClassLoader cl) {
        this.cl = cl;
    }

}
