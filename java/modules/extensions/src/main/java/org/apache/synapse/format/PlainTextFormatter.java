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

package org.apache.synapse.format;

import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.http.util.URLTemplatingUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.transport.base.BaseConstants;

import java.io.OutputStream;
import java.io.IOException;
import java.net.URL;

public class PlainTextFormatter implements MessageFormatter {

    public byte[] getBytes(MessageContext messageContext, OMOutputFormat format) throws AxisFault {
        OMElement textElt = messageContext.getEnvelope().getBody().getFirstElement();
        if (BaseConstants.DEFAULT_TEXT_WRAPPER.equals(textElt.getQName())) {
            return textElt.getText().getBytes();
        }
        return new byte[0];
    }

    public void writeTo(MessageContext messageContext, OMOutputFormat format, OutputStream outputStream, boolean preserve) throws AxisFault {
        try {
            outputStream.write(getBytes(messageContext, format));
        } catch (IOException e) {
            throw new AxisFault("Error writing text message to stream", e);
        }
    }

    public String getContentType(MessageContext messageContext, OMOutputFormat format, String soapAction) {
        String encoding = format.getCharSetEncoding();
        String contentType = "text/plain";

        if (encoding != null) {
            contentType += "; charset=" + encoding;
        }

        // if soap action is there (can be there is soap response MEP is used) add it.
        if ((soapAction != null)
                && !"".equals(soapAction.trim())
                && !"\"\"".equals(soapAction.trim())) {
            contentType = contentType + ";action=\"" + soapAction + "\";";
        }

        return contentType;
    }

    public URL getTargetAddress(MessageContext msgCtxt, OMOutputFormat format, URL targetURL) throws AxisFault {
        // Check whether there is a template in the URL, if so we have to replace then with data
        // values and create a new target URL.
        targetURL = URLTemplatingUtil.getTemplatedURL(targetURL, msgCtxt, false);
        return targetURL;
    }

    public String formatSOAPAction(MessageContext messageContext, OMOutputFormat format, String soapAction) {
        return null;
    }
}
