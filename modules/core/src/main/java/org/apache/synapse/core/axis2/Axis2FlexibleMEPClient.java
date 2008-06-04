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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.util.MessageHelper;

import javax.xml.namespace.QName;
import java.util.ArrayList;

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
     * 500 internal server error as possible responses.
     *
     * @param endpoint the endpoint being sent to, maybe null
     * @param synapseOutMessageContext the outgoing synapse message
     * @throws AxisFault on errors
     */
    public static void send(

        EndpointDefinition endpoint,
        org.apache.synapse.MessageContext synapseOutMessageContext) throws AxisFault {

        boolean separateListener    = false;
        boolean wsSecurityEnabled   = false;
        String wsSecPolicyKey       = null;
        boolean wsRMEnabled         = false;
        String wsRMPolicyKey        = null;
        boolean wsAddressingEnabled = false;
        String wsAddressingVersion  = null;

        if (endpoint != null) {
            separateListener    = endpoint.isUseSeparateListener();
            wsSecurityEnabled   = endpoint.isSecurityOn();
            wsSecPolicyKey      = endpoint.getWsSecPolicyKey();
            wsRMEnabled         = endpoint.isReliableMessagingOn();
            wsRMPolicyKey       = endpoint.getWsRMPolicyKey();
            wsAddressingEnabled = endpoint.isAddressingOn() || wsSecurityEnabled || wsRMEnabled;
            wsAddressingVersion = endpoint.getAddressingVersion();
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "Sending [add = " + wsAddressingEnabled +
                "] [sec = " + wsSecurityEnabled +
                "] [rm = " + wsRMEnabled +
                (endpoint != null ?
                    "] [mtom = " + endpoint.isUseMTOM() +
                    "] [swa = " + endpoint.isUseSwa() +
                    "] [format = " + endpoint.getFormat() +
                    "] [force soap11=" + endpoint.isForceSOAP11() +
                    "] [force soap12=" + endpoint.isForceSOAP12() +
                    "] [pox=" + endpoint.isForcePOX() +
                    "] [get=" + endpoint.isForceGET() +
                    "] [encoding=" + endpoint.getCharSetEncoding() : "") +
                "] [to " + synapseOutMessageContext.getTo() + "]");
        }

        // save the original message context wihout altering it, so we can tie the response
        MessageContext originalInMsgCtx
            = ((Axis2MessageContext) synapseOutMessageContext).getAxis2MessageContext();

        // create a new MessageContext to be sent out as this should not corrupt the original
        // we need to create the response to the original message later on
        MessageContext axisOutMsgCtx = cloneForSend(originalInMsgCtx);

        // set all the details of the endpoint only to the cloned message context
        // so that we can use the original message context for resending through different endpoints
        if (endpoint != null) {

            if (SynapseConstants.FORMAT_POX.equals(endpoint.getFormat())) {
                axisOutMsgCtx.setDoingREST(true);

            } else if (SynapseConstants.FORMAT_GET.equals(endpoint.getFormat())) {
                axisOutMsgCtx.setDoingREST(true);
                axisOutMsgCtx.setProperty(Constants.Configuration.HTTP_METHOD,
                    Constants.Configuration.HTTP_METHOD_GET);
                
            } else if (SynapseConstants.FORMAT_SOAP11.equals(endpoint.getFormat())) {
                axisOutMsgCtx.setDoingREST(false);
                axisOutMsgCtx.removeProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE);
                if (axisOutMsgCtx.getSoapAction() == null && axisOutMsgCtx.getWSAAction() != null) {
                    axisOutMsgCtx.setSoapAction(axisOutMsgCtx.getWSAAction());
                }
                if(!axisOutMsgCtx.isSOAP11()) {
                    SOAPUtils.convertSOAP12toSOAP11(axisOutMsgCtx);
                }
                
            } else if (SynapseConstants.FORMAT_SOAP12.equals(endpoint.getFormat())) {
                axisOutMsgCtx.setDoingREST(false);
                axisOutMsgCtx.removeProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE);                
                if (axisOutMsgCtx.getSoapAction() == null && axisOutMsgCtx.getWSAAction() != null) {
                    axisOutMsgCtx.setSoapAction(axisOutMsgCtx.getWSAAction());
                }
                if(axisOutMsgCtx.isSOAP11()) {
                    SOAPUtils.convertSOAP11toSOAP12(axisOutMsgCtx);
                }                
                
            }

            if (endpoint.isUseMTOM()) {
                axisOutMsgCtx.setDoingMTOM(true);
                // fix / workaround for AXIS2-1798
                axisOutMsgCtx.setProperty(
                        org.apache.axis2.Constants.Configuration.ENABLE_MTOM,
                        org.apache.axis2.Constants.VALUE_TRUE);
                axisOutMsgCtx.setDoingMTOM(true);

            } else if (endpoint.isUseSwa()) {
                axisOutMsgCtx.setDoingSwA(true);
                // fix / workaround for AXIS2-1798
                axisOutMsgCtx.setProperty(
                        org.apache.axis2.Constants.Configuration.ENABLE_SWA,
                        org.apache.axis2.Constants.VALUE_TRUE);
                axisOutMsgCtx.setDoingSwA(true);
            }

            if (endpoint.getCharSetEncoding() != null) {
                axisOutMsgCtx.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING,
                        endpoint.getCharSetEncoding());
            }
            
            if (endpoint.getAddress() != null) {
                axisOutMsgCtx.setTo(new EndpointReference(endpoint.getAddress()));
            }

            if (endpoint.isUseSeparateListener()) {
                axisOutMsgCtx.getOptions().setUseSeparateListener(true);
            }
        }

        if (wsAddressingEnabled) {
            
            if (wsAddressingVersion != null &&
                    SynapseConstants.ADDRESSING_VERSION_SUBMISSION.equals(wsAddressingVersion)) {

                axisOutMsgCtx.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,
                        AddressingConstants.Submission.WSA_NAMESPACE);

            } else if (wsAddressingVersion != null &&
                    SynapseConstants.ADDRESSING_VERSION_FINAL.equals(wsAddressingVersion)) {

                axisOutMsgCtx.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,
                        AddressingConstants.Final.WSA_NAMESPACE);
            }
            
            axisOutMsgCtx.setProperty
                    (AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.FALSE);
        } else {
            axisOutMsgCtx.setProperty
                    (AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);
        }

        ConfigurationContext axisCfgCtx = axisOutMsgCtx.getConfigurationContext();
        AxisConfiguration axisCfg       = axisCfgCtx.getAxisConfiguration();

        AxisService anoymousService =
            AnonymousServiceFactory.getAnonymousService(synapseOutMessageContext.getConfiguration(),
            axisCfg, wsAddressingEnabled, wsRMEnabled, wsSecurityEnabled);
        ServiceGroupContext sgc = new ServiceGroupContext(
            axisCfgCtx, (AxisServiceGroup) anoymousService.getParent());
        ServiceContext serviceCtx = sgc.getServiceContext(anoymousService);

        boolean outOnlyMessage = "true".equals(synapseOutMessageContext.getProperty(
                SynapseConstants.OUT_ONLY)) || WSDL2Constants.MEP_URI_IN_ONLY.equals(
                originalInMsgCtx.getOperationContext()
                        .getAxisOperation().getMessageExchangePattern());

        // get a reference to the DYNAMIC operation of the Anonymous Axis2 service
        AxisOperation axisAnonymousOperation = anoymousService.getOperation(
            outOnlyMessage ?
                new QName(AnonymousServiceFactory.OUT_ONLY_OPERATION) :
                new QName(AnonymousServiceFactory.OUT_IN_OPERATION));

        Options clientOptions = new Options();
        clientOptions.setUseSeparateListener(separateListener);
        // if RM is requested,
        if (wsRMEnabled) {
            // if a WS-RM policy is specified, use it
            if (wsRMPolicyKey != null) {
                clientOptions.setProperty(
                    SynapseConstants.SANDESHA_POLICY,
                    getPolicy(synapseOutMessageContext, wsRMPolicyKey));
            }
            copyRMOptions(originalInMsgCtx, clientOptions);
        }

        // if security is enabled,
        if (wsSecurityEnabled) {
            // if a WS-Sec policy is specified, use it
            if (wsSecPolicyKey != null) {
                clientOptions.setProperty(
                    SynapseConstants.RAMPART_POLICY,
                    getPolicy(synapseOutMessageContext, wsSecPolicyKey));
            }
            // temporary workaround for https://issues.apache.org/jira/browse/WSCOMMONS-197
            if (axisOutMsgCtx.getEnvelope().getHeader() == null) {
                SOAPFactory fac = axisOutMsgCtx.isSOAP11() ?
                    OMAbstractFactory.getSOAP11Factory() : OMAbstractFactory.getSOAP12Factory();
                fac.createSOAPHeader(axisOutMsgCtx.getEnvelope());
            }
        }

        OperationClient mepClient = axisAnonymousOperation.createClient(serviceCtx, clientOptions);
        mepClient.addMessageContext(axisOutMsgCtx);
        axisOutMsgCtx.setAxisMessage(
            axisAnonymousOperation.getMessage(WSDLConstants.MESSAGE_LABEL_OUT_VALUE));

        if (!outOnlyMessage) {
            // always set a callback as we decide if the send it blocking or non blocking within
            // the MEP client. This does not cause an overhead, as we simply create a 'holder'
            // object with a reference to the outgoing synapse message context
            // synapseOutMessageContext
            AsyncCallback callback = new AsyncCallback(synapseOutMessageContext);
            if (endpoint != null) {
                // set the timeout time and the timeout action to the callback, so that the
                // TimeoutHandler can detect timed out callbacks and take approprite action.
                callback.setTimeOutOn(System.currentTimeMillis() + endpoint.getTimeoutDuration());
                callback.setTimeOutAction(endpoint.getTimeoutAction());
            } else {
                callback.setTimeOutOn(System.currentTimeMillis());
            }
            mepClient.setCallback(callback);
        }

        // with the nio transport, this causes the listener not to write a 202
        // Accepted response, as this implies that Synapse does not yet know if
        // a 202 or 200 response would be written back.
        originalInMsgCtx.getOperationContext().setProperty(
            org.apache.axis2.Constants.RESPONSE_WRITTEN, "SKIP");

        mepClient.execute(true);        
   }

    private static MessageContext cloneForSend(MessageContext ori) throws AxisFault {

        MessageContext newMC = MessageHelper.clonePartially(ori);

        newMC.setEnvelope(ori.getEnvelope());        
        removeAddressingHeaders(newMC);

        newMC.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
            ori.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));

        return newMC;
    }

    private static void copyRMOptions(MessageContext oriContext, Options targetOptions) {
        Options oriOptions = oriContext.getOptions();
        if (oriOptions.getProperty(SynapseConstants.MERCURY_LAST_MESSAGE) != null) {
            targetOptions.setProperty(SynapseConstants.MERCURY_LAST_MESSAGE,
                    oriOptions.getProperty(SynapseConstants.MERCURY_LAST_MESSAGE));
        }
        if (oriOptions.getProperty(SynapseConstants.MERCURY_SPEC_VERSION) != null) {
            targetOptions.setProperty(SynapseConstants.MERCURY_SPEC_VERSION,
                    oriOptions.getProperty(SynapseConstants.MERCURY_SPEC_VERSION));
        }
        if (oriOptions.getProperty(SynapseConstants.MERCURY_SEQUENCE_KEY) != null) {
            targetOptions.setProperty(SynapseConstants.MERCURY_SEQUENCE_KEY,
                    oriOptions.getProperty(SynapseConstants.MERCURY_SEQUENCE_KEY));
        }
        // todo:[ruwan] migrate to Mercury
//        if (oriOptions.getProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID) != null) {
//            targetOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,
//                    oriOptions.getProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID));
//        }
    }

    /**
     * Get the Policy object for the given name from the Synapse configuration at runtime
     * @param synCtx the current synapse configuration to get to the synapse configuration
     * @param propertyKey the name of the property which holds the Policy required
     * @return the Policy object with the given name, from the configuration
     */
    private static Policy getPolicy(org.apache.synapse.MessageContext synCtx, String propertyKey) {
        Object property = synCtx.getEntry(propertyKey);
        if (property != null && property instanceof OMElement) {
            return PolicyEngine.getPolicy((OMElement) property);
        } else {
            handleException("Cannot locate policy from the property : " + propertyKey);
        }
        return null;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * Removes Submission and Final WS-Addressing headers and return the SOAPEnvelope
     * from the given message context
     * @param axisMsgCtx the Axis2 Message context
     * @return the resulting SOAPEnvelope
     */
    public static SOAPEnvelope removeAddressingHeaders(MessageContext axisMsgCtx) {

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
        for (Object o : headerInformation) {
            if (o instanceof SOAPHeaderBlock) {
                SOAPHeaderBlock headerBlock = (SOAPHeaderBlock) o;
                headerBlock.detach();
            } else if (o instanceof OMElement) {
                // work around for a known addressing bug which sends non SOAPHeaderBlock objects
                OMElement om = (OMElement) o;
                OMNamespace ns = om.getNamespace();
                if (ns != null && (
                    AddressingConstants.Submission.WSA_NAMESPACE.equals(ns.getNamespaceURI()) ||
                        AddressingConstants.Final.WSA_NAMESPACE.equals(ns.getNamespaceURI()))) {
                    om.detach();
                }
            }
        }
    }
}
