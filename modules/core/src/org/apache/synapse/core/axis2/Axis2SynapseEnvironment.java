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
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.axis2.engine.AxisConfiguration;

/**
 * <p> This is the Axis2 implementation of the MessageContext
 */
public class Axis2SynapseEnvironment implements SynapseEnvironment {

    private ClassLoader cl = null;
    /** If synapse is initialized by the SynapseAxis2Interceptor, the Axis2
     * class loaders were not initialized properly at init time. Hence in such
     * a case, the axisCfg would be set to refer to the Axis configuration
     * from which the correct and properly initialized classloader could be picked
     * up at runtime. This would be used only if the explicit classloader referrenced
     * by "cl" is null (i.e. has not been set) and the axisCfg is available.
     */
    private AxisConfiguration axisCfg = null;
    private static final Log log = LogFactory.getLog(Axis2SynapseEnvironment.class);

    public Axis2SynapseEnvironment() {
        super();
    }

    public Axis2SynapseEnvironment(ClassLoader cl) {
        super();
        this.cl = cl;
    }

    public Axis2SynapseEnvironment(AxisConfiguration axisCfg) {
        super();
        this.axisCfg = axisCfg;
    }

    public void injectMessage(MessageContext synCtx) {
        synCtx.setEnvironment(this);
        synCtx.getConfiguration().getMainMediator().mediate(synCtx);
    }

    public void send(MessageContext synCtx) {
        if (synCtx.isResponse())
            Axis2Sender.sendBack(synCtx);
        else
            Axis2Sender.sendOn(synCtx);
    }

    public ClassLoader getClassLoader() {
        if (cl != null) {
            return cl;
        } else if (axisCfg != null) {
            axisCfg.getServiceClassLoader();
        }
        return null;
    }

    public void setClassLoader(ClassLoader cl) {
        this.cl = cl;
    }

}
