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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAP11Constants;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

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
                        String contType = mbp.getContentType().toLowerCase();

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

}
