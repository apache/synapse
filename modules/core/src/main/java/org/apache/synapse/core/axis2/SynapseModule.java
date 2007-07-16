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
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.modules.Module;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.SynapseConfigurationBuilder;

import javax.xml.namespace.QName;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This is the Synapse Module implementation class, which would initialize Synapse when it is
 * deployed onto an Axis2 configuration.
 */
public class SynapseModule implements Module {

    private static final Log log = LogFactory.getLog(SynapseModule.class);

    private static final String SYNAPSE_SERVICE_NAME = "synapse";
    private static final QName MEDIATE_OPERATION_Q_NAME = new QName("mediate");

    public void init(ConfigurationContext configurationContext,
        AxisModule axisModule) throws AxisFault {

        try {
            InetAddress addr = InetAddress.getLocalHost();
            if (addr != null) {
                // Get IP Address
                String ipAddr = addr.getHostAddress();
                if (ipAddr != null) {
                    MDC.put("ip", ipAddr);
                }

                // Get hostname
                String hostname = addr.getHostName();
                if (hostname == null) {
                    hostname = ipAddr;
                }
                MDC.put("host", hostname);
            }
        } catch (UnknownHostException e) {
            log.warn("Unable to report hostname or IP address for tracing", e);
        }   
        // Initializing the SynapseEnvironment and SynapseConfiguration
        log.info("Initializing the Synapse configuration ...");
        SynapseConfiguration synCfg = initializeSynapse(configurationContext);

        log.info("Deploying the Synapse service..");
        // Dynamically initialize the Synapse Service and deploy it into Axis2
        AxisConfiguration axisCfg = configurationContext.getAxisConfiguration();
        AxisService synapseService = new AxisService(SYNAPSE_SERVICE_NAME);
        AxisOperation mediateOperation = new InOutAxisOperation(MEDIATE_OPERATION_Q_NAME);
        mediateOperation.setMessageReceiver(new SynapseMessageReceiver());
        synapseService.addOperation(mediateOperation);
        List transports = new ArrayList();
        transports.add(org.apache.axis2.Constants.TRANSPORT_HTTP);
        transports.add("https");
        synapseService.setExposedTransports(transports);
        axisCfg.addService(synapseService);

        log.info("Initializing Sandesha 2...");
        AxisModule sandeshaAxisModule = configurationContext.getAxisConfiguration().
            getModule(Constants.SANDESHA2_MODULE_NAME);
        if (sandeshaAxisModule != null) {
            Module sandesha2 = sandeshaAxisModule.getModule();
            sandesha2.init(configurationContext, sandeshaAxisModule);
        }

        log.info("Deploying Proxy services...");
        Iterator iter = synCfg.getProxyServices().iterator();
        while (iter.hasNext()) {
            ProxyService proxy = (ProxyService) iter.next();
            proxy.buildAxisService(synCfg, axisCfg);
            if (log.isDebugEnabled()) {
                log.debug("Deployed Proxy service : " + proxy.getName());
            }
            if (!proxy.isStartOnLoad()) {
                proxy.stop(synCfg);
            }
        }

        log.info("Synapse initialized successfully...!");
    }

    private static SynapseConfiguration initializeSynapse(
        ConfigurationContext cfgCtx) {

        cfgCtx.setProperty("addressing.validateAction", Boolean.FALSE);
        AxisConfiguration axisConfiguration = cfgCtx.getAxisConfiguration();
        SynapseConfiguration synapseConfiguration;

        String config = System.getProperty(Constants.SYNAPSE_XML);

        if (config != null) {
            if (log.isDebugEnabled()) {
                log.debug("System property '" + Constants.SYNAPSE_XML +
                        "' specifies synapse configuration as " + config);
            }
            synapseConfiguration = SynapseConfigurationBuilder.getConfiguration(config);
        } else {
            log.warn("System property '" + Constants.SYNAPSE_XML +
                "' is not specified. Using default configuration..");
            synapseConfiguration = SynapseConfigurationBuilder.getDefaultConfiguration();
        }

        // Set the Axis2 ConfigurationContext to the SynapseConfiguration
        synapseConfiguration.setAxisConfiguration(cfgCtx.getAxisConfiguration());

        // set the Synapse configuration and environment into the Axis2 configuration
        Parameter synapseCtxParam = new Parameter(Constants.SYNAPSE_CONFIG, null);
        synapseCtxParam.setValue(synapseConfiguration);
        MessageContextCreatorForAxis2.setSynConfig(synapseConfiguration);

        Parameter synapseEnvParam = new Parameter(Constants.SYNAPSE_ENV, null);
        Axis2SynapseEnvironment synEnv = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        synapseEnvParam.setValue(synEnv);
        MessageContextCreatorForAxis2.setSynEnv(synEnv);

        try {
            axisConfiguration.addParameter(synapseCtxParam);
            axisConfiguration.addParameter(synapseEnvParam);

        } catch (AxisFault e) {
            String msg =
                "Could not set parameters '" + Constants.SYNAPSE_CONFIG +
                    "' and/or '" + Constants.SYNAPSE_ENV +
                    "'to the Axis2 configuration : " + e.getMessage();
            log.fatal(msg, e);
            throw new SynapseException(msg, e);
        }
        
        return synapseConfiguration;
    }

    public void engageNotify(AxisDescription axisDescription) throws AxisFault {
        // ignore
    }

    public boolean canSupportAssertion(Assertion assertion) {
        return false;
    }

    public void applyPolicy(Policy policy, AxisDescription axisDescription) throws AxisFault {
        // no implementation
    }

    public void shutdown(ConfigurationContext configurationContext)
        throws AxisFault {
        // ignore
    }
}
