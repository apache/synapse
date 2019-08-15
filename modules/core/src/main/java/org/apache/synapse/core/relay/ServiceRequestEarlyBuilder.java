/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.core.relay;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.transport.passthru.util.RelayConstants;
import org.apache.synapse.transport.passthru.util.RelayUtils;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

public class ServiceRequestEarlyBuilder extends AbstractHandler {

    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {
        OperationContext operationContext = messageContext.getOperationContext();
        if (operationContext == null) {
            AxisService service = messageContext.getAxisService();
            if (service != null) {
                if ("__ADDR_ONLY__".equals(service.getName())) {
                    // handling dual channel invocations
                    return buildMessage(messageContext);
                }

                // Possible service mediation mode
                AxisConfiguration axisConfiguration = messageContext.getConfigurationContext().
                        getAxisConfiguration();
                SynapseConfiguration synapseConfig = (SynapseConfiguration) axisConfiguration.getParameterValue(
                        SynapseConstants.SYNAPSE_CONFIG);
                ProxyService proxy = synapseConfig.getProxyService(service.getName());
                if (proxy != null) {
                    AxisOperation operation = messageContext.getAxisOperation();
                    if (proxy.isModuleEngaged() || (operation == null && proxy.isWsdlPublished())) {
                        // We have some Axis2 level processing to do (security, RM, addressing etc)
                        // or we have more dispatching left to do
                        return buildMessage(messageContext);
                    } else if (operation == null && !proxy.isWsdlPublished()) {
                        operation = service.getOperation(SynapseConstants.SYNAPSE_OPERATION_NAME);
                    }

                    if (operation != null) {
                        return invokeMessageReceiver(messageContext, operation);
                    }
                }
            } else {
                // Message mediation mode
                service = messageContext.getConfigurationContext().
                        getAxisConfiguration().getService(SynapseConstants.SYNAPSE_SERVICE_NAME);
                messageContext.setAxisService(service);
                AxisOperation operation = service.getOperation(SynapseConstants.SYNAPSE_OPERATION_NAME);
                messageContext.setAxisOperation(operation);
                return invokeMessageReceiver(messageContext, operation);
            }
        } else {
            MessageContext outMessage = operationContext.getMessageContext(
                    WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
            if (outMessage != null && Boolean.TRUE.equals(
                    outMessage.getProperty(RelayConstants.FORCE_RESPONSE_EARLY_BUILD))) {
                return buildMessage(messageContext);
            }
        }

        return InvocationResponse.CONTINUE;
    }

    private InvocationResponse invokeMessageReceiver(MessageContext messageContext,
                                AxisOperation operation) throws AxisFault {
        messageContext.getConfigurationContext().
                fillServiceContextAndServiceGroupContext(messageContext);
        OperationContext opContext = operation.findOperationContext(messageContext,
                messageContext.getServiceContext());
        messageContext.setOperationContext(opContext);
        operation.getMessageReceiver().receive(messageContext);
        return InvocationResponse.ABORT;
    }

    private InvocationResponse buildMessage(MessageContext messageContext) throws AxisFault {
        try {
            RelayUtils.buildMessage(messageContext, true);
        } catch (IOException e) {
            throw new AxisFault("I/O error while reading from the input stream");
        } catch (XMLStreamException e) {
            throw new AxisFault("Unexpected error while parsing the XML content");
        }
        return InvocationResponse.CONTINUE;
    }
}