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

import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
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
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.EndpointDefinition;

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
     * @param endpoint
     * @param synapseOutMessageContext
     * @return The Axis2 reponse message context
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

        if (endpoint != null) {
            separateListener    = endpoint.isUseSeparateListener();
            wsSecurityEnabled   = endpoint.isSecurityOn();
            wsSecPolicyKey      = endpoint.getWsSecPolicyKey();
            wsRMEnabled         = endpoint.isReliableMessagingOn();
            wsRMPolicyKey       = endpoint.getWsRMPolicyKey();
            wsAddressingEnabled = endpoint.isAddressingOn() || wsSecurityEnabled || wsRMEnabled;
        }

        log.debug("sending [add = "+wsAddressingEnabled+"] [sec = "+wsSecurityEnabled + "] [ rm = "+wsRMEnabled+"] [ to "+synapseOutMessageContext.getTo()+"]");

        // save the original message context wihout altering it, so we can tie the response
        MessageContext originalInMsgCtx = ((Axis2MessageContext) synapseOutMessageContext).getAxis2MessageContext();

        // create a new MessageContext to be sent out as this should not corrupt the original
        // we need to create the response to the original message later on
        MessageContext axisOutMsgCtx = cloneForSend(originalInMsgCtx);

        if (endpoint != null) {
            if (endpoint.isForcePOX()) {
                axisOutMsgCtx.setDoingREST(true);
            } else if (endpoint.isForceSOAP()) {
                axisOutMsgCtx.setDoingREST(false);
            }

            if (endpoint.isUseMTOM()) {
                axisOutMsgCtx.setDoingMTOM(true);
            } else if (endpoint.isUseSwa()) {
                axisOutMsgCtx.setDoingSwA(true);
            }

            if (endpoint.isUseSeparateListener()) {
                axisOutMsgCtx.setProperty(Constants.OUTFLOW_USE_SEPARATE_LISTENER, Boolean.TRUE);
            }
        }

        if (wsAddressingEnabled) {
            axisOutMsgCtx.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.FALSE);
        }

        ConfigurationContext axisCfgCtx = axisOutMsgCtx.getConfigurationContext();
        AxisConfiguration axisCfg       = axisCfgCtx.getAxisConfiguration();

        AxisService anoymousService =
            AnonymousServiceFactory.getAnonymousService(
            axisCfg, wsAddressingEnabled, wsRMEnabled, wsSecurityEnabled);
        ServiceGroupContext sgc = new ServiceGroupContext(
            axisCfgCtx, (AxisServiceGroup) anoymousService.getParent());
        ServiceContext serviceCtx = sgc.getServiceContext(anoymousService);

        // get a reference to the DYNAMIC operation of the Anonymous Axis2 service
        AxisOperation axisAnonymousOperation = anoymousService.getOperation(
            new QName(AnonymousServiceFactory.DYNAMIC_OPERATION));

        Options clientOptions = new Options();
        clientOptions.setUseSeparateListener(separateListener);
        // if RM is requested,
        if (wsRMEnabled) {
            // if a WS-RM policy is specified, use it
            if (wsRMPolicyKey != null) {
                clientOptions.setProperty(
                    org.apache.synapse.config.xml.Constants.SANDESHA_POLICY,
                    getPolicy(synapseOutMessageContext, wsRMPolicyKey));
            }
            copyRMOptions(originalInMsgCtx, clientOptions);

            // always send each and every message in a new sequence and terminate sequence
            //clientOptions.setProperty("Sandesha2LastMessage", "true");
        }

        // if security is enabled,
        if (wsSecurityEnabled) {
            // if a WS-Sec policy is specified, use it
            if (wsSecPolicyKey != null) {
                clientOptions.setProperty(
                    org.apache.synapse.config.xml.Constants.RAMPART_POLICY,
                    getPolicy(synapseOutMessageContext, wsSecPolicyKey));
            }
        }
        OperationClient mepClient = axisAnonymousOperation.createClient(
            serviceCtx, clientOptions);
        mepClient.addMessageContext(axisOutMsgCtx);

        // always set a callback as we decide if the send it blocking or non blocking within
        // the MEP client. This does not cause an overhead, as we simply create a 'holder'
        // object with a reference to the outgoing synapse message context synapseOutMessageContext
        mepClient.setCallback(new AsyncCallback(synapseOutMessageContext));
        
        mepClient.execute(false);

        // with the nio transport, this causes the listener not to write a 202
        // Accepted response, as this implies that Synapse does not yet know if
        // a 202 or 200 response would be written back.
        originalInMsgCtx.getOperationContext().setProperty(
            org.apache.axis2.Constants.RESPONSE_WRITTEN, "SKIP");
   }

    private static MessageContext cloneForSend(MessageContext ori) throws AxisFault {
        MessageContext newMC = new MessageContext();

        // do not copy options from the original
        newMC.setConfigurationContext(ori.getConfigurationContext());
        newMC.setMessageID("uuid:" + UUIDGenerator.getUUID());
        newMC.setTo(ori.getTo());
        newMC.setSoapAction(ori.getSoapAction());

        newMC.setProperty(org.apache.axis2.Constants.Configuration.CHARACTER_SET_ENCODING,
                ori.getProperty(org.apache.axis2.Constants.Configuration.CHARACTER_SET_ENCODING));
        
        newMC.setDoingREST(ori.isDoingREST());
        newMC.setDoingMTOM(ori.isDoingMTOM());
        newMC.setServerSide(false);

        // set SOAP envelope on the message context, removing WS-A headers
        newMC.setEnvelope(removeAddressingHeaders(ori));
        return newMC;
    }
    
    private static void copyRMOptions(MessageContext oriContext, Options targetOptions) {
        Options oriOptions = oriContext.getOptions();
        if(oriOptions.getProperty(Constants.SANDESHA_LAST_MESSAGE) != null) {
            targetOptions.setProperty(Constants.SANDESHA_LAST_MESSAGE, 
                    oriOptions.getProperty(Constants.SANDESHA_LAST_MESSAGE));
        }
        if(oriOptions.getProperty(Constants.SANDESHA_SPEC_VERSION) != null) {
            targetOptions.setProperty(Constants.SANDESHA_SPEC_VERSION, 
                    oriOptions.getProperty(Constants.SANDESHA_SPEC_VERSION));
        }
        if(oriOptions.getProperty(Constants.SANDESHA_SEQUENCE_KEY) != null) {
            targetOptions.setProperty(Constants.SANDESHA_SEQUENCE_KEY, 
                    oriOptions.getProperty(Constants.SANDESHA_SEQUENCE_KEY));
        }
    }
    
    /**
     * Get the Policy object for the given name from the Synapse configuration at runtime
     * @param synCtx the current synapse configuration to get to the synapse configuration
     * @param propertyKey the name of the property which holds the Policy required
     * @return the Policy object with the given name, from the configuration
     */
    private static Policy getPolicy(org.apache.synapse.MessageContext synCtx, String propertyKey) {
        Object property = synCtx.getConfiguration().getEntry(propertyKey);
        if (property != null && property instanceof OMElement) {
            return PolicyEngine.getPolicy((OMElement) property);
        } else {
            handleException("Cannot locate Policy from the property : " + propertyKey);
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
           Object o = iterator.next();
           if (o instanceof SOAPHeaderBlock) {
               SOAPHeaderBlock headerBlock = (SOAPHeaderBlock) o;
               headerBlock.detach();
           } else if (o instanceof OMElement) {
               // work around for a known addressing bug which sends non SOAPHeaderBlock objects
               OMElement om = (OMElement) o;
               OMNamespace ns = om.getNamespace();
               if (ns != null &&
                       (AddressingConstants.Submission.WSA_NAMESPACE.equals(ns.getNamespaceURI())
                       || AddressingConstants.Final.WSA_NAMESPACE.equals(ns.getNamespaceURI()))) {
                   om.detach();
               }
           }
       }
   } 
}
