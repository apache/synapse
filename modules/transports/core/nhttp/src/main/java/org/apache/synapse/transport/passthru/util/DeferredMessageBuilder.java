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

package org.apache.synapse.transport.passthru.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.*;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.http.ApplicationXMLFormatter;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.transport.http.MultipartFormDataFormatter;
import org.apache.axis2.transport.http.SOAPMessageFormatter;
import org.apache.axis2.transport.http.XFormURLEncodedFormatter;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class DeferredMessageBuilder {

    private static Log log = LogFactory.getLog(DeferredMessageBuilder.class);

    private Map<String, Builder> builders = new HashMap<String, Builder>();
    private Map<String, MessageFormatter> formatters = new HashMap<String, MessageFormatter>();

    public final static String RELAY_FORMATTERS_MAP = "__RELAY_FORMATTERS_MAP";
    public final static String FORCED_RELAY_FORMATTER = "__FORCED_RELAY_FORMATTER";

    public DeferredMessageBuilder() {
        // first initialize with the default builders
        builders.put("multipart/related", new MIMEBuilder());
        builders.put("application/soap+xml", new SOAPBuilder());
        builders.put("text/xml", new SOAPBuilder());
        builders.put("application/xop+xml", new MTOMBuilder());
        builders.put("application/xml", new ApplicationXMLBuilder());
        builders.put("application/x-www-form-urlencoded",
                new XFormURLEncodedBuilder());

        // initialize the default formatters
        formatters.put("application/x-www-form-urlencoded", new XFormURLEncodedFormatter());
        formatters.put("multipart/form-data", new MultipartFormDataFormatter());
        formatters.put("application/xml", new ApplicationXMLFormatter());
        formatters.put("text/xml", new SOAPMessageFormatter());
        formatters.put("application/soap+xml", new SOAPMessageFormatter());
    }

    public Map<String, Builder> getBuilders() {
        return builders;
    }

    public Map<String, MessageFormatter> getFormatters() {
        return formatters;
    }

    public OMElement getDocument(MessageContext msgCtx, InputStream in) throws
            XMLStreamException, IOException {


        String contentType = (String) msgCtx.getProperty(Constants.Configuration.CONTENT_TYPE);
        String _contentType = contentType;
        in = HTTPTransportUtils.handleGZip(msgCtx, in);
        if (contentType != null) {
            int j = contentType.indexOf(";");
            if (j > 0) {
                _contentType = contentType.substring(0, j);
            }
        }

        AxisConfiguration configuration =
                msgCtx.getConfigurationContext().getAxisConfiguration();
        Parameter useFallbackParameter = configuration.getParameter(Constants.Configuration.USE_DEFAULT_FALLBACK_BUILDER);

        boolean useFallbackBuilder = false;

        if (useFallbackParameter != null) {
            useFallbackBuilder = JavaUtils.isTrueExplicitly(useFallbackParameter.getValue(), useFallbackBuilder);
        }

        OMElement element = null;
        Builder builder;
        if (contentType != null) {
            // loading builder from externally..
            builder = configuration.getMessageBuilder(_contentType, useFallbackBuilder);
            if (builder != null) {
                try {
                    element = builder.processDocument(in, contentType, msgCtx);
                } catch (AxisFault axisFault) {
                    log.error("Error building message", axisFault);
                    throw axisFault;
                }
            }
        }

        if (element == null) {
            if (msgCtx.isDoingREST()) {
                element = BuilderUtil.createPOXBuilder(in, null).getDocumentElement();
            } else {
                // switch to default
                builder = new SOAPBuilder();
                try {
                    element = builder.processDocument(in, contentType, msgCtx);
                } catch (AxisFault axisFault) {
                    log.error("Error building message using SOAP builder");
                    throw axisFault;
                }
            }
        }

        // build the soap headers and body
        if (element instanceof SOAPEnvelope) {
            SOAPEnvelope env = (SOAPEnvelope) element;
            env.hasFault();
        }

        //setting up original contentType (resetting the content type)
        if (contentType != null && !contentType.isEmpty()) {
            msgCtx.setProperty(Constants.Configuration.CONTENT_TYPE, contentType);
        }
        return element;
    }
}