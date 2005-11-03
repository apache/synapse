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

import javax.xml.namespace.QName;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.AxisService;


public class MediatorDescription {

    public static final String MEDIATOR_CLASSNAME = "mediatorClassName";

    private AxisService axisService;

    public MediatorDescription(AxisService axisService) {
        this.axisService = axisService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.synapse.mediator.MediatorDescription#getMediatorName()
     */
    public String getMediatorName() {
        QName serviceQname = axisService.getName();
        String mediatorName = serviceQname == null ? null : serviceQname
                .getLocalPart();
        return mediatorName;
    }

    /* (non-Javadoc)
     * @see org.apache.synapse.mediator.MediatorDescription#getClassName()
     */
    public String getClassName() {
        Parameter p = axisService.getParameter(MEDIATOR_CLASSNAME);
        String className = (String) (p == null ? null : p.getValue());
        return className;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.synapse.mediator.MediatorDescription#getClassLoader()
     */
    public ClassLoader getClassLoader() {
        return axisService.getClassLoader();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.synapse.mediator.MediatorDescription#getParameter(java.lang.String)
     */
    public Object getParameter(String name) {
        Parameter p = axisService.getParameter(name);
        Object value = p == null ? null : p.getValue();
        return value;
    }

}