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


import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;

import org.apache.axis2.util.UUIDGenerator;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.deployment.util.PhasesInfo;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;

import org.apache.synapse.Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;


import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.ArrayList;


/**
 * This is a simple client that handles both in only and in out
 */
public class Axis2FlexibleMEPClient {

    public static SOAPEnvelope outEnvelopeConfiguration(MessageContext smc) {
        SOAPEnvelope env = smc.getEnvelope();
        SOAPHeader soapHeader = env.getHeader();
        ArrayList addressingHeaders;
        if (soapHeader != null) {
            addressingHeaders = soapHeader.getHeaderBlocksWithNSURI(
                    AddressingConstants.Submission.WSA_NAMESPACE);
            if (addressingHeaders != null && addressingHeaders.size() != 0) {
                detachAddressingInformation(addressingHeaders);

            } else {
                addressingHeaders = soapHeader.getHeaderBlocksWithNSURI(
                        AddressingConstants.Final.WSA_NAMESPACE);
                if (addressingHeaders != null && addressingHeaders.size() != 0) {
                    detachAddressingInformation(addressingHeaders);
                }
            }
        }
        return env;
    }

    /**
     *
     * @param headerInformation
     */
    private static void detachAddressingInformation(ArrayList headerInformation) {
        Iterator iterator = headerInformation.iterator();
        while (iterator.hasNext()) {
            SOAPHeaderBlock headerBlock = (SOAPHeaderBlock) iterator.next();
            headerBlock.detach();
        }

    }

    // Following code is based on Axis2 Client code.
    public static MessageContext send(MessageContext smc) throws AxisFault {
        // In this logic Synapse Work as a Client to a Server
        // So here this logic should expect 200 ok, 202 ok and 500 internal server error
        // current state of the code in Synchronus

        // This is the original_configuration_context
        ConfigurationContext cc = smc.getConfigurationContext();
        AxisConfiguration ac = cc.getAxisConfiguration();
        PhasesInfo phasesInfo = ac.getPhasesInfo();

        // setting operation default chains
        if (ac.getService("__ANONYMOUS_SERVICE__") == null) {
            // Lets default be OUT_IN
            OutInAxisOperation outInOperation =
                    new OutInAxisOperation(new QName(
                            "__OPERATION_OUT_IN__"));
            AxisService axisAnonymousService =
                    new AxisService("__ANONYMOUS_SERVICE__");
            axisAnonymousService.addOperation(outInOperation);
            ac.addService(axisAnonymousService);
            phasesInfo.setOperationPhases(outInOperation);
        }
        ServiceGroupContext sgc = new ServiceGroupContext(cc,
                (AxisServiceGroup)ac.getService("__ANONYMOUS_SERVICE__").getParent());
        ServiceContext sc =
                sgc.getServiceContext(new AxisService("__ANONYMOUS_SERVICE__"));

        MessageContext mc = new MessageContext();
        mc.setConfigurationContext(sc.getConfigurationContext());
        ///////////////////////////////////////////////////////////////////////
        // filtering properties
        if (smc.getSoapAction() != null)
            mc.setSoapAction(smc.getSoapAction());
        if (smc.getWSAAction() != null)
            mc.setWSAAction(smc.getWSAAction());
        if (smc.getFrom() != null)
            mc.setFrom(smc.getFrom());
        if (smc.getMessageID() != null)
            mc.setMessageID(smc.getMessageID());
        else
            mc.setMessageID(String.valueOf("uuid:"
                    + UUIDGenerator.getUUID()));
        if (smc.getReplyTo() != null)
            mc.setReplyTo(smc.getReplyTo());
        if (smc.getRelatesTo() != null)
            mc.setRelatesTo(smc.getRelatesTo());
        if (smc.getTo() != null) {
            mc.setTo(smc.getTo());
        } else {
            throw new AxisFault(
                    "To canno't be null, if null Synapse can't infer the transport");
        }
        if (smc.isDoingREST()) {
            mc.setDoingREST(true);
        }

        // handling the outbound message with addressing
        AxisModule module = ac.getModule(new QName(org.apache.axis2.Constants.MODULE_ADDRESSING));
        if ((smc.getProperty(Constants.ENGAGE_ADDRESSING_IN_MESSAGE) != null) ||
                (smc.getProperty(
                        Constants.ENGAGE_ADDRESSING_OUT_BOUND_MESSAGE) != null)){
            if (!ac.getService("__ANONYMOUS_SERVICE__")
                    .isEngaged(module.getName())) {
                ac.getService("__ANONYMOUS_SERVICE__").engageModule(module, ac);
            }
        }


        //TODO; following line needed to be removed

        mc.setEnvelope(outEnvelopeConfiguration(smc));

        AxisOperation axisAnonymousOperation =
                ac.getService("__ANONYMOUS_SERVICE__")
                        .getOperation(new QName("__OPERATION_OUT_IN__"));

        //Options class from Axis2 holds client side settings
        Options options = new Options();
        OperationClient mepClient =
                axisAnonymousOperation.createClient(sc, options);
        mepClient.addMessageContext(mc);
        mepClient.execute(true);
        MessageContext response = mepClient
                .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
        response.setProperty(MessageContext.TRANSPORT_OUT,
                smc.getProperty(MessageContext.TRANSPORT_OUT));
        response.setProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO,
                smc.getProperty(
                        org.apache.axis2.Constants.OUT_TRANSPORT_INFO));


        // If request is REST we assume the response is REST, so set the
        // variable
        response.setDoingREST(smc.isDoingREST());
        response.setProperty(Constants.ISRESPONSE_PROPERTY, Boolean.TRUE);

        if (ac.getService("__ANONYMOUS_SERVICE__")
                .isEngaged(module.getName())) {
            ac.getService("__ANONYMOUS_SERVICE__")
                    .disEngageModule(ac.getModule(module.getName()));
        }
        return response;
    }


}
