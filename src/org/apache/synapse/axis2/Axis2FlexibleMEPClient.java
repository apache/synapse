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
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;

import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.CommonsHTTPTransportSender;
import org.apache.axis2.util.UUIDGenerator;

import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;


import javax.xml.namespace.QName;


/**
 * This is a simple client that handles both in only and in out
 */
public class Axis2FlexibleMEPClient {

    // wholesale cut and paste from axis2.clientapi.*
    public static MessageContext send(MessageContext smc) {
        try {

            // create a lightweight Axis Config with no addressing
            AxisConfiguration ac = new AxisConfiguration();
            ConfigurationContext cc = new ConfigurationContext(ac);
            AxisServiceGroup asg = new AxisServiceGroup(ac);
            AxisService as = new AxisService("AnonymousService");
            asg.addService(as);
            ServiceGroupContext sgc = new ServiceGroupContext(cc, asg);
            ServiceContext sc = sgc.getServiceContext(as);
            AxisOperation axisOperationTemplate = new OutInAxisOperation(
                    new QName("TemplateOperation"));
            as.addOperation(axisOperationTemplate);
            cc.getAxisConfiguration().addService(as);
            TransportOutDescription tod = new TransportOutDescription(
                    new QName(org.apache.axis2.Constants.TRANSPORT_HTTP));
            tod.setSender(new CommonsHTTPTransportSender());

            ac.addTransportOut(tod);

            MessageContext msgCtx = new MessageContext(sc
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

            msgCtx.setEnvelope(outEnvelopeConfiguration(smc));
            msgCtx.setServiceContext(sc);

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

                msgCtx.setTransportOut(sc.getConfigurationContext()
                        .getAxisConfiguration().getTransportOut(
                        new QName(transport)));

            } else {
                throw new SynapseException("cannotInferTransport");
            }
            // initialize and set the Operation Context

            msgCtx.setOperationContext(axisOperationTemplate
                    .findOperationContext(msgCtx, sc));
            AxisEngine engine = new AxisEngine(cc);

            // engage addressing if desired
            Boolean engageAddressing =
                    (Boolean) smc.getProperty(Constants.ADD_ADDRESSING);
            if (engageAddressing != null) {
                if (engageAddressing.booleanValue()) ac.engageModule(new QName(
                        org.apache.axis2.Constants.MODULE_ADDRESSING));
            }

            engine.send(msgCtx);
            /**
             * for the response to be handle from SynapseEnvironment, we need
             * AxisConfiguration from the first dispatchiing
             * so we should have first MessageContext properties
             */

            MessageContext response = new MessageContext(smc
                    .getConfigurationContext(), smc.getSessionContext(),
                    smc.getTransportIn(), smc.getTransportOut());


            response.setProperty(MessageContext.TRANSPORT_IN, msgCtx
                    .getProperty(MessageContext.TRANSPORT_IN));
            msgCtx.getAxisOperation().registerOperationContext(response,
                    msgCtx.getOperationContext());

            response.setServerSide(false);
            response.setServiceContext(smc.getServiceContext());
            response.setServiceGroupContext(smc.getServiceGroupContext());
            response.setProperty(MessageContext.TRANSPORT_OUT,
                    smc.getProperty(MessageContext.TRANSPORT_OUT));
            response.setProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO,
                    smc.getProperty(
                            org.apache.axis2.Constants.OUT_TRANSPORT_INFO));

            // If request is REST we assume the response is REST, so set the
            // variable
            response.setDoingREST(msgCtx.isDoingREST());

            SOAPEnvelope resenvelope = TransportUtils.createSOAPMessage(
                    response, msgCtx.getEnvelope().getNamespace().getName());

            System.out.println(resenvelope.toString());

            response.setEnvelope(resenvelope);
            engine = new AxisEngine(msgCtx.getConfigurationContext());
            engine.receive(response);
            response.setProperty(Constants.ISRESPONSE_PROPERTY, new Boolean(
                    true));
            response.getOperationContext()
                    .setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN,
                            org.apache.axis2.Constants.VALUE_TRUE);
            return response;
        } catch (Exception e) {
            throw new SynapseException(e);
        }

    }

    private static SOAPEnvelope outEnvelopeConfiguration(MessageContext smc) {
        SOAPEnvelope env = smc.getEnvelope();
        env.getHeader().detach();
        return env;
    }

}
