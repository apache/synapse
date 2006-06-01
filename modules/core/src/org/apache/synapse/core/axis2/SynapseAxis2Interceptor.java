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

import org.apache.axis2.engine.AxisObserver;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.AxisFault;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Constants;
import org.apache.synapse.config.SynapseConfigurationBuilder;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * The Synapse Axis2 interceptor will be invoked by Axis2 upon Axis initialization.
 * This allows the Synapse engine to be initialized at Axis2 startup, and store the
 * initial Synapse configuration into the AxisConfiguration for subsequent lookup.
 */
public class SynapseAxis2Interceptor implements AxisObserver, Constants {

    private static final Log log = LogFactory.getLog(SynapseAxis2Interceptor.class);

    /**
     * This is where Synapse is initialized at Axis2 startup
     * @param axisCfg the Axis2 Configuration
     */
    public void init(AxisConfiguration axisCfg) {

        log.info("Initializing Synapse...");

        SynapseConfiguration synCfg = null;

        // if the system property synapse.xml is specified, use it.. else default config
        String config = System.getProperty(Constants.SYNAPSE_XML);
        if (config != null) {
            log.info("System property '" + Constants.SYNAPSE_XML +
                "' specifies synapse configuration as " + config);
            synCfg = SynapseConfigurationBuilder.getConfiguration(config);
        } else {
            log.warn("System property '" + Constants.SYNAPSE_XML + "' is not specified. Using default configuration");
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
                "Could not set parameters '" + SYNAPSE_CONFIG + "' and/or '" + SYNAPSE_ENV +
                "'to the Axis2 configuration : " + e.getMessage(), e);
        }

        log.info("Synapse initialized...");
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    //---------------------------------------------------------------------------------------
    public void serviceUpdate(AxisEvent axisEvent, AxisService axisService) {
    }

    public void moduleUpdate(AxisEvent axisEvent, AxisModule axisModule) {
    }

    public void addParameter(Parameter parameter) throws AxisFault {
    }

    public void removeParameter(Parameter parameter) throws AxisFault {
    }

    public void deserializeParameters(OMElement elem) throws AxisFault {
    }

    public Parameter getParameter(String string) {
        return null;
    }

    public ArrayList getParameters() {
        return null;
    }

    public boolean isParameterLocked(String string) {
        return false;
    }
}
