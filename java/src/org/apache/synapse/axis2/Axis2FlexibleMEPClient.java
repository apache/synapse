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

package org.apache.synapse.axis2;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.deployment.util.PhasesInfo;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.om.OMAbstractFactory;

import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;


import javax.xml.namespace.QName;


/**
 * This is a simple client that handles both in only and in out
 *
 */
public class Axis2FlexibleMEPClient {

    // wholesale cut and paste from axis2.clientapi.*
    public static MessageContext send(MessageContext smc) {
        try {

            ConfigurationContext configContext = null;
            ConfigurationContextFactory efac = new ConfigurationContextFactory();
            configContext = efac.buildClientConfigurationContext(null);
            QName assumedServiceName = new QName("AnonymousService");
            AxisService axisService = new AxisService(assumedServiceName);
            AxisOperation axisOperationTemplate = new OutInAxisOperation(
                    new QName("TemplateOperation"));
            PhasesInfo info = ((AxisConfiguration) configContext
                    .getAxisConfiguration()).getPhasesinfo();
            if (info != null) {
                info.setOperationPhases(axisOperationTemplate);
            }
            axisService.addOperation(axisOperationTemplate);
            configContext.getAxisConfiguration().addService(axisService);
            ServiceContext serviceContext = axisService.getParent()
                    .getServiceGroupContext(configContext).getServiceContext(
                    assumedServiceName.getLocalPart());

            MessageContext msgCtx = new MessageContext(serviceContext
                    .getConfigurationContext());

            if (smc.getSoapAction() != null)
                msgCtx.setSoapAction(smc.getSoapAction());
            if (smc.getTo() != null)
                msgCtx.setTo(smc.getTo());
            if (smc.getWSAAction() != null)
                msgCtx.setWSAAction(smc.getWSAAction());
            if (smc.getFrom() != null)
                msgCtx.setFrom(smc.getFrom());
            if (smc.getMessageID() != null)
                msgCtx.setMessageID(smc.getMessageID());
            else
                msgCtx.setMessageID(String.valueOf("uuid:"
                        + UUIDGenerator.getUUID()));
            if (smc.getReplyTo() != null)
                msgCtx.setReplyTo(smc.getReplyTo());
            if (smc.getRelatesTo() != null)
                msgCtx.setRelatesTo(smc.getRelatesTo());
            /**
             * We need to detach the body of the Env and attach only the body
             * part that is necessary
             */

            //msgCtx.setEnvelope(smc.getEnvelope());
            msgCtx.setEnvelope(outEnvelopeConfiguration(smc));
            if (msgCtx.getEnvelope().getHeader() == null)
                msgCtx.getEnvelope().getBody().insertSiblingBefore(
                        OMAbstractFactory.getSOAP11Factory()
                                .getDefaultEnvelope().getHeader());

            msgCtx.setServiceContext(serviceContext);

            EndpointReference epr = msgCtx.getTo();
            String transport = null;
            if (epr != null) {
                String toURL = epr.getAddress();
                int index = toURL.indexOf(':');
                if (index > 0) {
                    transport = toURL.substring(0, index);
                }
            }

            if (transport != null) {

                msgCtx.setTransportOut(serviceContext.getConfigurationContext()
                        .getAxisConfiguration().getTransportOut(
                        new QName(transport)));

            } else {
                throw new SynapseException("cannotInferTransport");
            }
            // initialize and set the Operation Context

            msgCtx.setOperationContext(axisOperationTemplate
                    .findOperationContext(msgCtx, serviceContext));
            AxisEngine engine = new AxisEngine(configContext);
            engine.send(msgCtx);

            MessageContext response = new MessageContext(msgCtx
                    .getConfigurationContext(), msgCtx.getSessionContext(), msgCtx
                    .getTransportIn(), msgCtx.getTransportOut());
            response.setProperty(MessageContext.TRANSPORT_IN, msgCtx
                    .getProperty(MessageContext.TRANSPORT_IN));
            msgCtx.getAxisOperation().registerOperationContext(response,
                    msgCtx.getOperationContext());
            response.setServerSide(false);
            response.setServiceContext(msgCtx.getServiceContext());
            response.setServiceGroupContext(msgCtx.getServiceGroupContext());

            // If request is REST we assume the response is REST, so set the
            // variable
            response.setDoingREST(msgCtx.isDoingREST());

            SOAPEnvelope resenvelope = TransportUtils.createSOAPMessage(
                    response, msgCtx.getEnvelope().getNamespace().getName());

            response.setEnvelope(resenvelope);
            engine = new AxisEngine(msgCtx.getConfigurationContext());
            engine.receive(response);
            response.setProperty(Constants.ISRESPONSE_PROPERTY, new Boolean(
                    true));
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new SynapseException(e);
        }

    }

    private static SOAPEnvelope outEnvelopeConfiguration(MessageContext smc) {
        SOAPEnvelope env = smc.getEnvelope();
        env.getHeader().detach();
        return env;
    }

}
