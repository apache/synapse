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
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.util.MessageHelper;

import javax.xml.namespace.QName;
import java.util.Map;

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

        boolean separateListener = false;
        boolean wsSecurityEnabled = false;
        String wsSecPolicyKey = null;
        String inboundWsSecPolicyKey = null;
        String outboundWsSecPolicyKey = null;
        boolean wsAddressingEnabled = false;
        String wsAddressingVersion = null;

        if (endpoint != null) {
            separateListener = endpoint.isUseSeparateListener();
            wsSecurityEnabled = endpoint.isSecurityOn();
            wsSecPolicyKey = endpoint.getWsSecPolicyKey();
            inboundWsSecPolicyKey = endpoint.getInboundWsSecPolicyKey();
            outboundWsSecPolicyKey = endpoint.getOutboundWsSecPolicyKey();
            wsAddressingEnabled = endpoint.isAddressingOn();
            wsAddressingVersion = endpoint.getAddressingVersion();
        }

        if (log.isDebugEnabled()) {
            String to;
            if (endpoint != null && endpoint.getAddress() != null) {
                to = endpoint.getAddress(synapseOutMessageContext);
            } else {
                to = synapseOutMessageContext.getTo().toString();
            }

            log.debug(
                "Sending [add = " + wsAddressingEnabled +
                "] [sec = " + wsSecurityEnabled +
                (endpoint != null ?
                    "] [mtom = " + endpoint.isUseMTOM() +
                    "] [swa = " + endpoint.isUseSwa() +
                    "] [format = " + endpoint.getFormat() +
                    "] [force soap11=" + endpoint.isForceSOAP11() +
                    "] [force soap12=" + endpoint.isForceSOAP12() +
                    "] [pox=" + endpoint.isForcePOX() +
                    "] [get=" + endpoint.isForceGET() +
                    "] [encoding=" + endpoint.getCharSetEncoding() : "") +
                "] [to=" + to + "]");
        }

        // save the original message context without altering it, so we can tie the response
        MessageContext originalInMsgCtx
            = ((Axis2MessageContext) synapseOutMessageContext).getAxis2MessageContext();

        String session = (String) synapseOutMessageContext.getProperty("LB_COOKIE_HEADER");
        if (session != null) {
            Map headers = (Map) originalInMsgCtx.getProperty(MessageContext.TRANSPORT_HEADERS);
            headers.put("Cookie", session);
        }

        // create a new MessageContext to be sent out as this should not corrupt the original
        // we need to create the response to the original message later on
        String preserveAddressingProperty = (String) synapseOutMessageContext.getProperty(
                SynapseConstants.PRESERVE_WS_ADDRESSING);
        MessageContext axisOutMsgCtx = cloneForSend(originalInMsgCtx, preserveAddressingProperty);


        if (log.isDebugEnabled()) {
            log.debug("Message [Original Request Message ID : "
                    + synapseOutMessageContext.getMessageID() + "]"
                    + " [New Cloned Request Message ID : " + axisOutMsgCtx.getMessageID() + "]");
        }
        // set all the details of the endpoint only to the cloned message context
        // so that we can use the original message context for resending through different endpoints
        if (endpoint != null) {

            if (SynapseConstants.FORMAT_POX.equals(endpoint.getFormat())) {
                axisOutMsgCtx.setDoingREST(true);
                axisOutMsgCtx.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE,
                        org.apache.axis2.transport.http.HTTPConstants.MEDIA_TYPE_APPLICATION_XML);
                axisOutMsgCtx.setProperty(Constants.Configuration.CONTENT_TYPE,
                        org.apache.axis2.transport.http.HTTPConstants.MEDIA_TYPE_APPLICATION_XML);

            } else if (SynapseConstants.FORMAT_GET.equals(endpoint.getFormat())) {
                axisOutMsgCtx.setDoingREST(true);
                axisOutMsgCtx.setProperty(Constants.Configuration.HTTP_METHOD,
                        Constants.Configuration.HTTP_METHOD_GET);
                axisOutMsgCtx.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE,
                        org.apache.axis2.transport.http.HTTPConstants.MEDIA_TYPE_X_WWW_FORM);
                
            } else if (SynapseConstants.FORMAT_SOAP11.equals(endpoint.getFormat())) {
                axisOutMsgCtx.setDoingREST(false);
                axisOutMsgCtx.removeProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE);
                // We need to set this explicitly here in case the request was not a POST
                axisOutMsgCtx.setProperty(Constants.Configuration.HTTP_METHOD,
                        Constants.Configuration.HTTP_METHOD_POST);
                if (axisOutMsgCtx.getSoapAction() == null && axisOutMsgCtx.getWSAAction() != null) {
                    axisOutMsgCtx.setSoapAction(axisOutMsgCtx.getWSAAction());
                }
                if (!axisOutMsgCtx.isSOAP11()) {
                    SOAPUtils.convertSOAP12toSOAP11(axisOutMsgCtx);
                }
                
            } else if (SynapseConstants.FORMAT_SOAP12.equals(endpoint.getFormat())) {
                axisOutMsgCtx.setDoingREST(false);
                axisOutMsgCtx.removeProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE);
                // We need to set this explicitly here in case the request was not a POST
                axisOutMsgCtx.setProperty(Constants.Configuration.HTTP_METHOD,
                        Constants.Configuration.HTTP_METHOD_POST);
                if (axisOutMsgCtx.getSoapAction() == null && axisOutMsgCtx.getWSAAction() != null) {
                    axisOutMsgCtx.setSoapAction(axisOutMsgCtx.getWSAAction());
                }
                if (axisOutMsgCtx.isSOAP11()) {
                    SOAPUtils.convertSOAP11toSOAP12(axisOutMsgCtx);
                }                

            } else if (SynapseConstants.FORMAT_REST.equals(endpoint.getFormat())) {
                // format=rest is kept only for backward compatibility. We no longer needed that.
                // Remove Message Type  for GET and DELETE Request
                if (originalInMsgCtx.getProperty(Constants.Configuration.HTTP_METHOD) != null) {
                    Object method = originalInMsgCtx.getProperty(Constants.Configuration.HTTP_METHOD);
                    if (method.equals(Constants.Configuration.HTTP_METHOD_GET) ||
                           method.equals(Constants.Configuration.HTTP_METHOD_DELETE)) {
                        axisOutMsgCtx.removeProperty(
                                org.apache.axis2.Constants.Configuration.MESSAGE_TYPE);
                    }
                }
                axisOutMsgCtx.setDoingREST(true);
            } else {
                processWSDL2RESTRequestMessageType(originalInMsgCtx, axisOutMsgCtx);
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

            // add rest request suffix URI
            String restSuffix = (String) axisOutMsgCtx.getProperty(NhttpConstants.REST_URL_POSTFIX);
            boolean isRest = SynapseConstants.FORMAT_REST.equals(endpoint.getFormat());

            if (!isRest && !endpoint.isForceSOAP11() && !endpoint.isForceSOAP12()) {
                isRest = isRequestRest(originalInMsgCtx);
            }

            if (endpoint.getAddress() != null) {
                if (isRest && restSuffix != null && !"".equals(restSuffix)) {
                    String address = endpoint.getAddress(synapseOutMessageContext);
                    String url;
                    if (!address.endsWith("/") && !restSuffix.startsWith("/") &&
                            !restSuffix.startsWith("?")) {
                        url = address + "/" + restSuffix;
                    } else {
                        url = address + restSuffix;
                    }
                    axisOutMsgCtx.setTo(new EndpointReference(url));

                } else {
                    axisOutMsgCtx.setTo(
                            new EndpointReference(endpoint.getAddress(synapseOutMessageContext)));
                }

                axisOutMsgCtx.setProperty(NhttpConstants.ENDPOINT_PREFIX,
                        endpoint.getAddress(synapseOutMessageContext));
            } else {
                // Supporting RESTful invocation
                if (isRest && restSuffix != null && !"".equals(restSuffix)) {
                    EndpointReference epr = axisOutMsgCtx.getTo();
                    if (epr != null) {
                        String address = epr.getAddress();
                        String url;
                        if (!address.endsWith("/") && !restSuffix.startsWith("/") &&
                                !restSuffix.startsWith("?")) {
                            url = address + "/" + restSuffix;
                        } else {
                            url = address + restSuffix;
                        }
                        axisOutMsgCtx.setTo(new EndpointReference(url));
                    }
                }
            }
             
            if (endpoint.isUseSeparateListener()) {
                axisOutMsgCtx.getOptions().setUseSeparateListener(true);
            }
        } else {
            processWSDL2RESTRequestMessageType(originalInMsgCtx, axisOutMsgCtx);
        }

        // only put whttp:location for the REST (GET) requests, otherwise causes issues for POX messages
        if (axisOutMsgCtx.isDoingREST() && HTTPConstants.MEDIA_TYPE_X_WWW_FORM.equals(
                axisOutMsgCtx.getProperty(Constants.Configuration.MESSAGE_TYPE))) {
            if (axisOutMsgCtx.getProperty(WSDL2Constants.ATTR_WHTTP_LOCATION) == null
                    && axisOutMsgCtx.getEnvelope().getBody().getFirstElement() != null) {
                axisOutMsgCtx.setProperty(WSDL2Constants.ATTR_WHTTP_LOCATION,
                        axisOutMsgCtx.getEnvelope().getBody().getFirstElement()
                                .getLocalName());
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

        // remove the headers if we don't need to preserve them.
        // determine weather we need to preserve the processed headers
        String preserveHeaderProperty =
                (String) synapseOutMessageContext.getProperty(
                       SynapseConstants.PRESERVE_PROCESSED_HEADERS);
        if (preserveHeaderProperty == null || !Boolean.parseBoolean(preserveHeaderProperty)) {
            // default behaviour is to remove the headers
            MessageHelper.removeProcessedHeaders(axisOutMsgCtx,
                    (preserveAddressingProperty != null &&
                            Boolean.parseBoolean(preserveAddressingProperty)));
        }


        ConfigurationContext axisCfgCtx = axisOutMsgCtx.getConfigurationContext();
        AxisConfiguration axisCfg       = axisCfgCtx.getAxisConfiguration();

        AxisService anonymousService =
            AnonymousServiceFactory.getAnonymousService(synapseOutMessageContext.getConfiguration(),
                    axisCfg, wsAddressingEnabled, wsSecurityEnabled);
        // mark the anon services created to be used in the client side of synapse as hidden
        // from the server side of synapse point of view
        anonymousService.getParent().addParameter(SynapseConstants.HIDDEN_SERVICE_PARAM, "true");
        ServiceGroupContext sgc = new ServiceGroupContext(
                        axisCfgCtx, (AxisServiceGroup) anonymousService.getParent());
        ServiceContext serviceCtx = sgc.getServiceContext(anonymousService);

        boolean outOnlyMessage = "true".equals(synapseOutMessageContext.getProperty(
                SynapseConstants.OUT_ONLY));

        // get a reference to the DYNAMIC operation of the Anonymous Axis2 service
        AxisOperation axisAnonymousOperation = anonymousService.getOperation(
                outOnlyMessage ?
                        new QName(AnonymousServiceFactory.OUT_ONLY_OPERATION) :
                        new QName(AnonymousServiceFactory.OUT_IN_OPERATION));


        Options clientOptions = MessageHelper.cloneOptions(originalInMsgCtx.getOptions());
        clientOptions.setUseSeparateListener(separateListener);

        // if security is enabled,
        if (wsSecurityEnabled) {
            // if a WS-Sec policy is specified, use it
            if (wsSecPolicyKey != null) {
                clientOptions.setProperty(
                    SynapseConstants.RAMPART_POLICY,
                        MessageHelper.getPolicy(synapseOutMessageContext, wsSecPolicyKey));
            } else {
                if (inboundWsSecPolicyKey != null) {
                    clientOptions.setProperty(SynapseConstants.RAMPART_IN_POLICY,
                            MessageHelper.getPolicy(
                                    synapseOutMessageContext, inboundWsSecPolicyKey));
                }
                if (outboundWsSecPolicyKey != null) {
                    clientOptions.setProperty(SynapseConstants.RAMPART_OUT_POLICY,
                            MessageHelper.getPolicy(
                                    synapseOutMessageContext, outboundWsSecPolicyKey));
                }
            }
            // temporary workaround for https://issues.apache.org/jira/browse/WSCOMMONS-197
            axisOutMsgCtx.getEnvelope().getOrCreateHeader();
        }

        OperationClient mepClient = axisAnonymousOperation.createClient(serviceCtx, clientOptions);
        mepClient.addMessageContext(axisOutMsgCtx);
        axisOutMsgCtx.setAxisMessage(
                axisAnonymousOperation.getMessage(WSDLConstants.MESSAGE_LABEL_OUT_VALUE));

        // set the SEND_TIMEOUT for transport sender
        if (endpoint != null && endpoint.getTimeoutDuration() > 0) {
            axisOutMsgCtx.setProperty(SynapseConstants.SEND_TIMEOUT, endpoint.getTimeoutDuration());
        }

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

        // this is a temporary fix for converting messages from HTTP 1.1 chunking to HTTP 1.0.
        // Without this HTTP transport can block & become unresponsive because we are streaming
        // HTTP 1.1 messages and HTTP 1.0 require the whole message to calculate the content length
        if (originalInMsgCtx.isPropertyTrue(NhttpConstants.FORCE_HTTP_1_0)) {
            synapseOutMessageContext.getEnvelope().toString();
        }

        // with the nio transport, this causes the listener not to write a 202
        // Accepted response, as this implies that Synapse does not yet know if
        // a 202 or 200 response would be written back.
        originalInMsgCtx.getOperationContext().setProperty(
                org.apache.axis2.Constants.RESPONSE_WRITTEN, "SKIP");

        // if the transport out is explicitly set use it
        Object o = originalInMsgCtx.getProperty("TRANSPORT_OUT_DESCRIPTION");
        if (o != null && o instanceof TransportOutDescription) {
            axisOutMsgCtx.setTransportOut((TransportOutDescription) o);
            clientOptions.setTransportOut((TransportOutDescription) o);
            clientOptions.setProperty("TRANSPORT_OUT_DESCRIPTION", o);
        }

        mepClient.execute(true);
   }

    private static MessageContext cloneForSend(MessageContext ori, String preserveAddressing)
            throws AxisFault {

        MessageContext newMC = MessageHelper.clonePartially(ori);

        newMC.setEnvelope(ori.getEnvelope());
        if (preserveAddressing != null && Boolean.parseBoolean(preserveAddressing)) {
            newMC.setMessageID(ori.getMessageID());
        } else {
            MessageHelper.removeAddressingHeaders(newMC);
        }

        newMC.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
            ori.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));

        return newMC;
    }

    public static void clearSecurityProperties(Options options) {

        Options current = options;
        while (current != null && current.getProperty(SynapseConstants.RAMPART_POLICY) != null) {
            current.setProperty(SynapseConstants.RAMPART_POLICY, null);
            current = current.getParent();
        }
    }

    /**
     * This is is a workaround for axis2 RestUtils behaviour
     * Based on an internal property and the http method, we set the message type
     * @param originalInMsgCtx IN message
     * @param axisOutMsgCtx Out message
     */
    private static void processWSDL2RESTRequestMessageType(MessageContext originalInMsgCtx,
                                                           MessageContext axisOutMsgCtx) {

        // TODO - this is a workaround for axis2 RestUtils behaviour
        Object restContentType =
                originalInMsgCtx.getProperty(NhttpConstants.REST_REQUEST_CONTENT_TYPE);

        if (restContentType == null) {

            String httpMethod = (String) originalInMsgCtx.getProperty(
                    Constants.Configuration.HTTP_METHOD);
            if (Constants.Configuration.HTTP_METHOD_GET.equals(httpMethod)
                    || Constants.Configuration.HTTP_METHOD_DELETE.equals(httpMethod)) {
                restContentType = HTTPConstants.MEDIA_TYPE_X_WWW_FORM;
            }
        }

        if (restContentType != null && restContentType instanceof String) {
            String contentType = TransportUtils.getContentType((String) restContentType, originalInMsgCtx);
            axisOutMsgCtx.setProperty(
                    org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, contentType);
            originalInMsgCtx.setProperty(
                    org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, contentType);
        }
    }

    /**
     * Whether the original request received by the synapse is REST
     *
     * @param originalInMsgCtx request message
     * @return <code>true</code> if the request was a REST request
     */
    private static boolean isRequestRest(MessageContext originalInMsgCtx) {

        boolean isRestRequest =
                originalInMsgCtx.getProperty(NhttpConstants.REST_REQUEST_CONTENT_TYPE) != null;

        if (!isRestRequest) {

            String httpMethod = (String) originalInMsgCtx.getProperty(
                    Constants.Configuration.HTTP_METHOD);

            isRestRequest = Constants.Configuration.HTTP_METHOD_GET.equals(httpMethod)
                    || Constants.Configuration.HTTP_METHOD_DELETE.equals(httpMethod)
                    || Constants.Configuration.HTTP_METHOD_PUT.equals(httpMethod)
                    || RESTConstants.METHOD_OPTIONS.equals(httpMethod);

            if (!isRestRequest) {

                isRestRequest = Constants.Configuration.HTTP_METHOD_POST.equals(httpMethod)
                        && HTTPTransportUtils.isRESTRequest(
                        String.valueOf(originalInMsgCtx.getProperty(
                                Constants.Configuration.MESSAGE_TYPE)));
            }
        }
        return isRestRequest;
    }
}
