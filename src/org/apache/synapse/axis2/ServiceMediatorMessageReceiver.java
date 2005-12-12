package org.apache.synapse.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.DependencyManager;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.receivers.AbstractMessageReceiver;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.api.Mediator;
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
*/

public class ServiceMediatorMessageReceiver extends AbstractMessageReceiver {
    public void receive(MessageContext messageContext) throws AxisFault {
        Object obj = makeNewServiceObject(messageContext);
        
        Mediator mediator = (Mediator)obj;
        
        if (EnvironmentAware.class.isAssignableFrom(mediator.getClass())) {
        	SynapseEnvironment se = (SynapseEnvironment)messageContext.getProperty(Constants.MEDIATOR_SYNAPSE_ENV_PROPERTY);
			((EnvironmentAware) mediator).setSynapseEnvironment(se);
			((EnvironmentAware) mediator).setClassLoader(messageContext.getAxisService().getClassLoader());
		}
        SynapseMessage smc = new Axis2SynapseMessage(messageContext);
        boolean returnValue = mediator.mediate(smc);
        messageContext.setProperty(Constants.MEDIATOR_STATUS, new Boolean(returnValue));
    }
}
