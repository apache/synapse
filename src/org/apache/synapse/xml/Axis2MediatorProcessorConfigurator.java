package org.apache.synapse.xml;

import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.processors.Axis2MediatorProcessor;
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
*
*/

public class Axis2MediatorProcessorConfigurator extends AbstractProcessorConfigurator{

    private static final QName A2M_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"servicemediator");
    public Processor createProcessor(SynapseEnvironment se, OMElement el) {
        Axis2MediatorProcessor a2mp = new Axis2MediatorProcessor();
        super.setNameOnProcessor(se,el,a2mp);
        OMAttribute serviceName = el.getAttribute(new QName("service"));
        if (serviceName == null)
			throw new SynapseException("missing mediator attribute on element"
                    + el.toString());
		a2mp.setServiceMediatorName(serviceName.getAttributeValue());
		return a2mp;          
    }

    public QName getTagQName() {
        return A2M_Q;
    }
}
