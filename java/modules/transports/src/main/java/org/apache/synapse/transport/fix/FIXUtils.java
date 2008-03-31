/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
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

package org.apache.synapse.transport.fix;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import quickfix.*;
import quickfix.field.*;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

public class FIXUtils {

    private static final Log log = LogFactory.getLog(FIXUtils.class);
    private static FIXUtils _instance = new FIXUtils();

    public static FIXUtils getInstance() {
        return _instance;
    }

    /**
     * FIX messages are non-XML. So convert them into XML using the AXIOM API.
     * Put the FIX message into an Axis2 MessageContext.The basic format of the
     * generated SOAP envelope;
     * <p/>
     * <soapEnvelope>
     * <soapBody>
     * <message>
     * <header> ....</header>
     * <body> .... </body>
     * <trailer> .... </trailer>
     * </message>
     * </soapBody>
     * </soapEnvelope>
     *
     * @param message   the FIX message
     * @param counter   application level sequence number of the message
     * @param sessionID the incoming session
     * @param msgCtx    the Axis2 MessageContext to hold the FIX message
     * @throws AxisFault the exception thrown when invalid soap envelopes are set to the msgCtx
     */
    public void setSOAPEnvelope(Message message, int counter, String sessionID,
                                MessageContext msgCtx) throws AxisFault {

        if (log.isDebugEnabled()) {
            log.debug("Creating SOAP envelope for FIX message...");
        }

        SOAPFactory soapFactory = new SOAP11Factory();
        OMElement msg = soapFactory.createOMElement(FIXConstants.FIX_MESSAGE, null);
        msg.addAttribute(soapFactory.createOMAttribute(FIXConstants.FIX_MESSAGE_INCOMING_SESSION, null, sessionID));
        msg.addAttribute(soapFactory.createOMAttribute
                (FIXConstants.FIX_MESSAGE_COUNTER, null, String.valueOf(counter)));

        OMElement header = soapFactory.createOMElement(FIXConstants.FIX_HEADER, null);
        OMElement body = soapFactory.createOMElement(FIXConstants.FIX_BODY, null);
        OMElement trailer = soapFactory.createOMElement(FIXConstants.FIX_TRAILER, null);

        //process FIX header
        Iterator<Field<?>> iter = message.getHeader().iterator();
        if (iter != null) {
            while (iter.hasNext()) {
                Field<?> field = iter.next();
                OMElement msgField = soapFactory.createOMElement(FIXConstants.FIX_FIELD, null);
                msgField.addAttribute(soapFactory.
                        createOMAttribute(FIXConstants.FIX_FIELD_ID, null, String.valueOf(field.getTag())));
                Object value = field.getObject();

                if (value instanceof byte[]) {
                    DataSource dataSource = new ByteArrayDataSource((byte[]) value);
                    DataHandler dataHandler = new DataHandler(dataSource);
                    String contentID = msgCtx.addAttachment(dataHandler);
                    OMElement binaryData = soapFactory.createOMElement(FIXConstants.FIX_BINARY_FIELD, null);
                    String binaryCID = "cid:" + contentID;
                    binaryData.addAttribute(FIXConstants.FIX_MESSAGE_REFERENCE, binaryCID, null);
                    msgField.addChild(binaryData);
                } else {
                    soapFactory.createOMText(msgField, value.toString(), OMElement.CDATA_SECTION_NODE);
                }
                header.addChild(msgField);
            }
        }
        //process FIX body
        iter = message.iterator();
        if (iter != null) {
            while (iter.hasNext()) {
                Field<?> field = iter.next();
                OMElement msgField = soapFactory.createOMElement(FIXConstants.FIX_FIELD, null);
                msgField.addAttribute(soapFactory.
                        createOMAttribute(FIXConstants.FIX_FIELD_ID, null, String.valueOf(field.getTag())));
                Object value = field.getObject();
                if (value instanceof byte[]) {
                    DataSource dataSource = new ByteArrayDataSource((byte[]) value);
                    DataHandler dataHandler = new DataHandler(dataSource);
                    String contentID = msgCtx.addAttachment(dataHandler);
                    OMElement binaryData = soapFactory.createOMElement(FIXConstants.FIX_BINARY_FIELD, null);
                    String binaryCID = "cid:" + contentID;
                    binaryData.addAttribute(FIXConstants.FIX_MESSAGE_REFERENCE, binaryCID, null);
                    msgField.addChild(binaryData);
                } else {
                    soapFactory.createOMText(msgField, value.toString(), OMElement.CDATA_SECTION_NODE);
                }
                body.addChild(msgField);
            }
        }
        //process FIX trailer
        iter = message.getTrailer().iterator();
        if (iter != null) {
            while (iter.hasNext()) {
                Field<?> field = iter.next();
                OMElement msgField = soapFactory.createOMElement(FIXConstants.FIX_FIELD, null);
                msgField.addAttribute(soapFactory.
                        createOMAttribute(FIXConstants.FIX_FIELD_ID, null, String.valueOf(field.getTag())));
                Object value = field.getObject();

                if (value instanceof byte[]) {
                    DataSource dataSource = new ByteArrayDataSource((byte[]) value);
                    DataHandler dataHandler = new DataHandler(dataSource);
                    String contentID = msgCtx.addAttachment(dataHandler);
                    OMElement binaryData = soapFactory.createOMElement(FIXConstants.FIX_BINARY_FIELD, null);
                    String binaryCID = "cid:" + contentID;
                    binaryData.addAttribute(FIXConstants.FIX_MESSAGE_REFERENCE, binaryCID, null);
                    msgField.addChild(binaryData);
                } else {
                    soapFactory.createOMText(msgField, value.toString(), OMElement.CDATA_SECTION_NODE);
                }
                trailer.addChild(msgField);
            }
        }

        msg.addChild(header);
        msg.addChild(body);
        msg.addChild(trailer);
        SOAPEnvelope envelope = soapFactory.getDefaultEnvelope();
        envelope.getBody().addChild(msg);
        msgCtx.setEnvelope(envelope);
    }


    /**
     * Extract the FIX message embedded in an Axis2 MessageContext
     *
     * @param msgCtx the Axis2 MessageContext
     * @return a FIX message
     * @throws java.io.IOException the exception thrown when handling erroneous binary content
     */
    public Message createFIXMessage(MessageContext msgCtx) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Extracting FIX message from the message context (Message ID: " + msgCtx.getMessageID() + ")");
        }

        Message message = new Message();
        SOAPBody soapBody = msgCtx.getEnvelope().getBody();
        OMElement messageNode = soapBody.getFirstChildWithName(new QName(FIXConstants.FIX_MESSAGE));
        Iterator<OMElement> messageElements = messageNode.getChildElements();

        while (messageElements.hasNext()) {
            OMElement node = messageElements.next();
            //create FIX header
            if (node.getQName().getLocalPart().equals(FIXConstants.FIX_HEADER)) {
                Iterator<OMElement> headerElements = node.getChildElements();
                while (headerElements.hasNext()) {
                    OMElement headerNode = headerElements.next();
                    String tag = headerNode.getAttributeValue(new QName(FIXConstants.FIX_FIELD_ID));
                    String value = null;

                    OMElement child = headerNode.getFirstElement();
                    if (child != null) {
                        String href = headerNode.getFirstElement().
                                getAttributeValue(new QName(FIXConstants.FIX_MESSAGE_REFERENCE));
                        if (href != null) {
                            DataHandler binaryDataHandler = msgCtx.getAttachment(href.substring(4));
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            binaryDataHandler.writeTo(outputStream);
                            value = new String(outputStream.toByteArray());
                        }
                    } else {
                        value = headerNode.getText();
                    }

                    if (value != null) {
                        message.getHeader().setString(Integer.parseInt(tag), value);
                    }
                }

            } else if (node.getQName().getLocalPart().equals(FIXConstants.FIX_BODY)) {
                //create FIX body
                Iterator<OMElement> bodyElements = node.getChildElements();
                while (bodyElements.hasNext()) {
                    OMElement bodyNode = bodyElements.next();
                    String tag = bodyNode.getAttributeValue(new QName(FIXConstants.FIX_FIELD_ID));
                    String value = null;

                    OMElement child = bodyNode.getFirstElement();
                    if (child != null) {
                        String href = bodyNode.getFirstElement().
                                getAttributeValue(new QName(FIXConstants.FIX_MESSAGE_REFERENCE));
                        if (href != null) {
                            DataHandler binaryDataHandler = msgCtx.getAttachment(href.substring(4));
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            binaryDataHandler.writeTo(outputStream);
                            value = new String(outputStream.toByteArray());
                        }
                    } else {
                        value = bodyNode.getText();
                    }

                    if (value != null) {
                        message.setString(Integer.parseInt(tag), value);
                    }
                }
            } else if (node.getQName().getLocalPart().equals(FIXConstants.FIX_TRAILER)) {
                //create FIX trailer
                Iterator<OMElement> trailerElements = node.getChildElements();
                while (trailerElements.hasNext()) {
                    OMElement trailerNode = trailerElements.next();
                    String tag = trailerNode.getAttributeValue(new QName(FIXConstants.FIX_FIELD_ID));
                    String value = null;

                    OMElement child = trailerNode.getFirstElement();
                    if (child != null) {
                        String href = trailerNode.getFirstElement().
                                getAttributeValue(new QName(FIXConstants.FIX_MESSAGE_REFERENCE));
                        if (href != null) {
                            DataHandler binaryDataHandler = msgCtx.getAttachment(href.substring(4));
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            binaryDataHandler.writeTo(outputStream);
                            value = new String(outputStream.toByteArray());
                        }
                    } else {
                        value = trailerNode.getText();
                    }

                    if (value != null) {
                        message.getTrailer().setString(Integer.parseInt(tag), value);
                    }
                }
            }
        }
        return message;
    }


    /**
     * Generate EPRs for the specified FIX service. A FIX end point can be uniquely
     * identified by a <host(IP), port> pair. Add some additional FIX session details
     * so the EPRs are more self descriptive.
     * A FIX EPR generated here looks like;
     * fix://10.100.1.80:9898?BeginString=FIX.4.4&SenderCompID=BANZAI&TargetCompID=EXEC&
     * SessionQualifier=mySession&Serviec=StockQuoteProxy
     *
     * @param acceptor    the SocketAcceptor associated with the service
     * @param serviceName the name of the service
     * @param ip          the IP address of the host
     * @return an array of EPRs for the specified service in String format
     */
    public static String[] generateEPRs(SocketAcceptor acceptor, String serviceName, String ip) {
        //Get all the addresses associated with the acceptor
        Map<SessionID, SocketAddress> socketAddresses = acceptor.getAcceptorAddresses();
        //Get all the sessions (SessionIDs) associated with the acceptor
        ArrayList<SessionID> sessions = acceptor.getSessions();
        String[] EPRList = new String[sessions.size()];

        //Generate an EPR for each session/socket address
        for (int i = 0; i < sessions.size(); i++) {
            SessionID sessionID = sessions.get(i);
            InetSocketAddress socketAddress = (InetSocketAddress) socketAddresses.get(sessionID);
            EPRList[i] = FIXConstants.FIX_PREFIX + ip + ":" + socketAddress.getPort() +
                    "?" + FIXConstants.BEGIN_STRING + "=" + sessionID.getBeginString() +
                    "&" + FIXConstants.SENDER_COMP_ID + "=" + sessionID.getTargetCompID() +
                    "&" + FIXConstants.TARGET_COMP_ID + "=" + sessionID.getSenderCompID();

            String sessionQualifier = sessionID.getSessionQualifier();
            if (sessionQualifier != null && !sessionQualifier.equals("")) {
                EPRList[i] += "&" + FIXConstants.SESSION_QUALIFIER + "=" + sessionQualifier;
            }

            String senderSubID = sessionID.getSenderSubID();
            if (senderSubID != null && !senderSubID.equals("")) {
                EPRList[i] += "&" + FIXConstants.SENDER_SUB_ID + "=" + senderSubID;
            }

            String targetSubID = sessionID.getTargetSubID();
            if (targetSubID != null && !targetSubID.equals("")) {
                EPRList[i] += "&" + FIXConstants.TARGET_SUB_ID + "=" + targetSubID;
            }

            String senderLocationID = sessionID.getSenderLocationID();
            if (senderLocationID != null && !senderLocationID.equals("")) {
                EPRList[i] += "&" + FIXConstants.SENDER_LOCATION_ID + "=" + senderLocationID;
            }

            String targetLocationID = sessionID.getTargetLocationID();
            if (targetLocationID != null && !targetLocationID.equals("")) {
                EPRList[i] += "&" + FIXConstants.TARGET_LOCATION_ID + "=" + targetLocationID;
            }

            EPRList[i] += "&Service=" + serviceName;
        }
        return EPRList;
    }

    /**
     * Extracts parameters embedded in FIX EPRs
     *
     * @param url a valid FIX EPR
     * @return a Hashtable of FIX properties
     */
    public static Hashtable getProperties(String url) {
        Hashtable<String, String> h = new Hashtable<String, String>();
        int propPos = url.indexOf("?");
        if (propPos != -1) {
            StringTokenizer st = new StringTokenizer(url.substring(propPos + 1), "&");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int sep = token.indexOf("=");
                if (sep != -1) {
                    h.put(token.substring(0, sep), token.substring(sep + 1));
                }
            }
        }
        return h;
    }

    /*
     * This is here because AXIOM does not support removing CDATA tags yet. Given a String embedded in
     * CDATA tags this method will return the String element only.
     *
     * @param str the String with CDATA tags
     * @return String with CDATA tags stripped
     *
    private static String removeCDATA(String str) {
        if (str.indexOf("<![CDATA[") != -1) {
            str = str.split("CDATA")[1].split("]></field>")[0];
		    str= str.substring(1, str.length()-1);
		    return str;
        } else {
            return str;
        }
    }*/

    /**
     * Extracts the fields related to message forwarding (third party routing) from
     * the FIX header.
     *
     * @param message the FIX message
     * @return a Map of forwarding parameters
     */
    public static Map<String, String> getMessageForwardingParameters(Message message) {

        Map<String, String> map = new HashMap<String, String>();
        String value = getHeaderFieldValue(message, BeginString.FIELD);
        map.put(FIXConstants.BEGIN_STRING, value);
        value = getHeaderFieldValue(message, SenderCompID.FIELD);
        map.put(FIXConstants.SENDER_COMP_ID, value);
        value = getHeaderFieldValue(message, SenderSubID.FIELD);
        map.put(FIXConstants.SENDER_SUB_ID, value);
        value = getHeaderFieldValue(message, SenderLocationID.FIELD);
        map.put(FIXConstants.SENDER_LOCATION_ID, value);
        value = getHeaderFieldValue(message, TargetCompID.FIELD);
        map.put(FIXConstants.TARGET_COMP_ID, value);
        value = getHeaderFieldValue(message, DeliverToCompID.FIELD);
        map.put(FIXConstants.DELIVER_TO_COMP_ID, value);
        value = getHeaderFieldValue(message, DeliverToSubID.FIELD);
        map.put(FIXConstants.DELIVER_TO_SUB_ID, value);
        value = getHeaderFieldValue(message, DeliverToLocationID.FIELD);
        map.put(FIXConstants.DELIVER_TO_LOCATION_ID, value);
        value = getHeaderFieldValue(message, OnBehalfOfCompID.FIELD);
        map.put(FIXConstants.ON_BEHALF_OF_COMP_ID, value);
        value = getHeaderFieldValue(message, OnBehalfOfSubID.FIELD);
        map.put(FIXConstants.ON_BEHALF_OF_SUB_ID, value);
        value = getHeaderFieldValue(message, OnBehalfOfLocationID.FIELD);
        map.put(FIXConstants.ON_BEHALF_OF_LOCATION_ID, value);
        return map;
    }

    private static String getHeaderFieldValue(Message message, int tag) {
        try {
            return message.getHeader().getString(tag);
        } catch (FieldNotFound fieldNotFound) {
            return null;
        }
    }

    /**
     * Extracts the name of the service which processed the message from the MessageContext
     *
     * @param msgCtx Axis2 MessageContext of a message
     * @return name of the AxisService
     * @throws org.apache.axis2.AxisFault on error
     */
    public static String getServiceName(MessageContext msgCtx) throws AxisFault {

        Object serviceParam = msgCtx.getProperty(FIXConstants.FIX_SERVICE_NAME);
        if (serviceParam != null) {
            String serviceName = serviceParam.toString();
            if (serviceName != null && !serviceName.equals("")) {
                return serviceName;
            }
        }

        Map<String, String> trpHeaders = (Map) msgCtx.getProperty(MessageContext.TRANSPORT_HEADERS);
        //try to get the service from the transport headers
        if (trpHeaders != null) {
            String serviceName = (trpHeaders.get(FIXConstants.FIX_MESSAGE_SERVICE));
            if (serviceName != null) {
                return serviceName;
            }
        }
        throw new AxisFault("Unable to find a valid service for the message");
    }

    /**
     * Extracts the application type for the message from the message context
     *
     * @param msgCtx Axis2 Message Context
     * @return application type of the message
     */
    public static String getFixApplication(MessageContext msgCtx) {
        Map<String, String> trpHeaders = (Map) msgCtx.getProperty(MessageContext.TRANSPORT_HEADERS);
        //try to get the application type from the transport headers
        String fixApplication = null;
        if (trpHeaders != null) {
            fixApplication = trpHeaders.get(FIXConstants.FIX_MESSAGE_APPLICATION);
        }
        return fixApplication;
    }

    /**
     * Creates a Map of transport headers for a message
     *
     * @param serviceName    name of the service to which the message belongs to
     * @param fixApplication FIX application type
     * @return a Map of transport headers
     */
    public static Map<String, String> getTransportHeaders(String serviceName, String fixApplication) {
        Map<String, String> trpHeaders = new HashMap<String, String>();
        trpHeaders.put(FIXConstants.FIX_MESSAGE_SERVICE, serviceName);
        trpHeaders.put(FIXConstants.FIX_MESSAGE_APPLICATION, fixApplication);
        return trpHeaders;
    }

    /**
     * Reads a FIX EPR and returns the host and port on a String array
     *
     * @param fixEPR a FIX EPR
     * @return an array of Strings containing addressing elements
     * @throws AxisFault on error
     */
    public static String[] getSocketAddressElements(String fixEPR) throws AxisFault {
        int propPos = fixEPR.indexOf("?");
        if (propPos != -1 && fixEPR.startsWith(FIXConstants.FIX_PREFIX)) {
            String address = fixEPR.substring(FIXConstants.FIX_PREFIX.length(), propPos);
            String[] socketAddressElemets = address.split(":");
            if (socketAddressElemets.length == 2) {
                return socketAddressElemets;
            }
        }
        throw new AxisFault("Malformed FIX EPR: " + fixEPR);
    }

    /**
     * Reads the SOAP body of a message and attempts to retreive the application level sequence number
     *
     * @param msgCtx Axis2 MessageContext
     * @return application level sequence number or -1
     */
    public static int getSequenceNumber(MessageContext msgCtx) {
        SOAPBody body = msgCtx.getEnvelope().getBody();
        OMElement messageNode = body.getFirstChildWithName(new QName(FIXConstants.FIX_MESSAGE));
        String value = messageNode.getAttributeValue(new QName(FIXConstants.FIX_MESSAGE_COUNTER));
        if (value != null) {
            return Integer.parseInt(value);
        } else {
            return -1;
        }
    }

    /**
     * Reads the SOAP body of a message and attempts to retreive the session identifier string
     *
     * @param msgCtx Axis2 MessageContext
     * @return a String uniquely identifying a session or null
     */
    public static String getSourceSession(MessageContext msgCtx) {
        SOAPBody body = msgCtx.getEnvelope().getBody();
        OMElement messageNode = body.getFirstChildWithName(new QName(FIXConstants.FIX_MESSAGE));
        return messageNode.getAttributeValue(new QName(FIXConstants.FIX_MESSAGE_INCOMING_SESSION));
    }
}