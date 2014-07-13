package org.apache.synapse.dipatcher;

import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.description.OperationDescription;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.synapse.SynapseConstant;
import org.apache.synapse.RuleEngine;
import org.apache.synapse.Mediators;
import org.apache.synapse.RuleObject;

import javax.xml.namespace.QName;
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
*
* @author : Deepal Jayasinghe (deepal@apache.org)
*
*/

public class SynapseDispatcher extends AbstractDispatcher {

    public static final QName NAME =
            new QName("http://synapse.ws.apache.org",
                    "SynapseDispatcher");
    String serviceName = null;
    QName operationName = null;

    public void initDispatcher() {
        init(new HandlerDescription(NAME));
    }
    

    public ServiceDescription findService(MessageContext messageContext) throws AxisFault {
        Parameter ruleeng= messageContext.getParameter(SynapseConstant.RULE_ENGINE);
        RuleEngine eng =(RuleEngine)ruleeng.getValue();
        RuleObject rulObj = (RuleObject)messageContext.getProperty(SynapseConstant.CURRENT_RULE_OBJECT);
        if(rulObj == null){
            System.out.println("rule object null");
            rulObj = eng.processRule(messageContext);
            messageContext.setProperty(SynapseConstant.CURRENT_RULE_OBJECT,rulObj);
        } else {
            System.out.println("Found object");
        }
        Mediators med = rulObj.processRule(messageContext);
        messageContext.setProperty(SynapseConstant.CURRENT_MEDIATOR,med);
        return messageContext.getSystemContext().getAxisConfiguration()
                .getService(SynapseConstant.SYNAPSE_SERVICE);
    }

    public OperationDescription findOperation(ServiceDescription serviceDescription,
                                              MessageContext messageContext) throws AxisFault {
        return serviceDescription.getOperation(SynapseConstant.SYNAPSE_OPERATION);
    }
}
