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

package org.apache.synapse.mediators.json;

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class SynapseJsonSender extends AbstractHandler
        implements TransportSender {

    private static final Log log = LogFactory.getLog(SynapseJsonSender.class);

    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
        //Fix Me For Sending Only
        // Trasnport URL can be different from the WSA-To. So processing
        // that now.
        EndpointReference epr = null;
        String transportURL =
                (String) msgContext
                        .getProperty(Constants.Configuration.TRANSPORT_URL);

        if (transportURL != null) {
            epr = new EndpointReference(transportURL);
        } else if ((msgContext.getTo() != null)
                   && !AddressingConstants.Submission.WSA_ANONYMOUS_URL
                .equals(msgContext.getTo().getAddress()) &&
                                                         !AddressingConstants
                                                                 .Final
                                                                 .WSA_ANONYMOUS_URL
                                                                 .equals(msgContext
                                                                         .getTo().getAddress()))
        {
            epr = msgContext.getTo();
        }

        if (epr == null) {
            throw new AxisFault("EndpointReference is not available");
        }

        // Get the JSONObject
        JSONObject jsonObject =
                (JSONObject) msgContext.getProperty("JSONObject");
        if (jsonObject == null) {
            throw new AxisFault("Couldn't Find JSONObject");

        }
        HttpClient agent = new HttpClient();
        PostMethod postMethod = new PostMethod(epr.getAddress());
        try {
            postMethod.setRequestEntity(new ByteArrayRequestEntity(
                    XML.toString(jsonObject).getBytes()));
            agent.executeMethod(postMethod);

            if (postMethod.getStatusCode() == HttpStatus.SC_OK) {
                processResponse(postMethod, msgContext);
            } else if (postMethod.getStatusCode() == HttpStatus.SC_ACCEPTED) {
            } else if (postMethod.getStatusCode() == HttpStatus
                    .SC_INTERNAL_SERVER_ERROR) {
                Header contenttypeHheader =
                        postMethod.getResponseHeader(
                                HTTPConstants.HEADER_CONTENT_TYPE);
                String value = contenttypeHheader.getValue();

                if (value != null) {
                    if ((value.indexOf(SOAP11Constants.SOAP_11_CONTENT_TYPE) >=
                         0)
                        || (value
                            .indexOf(SOAP12Constants.SOAP_12_CONTENT_TYPE) >=
                                                                           0)) {
                        processResponse(postMethod, msgContext);
                    }
                }
            } else {
                throw new AxisFault(Messages.getMessage("transportError",
                                                        String.valueOf(
                                                                postMethod.getStatusCode()),
                                                        postMethod.getResponseBodyAsString()));
            }
        } catch (JSONException e) {
            log.error(e);
            throw new AxisFault(e);
        } catch (IOException e) {
            log.error(e);
            throw new AxisFault(e);
        }

		return InvocationResponse.CONTINUE;
    }

    public void cleanup(MessageContext msgContext) throws AxisFault {
        //FixMe
    }

    public void init(ConfigurationContext confContext,
                     TransportOutDescription transportOut) throws AxisFault {
        //As This is an Extension we can Left this out
    }

    public void stop() {
        //FixMe
    }

    private void processResponse(HttpMethodBase httpMethod,
                                 MessageContext msgContext)
            throws IOException {
        obtainHTTPHeaderInformation(httpMethod, msgContext);

        InputStream in = httpMethod.getResponseBodyAsStream();

        Header contentEncoding =
                httpMethod.getResponseHeader(
                        HTTPConstants.HEADER_CONTENT_ENCODING);
        if (contentEncoding != null) {
            if (contentEncoding.getValue().
                    equalsIgnoreCase(HTTPConstants.COMPRESSION_GZIP)) {
                in =
                        new GZIPInputStream(in);
            } else {
                throw new AxisFault("HTTP :"
                                    + "unsupported content-encoding of '"
                                    + contentEncoding.getValue()
                                    + "' found");
            }
        }

        if (in == null) {
            throw new AxisFault(
                    Messages.getMessage("canNotBeNull", "InputStream"));
        }

        if (msgContext.getOperationContext() != null) {
            msgContext.getOperationContext()
                    .setProperty(MessageContext.TRANSPORT_IN, in);
        }
    }

    private void obtainHTTPHeaderInformation(HttpMethodBase method,
                                             MessageContext msgContext) {
        Header header =
                method.getResponseHeader(HTTPConstants.HEADER_CONTENT_TYPE);

        if (header != null) {
            HeaderElement[] headers = header.getElements();

            for (int i = 0; i < headers.length; i++) {
                NameValuePair charsetEnc =
                        headers[i].getParameterByName(
                                HTTPConstants.CHAR_SET_ENCODING);
                OperationContext opContext = msgContext.getOperationContext();
                if (charsetEnc != null) {
                    if (opContext != null) {
                        opContext.setProperty(
                                Constants.Configuration.CHARACTER_SET_ENCODING,
                                charsetEnc.getValue());
                    }
                }
            }
        }

    }

}
