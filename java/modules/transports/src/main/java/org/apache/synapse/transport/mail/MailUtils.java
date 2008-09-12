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

package org.apache.synapse.transport.mail;

import org.apache.synapse.transport.base.BaseUtils;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axiom.om.impl.builder.StAXBuilder;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.builder.BuilderUtil;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.activation.DataHandler;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

public class MailUtils extends BaseUtils {

    private static final Log log = LogFactory.getLog(MailUtils.class);

    private static BaseUtils _instance = new MailUtils();

    public static BaseUtils getInstace() {
        return _instance;
    }

    /**
     * Get a String property from FileContent message
     *
     * @param message  the File message
     * @param property property name
     * @return property value
     */
    @Override
    public String getProperty(Object message, String property) {
        try {
            Object o = ((Message) message).getHeader(property);
            if (o instanceof String) {
                return (String) o;
            } else if (o instanceof String[] && ((String[]) o).length > 0) {
                return ((String[]) o)[0];
            }
        } catch (MessagingException e) {}
        return null;
    }

    @Override
    public InputStream getInputStream(Object message) {
        try {
            if (message instanceof MimeMessage) {
                MimeMessage msg = (MimeMessage) message;
                if (msg.getContent() instanceof Multipart) {
                    MimeBodyPart firstTextPart = null;

                    Multipart mp = (Multipart) msg.getContent();
                    for (int i=0; i<mp.getCount(); i++) {
                        MimeBodyPart mbp = (MimeBodyPart) mp.getBodyPart(i);
                        String contType = mbp.getContentType();

                        if (contType != null &&
                            (contType.indexOf(SOAP11Constants.SOAP_11_CONTENT_TYPE) != -1 ||
                             contType.indexOf(SOAP12Constants.SOAP_12_CONTENT_TYPE) != -1)) {
                            // this part is a SOAP 11 or 12 payload, treat this as the message
                            return mbp.getInputStream();
                        } else if (mbp != null && contType.indexOf("text/plain") != -1) {
                            firstTextPart = mbp;
                        }
                    }
                    // if a soap 11 or soap12 payload was not found, treat first text part as message
                    if (firstTextPart != null) {
                        return firstTextPart.getInputStream();
                    }

                } else {
                    return ((Message) message).getInputStream();
                }
            }
        } catch (Exception e) {
            handleException("Error creating an input stream to : " +
                ((Message) message).getMessageNumber(), e);
        }
        
        return null;
    }

    @Override
    public String getMessageTextPayload(Object message) {
        try {
            return new String(getBytesFromInputStream(getInputStream(message)));
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error reading message payload as text for : " +
                    ((Message) message).getMessageNumber(), e);
            }
        }
        return null;
    }

    @Override
    public byte[] getMessageBinaryPayload(Object message) {
        try {
            return getBytesFromInputStream(getInputStream(message));
        } catch (Exception e) {
            handleException("Error reading message payload as a byte[] for : " +
                ((Message) message).getMessageNumber(), e);
        }
        return new byte[0];
    }

    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {

        try {
            byte[] buffer = new byte[4096];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int c;
            while ((c = is.read(buffer)) != -1) {
                baos.write(buffer, 0, c);
            }
            return baos.toByteArray();
        } finally {
            try {
                is.close();
            } catch (IOException ignore) {}
        }
    }

    @Override
    public void setSOAPEnvelope(Object message, MessageContext msgContext, String contentType) throws AxisFault {

        if (message instanceof MimeMessage &&
            (contentType.toLowerCase().contains("multipart/alternative") ||
             contentType.toLowerCase().contains("multipart/mixed"))) {

            MultipartParser mp = new MultipartParser((MimeMessage) message);
            try {
                mp.parse();
            } catch (Exception e) {
                throw new AxisFault("Error parsing multipart message", e);
            }

            SOAPFactory soapFactory = new SOAP11Factory();
            SOAPEnvelope envelope = null;
            StAXBuilder builder = null;
            String charSetEnc = null;

            try {
                if (mp.getMainTextContentType() != null) {
                    charSetEnc = new ContentType(mp.getMainTextContentType()).getParameter("charset");
                }
            } catch (ParseException ignore) {
                charSetEnc = MessageContext.DEFAULT_CHAR_SET_ENCODING;
            }

            try {
                // select primary message - in the following order of priority
                // SOAP 1.2, SOAP 1.1 / POX, text/plain, text/html
                if (mp.getMainTextContentType().contains(SOAP12Constants.SOAP_12_CONTENT_TYPE) ||
                    mp.getMainTextContentType().contains((SOAP11Constants.SOAP_11_CONTENT_TYPE))) {
                    builder = BuilderUtil.getSOAPBuilder(
                        new ByteArrayInputStream(mp.getMainText().getBytes(charSetEnc)), charSetEnc);
                    envelope = (SOAPEnvelope) builder.getDocumentElement();

                } else if (mp.getMainTextContentType().toLowerCase().contains(("text/plain"))) {

                    // pick the name of the element that will act as the wrapper element for the
                    // non-xml payload. If service doesn't define one, default
                    Parameter wrapperParam = msgContext.getAxisService().
                        getParameter(BaseConstants.WRAPPER_PARAM);

                    QName wrapperQName = null;
                    OMElement wrapper = null;
                    if (wrapperParam != null) {
                        wrapperQName = BaseUtils.getQNameFromString(wrapperParam.getValue());
                    }

                    OMTextImpl textData = (OMTextImpl) soapFactory.createOMText(mp.getMainText());

                    if (wrapperQName == null) {
                        wrapperQName = BaseConstants.DEFAULT_TEXT_WRAPPER;
                    }
                    wrapper = soapFactory.createOMElement(wrapperQName, null);
                    wrapper.addChild(textData);

                    envelope = soapFactory.getDefaultEnvelope();
                    envelope.getBody().addChild(wrapper);
                }
            } catch (XMLStreamException e) {
                handleException("Error building SOAP or POX payload", e);
            } catch (UnsupportedEncodingException e) {
                handleException("Encoding error building SOAP or POX payload", e);
            }

            // Set the encoding scheme in the message context
            msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEnc);

            String charEncOfMessage =
                builder == null ? null :
                    builder.getDocument() == null ? null : builder.getDocument().getCharsetEncoding();

            if (StringUtils.isNotBlank(charEncOfMessage) &&
                StringUtils.isNotBlank(charSetEnc) &&
                !charSetEnc.equalsIgnoreCase(charEncOfMessage)) {
                handleException("Charset encodings differs from whats used in the payload");
            }

            msgContext.setEnvelope(envelope);

            int cid = 1;
            for (DataHandler dh : mp.getAttachments()) {
                msgContext.addAttachment(Integer.toString(cid++), dh);
            }

        } else {
            super.setSOAPEnvelope(message, msgContext, contentType);
        }
    }

    private class MultipartParser {

        final MimeMessage msg;
        private String mainText = null;
        private String mainTextContentType = null;
        private List<DataHandler> attachments = new ArrayList<DataHandler>();

        MultipartParser(MimeMessage msg) {
            this.msg = msg;
        }

        public void parse() throws MessagingException, IOException {
            Multipart mp = (Multipart) msg.getContent();
            for (int i=0; i<mp.getCount(); i++) {
                buildContentMap(mp.getBodyPart(i));
            }
        }

        private void buildContentMap(Part p) throws MessagingException, IOException {

            if (p.isMimeType("multipart/alternative")) {

                Multipart mp = (Multipart) p.getContent();
                for (int i = 0; i < mp.getCount(); i++) {

                    Part bp = mp.getBodyPart(i);
                    processBodyPart(bp);
                }

            } else if (p.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) p.getContent();
                for (int i = 0; i < mp.getCount(); i++) {
                    buildContentMap(mp.getBodyPart(i));
                }

            } else {
                processBodyPart(p);
            }
        }

        private void processBodyPart(Part bp) throws MessagingException, IOException {
            if (bp.isMimeType(SOAP12Constants.SOAP_12_CONTENT_TYPE) ||
                bp.isMimeType(SOAP11Constants.SOAP_11_CONTENT_TYPE) ||
                bp.isMimeType("text/plain")) {

                if (mainText == null) {
                    mainText = (String) bp.getContent();
                    mainTextContentType = bp.getContentType();
                }
            } else {
                attachments.add(bp.getDataHandler());
            }
        }

        public String getMainText() {
            return mainText;
        }

        public String getMainTextContentType() {
            return mainTextContentType;
        }

        public List<DataHandler> getAttachments() {
            return attachments;
        }
    }
}
