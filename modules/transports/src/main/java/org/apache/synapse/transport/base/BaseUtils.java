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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.builder.StAXBuilder;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.format.BinaryFormatter;
import org.apache.synapse.format.PlainTextFormatter;

public abstract class BaseUtils {

    private static final Log log = LogFactory.getLog(BaseUtils.class);

    /**
     * Return a QName from the String passed in of the form {ns}element
     * @param obj a QName or a String containing a QName in {ns}element form
     * @return a corresponding QName object
     */
    public static QName getQNameFromString(Object obj) {
        String value;
        if (obj instanceof QName) {
            return (QName) obj;
        } else {
            value = obj.toString();
        }
        int open = value.indexOf('{');
        int close = value.indexOf('}');
        if (close > open && open > -1 && value.length() > close) {
            return new QName(value.substring(open+1, close-open), value.substring(close+1));
        } else {
            return new QName(value);
        }
    }

    /**
     * Marks the given service as faulty with the given comment
     *
     * @param serviceName service name
     * @param msg         comment for being faulty
     * @param axisCfg     configuration context
     */
    public static void markServiceAsFaulty(String serviceName, String msg,
                                           AxisConfiguration axisCfg) {
        if (serviceName != null) {
            try {
                AxisService service = axisCfg.getService(serviceName);
                axisCfg.getFaultyServices().put(service.getName(), msg);

            } catch (AxisFault axisFault) {
                log.warn("Error marking service : " + serviceName + " as faulty", axisFault);
            }
        }
    }

    /**
     * Create a SOAP envelope using SOAP 1.1 or 1.2 depending on the namespace
     * @param in InputStream for the payload
     * @param namespace the SOAP namespace
     * @return the SOAP envelope for the correct version
     * @throws javax.xml.stream.XMLStreamException on error
     */
    public static SOAPEnvelope getEnvelope(InputStream in, String namespace) throws XMLStreamException {

        try {
            in.reset();
        } catch (IOException ignore) {}
        XMLStreamReader xmlreader =
            StAXUtils.createXMLStreamReader(in, MessageContext.DEFAULT_CHAR_SET_ENCODING);
        StAXBuilder builder = new StAXSOAPModelBuilder(xmlreader, namespace);
        return (SOAPEnvelope) builder.getDocumentElement();
    }

    /**
     * Get the OMOutput format for the given message
     * @param msgContext the axis message context
     * @return the OMOutput format to be used
     */
    public static OMOutputFormat getOMOutputFormat(MessageContext msgContext) {

        OMOutputFormat format = new OMOutputFormat();
        msgContext.setDoingMTOM(HTTPTransportUtils.doWriteMTOM(msgContext));
        msgContext.setDoingSwA(HTTPTransportUtils.doWriteSwA(msgContext));
        msgContext.setDoingREST(HTTPTransportUtils.isDoingREST(msgContext));
        format.setSOAP11(msgContext.isSOAP11());
        format.setDoOptimize(msgContext.isDoingMTOM());
        format.setDoingSWA(msgContext.isDoingSwA());

        format.setCharSetEncoding(HTTPTransportUtils.getCharSetEncoding(msgContext));
        Object mimeBoundaryProperty = msgContext.getProperty(Constants.Configuration.MIME_BOUNDARY);
        if (mimeBoundaryProperty != null) {
            format.setMimeBoundary((String) mimeBoundaryProperty);
        }
        return format;
    }
    
    /**
     * Get the MessageFormatter for the given message.
     * @param msgContext the axis message context
     * @return the MessageFormatter to be used
     */
    public static MessageFormatter getMessageFormatter(MessageContext msgContext) {
        // check the first element of the SOAP body, do we have content wrapped using the
        // default wrapper elements for binary (BaseConstants.DEFAULT_BINARY_WRAPPER) or
        // text (BaseConstants.DEFAULT_TEXT_WRAPPER) ? If so, select the appropriate
        // message formatter directly ...
        OMElement firstChild = msgContext.getEnvelope().getBody().getFirstElement();
        if (firstChild != null) {
            if (BaseConstants.DEFAULT_BINARY_WRAPPER.equals(firstChild.getQName())) {
                return new BinaryFormatter();
            } else if (BaseConstants.DEFAULT_TEXT_WRAPPER.equals(firstChild.getQName())) {
                return new PlainTextFormatter();
            }
        }
        
        // ... otherwise, let Axis choose the right message formatter:
        try {
            return TransportUtils.getMessageFormatter(msgContext);
        } catch (AxisFault axisFault) {
            throw new BaseTransportException("Unable to get the message formatter to use");
        }
    }

    /**
     * Create a SOAPEnvelope from the given message and set it into
     * the axis MessageContext passed
     *
     * @param message the message object
     * @param msgContext the axis MessageContext
     * @param contentType
     * @throws AxisFault on errors encountered while setting the envelope to the message context
     */
    public void setSOAPEnvelope(Object message, MessageContext msgContext, String contentType)
            throws AxisFault {

        SOAPEnvelope envelope = null;
        StAXBuilder builder = null;

        String charSetEnc = null;
        try {
            if (contentType != null) {
                charSetEnc = new ContentType(contentType).getParameter("charset");
            }
        } catch (ParseException ex) {
            // ignore
        }
        
        InputStream in = getInputStream(message);

        // handle SOAP payloads when a correct content type is available
        try {
            if (contentType != null) {
                if (contentType.indexOf(BaseConstants.MULTIPART_RELATED) > -1) {
                    builder = BuilderUtil.getAttachmentsBuilder(msgContext, in, contentType, true);
                    envelope = (SOAPEnvelope) builder.getDocumentElement();
                    msgContext.setDoingSwA(true);

                } else {
                    builder = BuilderUtil.getSOAPBuilder(in, charSetEnc);
                    envelope = (SOAPEnvelope) builder.getDocumentElement();
                }
            }
        } catch (Exception ignore) {
            try {
                in.close();
            } catch (IOException e) {
                // ignore
            }
            in = getInputStream(message);
        }


        // handle SOAP when content type is missing, or any other POX, binary or text payload
        if (builder == null) {

            SOAPFactory soapFactory = new SOAP11Factory();
            try {
                builder = new StAXOMBuilder(StAXUtils.createXMLStreamReader(in, charSetEnc));
                builder.setOMBuilderFactory(OMAbstractFactory.getOMFactory());
                OMNamespace ns = builder.getDocumentElement().getNamespace();
                if (ns != null) {
                    String nsUri = ns.getNamespaceURI();

                    if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(nsUri)) {
                        envelope = BaseUtils.getEnvelope(in, SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
    
                    } else if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(nsUri)) {
                        envelope = BaseUtils.getEnvelope(in, SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
    
                    }
                }
                if (envelope == null) {
                    // this is POX ... mark message as REST
                    msgContext.setDoingREST(true);
                    envelope = soapFactory.getDefaultEnvelope();
                    envelope.getBody().addChild(builder.getDocumentElement());
                }

            } catch (Exception e) {
                envelope = handleLegacyMessage(msgContext, message);
            }
        }

        // Set the encoding scheme in the message context
        msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEnc);

        String charEncOfMessage =
            builder == null ? null :
                builder.getDocument() == null ? null : builder.getDocument().getCharsetEncoding();

        if (!isBlank(charEncOfMessage) &&
            !isBlank(charSetEnc) &&
            !charEncOfMessage.equalsIgnoreCase(charSetEnc)) {
            handleException("Charset encoding of transport differs from that of the payload");
        }

        msgContext.setEnvelope(envelope);
    }

    /**
     * Handle a non SOAP and non XML message, and create a SOAPEnvelope to hold the
     * pure text or binary content as necessary
     *
     * @param msgContext the axis message context
     * @param message the legacy message
     * @return the SOAP envelope
     */
    private SOAPEnvelope handleLegacyMessage(MessageContext msgContext, Object message) {

        SOAPFactory soapFactory = new SOAP11Factory();
        SOAPEnvelope envelope;

        if (log.isDebugEnabled()) {
            log.debug("Non SOAP/XML message received");
        }

        // pick the name of the element that will act as the wrapper element for the
        // non-xml payload. If service doesn't define one, default
        Parameter wrapperParam = msgContext.getAxisService().
            getParameter(BaseConstants.WRAPPER_PARAM);

        QName wrapperQName = null;
        OMElement wrapper = null;
        if (wrapperParam != null) {
            wrapperQName = BaseUtils.getQNameFromString(wrapperParam.getValue());
        }

        String textPayload = getMessageTextPayload(message);
        if (textPayload != null) {
            OMTextImpl textData = (OMTextImpl) soapFactory.createOMText(textPayload);

            if (wrapperQName == null) {
                wrapperQName = BaseConstants.DEFAULT_TEXT_WRAPPER;
            }
            wrapper = soapFactory.createOMElement(wrapperQName, null);
            wrapper.addChild(textData);

        } else {
            byte[] msgBytes = getMessageBinaryPayload(message);
            if (msgBytes != null) {
                DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(msgBytes));
                OMText textData = soapFactory.createOMText(dataHandler, true);
                if (wrapperQName == null) {
                    wrapperQName = BaseConstants.DEFAULT_BINARY_WRAPPER;
                }
                wrapper = soapFactory.createOMElement(wrapperQName, null);
                wrapper.addChild(textData);
                msgContext.setDoingMTOM(true);
                
            } else {
                handleException("Unable to read payload from message of type : "
                    + message.getClass().getName());
            }
        }

        envelope = soapFactory.getDefaultEnvelope();
        envelope.getBody().addChild(wrapper);

        return envelope;
    }

     /**
     * Get a String property from a message
     *
     * @param message the message object
     * @param property property name
     * @return property value
     */
    public abstract String getProperty(Object message, String property);

    /**
     * Get an InputStream to the message payload
     *
     * @param message Object
     * @return an InputStream to the payload
     */
    public abstract InputStream getInputStream(Object message);

    /**
     * Get the message payload as a String, if the message is a non-SOAP, non-XML, plain text message
     *
     * @param message the message Object
     * @return the plain text payload of the message if applicable
     */
    public abstract String getMessageTextPayload(Object message);

    /**
     * Get the message payload as a byte[], if the message is a non-SOAP, non-XML, binary message
     *
     * @param message the message Object
     * @return the payload of the message as a byte[]
     */
    public abstract byte[] getMessageBinaryPayload(Object message);

    protected static void handleException(String s) {
        log.error(s);
        throw new BaseTransportException(s);
    }

    protected static void handleException(String s, Exception e) {
        log.error(s, e);
        throw new BaseTransportException(s, e);
    }

    /**
     * Utility method to check if a string is null or empty
     * @param str the string to check
     * @return true if the string is null or empty
     */
    public static boolean isBlank(String str) {
        if (str == null || str.length() == 0) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;    
    }

    public static boolean isUsingTransport(AxisService service, String transportName) {
        boolean process = service.isEnableAllTransports();
        if (process) {
            return true;

        } else {
            List transports = service.getExposedTransports();
            for (Object transport : transports) {
                if (transportName.equals(transport)) {
                    return true;
                }
            }
        }
        return false;
    }
}
