package org.apache.synapse.resources.xml;

import org.apache.synapse.api.Mediator;
import org.apache.synapse.xml.MediatorFactory;
import org.apache.synapse.xml.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;

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
 */

public class PropertyMediatorFactory implements MediatorFactory {

    private static final String PROPERTY = "property";

    private static final QName PROPERTY_Q =
            new QName(Constants.SYNAPSE_NAMESPACE,
                    PROPERTY);

    private static final QName PROPERTY_NAME_ATT_Q = new QName("name");

    public Mediator createMediator(SynapseEnvironment se, OMElement el) {
        PropertyMediator pm = new PropertyMediator();

        OMAttribute name = el.getAttribute(PROPERTY_NAME_ATT_Q);
        if (name == null) {
            throw new SynapseException(PROPERTY + " must have "
                    + PROPERTY_NAME_ATT_Q + " attribute: " + el.toString());
        }

        String value = el.getText();
        pm.setName(name.getAttributeValue());
        pm.setValue(value);

        return pm;  
    }

    public QName getTagQName() {
        return PROPERTY_Q;
    }
}
