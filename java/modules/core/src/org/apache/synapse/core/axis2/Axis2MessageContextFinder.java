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

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.config.xml.SynapseConfigurationBuilder;
import org.apache.synapse.config.SynapseConfiguration;

import java.io.InputStream;

/**
 * <p/>
 * The MessageContext needs to be set up and then is used by the SynapseMessageReceiver to inject messages.
 * This class is used by the SynapseMessageReceiver to find the environment. The env is stored in a Parameter to the Axis2 config
 */
public class Axis2MessageContextFinder implements Constants {

    private static Log log = LogFactory.getLog(Axis2MessageContextFinder.class);

    public static synchronized MessageContext getSynapseMessageContext(org.apache.axis2.context.MessageContext axisMsgCtx) {

        SynapseConfiguration synCfg = getSynapseConfig(axisMsgCtx);
        SynapseEnvironment   synEnv = getSynapseEnvironment(axisMsgCtx);

        if (synCfg == null || synEnv == null) {
            initializeSynapse(axisMsgCtx);
            synCfg = getSynapseConfig(axisMsgCtx);
            synEnv = getSynapseEnvironment(axisMsgCtx);
        }

        if (synCfg == null || synEnv == null) {
            String msg = "Synapse could/has not been properly initialized";
            log.error(msg);
            throw new SynapseException(msg);
        }

        MessageContext synCtx = new Axis2MessageContext(axisMsgCtx, synCfg, synEnv);
        return synCtx;
    }

    /**
     * Create the SynapseConfiguration and SynapseEnvironment objects and set them into the Axis2 configuration
     * for reuse
     * @param mc the current Axis2 message context
     */
    private static synchronized void initializeSynapse(org.apache.axis2.context.MessageContext mc) {

        if (getSynapseConfig(mc) != null && getSynapseEnvironment(mc) != null) {
            // is this a second thread which came in just after initialization?
            return;
        }

        log.debug("Synapse Config not available. Creating...");
        AxisConfiguration ac = mc.getConfigurationContext().getAxisConfiguration();

        InputStream is = null;
        // Has a system property synapse.xml overwritten the synapse config location?
        if (System.getProperty(SYNAPSE_XML) == null) {
            Parameter param = ac.getParameter(SYNAPSE_CONFIGURATION);
            if (param == null) {
                throw new SynapseException(
                    "Axis2 configuration does not specify a '" + SYNAPSE_CONFIGURATION + "' parameter");
            }
            log.debug("Loading configuration from : " + ((String) param.getValue()));
            is = mc.getAxisService().getClassLoader().getResourceAsStream(((String) param.getValue()).trim());
        } else {
            log.debug("Loading configuration from : " + System.getProperty(SYNAPSE_XML));
            is = mc.getAxisService().getClassLoader().getResourceAsStream(System.getProperty(SYNAPSE_XML));
        }

        SynapseConfigurationBuilder cfgBuilder = new SynapseConfigurationBuilder();
        cfgBuilder.setConfiguration(is);

        Parameter synapseCtxParam = new Parameter(SYNAPSE_CONFIG, null);
        synapseCtxParam.setValue(cfgBuilder.getConfig());

        Parameter synapseEnvParam = new Parameter(SYNAPSE_ENV, null);
        synapseEnvParam.setValue(new Axis2SynapseEnvironment(mc.getAxisService().getClassLoader()));

        try {
            ac.addParameter(synapseCtxParam);
            ac.addParameter(synapseEnvParam);

        } catch (AxisFault e) {
            String msg = "Could not set parameters '" + SYNAPSE_CONFIG + "' and/or '" + SYNAPSE_ENV +
                "'to the Axis2 configuration";
            log.error(msg);
            throw new SynapseException(msg, e);
        }
    }

    private static SynapseConfiguration getSynapseConfig(org.apache.axis2.context.MessageContext mc) {
        AxisConfiguration ac = mc.getConfigurationContext().getAxisConfiguration();
        Parameter synConfigParam = ac.getParameter(SYNAPSE_CONFIG);
        if (synConfigParam != null) {
            return (SynapseConfiguration) synConfigParam.getValue();
        }
        return null;
    }

    private static SynapseEnvironment getSynapseEnvironment(org.apache.axis2.context.MessageContext mc) {
        AxisConfiguration ac = mc.getConfigurationContext().getAxisConfiguration();
        Parameter synEnvParam = ac.getParameter(SYNAPSE_ENV);
        if (synEnvParam != null) {
            return (SynapseEnvironment) synEnvParam.getValue();
        }
        return null;
    }

}
