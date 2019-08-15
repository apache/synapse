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

package org.apache.synapse.transport.fix.message;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.synapse.transport.fix.FIXUtils;
import quickfix.InvalidMessage;

/**
 * Fix message builder prepares a payload based on the incoming raw fix message
 * read from the destination. This implementation only focuses on building the message
 * context. There will be limitations such as when building message there won't be
 * fix session attribute involved and the assumption is that the fix client and executor
 * has the responsibility of managing fix session accordingly.
 */
public class FIXMessageBuilder implements Builder {

	private static final Log log = LogFactory.getLog(FIXMessageBuilder.class);

	public OMElement processDocument(InputStream inputStream, String contentType,
                                     MessageContext messageContext) throws AxisFault {
		Reader reader;
        quickfix.Message message;
        StringBuilder messageString = new StringBuilder();
		try {
			String charSetEncoding = (String) messageContext.getProperty(
                    Constants.Configuration.CHARACTER_SET_ENCODING);
			if (charSetEncoding == null) {
				charSetEncoding = MessageContext.DEFAULT_CHAR_SET_ENCODING;
			}
			reader = new InputStreamReader(inputStream, charSetEncoding);
			try {
				int data = reader.read();
				while (data != -1) {
					char dataChar = (char) data;
					data = reader.read();
					messageString.append(dataChar);
				}
			} catch (Exception e) {
                handleException("Error while creating FIX SOAP envelope", e);
			}

		} catch (Exception e) {
            handleException("Error while creating FIX SOAP envelope", e);
		}

		try {
			message = new quickfix.Message(messageString.toString(), null, false);
		} catch (InvalidMessage e) {
			handleException("Error while creating FIX SOAP envelope", e);
            return null;
		}

		if (log.isDebugEnabled()) {
			log.debug("Creating SOAP envelope for FIX message...");
		}

        FIXUtils.getInstance().setSOAPEnvelope(message, -1, "", messageContext);
		return messageContext.getEnvelope().getBody().getFirstElement();
	}

    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

}
