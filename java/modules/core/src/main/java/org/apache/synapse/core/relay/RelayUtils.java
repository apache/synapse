/*
 *  Copyright 2013 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.core.relay;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.AddressingHelper;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.Pipe;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class RelayUtils {

    private static final DeferredMessageBuilder messageBuilder = new DeferredMessageBuilder();

    private static volatile Handler addressingInHandler = null;
    private static boolean noAddressingHandler = false;

    public static void buildMessage(org.apache.synapse.MessageContext msgCtx) throws IOException,
            XMLStreamException {

        org.apache.axis2.context.MessageContext messageContext =
                ((Axis2MessageContext) msgCtx).getAxis2MessageContext();
        buildMessage(messageContext, false);
    }

    public static void buildMessage(MessageContext messageContext, boolean earlyBuild) throws IOException,
            XMLStreamException {

        final Pipe pipe = (Pipe) messageContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
        if (pipe != null && !Boolean.TRUE.equals(messageContext.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED))) {
            InputStream in = pipe.getInputStream();
            String contentType = (String) messageContext.getProperty(
                    Constants.Configuration.CONTENT_TYPE);
            OMElement element = messageBuilder.getDocument(contentType, messageContext, in);
            if (element != null) {
                messageContext.setEnvelope(TransportUtils.createSOAPEnvelope(element));
                messageContext.setProperty(DeferredMessageBuilder.RELAY_FORMATTERS_MAP,
                        messageBuilder.getFormatters());
                messageContext.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED,
                        Boolean.TRUE);

                if (!earlyBuild) {
                    processAddressing(messageContext);
                }
            }
            return;
        }

        SOAPEnvelope envelope = messageContext.getEnvelope();
        OMElement contentEle = envelope.getBody().getFirstChildWithName(
                RelayConstants.BINARY_CONTENT_QNAME);

        if (contentEle != null) {
            OMNode node = contentEle.getFirstOMChild();

            if (node != null && (node instanceof OMText)) {
                OMText binaryDataNode = (OMText) node;
                DataHandler dh = (DataHandler) binaryDataNode.getDataHandler();
                if (dh == null) {
                    throw new AxisFault("Error while building message");
                }

                DataSource dataSource = dh.getDataSource();
                //Ask the data source to stream, if it has not already cached the request
                if (dataSource instanceof StreamingOnRequestDataSource) {
                    ((StreamingOnRequestDataSource) dataSource).setLastUse(true);
                }

                InputStream in = dh.getInputStream();
                String contentType = (String) messageContext.getProperty(
                        Constants.Configuration.CONTENT_TYPE);

                OMElement element = messageBuilder.getDocument(contentType, messageContext, in);
                if (element != null) {
                    messageContext.setEnvelope(TransportUtils.createSOAPEnvelope(element));
                    messageContext.setProperty(DeferredMessageBuilder.RELAY_FORMATTERS_MAP,
                            messageBuilder.getFormatters());

                    if (!earlyBuild) {
                        processAddressing(messageContext);
                    }
                }
            }
        }
    }

    private static void processAddressing(MessageContext messageContext) throws AxisFault {
        if (noAddressingHandler) {
            return;
        } else if (addressingInHandler == null) {
            synchronized (messageBuilder) {
                if (addressingInHandler == null) {
                    AxisConfiguration axisConfig = messageContext.getConfigurationContext().
                            getAxisConfiguration();
                    List<Phase> phases = axisConfig.getInFlowPhases();
                    boolean handlerFound = false;
                    for (Phase phase : phases) {
                        if ("Addressing".equals(phase.getName())) {
                            List<Handler> handlers = phase.getHandlers();
                            for (Handler handler : handlers) {
                                if ("AddressingInHandler".equals(handler.getName())) {
                                    addressingInHandler = handler;
                                    handlerFound = true;
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    if (!handlerFound) {
                        noAddressingHandler = true;
                        return;
                    }
                }
            }
        }

        messageContext.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_IN_MESSAGES, "false");
        addressingInHandler.invoke(messageContext);

        if (messageContext.getAxisOperation() == null) {
            return;
        }

        String mepString = messageContext.getAxisOperation().getMessageExchangePattern();

        if (isOneWay(mepString)) {
            Object requestResponseTransport = messageContext.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
            if (requestResponseTransport != null) {

                Boolean disableAck = getDisableAck(messageContext);
                if (disableAck == null || disableAck.booleanValue() == false) {
                    ((RequestResponseTransport) requestResponseTransport).acknowledgeMessage(messageContext);
                }
            }
        } else if (AddressingHelper.isReplyRedirected(messageContext) && AddressingHelper.isFaultRedirected(messageContext)) {
            if (mepString.equals(WSDL2Constants.MEP_URI_IN_OUT)
                    || mepString.equals(WSDL2Constants.MEP_URI_IN_OUT)) {
                // OR, if 2 way operation but the response is intended to not use the response channel of a 2-way transport
                // then we don't need to keep the transport waiting.

                Object requestResponseTransport = messageContext.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
                if (requestResponseTransport != null) {

                    // We should send an early ack to the transport whenever possible, but some modules need
                    // to use the back channel, so we need to check if they have disabled this code.
                    Boolean disableAck = getDisableAck(messageContext);

                    if (disableAck == null || disableAck.booleanValue() == false) {
                        ((RequestResponseTransport) requestResponseTransport).acknowledgeMessage(messageContext);
                    }

                }
            }
        }
    }

    private static Boolean getDisableAck(MessageContext msgContext) throws AxisFault {
        // We should send an early ack to the transport whenever possible, but some modules need
        // to use the back channel, so we need to check if they have disabled this code.
        Boolean disableAck = (Boolean) msgContext.getProperty(Constants.Configuration.DISABLE_RESPONSE_ACK);
        if (disableAck == null) {
            disableAck = (Boolean) (msgContext.getAxisService() != null ? msgContext.getAxisService().getParameterValue(Constants.Configuration.DISABLE_RESPONSE_ACK) : null);
        }

        return disableAck;
    }

    private static boolean isOneWay(String mepString) {
        return mepString.equals(WSDL2Constants.MEP_URI_IN_ONLY)
                || mepString.equals(WSDL2Constants.MEP_URI_IN_ONLY);
    }
}
