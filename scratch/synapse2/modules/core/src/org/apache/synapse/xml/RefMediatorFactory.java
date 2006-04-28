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

import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.base.RefMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;


public class RefMediatorFactory extends
        AbstractMediatorFactory {
    private static final QName REF_Q = new QName(Constants.SYNAPSE_NAMESPACE,
            "ref");

    public QName getTagQName() {
        return REF_Q;
    }

    public Mediator createMediator(SynapseEnvironment se, OMElement el) {
        RefMediator rm = new RefMediator();
        super.setNameOnMediator(se, el, rm);
        OMAttribute attr = el.getAttribute(new QName("ref"));
        if (attr==null) throw new SynapseException("<ref> must have attribute ref");
        rm.setRef(attr.getAttributeValue());
        return rm;
    }

}
