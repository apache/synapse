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

package org.apache.synapse.format.hessian;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMText;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.http.util.URLTemplatingUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.util.SynapseBinaryDataSource;

import javax.activation.DataHandler;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;

/**
 * Enables a message encoded using the Hessian binary protocol to be written to transport by
 * axis2/synapse and this formats the HessianDataSource to a hessian message
 *
 * @see org.apache.axis2.transport.MessageFormatter
 * @see org.apache.synapse.util.SynapseBinaryDataSource
 */
public class HessianMessageFormatter implements MessageFormatter {

	private static final Log log = LogFactory.getLog(HessianMessageFormatter.class);

    /**
	 * Formats the content type to be written in to the transport
	 *
	 * @param msgCtxt message of which the content type has to be formatted
	 * @param format fomat of the expected formatted message
	 * @param soapActionString soap action of the message
	 * @return contentType formatted content type as a String
	 */
	public String getContentType(MessageContext msgCtxt, OMOutputFormat format,
        String soapActionString) {

        String contentType = (String) msgCtxt.getProperty(Constants.Configuration.CONTENT_TYPE);
		String encoding = format.getCharSetEncoding();

        if (contentType == null) {
			contentType = HessianConstants.HESSIAN_CONTENT_TYPE;
		}

        if (encoding != null) {
			contentType += "; charset=" + encoding;
		}

        return contentType;
	}

	/**
	 * Extract Hessian bytes from the received SOAP message and write it onto the wire
	 *
	 * @param msgCtxt message from which the hessian message has to be extracted
	 * @param format mwssage format to be written
	 * @param out stream to which the message is written
	 * @param preserve whether to preserve the indentations
	 * @throws AxisFault in case of a failure in writing the message to the provided stream
	 */
	public void writeTo(MessageContext msgCtxt, OMOutputFormat format, OutputStream out,
        boolean preserve) throws AxisFault {

        if (log.isDebugEnabled()) {
			log.debug("Start writing the message to OutputStream");
		}

        // Check whther the message to be written is a fault message
        if (msgCtxt.getFLOW() == MessageContext.OUT_FAULT_FLOW
                || msgCtxt.getEnvelope().hasFault()) {

            try {

                SOAPFault fault = msgCtxt.getEnvelope().getBody().getFault();
                String hessianFaultDetail = "";
                String hessianFaultMessage = "";
                String hessianFaultCode = "500";

                if (fault.getDetail() != null) {
					hessianFaultDetail = fault.getDetail().getText();
				}

                if (fault.getReason() != null) {
					hessianFaultMessage = fault.getReason().getText();
				}

                if (fault.getCode() != null) {
                    hessianFaultCode = fault.getCode().getText();
                }

                BufferedOutputStream faultOutStream = new BufferedOutputStream(out);
                HessianUtils.writeFault(
                        hessianFaultCode, hessianFaultMessage, hessianFaultDetail, faultOutStream);
                faultOutStream.flush();
                faultOutStream.close();

            } catch (IOException e) {
				handleException("Unalbe to write the fault as a hessian message", e);
			}

            // if the message is not a fault extract the Hessian bytes and write it to the wire
        } else {

            OMText hessianOMText = null;
            OMElement omElement = msgCtxt.getEnvelope().getBody().getFirstElement();

            Iterator it = omElement.getChildren();
            while (it.hasNext()) {

                OMNode hessianElement =  (OMNode) it.next();
                if (hessianElement instanceof OMText) {

                    OMText tempNode = (OMText) hessianElement;
                    if (tempNode.getDataHandler() != null && ((DataHandler) tempNode
                            .getDataHandler()).getDataSource() instanceof SynapseBinaryDataSource) {

                        hessianOMText = tempNode;
                    }
                }
            }

            if (hessianOMText != null) {

                try {

                    SynapseBinaryDataSource synapseBinaryDataSource = (SynapseBinaryDataSource) (
                            (DataHandler) hessianOMText.getDataHandler()).getDataSource();

                    InputStream inputStream = synapseBinaryDataSource.getInputStream();
                    BufferedOutputStream outputStream = new BufferedOutputStream(out);

                    byte[] buffer = new byte[1024];
                    int byteCount;
                    while ((byteCount=inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, byteCount);
                    }

                    inputStream.close();
                    outputStream.flush();
                    outputStream.close();

                } catch (IOException e) {
                    handleException("Couldn't get the bytes from the HessianDataSource", e);
                }

            } else {
                handleException("Unable to find the hessian content in the payload");
            }

        }

        if (log.isDebugEnabled()) {
            log.debug("Writing message as a hessian message is successful");
        }
    }

    /**
     * This method is not supported because of large file handling limitations
     *
     * @param msgCtxt message which contains the Hessian message inside the HessianDataSource
     * @param format message fromat to be written
     * @return Hessian binary bytes of the message
     * @throws AxisFault for any invoke
     */
    public byte[] getBytes(MessageContext msgCtxt, OMOutputFormat format) throws AxisFault {
        throw new AxisFault("Method not supported. Use the " +
                "HessianMessageFormatter#writeTo method instead");
    }

    public String formatSOAPAction(MessageContext messageContext, OMOutputFormat format,
        String soapAction) {

        return soapAction;
	}

    public URL getTargetAddress(MessageContext messageContext, OMOutputFormat format,
        URL targetURL) throws AxisFault {

        return URLTemplatingUtil.getTemplatedURL(targetURL, messageContext, false);
	}

    private void handleException(String msg, Throwable e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    private void handleException(String msg) throws AxisFault {
        log.error(msg);
        throw new AxisFault(msg);
    }
}
