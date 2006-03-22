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

package org.apache.synapse.mediators.failover;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.axis2.Axis2Sender;
import org.apache.synapse.axis2.Axis2SynapseMessage;

import java.io.InputStream;
import java.util.Map;


public class FailoverMediator implements Mediator, EnvironmentAware {

    FailoverConfiguration configuration;
    private ClassLoader classLoader;
    private SynapseEnvironment synapseEnvironment;
    private Log log = LogFactory.getLog(getClass());
    CallService callService;

    Boolean isNetworkErrorEnabled, isSOAPFaultEnabled, isTimeoutEnabled;


    public FailoverMediator() {
    }

    public boolean mediate(SynapseMessage synapseMessage) {
         SynapseMessage request;
        FailoverConfigurator failoverConfigurator;
        try {


            synapseEnvironment.setProperty("copy_message", ((Axis2SynapseMessage) synapseMessage).getMessageContext());
            log.info("FAILOVER MEDIATION");
            if(synapseEnvironment.getProperty("faoliver_config")!=null){
            failoverConfigurator  = (FailoverConfigurator) synapseEnvironment.getProperty("faoliver_config");
            }
            else
            {
            InputStream failoverInStream;
            String resource = FailoverConstants.CFG_XML_FOLDER + "/" + FailoverConstants.CFG_FAILOVER_XML;

            if ((failoverInStream = this.classLoader.getResourceAsStream(resource)) == null)
                log.info("INPUT STREAM NULL");

            failoverConfigurator =
                    new FailoverConfigurator(failoverInStream);
            synapseEnvironment.setProperty("faoliver_config",failoverConfigurator);
            }
            Map mediatorConfig = failoverConfigurator
                    .getConfig(synapseMessage.getTo());

            loadConfiguration(mediatorConfig);

            FailoverRule rules[] = configuration.getRules();
            int length = rules.length;
            boolean shouldFailover;
            if (this.synapseEnvironment == null) log.info("EMPTY ENVIRONMENT TO PASS");
            SynapseMessage reply = null;

            for (int i = 0; i < length; i++) {


                request = new Axis2SynapseMessage((MessageContext) synapseEnvironment.getProperty("copy_message"));



                String currentService = rules[i].getService();
                log.info("Service" + i + " : " + currentService);
                request.setTo(new EndpointReference(currentService));
				long timeoutValue = 0;
				if(isTimeoutEnabled.booleanValue())
				{
					timeoutValue=rules[i].getTimeout();
				}
                reply = callService.execute(timeoutValue, request);
                if (reply.isFaultResponse()) {
                    shouldFailover = evaluateFailoverScenario(reply);
                    if (!shouldFailover) break;
                } else break;
            }

            synapseMessage.getEnvelope().getBody().getFirstElement().detach();
            synapseMessage.getEnvelope().getBody().setFirstChild(reply.getEnvelope().getBody().getFirstElement());
            EndpointReference temp = synapseMessage.getTo();
            synapseMessage.setTo(synapseMessage.getReplyTo());
            synapseMessage.setReplyTo(temp);
            synapseMessage.setResponse(true);
            Axis2Sender.sendBack(synapseMessage);
            return true;
        } catch (Exception e) {
            log.info(e);
            return false;
        }
    }


    private boolean evaluateFailoverScenario(SynapseMessage synMessage) {

        // TODO : work out for the fault string

        return isNetworkErrorEnabled.booleanValue();
    }


    private void loadConfiguration(Map mediatorConfig) {

        configuration = new FailoverConfiguration();
        isNetworkErrorEnabled = (Boolean) mediatorConfig.get(FailoverConstants.CFG_FAILOVER_ON_NTWRK_ERROR);
        //isSOAPFaultEnabled = (Boolean) mediatorConfig.get(FailoverConstants.CFG_FAILOVER_ON_SOPAFAULT);
        isTimeoutEnabled = (Boolean) mediatorConfig.get(FailoverConstants.CFG_FAILOVER_ON_TIMEOUT);

        for (int i = 0; true; i++) {

            String serviceKey = FailoverConstants.CFG_PARAM_SERVICE +
                    "[" + i + "]";
            String activeKey = FailoverConstants.CFG_PARAM_ACTIVE + "[" + i + "]";
            String primaryKey = FailoverConstants.CFG_PARAM_PRIMARY + "[" + i + "]";
            String timeoutKey = FailoverConstants.CFG_PARAM_TIMEOUT + "[" + i + "]";

            if (mediatorConfig.get(serviceKey) == null) {
                break;
            }


            FailoverRule rule = new FailoverRule();
            rule.setService((String) mediatorConfig.get(serviceKey));
            rule.setActive((String) mediatorConfig.get(activeKey));
            rule.setPrimary((String) mediatorConfig.get(primaryKey));
            long temp = Long.parseLong(mediatorConfig.get(timeoutKey).toString());
            rule.setTimeout(temp);
            configuration.addRule(rule);
        }

        log.info("FAILOVER CONFIG MAP LOADED INTO RULESET!");

    }

    public void setSynapseEnvironment(SynapseEnvironment se) {

        if (se == null) {
            log.info("The Environment is null");
        }
        this.synapseEnvironment = se;
        callService = new CallService(this.synapseEnvironment);
    }

    public void setClassLoader(ClassLoader cl) {
        this.classLoader = cl;
    }
}
