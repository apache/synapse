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
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.receivers.AbstractMessageReceiver;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediator.LifecycleAware;
import org.apache.synapse.mediator.Mediator;
import org.apache.synapse.mediator.MediatorDescription;
import org.apache.synapse.mediator.MediatorFactory;

/**
 * An Axis2 MessageReceiver which invokes a Synapse Mediator
 */
public class MediatorMessageReceiver extends AbstractMessageReceiver {

    public static final String RESPONSE_PROPERTY = 
        MediatorMessageReceiver.class.getName()
            + ".mediatorResponse";

    public MediatorMessageReceiver() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.axis2.engine.MessageReceiver#receive(org.apache.axis2.context.MessageContext)
     */
    public void receive(MessageContext mc) throws AxisFault {
        Mediator mediator = (Mediator) getTheImplementationObject(mc);
        boolean resp = mediator.mediate(mc);
        mc.setProperty(RESPONSE_PROPERTY, Boolean.valueOf(resp));
    }

    /**
     * Override AbstractMessageReceiver.makeNewServiceObject so it creates
     * Mediators;
     */
    protected Object makeNewServiceObject(MessageContext msgContext) {

        AxisService axisService = msgContext.getOperationContext()
                .getServiceContext().getAxisService();
        MediatorDescription md = new MediatorDescription(axisService);
        
        Parameter param = axisService.getParameter(SERVICE_CLASS);
        String className = (String) param.getValue();

        ClassLoader cl = md.getClassLoader();
        Object o = instantiateObject(className, cl);

        if (o instanceof MediatorFactory) {
            o = ((MediatorFactory) o).createMediator(md);
        }

        if ((o instanceof Mediator) == false) {
            throw new SynapseException("Class does not implement "
                    + Mediator.class.getName() + ": " + o.getClass().getName());
        }

        if (o instanceof LifecycleAware) {
            ((LifecycleAware) o).init(md);
        }

        return o;
    }

    private Object instantiateObject(String className, ClassLoader cl) {
        Class objectClass;
        try {
            objectClass = Class.forName(className, true, cl);
        } catch (ClassNotFoundException e) {
            throw new SynapseException(
                    "Class not found exception creating mediator: " + className);
        }

        Object o;
        try {
            o = objectClass.newInstance();
        } catch (Exception e) {
            throw new SynapseException("exception creating mediator: "
                    + className, e);
        }
        return o;
    }

}