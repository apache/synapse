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

import org.apache.axis2.modules.Module;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.*;
import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.SynapseConfigurationBuilder;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;

import javax.xml.namespace.QName;

public class SynapseModule implements Module, Constants {

    private static final String SYNAPSE_SERVICE_NAME = "synapse";
    private static final QName MEDIATE_OPERATION_Q_NAME = new QName("mediate");

    private static final Log log = LogFactory.getLog(SynapseModule.class);

    public void init(ConfigurationContext configurationContext,
                     AxisModule axisModule) throws AxisFault {
        log.info("Initializing Synapse Service from SynapseModule ..");
        // Dynamically initialize the Empty Synapse Service
        AxisConfiguration axisCfg = configurationContext.getAxisConfiguration();
        AxisService synapseService = new AxisService(SYNAPSE_SERVICE_NAME);
        AxisOperation mediateOperation =
                new InOutAxisOperation(MEDIATE_OPERATION_Q_NAME);
        mediateOperation.setMessageReceiver(new SynapseMessageReceiver());
        synapseService.addOperation(mediateOperation);
        axisCfg.addService(synapseService);

        // Initializing the SynapseEnvironment For Synapse to work

        log.info("Initializing Synapse Environment ...");

        SynapseConfiguration synCfg = null;

        // if the system property synapse.xml is specified, use it.. else default config
        String config = System.getProperty(Constants.SYNAPSE_XML);

        if (config != null) {
            log.info("System property '" + Constants.SYNAPSE_XML +
                     "' specifies synapse configuration as " + config);
            synCfg = SynapseConfigurationBuilder.getConfiguration(config);
        } else {
            log.info("System property '" + Constants.SYNAPSE_XML +
                     "' is not specified. Using default configuration");
            synCfg = SynapseConfigurationBuilder.getDefaultConfiguration();
        }

        // set the Synapse configuration and environment into the Axis2 configuration
        Parameter synapseCtxParam = new Parameter(SYNAPSE_CONFIG, null);
        synapseCtxParam.setValue(synCfg);

        Parameter synapseEnvParam = new Parameter(SYNAPSE_ENV, null);
        synapseEnvParam.setValue(new Axis2SynapseEnvironment(axisCfg));

        try {
            axisCfg.addParameter(synapseCtxParam);
            axisCfg.addParameter(synapseEnvParam);

        } catch (AxisFault e) {
            handleException(
                    "Could not set parameters '" + SYNAPSE_CONFIG +
                    "' and/or '" + SYNAPSE_ENV +
                    "'to the Axis2 configuration : " + e.getMessage(), e);
        }

        log.info("Synapse Environment initialized...");


    }

    public void engageNotify(AxisDescription axisDescription) throws AxisFault {
        // FixMe
    }

    public void shutdown(ConfigurationContext configurationContext)
            throws AxisFault {
        // FixMe
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
