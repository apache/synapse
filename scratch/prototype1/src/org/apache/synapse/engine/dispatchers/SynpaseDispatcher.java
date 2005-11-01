package org.apache.synapse.engine.dispatchers;

import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.synapse.SynapseConstants;

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
* @author : Deepal Jayasinghe (deepal@apache.org)
*
*/

public class SynpaseDispatcher extends AbstractDispatcher {

    public static final QName NAME =
            new QName("http://synapse.ws.apache.org",
                    "SynapseDispatcher");
    String serviceName = null;
    QName operationName = null;

    public void initDispatcher() {
        init(new HandlerDescription(NAME));
    }

    public AxisService findService(MessageContext messageContext) throws AxisFault {
        return messageContext.getSystemContext().getAxisConfiguration()
                .getService(SynapseConstants.SYNAPSE_SERVICE);
    }

    public AxisOperation findOperation(AxisService service,
                                              MessageContext messageContext) throws AxisFault {
        return service.getOperation(SynapseConstants.SYNAPSE_OPERATION);
    }

}
