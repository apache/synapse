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
package org.apache.synapse.mediator;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.MessageReceiver;

import org.apache.synapse.SynapseException;
import org.apache.synapse.mediator.Mediator;




/**
 * An Axis2 MessageReceiver which invokes a Synapse Mediator
 */
public class ClassMediatorMessageReceiver implements MessageReceiver {



    public static final String RESPONSE_PROPERTY = 
        MediatorMessageReceiver.class.getName()
            + ".mediatorResponse";
	
    public void receive(MessageContext mc) throws AxisFault {
        Mediator mediator = (Mediator) makeNewServiceObject(mc);
        boolean resp = mediator.mediate(mc);
        mc.setProperty(RESPONSE_PROPERTY, Boolean.valueOf(resp));
    }

    
    protected Object makeNewServiceObject(MessageContext msgContext) {

        
        
        Class c= (Class)msgContext.getProperty("synapse.mediator.class");
        System.out.println(c.getName());
        Object o;
		try {
			o = c.newInstance();
		} catch (Exception e) {
			throw new SynapseException(e);
		}
       	        
        return o;
    }
}