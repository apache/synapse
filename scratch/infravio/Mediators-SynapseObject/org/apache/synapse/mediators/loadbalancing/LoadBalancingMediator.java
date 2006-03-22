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

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseException;
import org.apache.synapse.synapseobject.SynapseObject;
import org.apache.synapse.synapseobject.Utils;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.axis2.Axis2FlexibleMEPClient;
import org.apache.synapse.axis2.Axis2Sender;
import org.apache.synapse.axis2.Axis2SynapseMessage;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;

public class LoadBalancingMediator implements Mediator, EnvironmentAware {
    private static long modifiedTime;
    private ClassLoader classLoader;
    private SynapseEnvironment synapseEnvironment;
    private Log log = LogFactory.getLog(getClass());
    SynapseMessage request;
    SynapseMessage synMessage;
    MessageContext newMsg;
    private int currentService;
    SynapseObject loadbalancing;



    public LoadBalancingMediator() {
        log.info("LoadBalancing mediator created!");
    }

    public boolean mediate(SynapseMessage synapseMessage) {

        try {

            synapseEnvironment.setProperty("copy_message", ((Axis2SynapseMessage) synapseMessage).getMessageContext());

            loadConfig(synapseMessage);
            SynapseObject contract = loadbalancing.findSynapseObjectByAttributeValue(synapseMessage.getTo().getAddress());

            String strategy = contract.getString(LoadBalancingConstants.CFG_LOADBALANCING_STRATEGY);

            if(strategy.equalsIgnoreCase(LoadBalancingConstants.STRATEGY_PERF)){
                performanceStrategy(contract);
            }
            else
            {
                currentService = contract.getInteger("currentService").intValue();
                roundRobinStrategy(contract);
            }


            currentService=(currentService + 1)%(contract.getChildren().length);
            contract.setInteger("currentService", Integer.toString(currentService));

            synapseMessage.getEnvelope().getBody().getFirstElement().detach();
            synapseMessage.getEnvelope().getBody().setFirstChild(synMessage.getEnvelope().getBody().getFirstElement());
            EndpointReference temp = synapseMessage.getTo();
            synapseMessage.setTo(synapseMessage.getReplyTo());
            synapseMessage.setReplyTo(temp);
            synapseMessage.setResponse(true);
            Axis2Sender.sendBack(synapseMessage);
            return false;

        } catch (Exception e) {

            log.info(e);
            e.printStackTrace();
            return false;
        }
    }

    private void addCurrentServiceAttrib() {
        SynapseObject[] contracts = loadbalancing.getChildren();
        int length = contracts.length;
        int i;
        for(i=0;i<length;i++){
            contracts[i].setInteger("currentService","0");
        }
    }

    private void roundRobinStrategy(SynapseObject contract) {

        log.info("RoundRobin LOADBALNCING");
        SynapseObject[] services = contract.getChildren();
        int count = services.length;
        boolean flag;
        for(int i=0;i<count;i++){
             flag = callService(services[(i+currentService)%count].getString("url"));
             if(!flag) break;
        }
    }

    private boolean callService(String service){
       boolean isFaulting;
       request = new Axis2SynapseMessage((MessageContext) synapseEnvironment.getProperty("copy_message"));
       request.setTo(new EndpointReference(service));
       try {
            newMsg = Axis2FlexibleMEPClient.send(((Axis2SynapseMessage)request).getMessageContext());
            synMessage = new Axis2SynapseMessage(newMsg);
            isFaulting = false;
       }
       catch (AxisFault axisFault) {
            isFaulting = true;
            AxisEngine ae =
                        new AxisEngine(((Axis2SynapseMessage) request).getMessageContext().getConfigurationContext());
                try {
                    synMessage = new Axis2SynapseMessage(ae.createFaultMessageContext(((Axis2SynapseMessage) request).getMessageContext(), axisFault));
                } catch (AxisFault axisFault1) {
                    axisFault1.printStackTrace();
                }
            log.info(axisFault);
       }
       return isFaulting;
    }

    private void performanceStrategy(SynapseObject contract) {

          //TODO : ADD CALL LOGIC
    }


    private void loadConfig(SynapseMessage synapseMessageContext) {
        boolean flag = false;
        InputStream loadbalancingInStream;
        String resource;
        AxisConfiguration ac = ((Axis2SynapseMessage) synapseMessageContext).getMessageContext().getConfigurationContext().getAxisConfiguration();
        Parameter param = ac.getParameter("PolicyRepo");
        if (param != null) {
            resource = param.getValue().toString().trim() + File.separator + "policy"+File.separator+"mediators" + File.separator + LoadBalancingConstants.CFG_LOADBALANCING_XML;
            resource = resource.replace('\\','/');
            log.info("Resource from repo***" + resource);
            File file = new File(resource);
            try{

            flag = (file.lastModified() != modifiedTime);
            log.info("Flag***" + flag);
            modifiedTime = file.lastModified();
            }  catch(Exception e){
                e.printStackTrace();
            }
            log.info("Getting the stream***");
            try
            {
                loadbalancingInStream = new FileInputStream(file);
            }

            catch(Exception e)
            {
             log.info("ERROR HERE****"); e.printStackTrace();
             resource = LoadBalancingConstants.CFG_XML_FOLDER + "/" + LoadBalancingConstants.CFG_LOADBALANCING_XML;
             log.info("Resource from AAR***" + resource);
             loadbalancingInStream = this.classLoader.getResourceAsStream(resource);
            }
        } else {
            resource = LoadBalancingConstants.CFG_XML_FOLDER + "/" + LoadBalancingConstants.CFG_LOADBALANCING_XML;
            log.info("Resource from AAR***" + resource);
            loadbalancingInStream = this.classLoader.getResourceAsStream(resource);
       }
        log.info("Loadbalancing in stream " + resource);

        if ((!flag)&&(synapseEnvironment.getProperty("loadbalancing_synapseObject") != null))
        {
            loadbalancing = (SynapseObject) synapseEnvironment.getProperty("loadbalancing_synapseObject");
        } else {
            if(loadbalancingInStream!=null){
            loadbalancing = Utils.xmlToSynapseObject(loadbalancingInStream);
            addCurrentServiceAttrib();
            synapseEnvironment.setProperty("loadbalancing_synapseObject", loadbalancing);
            }
            else
            {
                log.info("NULL STREAM");
                throw new SynapseException("No config found");
            }
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
