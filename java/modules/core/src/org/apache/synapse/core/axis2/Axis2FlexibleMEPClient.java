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

package org.apache.synapse.core.axis2;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.ws.policy.Policy;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This is a simple client that handles both in only and in out
 */
public class Axis2FlexibleMEPClient {

    private static final Log log = LogFactory.getLog(Axis2FlexibleMEPClient.class);

    /**
     * Based on the Axis2 client code. Sends the Axis2 Message context out and returns
     * the Axis2 message context for the response.
     *
     * Here Synapse works as a Client to the service. It would expect 200 ok, 202 ok and
     * 500 internal server error as possible responses. Currently the code expects
     * Synchronus operation
     *
     * @param wsAddressingEnabled
     * @param wsSecurityEnabled
     * @param outflowSecurityParameter
     * @param wsRMEnabled
     * @param wsRMPolicy
     * @param axisMsgCtx
     * @return The Axis2 reponse message context
     */
    public static MessageContext send(
        boolean wsAddressingEnabled,
        boolean wsSecurityEnabled,
        Parameter outflowSecurityParameter,
        Parameter inflowSecurityParameter,
        boolean wsRMEnabled,
        Policy wsRMPolicy,
        MessageContext axisMsgCtx) throws AxisFault {

        TransportOutDescription savedTransportOut = axisMsgCtx.getTransportOut();

        ConfigurationContext axisCfgCtx = axisMsgCtx.getConfigurationContext();
        AxisConfiguration axisCfg       = axisCfgCtx.getAxisConfiguration();

        AxisService anoymousService =
            AnonymousServiceFactory.getAnonymousService(
            axisCfg, wsAddressingEnabled, wsRMEnabled, wsSecurityEnabled);
        ServiceGroupContext sgc = new ServiceGroupContext(
            axisCfgCtx, (AxisServiceGroup) anoymousService.getParent());
        ServiceContext serviceCtx = sgc.getServiceContext(anoymousService);

        if (axisMsgCtx.getMessageID() != null) {
            axisMsgCtx.setMessageID(String.valueOf("uuid:" + UUIDGenerator.getUUID()));
        }

        axisMsgCtx.setConfigurationContext(serviceCtx.getConfigurationContext());

        // set SOAP envelope on the message context, removing WS-A headers
        axisMsgCtx.setEnvelope(removeAddressingHeaders(axisMsgCtx));

        // get a reference to the OUT-IN operation of the Anonymous Axis2 service
        AxisOperation axisAnonymousOperation = anoymousService.getOperation(
            new QName(AnonymousServiceFactory.OPERATION_OUT_IN));

        Options clientOptions = new Options();

        // if RM is requested, and if a WS-RM policy is specified, use it
        if (wsRMEnabled && wsRMPolicy != null) {
            axisAnonymousOperation.getPolicyInclude().
                addPolicyElement(PolicyInclude.OPERATION_POLICY, wsRMPolicy);
        }

        // if security is enabled,
        if (wsSecurityEnabled) {
            // if a WS-Sec OutflowSecurity parameter is specified, use it
            if (outflowSecurityParameter != null) {
                clientOptions.setProperty(
                org.apache.synapse.config.xml.Constants.OUTFLOW_SECURITY,
                outflowSecurityParameter);
            }

            // if a WS-Sec InflowSecurity parameter is specified, use it
            if (inflowSecurityParameter != null) {
                clientOptions.setProperty(
                org.apache.synapse.config.xml.Constants.INFLOW_SECURITY,
                inflowSecurityParameter);
            }
        }

        OperationClient mepClient = axisAnonymousOperation.createClient(
            serviceCtx, clientOptions);
        mepClient.addMessageContext(axisMsgCtx);
        mepClient.execute(true);

        MessageContext response = mepClient.getMessageContext(
            WSDLConstants.MESSAGE_LABEL_IN_VALUE);

        // set properties on response
        response.setServerSide(true);
        response.setProperty(Constants.ISRESPONSE_PROPERTY, Boolean.TRUE);
        response.setProperty(MessageContext.TRANSPORT_OUT,
            axisMsgCtx.getProperty(MessageContext.TRANSPORT_OUT));
        response.setProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO,
            axisMsgCtx.getProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO));
        response.setProperty(
                org.apache.synapse.Constants.PROCESSED_MUST_UNDERSTAND,
                axisMsgCtx.getProperty(
                        org.apache.synapse.Constants.PROCESSED_MUST_UNDERSTAND));
        response.setTransportIn(axisMsgCtx.getTransportIn());
        response.setTransportOut(savedTransportOut);

        // If request is REST assume that the response is REST too
        response.setDoingREST(axisMsgCtx.isDoingREST());

        return response;
    }

    /**
     * Removes Submission and Final WS-Addressing headers and return the SOAPEnvelope
     * from the given message context
     * @param axisMsgCtx the Axis2 Message context
     * @return the resulting SOAPEnvelope
     */
    private static SOAPEnvelope removeAddressingHeaders(MessageContext axisMsgCtx) {

        SOAPEnvelope env = axisMsgCtx.getEnvelope();
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
     * Remove WS-A headers
     * @param headerInformation headers to be removed
     */
    private static void detachAddressingInformation(ArrayList headerInformation) {
        Iterator iterator = headerInformation.iterator();
        while (iterator.hasNext()) {
            SOAPHeaderBlock headerBlock = (SOAPHeaderBlock) iterator.next();
            headerBlock.detach();
        }
    }
}
