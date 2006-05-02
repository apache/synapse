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
package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.synapse.config.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.ext.ClassMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;

/**
 *
 * <xmp><synapse:classmediator name="nm" class="org.fremantle.mediator"</synapse:classmediator></xmp>
 * TODO add ability to configure properties with Strings/OMElements based on children.
 */
public class ClassMediatorFactory extends AbstractMediatorFactory {
    private static final QName CLM_Q = new QName(Constants.SYNAPSE_NAMESPACE,
            "classmediator");
    public Mediator createMediator(SynapseEnvironment se, OMElement el) {
        ClassMediator cmp = new ClassMediator();
        super.setNameOnMediator(se, el, cmp);

        OMAttribute clsName = el.getAttribute(new QName("class"));
        if (clsName == null)
            throw new SynapseException("missing class attribute on element"
                    + el.toString());
        try {
            Class clazz = se.getClassLoader().loadClass(clsName.getAttributeValue());
            cmp.setClazz(clazz);
        } catch (ClassNotFoundException e) {
            throw new SynapseException("class loading error", e);
        }
        return cmp;

    }


    public QName getTagQName() {
        return CLM_Q;
    }

}
