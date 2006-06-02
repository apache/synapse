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
import org.apache.synapse.config.SynapseConfigurationBuilder;
import org.apache.synapse.config.SynapseConfiguration;

import java.util.Iterator;

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

        SynapseConfiguration synCfg = getSynapseConfig(axisMsgCtx);
        SynapseEnvironment synEnv = getSynapseEnvironment(axisMsgCtx);

        if (synCfg == null || synEnv == null) {
            initializeSynapse(axisMsgCtx);
            synCfg = getSynapseConfig(axisMsgCtx);
            synEnv = getSynapseEnvironment(axisMsgCtx);
        }

        return new Axis2MessageContext(axisMsgCtx, synCfg, synEnv);
    }

    public static SynapseConfiguration initializeSynapseConfigurationBuilder(
            AxisConfiguration axisConfiguration) {
        /*
        First Check, if synapse.xml is provided as an system property, use it..
        else
        check if the synapse.xml is available via Axis2.xml SynapseConfiguration
        else
        default config [which is only the passthrow case]

        Priorty will be given to the System property.
        */
        SynapseConfiguration synapseConfiguration;
        Parameter configParam =
                axisConfiguration.getParameter(SYNAPSE_CONFIGURATION);

        String config = System.getProperty(Constants.SYNAPSE_XML);

        if (config != null) {
            log.info("System property '" + Constants.SYNAPSE_XML +
                     "' specifies synapse configuration as " + config);
            synapseConfiguration =
                    SynapseConfigurationBuilder.getConfiguration(config);
        } else if (configParam != null) {
            log.info(
                    "Synapse.xml is available via SynapseConfiguration in Axis2.xml");
            synapseConfiguration = SynapseConfigurationBuilder
                    .getConfiguration(configParam.getValue().toString().trim());
        } else {
            log.warn("System property '" + Constants.SYNAPSE_XML +
                     "' is not specified or SynapseConfiguration" +
                     "is not available via Axis2.xml.Thus,  Using default configuration");
            synapseConfiguration =
                    SynapseConfigurationBuilder.getDefaultConfiguration();
        }

        // set the Synapse configuration and environment into the Axis2 configuration
        Parameter synapseCtxParam = new Parameter(SYNAPSE_CONFIG, null);
        synapseCtxParam.setValue(synapseConfiguration);

        Parameter synapseEnvParam = new Parameter(SYNAPSE_ENV, null);
        synapseEnvParam
                .setValue(new Axis2SynapseEnvironment(axisConfiguration));

        try {
            axisConfiguration.addParameter(synapseCtxParam);
            axisConfiguration.addParameter(synapseEnvParam);

        } catch (AxisFault e) {
            handleException(
                    "Could not set parameters '" + SYNAPSE_CONFIG +
                    "' and/or '" + SYNAPSE_ENV +
                    "'to the Axis2 configuration : " + e.getMessage(), e);
        }
        return synapseConfiguration;

    }

    /**
     * Create the SynapseConfiguration and SynapseEnvironment objects and set them into the Axis2 configuration
     * for reuse
     *
     * @param mc the current Axis2 message context
     */
    private static synchronized void initializeSynapse(
            org.apache.axis2.context.MessageContext mc) throws AxisFault {

        if (getSynapseConfig(mc) != null && getSynapseEnvironment(mc) != null) {
            // is this a second thread which came in just after initialization?
            return;
        }

        log.info("Initializing Synapse...");
        AxisConfiguration axisCfg =
                mc.getConfigurationContext().getAxisConfiguration();

        SynapseConfiguration synCfg =
                initializeSynapseConfigurationBuilder(axisCfg);

        log.info("Initializing Proxy services...");
        if (synCfg == null) {
            handleException("SynapseConfiguration wouldn't initialize");
        } else {
            Iterator iter = synCfg.getProxyServices().iterator();
            while (iter.hasNext()) {
                ProxyService proxy = (ProxyService) iter.next();
                axisCfg.addService(proxy.buildAxisService());
            }
        }


        log.info("Synapse initialized...");
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static SynapseConfiguration getSynapseConfig(
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
