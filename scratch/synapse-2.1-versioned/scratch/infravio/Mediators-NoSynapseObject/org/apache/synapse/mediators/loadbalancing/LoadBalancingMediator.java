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

package org.apache.synapse.mediators.loadbalancing;

import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.axis2.Axis2FlexibleMEPClient;
import org.apache.synapse.axis2.Axis2Sender;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMDocument;
import org.apache.axis2.soap.SOAPFactory;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.soap.SOAP12Constants;

import java.io.InputStream;
import java.util.Map;

public class LoadBalancingMediator implements Mediator, EnvironmentAware {

    LoadBalancingConfiguration configuration;
    static LoadBalancingConfigurator loadBalancingConfigurator;
    private ClassLoader classLoader;
    private SynapseEnvironment synapseEnvironment;
    private Map mediatorConfig;
    private Log log = LogFactory.getLog(getClass());
    SynapseMessage request;
    SynapseMessage synMessage;
    MessageContext newMsg;
    private int lastService;


    public LoadBalancingMediator() {
        log.info("LoadBalancing mediator created!");
    }

    public boolean mediate(SynapseMessage synapseMessage) {

        try {

            synapseEnvironment.setProperty("copy_message", ((Axis2SynapseMessage) synapseMessage).getMessageContext());
            String serviceName = synapseMessage.getTo().getAddress();
            if(synapseEnvironment.getProperty("loadbalancing_config")!=null){
            loadBalancingConfigurator  = (LoadBalancingConfigurator) synapseEnvironment.getProperty("loadbalancing_config");
            }
            else
            {
            InputStream loadBalancingInStream;
            String resource = LoadBalancingConstants.CFG_XML_FOLDER + "/" + LoadBalancingConstants.CFG_LOADBALANCING_XML;
                log.info("Resource = " + resource);
            if ((loadBalancingInStream = classLoader.getResourceAsStream(resource)) == null)
            log.info("INPUT STREAM NULL");

            loadBalancingConfigurator =
                    new LoadBalancingConfigurator(loadBalancingInStream);
            synapseEnvironment.setProperty("loadbalancing_config",loadBalancingConfigurator);
            }
            log.info("Before getting the MAP *********** \n " + ((Axis2SynapseMessage)synapseMessage).getMessageContext().getEnvelope());
            mediatorConfig = loadBalancingConfigurator
                    .getConfig(synapseMessage.getTo());
            loadConfiguration(mediatorConfig);
            LoadBalancingRule rules[] = configuration.getRules();
            String strategy = (String)mediatorConfig.get(LoadBalancingConstants.CFG_LOADBALANCING_STRATEGY);
            if(strategy.equalsIgnoreCase(LoadBalancingConstants.STRATEGY_PERF)){
                performanceStrategy(rules);
            }
            else //(strategy.equalsIgnoreCase(LoadBalancingConstants.STRATEGY_RR))
            {
                roundRobinStrategy(rules);
            }
            lastService = (lastService+1)%(rules.length);
            Integer service  = new Integer(lastService);
            mediatorConfig.put(LoadBalancingConstants.CFG_LOADBALANCING_LAST_SERVICE,service);
            LoadBalancingConfigurator.configMap.put(serviceName,mediatorConfig);
            synapseEnvironment.setProperty("loadbalancing_config",loadBalancingConfigurator);
            synapseMessage.getEnvelope().getBody().getFirstElement().detach();
            synapseMessage.getEnvelope().getBody().setFirstChild(synMessage.getEnvelope().getBody().getFirstElement());
            EndpointReference temp = synapseMessage.getTo();
            synapseMessage.setTo(synapseMessage.getReplyTo());
            synapseMessage.setReplyTo(temp);
            synapseMessage.setResponse(true);
            log.info("The final message : " + ((Axis2SynapseMessage)synapseMessage).getMessageContext().getEnvelope());
            Axis2Sender.sendBack(synapseMessage);
            return true;

        } catch (Exception e) {

            log.info(e);
            return false;
        }
    }

    private void roundRobinStrategy(LoadBalancingRule[] rules) {

        //TODO : ADD CALL LOGIC - added but check
        log.info("RR LOADBALNCING");
        LoadBalancingRule loadBalancedRules[] = sortForRR(rules);
        int count = loadBalancedRules.length;
        boolean flag=true;
        for(int i=0;i<count;i++){
             log.info("The service = " + loadBalancedRules[i].getService());
             flag = callService(loadBalancedRules[i].getService());
             if(!flag) break;
        }
        if(flag){
            log.info("Generating fault message!");
            generateFaultMsg();
        }
    }

    private void generateFaultMsg()
        {

        log.info("EXCEPTION CAUGHT FOR FAILOVER!");

        SOAPFactory factory;
        SOAPEnvelope envelope = synMessage.getEnvelope();
        if (envelope.getNamespace().getName().equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
            factory = OMAbstractFactory.getSOAP12Factory();
        } else {
            factory = OMAbstractFactory.getSOAP11Factory();
        }
        try {
            OMDocument soapFaultDocument = factory.createOMDocument();
            SOAPEnvelope faultEnvelope = factory.getDefaultFaultEnvelope();
            soapFaultDocument.addChild(faultEnvelope);
            newMsg.setEnvelope(faultEnvelope);
            synMessage = new Axis2SynapseMessage(newMsg);
            synMessage.setFaultResponse(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        }

    private boolean callService(String service){
       boolean isFaulting;
       log.info("Calling service : " + service);
       request = new Axis2SynapseMessage((MessageContext) synapseEnvironment.getProperty("copy_message"));
       request.setTo(new EndpointReference(service));
       log.info("The request : " + ((Axis2SynapseMessage)request).getMessageContext().getEnvelope());
       try {
            log.info("Sent request");
            newMsg = Axis2FlexibleMEPClient.send(((Axis2SynapseMessage)request).getMessageContext());
            synMessage = new Axis2SynapseMessage(newMsg);
            log.info("A OK - Received reply : " + ((Axis2SynapseMessage)synMessage).getMessageContext().getEnvelope());
            isFaulting = false;
       }
       catch (AxisFault axisFault) {
            isFaulting = true;
            log.info(axisFault);
       }
       return isFaulting;
    }

    private LoadBalancingRule[] sortForRR(LoadBalancingRule[] rules) {

        int length = rules.length;
        LoadBalancingRule tempRules[] = new LoadBalancingRule[length];
        for(int i=0;i<length;i++){
            tempRules[i] = rules[(i+lastService+1)%length];
        }
        return tempRules;
    }

    private void performanceStrategy(LoadBalancingRule[] rules) {

          //TODO : ADD CALL LOGIC
         LoadBalancingRule loadBalancedRules[] = sortForPerf(rules);
         System.out.println(loadBalancedRules);

    }

    private LoadBalancingRule[] sortForPerf(LoadBalancingRule[] rules) {

        LoadBalancingRule tempRules[] = rules;
        LoadBalancingRule ruleHolder;
        int length = tempRules.length;
        for(int i=0;i<length-1;i++)
            for(int j = i+1; j<length;j++)
                if(tempRules[i].getAvgResponseTime()>tempRules[j].getAvgResponseTime()){
                    ruleHolder = tempRules[i];
                    tempRules[i] = tempRules[j];
                    tempRules[j] = ruleHolder;
        }
        return tempRules;
    }

    private void loadConfiguration(Map mediatorConfig) {
        configuration = new LoadBalancingConfiguration();
        lastService = Integer.parseInt(mediatorConfig.get(LoadBalancingConstants.CFG_LOADBALANCING_LAST_SERVICE).toString());
        for (int i = 0; true; i++) {

            String serviceKey = LoadBalancingConstants.CFG_PARAM_SERVICE +
                    "[" + i + "]";
            String activeKey = LoadBalancingConstants.CFG_PARAM_ACTIVE + "[" + i + "]";
            String avgResponseKey = LoadBalancingConstants.CFG_PARAM_AVG_TIME + "[" + i + "]";
            String lastResponseKey = LoadBalancingConstants.CFG_PARAM_LAST_TIME + "[" + i + "]";
            String reqCountKey = LoadBalancingConstants.CFG_PARAM_REQ_COUNT + "[" + i + "]";

            if (mediatorConfig.get(serviceKey) == null) {
                break;
            }

            LoadBalancingRule rule = new LoadBalancingRule();
            rule.setService((String) mediatorConfig.get(serviceKey));
            rule.setActive((String) mediatorConfig.get(activeKey));
            long temp = Long.parseLong(mediatorConfig.get(avgResponseKey).toString());
            rule.setAvgResponseTime(temp);
            temp = Long.parseLong(mediatorConfig.get(lastResponseKey).toString());
            rule.setLastResponseTime(temp);
            temp = Long.parseLong(mediatorConfig.get(reqCountKey).toString());
            rule.setRequestCount(temp);
            configuration.addRule(rule);
        }

    }

    public void setSynapseEnvironment(SynapseEnvironment se) {

        if (se !=null) {
            this.synapseEnvironment = se;
        }
        else{
            log.info("The Environment is null");
        }
    }

    public void setClassLoader(ClassLoader cl) {
        if(cl!=null){
            this.classLoader = cl;
        }
        else{
            log.info("ClassLoader not set!");
        }
    }
}
