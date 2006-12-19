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

package org.apache.synapse.mediators.builtin;

import org.apache.axiom.soap.SOAPHeader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Logs the specified message into the configured logger. The log levels specify
 * which attributes would be logged, and is configurable. Additionally custom
 * properties may be defined to the logger, where literal values or expressions
 * could be specified for logging. The custom properties are printed into the log
 * using the defined separator (\n, "," etc)
 */
public class LogMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(LogMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);
    public static final int CUSTOM = 0;
    public static final int SIMPLE = 1;
    public static final int HEADERS = 2;
    public static final int FULL = 3;
    public static final String DEFAULT_SEP = ", ";

    private int logLevel = SIMPLE;
    private String separator = DEFAULT_SEP;
    private List properties = new ArrayList();

    /**
     * Logs the current message according to the supplied semantics
     *
     * @param synCtx (current) message to be logged
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {
        log.debug("Log mediator :: mediate()");
        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        if (shouldTrace) {
            trace.trace("Start : Log mediator");
        }
        String logMessage = getLogMessage(synCtx);
        log.info(logMessage);
        if (shouldTrace) {
            trace.trace(logMessage);
            trace.trace("End : Log mediator");
        }
        return true;
    }

    private String getLogMessage(MessageContext synCtx) {
        switch (logLevel) {
            case CUSTOM:
                return getCustomLogMessage(synCtx);
            case SIMPLE:
                return getSimpleLogMessage(synCtx);
            case HEADERS:
                return getHeadersLogMessage(synCtx);
            case FULL:
                return getFullLogMessage(synCtx);
            default:
                return "Invalid log level specified";
        }
    }

    private String getCustomLogMessage(MessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        setCustomProperties(sb, synCtx);
        return trimLeadingSeparator(sb);
    }

    private String getSimpleLogMessage(MessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        if (synCtx.getTo() != null)
            sb.append("To: " + synCtx.getTo().getAddress());
        else
            sb.append("To: ");
        if (synCtx.getFrom() != null)
            sb.append(separator + "From: " + synCtx.getFrom().getAddress());
        if (synCtx.getWSAAction() != null)
            sb.append(separator + "WSAction: " + synCtx.getWSAAction());
        if (synCtx.getSoapAction() != null)
            sb.append(separator + "SOAPAction: " + synCtx.getSoapAction());
        if (synCtx.getReplyTo() != null)
            sb.append(separator + "ReplyTo: " + synCtx.getReplyTo().getAddress());
        if (synCtx.getMessageID() != null)
            sb.append(separator + "MessageID: " + synCtx.getMessageID());
        setCustomProperties(sb, synCtx);
        return trimLeadingSeparator(sb);
    }

    private String getHeadersLogMessage(MessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        Iterator iter = synCtx.getEnvelope().getHeader().examineAllHeaderBlocks();
        while (iter.hasNext()) {
            SOAPHeader header = (SOAPHeader) iter.next();
            sb.append(separator + header.getLocalName() + " : " + header.getText());
        }
        setCustomProperties(sb, synCtx);
        return trimLeadingSeparator(sb);
    }

    private String getFullLogMessage(MessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        sb.append(getSimpleLogMessage(synCtx));
        if (synCtx.getEnvelope() != null)
            sb.append(separator + "Envelope: " + synCtx.getEnvelope());
        setCustomProperties(sb, synCtx);
        return trimLeadingSeparator(sb);
    }

    private void setCustomProperties(StringBuffer sb, MessageContext synCtx) {
        if (properties != null && !properties.isEmpty()) {
            Iterator iter = properties.iterator();
            while (iter.hasNext()) {
                MediatorProperty prop = (MediatorProperty) iter.next();
                sb.append(separator + prop.getName() + " = " +
                        (prop.getValue() != null ? prop.getValue() : prop.getEvaluatedExpression(synCtx)));
            }
        }
    }

    public int getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public void addProperty(MediatorProperty p) {
        properties.add(p);
    }

    public void addAllProperties(List list) {
        properties.addAll(list);
    }

    public List getProperties() {
        return properties;
    }

    private String trimLeadingSeparator(StringBuffer sb) {
        String retStr = sb.toString();
        if (retStr.startsWith(separator)) {
            return retStr.substring(separator.length());
        } else {
            return retStr;
        }
    }
}
