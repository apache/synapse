/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.core.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.IndirectEndpoint;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.util.MessageHelper;

import javax.xml.namespace.QName;

public class Axis2BlockingClient {

    private static final Log log = LogFactory.getLog(Axis2BlockingClient.class);

    private final static String DEFAULT_CLIENT_REPO = "./repository";
    private final static String DEFAULT_AXIS2_XML = "./repository/conf/axis2_blocking_client.xml";

    private ConfigurationContext configurationContext = null;
    private boolean initClientOptions = true;
    private boolean configurationContextCreated = false;

    public Axis2BlockingClient(String clientRepository, String axis2xml) {
        try {
            configurationContext
                    = ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                    clientRepository != null ? clientRepository : DEFAULT_CLIENT_REPO,
                    axis2xml != null ? axis2xml : DEFAULT_AXIS2_XML);
            configurationContextCreated = true;
        } catch (AxisFault e) {
            handleException("Error initializing Axis2 Blocking Client", e);
        }
    }

    public Axis2BlockingClient(ConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
    }

    public void cleanup() throws AxisFault {
        if (configurationContextCreated) {
            configurationContext.terminate();
        }
    }

    /**
     * Send the message to a given Leaf endpoint (Address/WSDL/Default) in a blocking manner
     *
     * @param endpoint  leaf Endpoint
     * @param synapseInMsgCtx Synapse Message Context to be sent
     * @return OutPut message Context
     * @throws Exception
     */
    public MessageContext send(Endpoint endpoint, MessageContext synapseInMsgCtx)
            throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("Start Sending the Message ");
        }

        if (endpoint instanceof IndirectEndpoint) {
            // Get the real endpoint if endpoint is an indirect one
            endpoint = ((IndirectEndpoint) endpoint).getRealEndpoint(synapseInMsgCtx);
        }

        AbstractEndpoint abstractEndpoint = (AbstractEndpoint) endpoint;
        if (!abstractEndpoint.isLeafEndpoint()) {
            handleException("Endpoint type not supported. Only leaf endpoints are supported");
        }
        EndpointDefinition endpointDefinition = abstractEndpoint.getDefinition();

        org.apache.axis2.context.MessageContext axisInMsgCtx =
                ((Axis2MessageContext) synapseInMsgCtx).getAxis2MessageContext();
        org.apache.axis2.context.MessageContext axisOutMsgCtx =
                new org.apache.axis2.context.MessageContext();

        String endpointReferenceValue = null;
        if (endpointDefinition.getAddress() != null) {
            endpointReferenceValue = endpointDefinition.getAddress();
        } else if (axisInMsgCtx.getTo() != null) {
            endpointReferenceValue = axisInMsgCtx.getTo().getAddress();
        } else {
            handleException("Service url, Endpoint or 'To' header is required");
        }
        if (log.isDebugEnabled()) {
            log.debug("EPR is set to : " + endpointReferenceValue);
        }
        axisOutMsgCtx.setTo(new EndpointReference(endpointReferenceValue));

        // Use the configuration context of the original ctx if local transport is selected
        if (endpointReferenceValue != null && endpointReferenceValue.startsWith(Constants.TRANSPORT_LOCAL)) {
            configurationContext = axisInMsgCtx.getConfigurationContext();
        }

        axisOutMsgCtx.setConfigurationContext(configurationContext);
        axisOutMsgCtx.setEnvelope(axisInMsgCtx.getEnvelope());

        // Fill MessageContext
        BlockingClientUtils.fillMessageContext(endpointDefinition, axisOutMsgCtx, synapseInMsgCtx);

        Options clientOptions;
        if (initClientOptions) {
            clientOptions = new Options();
        } else {
            clientOptions = axisInMsgCtx.getOptions();
        }
        // Fill Client options
        BlockingClientUtils.fillClientOptions(endpointDefinition, clientOptions, synapseInMsgCtx);

        AxisService anonymousService = AnonymousServiceFactory.getAnonymousService(null,
                configurationContext.getAxisConfiguration(), endpointDefinition.isAddressingOn(),
                endpointDefinition.isSecurityOn(), false);
        anonymousService.getParent().addParameter(SynapseConstants.HIDDEN_SERVICE_PARAM, "true");
        ServiceGroupContext serviceGroupContext = new ServiceGroupContext(configurationContext, (AxisServiceGroup) anonymousService.getParent());
        ServiceContext serviceCtx = serviceGroupContext.getServiceContext(anonymousService);
        axisOutMsgCtx.setServiceContext(serviceCtx);

        // Invoke
        boolean isOutOnly = isOutOnly(synapseInMsgCtx, axisOutMsgCtx);
        try {
            if (isOutOnly) {
                if (log.isDebugEnabled()) {
                    log.debug("invoking service in OUT_ONLY manner");
                }
                sendRobust(axisOutMsgCtx, clientOptions, anonymousService, serviceCtx);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("invoking service in OUT_IN manner");
                }
                org.apache.axis2.context.MessageContext result = sendReceive(axisOutMsgCtx,
                        clientOptions, anonymousService, serviceCtx);
                synapseInMsgCtx.setEnvelope(result.getEnvelope());
                synapseInMsgCtx.setProperty(NhttpConstants.HTTP_SC, result.getProperty(
                        SynapseConstants.HTTP_SENDER_STATUSCODE));
                return synapseInMsgCtx;
            }
        } catch (Exception ex) {
            synapseInMsgCtx.setProperty(SynapseConstants.BLOCKING_CLIENT_ERROR, "true");
            axisOutMsgCtx.getTransportOut().getSender().cleanup(axisOutMsgCtx);
            if (!isOutOnly) {
                if (ex instanceof AxisFault) {
                    AxisFault fault = (AxisFault) ex;
                    if (fault.getFaultCode() != null) {
                        synapseInMsgCtx.setProperty(SynapseConstants.ERROR_CODE,
                                fault.getFaultCode().getLocalPart());
                    }
                    synapseInMsgCtx.setProperty(SynapseConstants.ERROR_MESSAGE, fault.getMessage());

                    if (fault.getDetail() != null) {
                        synapseInMsgCtx.setProperty(SynapseConstants.ERROR_DETAIL, fault.getDetail());
                    }
                    synapseInMsgCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, ex);
                    org.apache.axis2.context.MessageContext faultMC = fault.getFaultMessageContext();
                    if (faultMC != null) {
                        synapseInMsgCtx.setProperty(NhttpConstants.HTTP_SC,
                                faultMC.getProperty(SynapseConstants.HTTP_SENDER_STATUSCODE));
                        synapseInMsgCtx.setEnvelope(faultMC.getEnvelope());
                    }
                }
                return synapseInMsgCtx;
            }
            handleException("Error sending Message to url : " +
                    ((AbstractEndpoint) endpoint).getDefinition().getAddress());
        }
        return null;
    }

    private void sendRobust(org.apache.axis2.context.MessageContext axisOutMsgCtx,
                            Options clientOptions, AxisService anonymousService,
                            ServiceContext serviceCtx) throws AxisFault {

        AxisOperation axisAnonymousOperation = anonymousService.getOperation(
                new QName(AnonymousServiceFactory.OUT_ONLY_OPERATION));
        OperationClient operationClient = axisAnonymousOperation.createClient(
                serviceCtx, clientOptions);
        operationClient.addMessageContext(axisOutMsgCtx);
        axisOutMsgCtx.setAxisMessage(axisAnonymousOperation.getMessage(
                WSDLConstants.MESSAGE_LABEL_OUT_VALUE));
        operationClient.execute(true);
        axisOutMsgCtx.getTransportOut().getSender().cleanup(axisOutMsgCtx);

    }

    private org.apache.axis2.context.MessageContext sendReceive(org.apache.axis2.context.MessageContext axisOutMsgCtx,
                                                                Options clientOptions,
                                                                AxisService anonymousService,
                                                                ServiceContext serviceCtx) throws AxisFault {

        AxisOperation axisAnonymousOperation = anonymousService.getOperation(
                new QName(AnonymousServiceFactory.OUT_IN_OPERATION));
        OperationClient operationClient = axisAnonymousOperation.createClient(
                serviceCtx, clientOptions);
        operationClient.addMessageContext(axisOutMsgCtx);
        axisOutMsgCtx.setAxisMessage(axisAnonymousOperation.getMessage(
                WSDLConstants.MESSAGE_LABEL_OUT_VALUE));
        operationClient.execute(true);
        org.apache.axis2.context.MessageContext resultMsgCtx = operationClient.getMessageContext(
                WSDLConstants.MESSAGE_LABEL_IN_VALUE);

        org.apache.axis2.context.MessageContext returnMsgCtx =
                new org.apache.axis2.context.MessageContext();
        returnMsgCtx.setEnvelope(MessageHelper.cloneSOAPEnvelope(resultMsgCtx.getEnvelope()));
        returnMsgCtx.setProperty(SynapseConstants.HTTP_SENDER_STATUSCODE,
                resultMsgCtx.getProperty(SynapseConstants.HTTP_SENDER_STATUSCODE));
        axisOutMsgCtx.getTransportOut().getSender().cleanup(axisOutMsgCtx);

        return returnMsgCtx;
    }

    private boolean isOutOnly(MessageContext messageIn,
                              org.apache.axis2.context.MessageContext axis2Ctx) {
        return "true".equals(messageIn.getProperty(SynapseConstants.OUT_ONLY)) ||
                axis2Ctx.getOperationContext() != null && WSDL2Constants.MEP_URI_IN_ONLY.equals(
                        axis2Ctx.getOperationContext().getAxisOperation().getMessageExchangePattern());
    }

    /**
     * Set whether to create new client options.
     * If set to false, client options from incoming message context is used.
     * @param initClientOptions whether to initialize client options
     */
    public void setInitClientOptions(boolean initClientOptions) {
        this.initClientOptions = initClientOptions;
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
