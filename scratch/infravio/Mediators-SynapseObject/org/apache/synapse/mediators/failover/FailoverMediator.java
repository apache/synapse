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
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseException;
import org.apache.synapse.management.impl.ManagementInformationExchange;
import org.apache.synapse.management.MediatorExecInfoObject;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.axis2.Axis2Sender;
import org.apache.synapse.synapseobject.SynapseObject;
import org.apache.synapse.synapseobject.Utils;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Iterator;


public class FailoverMediator implements Mediator, EnvironmentAware {

    private ClassLoader classLoader;
    private SynapseEnvironment synapseEnvironment;
    private Log log = LogFactory.getLog(getClass());
    private static long modifiedTime;
    SynapseObject failover;
    CallService callService;
    int dirtyCount = 5;
    Boolean isNetworkErrorEnabled, isSOAPFaultEnabled, isTimeoutEnabled;


    public FailoverMediator() {
    }

    public boolean mediate(SynapseMessage synapseMessage) {
        SynapseMessage request;
        Map map = ((Axis2SynapseMessage)synapseMessage).getMessageContext().getOperationContext().getServiceContext().getProperties();
        Iterator itr = map.keySet().iterator();
        System.out.println("Count****" + map.size());
        while(itr.hasNext()){
            String so = (String)itr.next();
            System.out.println("THE OBJECT****" + so);
        }
        
        if(((Axis2SynapseMessage)synapseMessage).getMessageContext().getProperty("MIX")!=null){
            ManagementInformationExchange mix = (ManagementInformationExchange)((Axis2SynapseMessage)synapseMessage).getMessageContext().getProperty("MIX");
            mix.setSOAPRequest(((Axis2SynapseMessage)synapseMessage).getMessageContext().getEnvelope().toString());
            ((Axis2SynapseMessage)synapseMessage).getMessageContext().setProperty("MIX",mix);
        }
        long start = System.currentTimeMillis();
        MediatorExecInfoObject meio  = new MediatorExecInfoObject();
        meio.setMediatorName("FAILOVER");
        try {

            synapseEnvironment.setProperty("copy_message", ((Axis2SynapseMessage) synapseMessage).getMessageContext());
            log.info("FAILOVER MEDIATION");
            log.info("LOADING CONFIG");
            loadConfig(synapseMessage);
            log.info("--------Getting failover data for------ " + synapseMessage.getTo().getAddress());
            if(failover.findSynapseObjectByAttributeValue(synapseMessage.getTo().getAddress())!=null){
            SynapseObject contract = failover.findSynapseObjectByAttributeValue(synapseMessage.getTo().getAddress());
            int currentCount = contract.getInteger("count").intValue();
            if(currentCount==dirtyCount){
                resetDirtySettings(contract);
            }
            currentCount+=1;
            contract.setInteger("count", Integer.toString(currentCount));
            isTimeoutEnabled = contract.getBoolean(FailoverConstants.CFG_FAILOVER_ON_TIMEOUT);
            isSOAPFaultEnabled = contract.getBoolean(FailoverConstants.CFG_FAILOVER_ON_SOPAFAULT);
            isNetworkErrorEnabled = contract.getBoolean(FailoverConstants.CFG_FAILOVER_ON_NTWRK_ERROR);
            SynapseObject[] services = contract.getChildren();
            int length = services.length;
            if (this.synapseEnvironment == null) log.info("EMPTY ENVIRONMENT TO PASS");
            SynapseMessage reply = null;
            for (int i = 0; i < length; i++) {
                if((services[i].getBoolean(FailoverConstants.CFG_PARAM_ACTIVE).booleanValue())&&(!services[i].getBoolean("isFailing").booleanValue()))
                {
                request = new Axis2SynapseMessage((MessageContext) synapseEnvironment.getProperty("copy_message"));
                String currentService = services[i].getString("url");
                log.info("Service" + i + " : " + currentService);
                request.setTo(new EndpointReference(currentService));
                long timeoutValue = 0;
                if (isTimeoutEnabled.booleanValue()) {
                    timeoutValue = services[i].getLong(FailoverConstants.CFG_PARAM_TIMEOUT).longValue();
                }
                reply = callService.execute(timeoutValue, request);
                if (reply.isFaultResponse()) {
                    boolean shouldFailover = evaluateFailoverScenario(reply);
                    services[i].setBoolean("isFailing","true");
                    if (!shouldFailover) break;
                } else break;
            }
           }

            synapseMessage.getEnvelope().getBody().getFirstElement().detach();
            synapseMessage.getEnvelope().getBody().setFirstChild(reply.getEnvelope().getBody().getFirstElement());
            EndpointReference temp = synapseMessage.getTo();
            synapseMessage.setTo(synapseMessage.getReplyTo());
            synapseMessage.setReplyTo(temp);
            synapseMessage.setResponse(true);
            log.info("----The failover mediation ends----");
            if(((Axis2SynapseMessage)synapseMessage).getMessageContext().getProperty("MIX")!=null){
                ManagementInformationExchange mix = (ManagementInformationExchange)((Axis2SynapseMessage)synapseMessage).getMessageContext().getProperty("MIX");
                mix.setSOAPResponse(((Axis2SynapseMessage)synapseMessage).getMessageContext().getEnvelope().toString());
                meio.setMediatorExecutionTime(System.currentTimeMillis() - start);
                mix.setMediatorExecutionTime(meio);
                ((Axis2SynapseMessage)synapseMessage).getMessageContext().setProperty("MIX",mix);
            }
            Axis2Sender.sendBack(synapseMessage);
           }
             return true;
        } catch (Exception e) {
            log.info(e);
            return false;
        }

    }

    private void resetDirtySettings(SynapseObject service) {
            service.setInteger("count", "0");
            SynapseObject[] con_services = service.getChildren();
            int ser_length = con_services.length;
            int j;
            for (j = 0; j < ser_length; j++) {
                con_services[j].setBoolean("isFailing", "false");
            }
    }


    private void addDirtySettings() {
        SynapseObject[] contracts = failover.getChildren();
        int con_length = contracts.length;
        int i;
        for (i = 0; i < con_length; i++) {
            contracts[i].setInteger("count", "0");
            SynapseObject[] con_services = contracts[i].getChildren();
            int ser_length = con_services.length;
            int j;
            for (j = 0; j < ser_length; j++) {
                con_services[j].setBoolean("isFailing", "false");
            }
        }
    }


    private boolean evaluateFailoverScenario(SynapseMessage synMessage) {

       // TODO : Add logic to process the SOAP Faults and return based on the fault codes and strings

        return ((isSOAPFaultEnabled.booleanValue())||(isNetworkErrorEnabled.booleanValue()));
    }

    private void loadConfig(SynapseMessage synapseMessageContext) {
        boolean flag = false;
        InputStream failoverInStream;
        String resource;
        AxisConfiguration ac = ((Axis2SynapseMessage) synapseMessageContext).getMessageContext().getConfigurationContext().getAxisConfiguration();
        Parameter param = ac.getParameter("PolicyRepo");
        if (param != null) {
            resource = param.getValue().toString().trim() + File.separator + "policy"+File.separator+"mediators" + File.separator + FailoverConstants.CFG_FAILOVER_XML;
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
                failoverInStream = new FileInputStream(file);
             }

            catch(Exception e)
            {
             log.info("ERROR HERE****"); e.printStackTrace();
             resource = FailoverConstants.CFG_XML_FOLDER + "/" + FailoverConstants.CFG_FAILOVER_XML;
             log.info("Resource from AAR***" + resource);
             failoverInStream = this.classLoader.getResourceAsStream(resource);
            }
        } else {
            resource = FailoverConstants.CFG_XML_FOLDER + "/" + FailoverConstants.CFG_FAILOVER_XML;
            log.info("Resource from AAR***" + resource);
            failoverInStream = this.classLoader.getResourceAsStream(resource);
       }
        log.info("Failover in stream " + resource);

        if ((!flag)&&(synapseEnvironment.getProperty("failover_synapseObject") != null))
        {
            failover = (SynapseObject) synapseEnvironment.getProperty("failover_synapseObject");
        } else {
            if(failoverInStream!=null){
            failover = Utils.xmlToSynapseObject(failoverInStream);
            addDirtySettings();
            synapseEnvironment.setProperty("failover_synapseObject", failover);
            }
            else
            {
                log.info("NULL STREAM");
                throw new SynapseException("No config found");
            }
        }
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
