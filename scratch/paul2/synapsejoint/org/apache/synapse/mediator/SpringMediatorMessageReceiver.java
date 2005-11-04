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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.MessageReceiver;

import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediator.Mediator;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ByteArrayResource;


/**
 * An Axis2 MessageReceiver which invokes a Synapse Mediator
 */
public class SpringMediatorMessageReceiver implements MessageReceiver {



    public static final String RESPONSE_PROPERTY = 
        MediatorMessageReceiver.class.getName()
            + ".mediatorResponse";
	
    public void receive(MessageContext mc) throws AxisFault {
        Mediator mediator = (Mediator) makeNewServiceObject(mc);
        boolean resp = mediator.mediate(mc);
        mc.setProperty(RESPONSE_PROPERTY, Boolean.valueOf(resp));
    }

    
    protected Object makeNewServiceObject(MessageContext msgContext) {

          
        
        byte[] xmlBytes= (byte[])msgContext.getProperty(Constants.SYNAPSE_MEDIATOR_XML_BYTES);
        String mediatorName = (String)msgContext.getProperty(Constants.SYNAPSE_SPRING_MEDIATOR_NAME);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN//EN\" \"http://www.springframework.org/dtd/spring-beans.dtd\">"
							.getBytes());
			baos.write(xmlBytes);
		} catch (IOException e) {
			throw new SynapseException(e);
		}
				
		GenericApplicationContext ctx = new GenericApplicationContext();
		XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(
				ctx);
		xbdr.setValidating(false);
		xbdr.loadBeanDefinitions(new ByteArrayResource(baos.toByteArray()));
		ctx.setClassLoader(msgContext.getAxisService().getClassLoader());
		ctx.refresh();
        Object o = ctx.getBean(mediatorName);
       	        
        return o;
    }
}