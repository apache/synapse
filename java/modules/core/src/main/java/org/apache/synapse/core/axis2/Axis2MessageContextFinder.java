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

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

/**
 * <p/>
 * The MessageContext needs to be set up and then is used by the SynapseMessageReceiver to inject messages.
 * This class is used by the SynapseMessageReceiver to find the environment. The env is stored in a Parameter to the Axis2 config
 */
public class Axis2MessageContextFinder implements Constants {

    private static Log log = LogFactory.getLog(Axis2MessageContextFinder.class);

    public static MessageContext getSynapseMessageContext(
            org.apache.axis2.context.MessageContext axisMsgCtx)
            throws AxisFault {

        // we get the configuration on each message from the Axis2 configuration since the Synapse configuration
        // may be updated externally and thus should not be cached.

        SynapseConfiguration synCfg = getSynapseConfigFromAxisConfig(axisMsgCtx);
        SynapseEnvironment synEnv = getSynapseEnvironment(axisMsgCtx);

        if (synCfg == null || synEnv == null) {
            String msg = "Synapse environment has not initialized properly..";
            log.fatal(msg);
            throw new SynapseException(msg);
        }

        return new Axis2MessageContext(axisMsgCtx, synCfg, synEnv);
    }

    private static SynapseConfiguration getSynapseConfigFromAxisConfig(
            org.apache.axis2.context.MessageContext mc) {
        AxisConfiguration ac =
                mc.getConfigurationContext().getAxisConfiguration();
        Parameter synConfigParam = ac.getParameter(SYNAPSE_CONFIG);
        if (synConfigParam != null) {
            return (SynapseConfiguration) synConfigParam.getValue();
        }
        return null;
    }

    private static SynapseEnvironment getSynapseEnvironment(
            org.apache.axis2.context.MessageContext mc) {
        AxisConfiguration ac =
                mc.getConfigurationContext().getAxisConfiguration();
        Parameter synEnvParam = ac.getParameter(SYNAPSE_ENV);
        if (synEnvParam != null) {
            return (SynapseEnvironment) synEnvParam.getValue();
        }
        return null;
    }

}
