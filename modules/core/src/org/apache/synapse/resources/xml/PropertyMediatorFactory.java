package org.apache.synapse.resources.xml;

import org.apache.synapse.xml.ProcessorConfigurator;
import org.apache.synapse.xml.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMAttribute;

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

public class PropertyMediatorFactory implements ProcessorConfigurator {

    private static final String PROPERTY = "property";

    private static final QName PROPERTY_Q =
            new QName(Constants.SYNAPSE_NAMESPACE,
                    PROPERTY);

    private static final QName PROPERTY_NAME_ATT_Q = new QName("name");

    public Processor createProcessor(SynapseEnvironment se, OMElement el) {
        PropertyMediator pp = new PropertyMediator();

        OMAttribute name = el.getAttribute(PROPERTY_NAME_ATT_Q);
        if (name == null) {
            throw new SynapseException(PROPERTY + " must have "
                    + PROPERTY_NAME_ATT_Q + " attribute: " + el.toString());
        }

        String value = el.getText();
        pp.setName(name.getAttributeValue());
        pp.setValue(value);

        return pp;  
    }

    public QName getTagQName() {
        return PROPERTY_Q;
    }
}
