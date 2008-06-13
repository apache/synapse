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

import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
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

    /** Only properties specified to the Log mediator */
    public static final int CUSTOM  = 0;
    /** To, From, WSAction, SOAPAction, ReplyTo, MessageID and any properties */
    public static final int SIMPLE  = 1;
    /** All SOAP header blocks and any properties */
    public static final int HEADERS = 2;
    /** all attributes of level 'simple' and the SOAP envelope and any properties */
    public static final int FULL    = 3;

    public static final String DEFAULT_SEP = ", ";

    /** The default log level is set to SIMPLE */
    private int logLevel = SIMPLE;
    /** The separator for which used to separate logging information */
    private String separator = DEFAULT_SEP;
    /** The holder for the custom properties */
    private List properties = new ArrayList();

    /**
     * Logs the current message according to the supplied semantics
     *
     * @param synCtx (current) message to be logged
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Log mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        String logMessage = getLogMessage(synCtx);
        synCtx.getServiceLog().info(logMessage);

        if (log.isInfoEnabled()) {
            log.info(logMessage);
        }
        if (traceOn) {
            trace.info("Log message : " + logMessage);
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Log mediator");
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
        sb.append(separator + "Direction: " + (synCtx.isResponse() ? "response" : "request"));
        setCustomProperties(sb, synCtx);
        return trimLeadingSeparator(sb);
    }

    private String getHeadersLogMessage(MessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        if (synCtx.getEnvelope() != null) {
            SOAPHeader header = synCtx.getEnvelope().getHeader();
            if (header != null) {
                for (Iterator iter = header.examineAllHeaderBlocks(); iter.hasNext();) {
                    Object o = iter.next();
                    if (o instanceof SOAPHeaderBlock) {
                        SOAPHeaderBlock headerBlk = (SOAPHeaderBlock) o;
                        sb.append(separator + headerBlk.getLocalName() + " : " +
                                headerBlk.getText());
                    } else if (o instanceof OMElement) {
                        OMElement headerElem = (OMElement) o;
                        sb.append(separator + headerElem.getLocalName() + " : " +
                                headerElem.getText());
                    }
                }
            }
        }
        setCustomProperties(sb, synCtx);
        return trimLeadingSeparator(sb);
    }

    private String getFullLogMessage(MessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        sb.append(getSimpleLogMessage(synCtx));
        if (synCtx.getEnvelope() != null)
            sb.append(separator + "Envelope: " + synCtx.getEnvelope());        
        return trimLeadingSeparator(sb);
    }

    private void setCustomProperties(StringBuffer sb, MessageContext synCtx) {
        if (properties != null && !properties.isEmpty()) {
            for (Iterator iter = properties.iterator(); iter.hasNext();) {
                MediatorProperty prop = (MediatorProperty) iter.next();
                sb.append(separator + prop.getName() + " = " +
                        (prop.getValue() != null ? prop.getValue() :
                                prop.getEvaluatedExpression(synCtx)));
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
