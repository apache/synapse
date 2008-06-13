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

package org.apache.synapse.handler;

import org.apache.axis2.modules.Module;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.AxisFault;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.SynapseInitializationModule;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * This will be the Module class for the Synapse handler based mediations inside axis2 server. This
 * will just set the default system property of SYNAPSE_XML to the repository/conf/synapse.xml in
 * the axis2 servers repository and call the normal SynapseInitializationModule.init()
 *
 * @see org.apache.synapse.core.axis2.SynapseInitializationModule
 */
public class SynapseModule implements Module {

    /**
     * Log variable to be used as the logging appender
     */
    private static final Log log = LogFactory.getLog(SynapseModule.class);

    /**
     * Normal SynapseInitializationModule which initiates the Synapse
     */
    private SynapseInitializationModule initializationModule = null;

    /**
     * This method will call the normal initiation after setting the SYNAPSE_XML file to get from
     * the axis2 respository/conf folder
     * 
     * @param configurationContext - ConfigurationContext of the Axis2 env
     * @param axisModule - AxisModule describing handler initializationModule of Synapse
     * @throws AxisFault - incase of a failure in initiation
     */
    public void init(ConfigurationContext configurationContext,
                     AxisModule axisModule) throws AxisFault {
        if (System.getProperty(SynapseConstants.SYNAPSE_XML) == null) {
            System.setProperty(SynapseConstants.SYNAPSE_XML, configurationContext.
                    getAxisConfiguration().getRepository().getPath() + "/conf/synapse.xml");
        }
        if (new File(System.getProperty(SynapseConstants.SYNAPSE_XML)).exists()) {
            initializationModule = new org.apache.synapse.core.axis2.SynapseInitializationModule();
            initializationModule.init(configurationContext, axisModule);

            // now initialize SynapseConfig
            Parameter synEnv = configurationContext
                .getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_ENV);
            Parameter synCfg = configurationContext
                .getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_CONFIG);
            String message = "Unable to initialize the Synapse Configuration : Can not find the ";
            if (synCfg == null || synCfg.getValue() == null
                || !(synCfg.getValue() instanceof SynapseConfiguration)) {
                log.fatal(message + "Synapse Configuration");
                throw new SynapseException(message + "Synapse Configuration");
            }

            if (synEnv == null || synEnv.getValue() == null
                || !(synEnv.getValue() instanceof SynapseEnvironment)) {
                log.fatal(message + "Synapse Environment");
                throw new SynapseException(message + "Synapse Environment");
            }

            ((SynapseConfiguration) synCfg.getValue()).init((SynapseEnvironment) synEnv.getValue());
        } else {
            handleException("Unable to initialize the Synapse initializationModule. Couldn't " +
                    "find the configuration file in the location "
                    + System.getProperty(SynapseConstants.SYNAPSE_XML));
        }
    }

    /**
     * Just do what the main SynapseInitializationModule tells you to do
     * 
     * @param axisDescription
     * @throws AxisFault
     */
    public void engageNotify(AxisDescription axisDescription) throws AxisFault {
        if (initializationModule != null) {
            initializationModule.engageNotify(axisDescription);
        } else {
            handleException("Couldn't find the initializationModule");
        }
    }

    /**
     * Just do what the main SynapseInitializationModule tells you to do
     * 
     * @param assertion
     * @return
     */
    public boolean canSupportAssertion(Assertion assertion) {
        if (initializationModule != null) {
            return initializationModule.canSupportAssertion(assertion);
        } else {
            return false;
        }
    }

    /**
     * Just do what the main SynapseInitializationModule tells you to do
     * 
     * @param policy
     * @param axisDescription
     * @throws AxisFault
     */
    public void applyPolicy(Policy policy, AxisDescription axisDescription) throws AxisFault {
        if (initializationModule != null) {
            initializationModule.applyPolicy(policy, axisDescription);
        } else {
            handleException("Couldn't find the initializationModule");
        }
    }

    /**
     * Just do what the main SynapseInitializationModule tells you to do
     * 
     * @param configurationContext
     * @throws AxisFault
     */
    public void shutdown(ConfigurationContext configurationContext) throws AxisFault {
        if (initializationModule != null) {
            initializationModule.shutdown(configurationContext);
        } else {
            handleException("Couldn't find the initializationModule");
        }
    }

    private void handleException(String message) throws AxisFault {
        log.error(message);
        throw new AxisFault(message);
    }
}
