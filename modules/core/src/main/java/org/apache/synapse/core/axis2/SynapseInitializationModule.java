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
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.modules.Module;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.ServerManager;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.SynapseConfigurationBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This is the Synapse Module implementation class, which would initialize Synapse when it is
 * deployed onto an Axis2 configuration.
 */
public class SynapseInitializationModule implements Module {

    private static final Log log = LogFactory.getLog(SynapseInitializationModule.class);
    private SynapseConfiguration synCfg;

    public void init(ConfigurationContext configurationContext,
        AxisModule axisModule) throws AxisFault {

        log.info("Initializing Synapse at : " + new Date());
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
            log.warn("Unable to determine hostname or IP address of the server for logging", e);
        }

        // this will deploy the mediators in the mediator extensions folder
        log.info("Loading mediator extensions...");
        configurationContext.getAxisConfiguration().getConfigurator().loadServices();

        // Initializing the SynapseEnvironment and SynapseConfiguration
        log.info("Initializing the Synapse configuration ...");
        synCfg = getConfiguration(configurationContext);

        log.info("Deploying the Synapse service..");
        // Dynamically initialize the Synapse Service and deploy it into Axis2
        AxisConfiguration axisCfg = configurationContext.getAxisConfiguration();
        AxisService synapseService = new AxisService(SynapseConstants.SYNAPSE_SERVICE_NAME);
        AxisOperation mediateOperation = new InOutAxisOperation(
            SynapseConstants.SYNAPSE_OPERATION_NAME);
        mediateOperation.setMessageReceiver(new SynapseMessageReceiver());
        synapseService.addOperation(mediateOperation);
        List transports = new ArrayList();
        transports.add(Constants.TRANSPORT_HTTP);
        transports.add(Constants.TRANSPORT_HTTPS);
        synapseService.setExposedTransports(transports);
        axisCfg.addService(synapseService);
        
        // this server name is given by system property SynapseServerName
        // otherwise take host-name
        // if nothing found assume localhost
        String thisServerName = ServerManager.getInstance().getServerName();
        if(thisServerName == null || thisServerName.equals("")) {
          try {
            InetAddress addr = InetAddress.getLocalHost();
            thisServerName = addr.getHostName();

          } catch (UnknownHostException e) {
            log.warn("Could not get local host name", e);
          }
          
          if(thisServerName == null || thisServerName.equals("")) {
            thisServerName = "localhost";
          }
        }
        log.info("Synapse server name : " + thisServerName);
        
        log.info("Deploying Proxy services...");
        
        for (ProxyService proxy : synCfg.getProxyServices()) {

            // start proxy service if either,
            // pinned server name list is empty
            // or pinned server list has this server name
            List pinnedServers = proxy.getPinnedServers();
            if (pinnedServers != null && !pinnedServers.isEmpty()) {
                if (!pinnedServers.contains(thisServerName)) {
                    log.info("Server name not in pinned servers list. Not deploying Proxy service : " + proxy.getName());
                    continue;
                }
            }

            proxy.buildAxisService(synCfg, axisCfg);
            log.info("Deployed Proxy service : " + proxy.getName());
            if (!proxy.isStartOnLoad()) {
                proxy.stop(synCfg);
            }
        }
        
        log.info("Synapse initialized successfully...!");
    }

    private static SynapseConfiguration getConfiguration(ConfigurationContext cfgCtx) {

        cfgCtx.setProperty("addressing.validateAction", Boolean.FALSE);
        AxisConfiguration axisConfiguration = cfgCtx.getAxisConfiguration();
        SynapseConfiguration synapseConfiguration;

        String config = ServerManager.getInstance().getSynapseXMLPath();

        if (config != null) {
            synapseConfiguration = SynapseConfigurationBuilder.getConfiguration(config);
        } else {
            log.warn("System property or init-parameter '" + SynapseConstants.SYNAPSE_XML +
                "' is not specified. Using default configuration..");
            synapseConfiguration = SynapseConfigurationBuilder.getDefaultConfiguration();
        }

        // Set the Axis2 ConfigurationContext to the SynapseConfiguration
        synapseConfiguration.setAxisConfiguration(cfgCtx.getAxisConfiguration());

        // set the Synapse configuration and environment into the Axis2 configuration
        Parameter synapseCtxParam = new Parameter(SynapseConstants.SYNAPSE_CONFIG, null);
        synapseCtxParam.setValue(synapseConfiguration);
        MessageContextCreatorForAxis2.setSynConfig(synapseConfiguration);

        Parameter synapseEnvParam = new Parameter(SynapseConstants.SYNAPSE_ENV, null);
        Axis2SynapseEnvironment synEnv = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        synapseEnvParam.setValue(synEnv);
        MessageContextCreatorForAxis2.setSynEnv(synEnv);

        try {
            axisConfiguration.addParameter(synapseCtxParam);
            axisConfiguration.addParameter(synapseEnvParam);

        } catch (AxisFault e) {
            String msg =
                "Could not set parameters '" + SynapseConstants.SYNAPSE_CONFIG +
                    "' and/or '" + SynapseConstants.SYNAPSE_ENV +
                    "'to the Axis2 configuration : " + e.getMessage();
            log.fatal(msg, e);
            throw new SynapseException(msg, e);
        }
        synapseConfiguration.init(synEnv);
        
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
    	synCfg.destroy();
    }
}
