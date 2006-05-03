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


package org.apache.synapse.config;

import javax.xml.namespace.QName;


import org.apache.synapse.SynapseContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Constants;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.ext.ServiceMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;

public class ServiceMediatorFactory extends AbstractMediatorFactory {
    private static final QName tagName = new QName(Constants.SYNAPSE_NAMESPACE,
            "servicemediator");
    public Mediator createMediator(SynapseContext se, OMElement el) {
        ServiceMediator sm = new ServiceMediator();

        OMAttribute attr = el.getAttribute(new QName("service"));
        if (attr == null)
            throw new SynapseException(
                    "<servicemediator> must have 'service' attribute");
        sm.setServiceName(attr.getAttributeValue());
        return sm;
    }

    public QName getTagQName() {

        return tagName;
    }

}
