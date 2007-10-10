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
package org.apache.synapse.transport.base;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.commons.logging.Log;
import org.apache.axiom.om.util.UUIDGenerator;

import java.util.Map;

public abstract class AbstractTransportSender extends AbstractHandler implements TransportSender {

    /** the reference to the actual commons logger to be used for log messages */
    protected static Log log = null;

    /** the name of the transport */
    protected String transportName = null;
    /** the axis2 configuration context */
    protected ConfigurationContext cfgCtx = null;
    /** an axis2 engine over the above configuration context to process messages */
    protected AxisEngine engine = null;
    /** transport in description */
    private TransportInDescription transportIn  = null;
    /** transport out description */
    private TransportOutDescription transportOut = null;
    /** is this transport started? */
    protected boolean started = false;

    /**
     * Initialize the generic transport sender.
     *
     * @param cfgCtx the axis configuration context
     * @param transportOut the transport-out description
     * @throws AxisFault on error
     */
    public void init(ConfigurationContext cfgCtx, TransportOutDescription transportOut)
        throws AxisFault {
        this.cfgCtx = cfgCtx;
        this.engine = new AxisEngine(cfgCtx);
        this.transportIn  = cfgCtx.getAxisConfiguration().getTransportIn(transportName);
        this.transportOut = transportOut;
    }

    public void stop() {
        if (started) {
            started = false;
        }
    }

    public void cleanup(MessageContext msgContext) throws AxisFault {}

    public abstract void sendMessage(MessageContext msgCtx, String targetEPR,
        OutTransportInfo outTransportInfo) throws AxisFault;

    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {

        // is there a transport url which may be different to the WS-A To but has higher precedence
        String targetAddress = (String) msgContext.getProperty(
            Constants.Configuration.TRANSPORT_URL);

        if (targetAddress != null) {
            sendMessage(msgContext, targetAddress, null);
        } else if (msgContext.getTo() != null && !msgContext.getTo().hasAnonymousAddress()) {
            targetAddress = msgContext.getTo().getAddress();

            if (!msgContext.getTo().hasNoneAddress()) {
                sendMessage(msgContext, targetAddress, null);
            } else {
                //Don't send the message.
                return InvocationResponse.CONTINUE;
            }
        } else if (msgContext.isServerSide()) {
            // get the out transport info for server side when target EPR is unknown
            sendMessage(msgContext, null,
                (OutTransportInfo) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO));
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Process a new incoming message (Response) through the axis engine
     * @param msgCtx the axis MessageContext
     * @param trpHeaders the map containing transport level message headers
     * @param soapAction the optional soap action or null
     * @param contentType the optional content-type for the message
     */
    public void handleIncomingMessage(
        MessageContext msgCtx, Map trpHeaders,
        String soapAction, String contentType) {

        // set the soapaction if one is available via a transport header
        if (soapAction != null) {
            msgCtx.setSoapAction(soapAction);
        }

        // set the transport headers to the message context
        msgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, trpHeaders);
        
        // send the message context through the axis engine
        try {
                try {
                    engine.receive(msgCtx);
                } catch (AxisFault e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Error receiving message", e);
                    }
                    if (msgCtx.isServerSide()) {
                        engine.sendFault(MessageContextBuilder.createFaultMessageContext(msgCtx, e));
                    }
                }
        } catch (AxisFault axisFault) {
            logException("Error processing response message", axisFault);
        }
    }

    /**
     * Create a new axis MessageContext for an incoming response message
     * through this transport, for the given outgoing message
     *
     * @param outMsgCtx the outgoing message
     * @return the newly created message context
     */
    public MessageContext createResponseMessageContext(MessageContext outMsgCtx) {

        MessageContext responseMsgCtx = null;
        try {
            responseMsgCtx = outMsgCtx.getOperationContext().
                getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN);
        } catch (AxisFault af) {
            log.error("Error getting IN message context from the operation context", af);
        }

        if (responseMsgCtx == null) {
            responseMsgCtx = new MessageContext();
            responseMsgCtx.setOperationContext(outMsgCtx.getOperationContext());
        }

        responseMsgCtx.setIncomingTransportName(transportName);
        responseMsgCtx.setTransportOut(transportOut);
        responseMsgCtx.setTransportIn(transportIn);

        responseMsgCtx.setMessageID(UUIDGenerator.getUUID());

        responseMsgCtx.setDoingREST(outMsgCtx.isDoingREST());
        responseMsgCtx.setProperty(
            MessageContext.TRANSPORT_IN, outMsgCtx.getProperty(MessageContext.TRANSPORT_IN));
        responseMsgCtx.setAxisMessage(outMsgCtx.getOperationContext().getAxisOperation().
            getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE));
        responseMsgCtx.setTo(null);
        //msgCtx.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, isNonBlocking);


        // are these relevant?
        //msgCtx.setServiceGroupContextId(UUIDGenerator.getUUID());
        // this is required to support Sandesha 2
        //msgContext.setProperty(RequestResponseTransport.TRANSPORT_CONTROL,
        //        new HttpCoreRequestResponseTransport(msgContext));

        return responseMsgCtx;
    }

    /**
     * Should the transport sender wait for a synchronous response to be received?
     * @param msgCtx the outgoing message context
     * @return true if a sync response is expected
     */
    protected boolean waitForSynchronousResponse(MessageContext msgCtx) {
        return
            msgCtx.getOperationContext() != null &&
            WSDL2Constants.MEP_URI_OUT_IN.equals(
                msgCtx.getOperationContext().getAxisOperation().getMessageExchangePattern());
    }

    public String getTransportName() {
        return transportName;
    }

    public void setTransportName(String transportName) {
        this.transportName = transportName;
    }

    protected void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    protected void handleException(String msg) throws AxisFault {
        log.error(msg);
        throw new AxisFault(msg);
    }

    protected void logException(String msg, Exception e) {
        log.error(msg, e);
    }
}
