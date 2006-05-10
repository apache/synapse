/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.mediators.builtin;

import org.apache.axiom.soap.SOAPHeader;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;


/**
 * Logs the specified message into the configured logger. The log levels specify
 * which attributes would be logged, and is configurable. Additionally custom
 * properties may be defined to the logger, where literal values or expressions
 * could be specified for logging. The custom properties are printed into the log
 * using the defined seperator (\n, "," etc)
 */
public class LogMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(LogMediator.class);

    public static final int CUSTOM = 0;
    public static final int SIMPLE = 1;
    public static final int HEADERS = 2;
    public static final int FULL = 3;

    private int logLevel = SIMPLE;
    private String SEP = ", ";
    private List properties = new ArrayList();

    /**
     * Logs the current message according to the supplied semantics
     * @param synCtx (current) message to be logged
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {
        log.debug(getType() + " mediate()");
        log.info(getLogMessage(synCtx));
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
        return sb.toString();
    }

    private String getSimpleLogMessage(MessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        if (synCtx.getTo() != null)
            sb.append("To: " + synCtx.getTo().getAddress());
        if (synCtx.getFrom() != null)
            sb.append(SEP + "From: " + synCtx.getFrom().getAddress());
        if (synCtx.getWSAAction() != null)
            sb.append(SEP + "WSAction: " + synCtx.getWSAAction());
        if (synCtx.getSoapAction() != null)
            sb.append(SEP + "SOAPAction: " + synCtx.getSoapAction());
        if (synCtx.getReplyTo() != null)
            sb.append(SEP + "ReplyTo: " + synCtx.getReplyTo().getAddress());
        if (synCtx.getMessageID() != null)
            sb.append(SEP + "MessageID: " + synCtx.getMessageID());
        setCustomProperties(sb, synCtx);
        return sb.toString();
    }

    private String getHeadersLogMessage(MessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        Iterator iter = synCtx.getEnvelope().getHeader().examineAllHeaderBlocks();
        while (iter.hasNext()) {
            SOAPHeader header = (SOAPHeader) iter.next();
            sb.append(SEP + header.getLocalName() + " : " + header.getText());
        }
        setCustomProperties(sb, synCtx);
        return sb.toString();
    }

    private String getFullLogMessage(MessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        sb.append(getSimpleLogMessage(synCtx));
        if (synCtx.getEnvelope() != null)
            sb.append(SEP + "Envelope: " + synCtx.getEnvelope());
        setCustomProperties(sb, synCtx);
        return sb.toString();
    }

    private void setCustomProperties(StringBuffer sb, MessageContext synCtx) {
        if (properties != null && !properties.isEmpty()) {
            Iterator iter = properties.iterator();
            while (iter.hasNext()) {
                MediatorProperty prop = (MediatorProperty) iter.next();
                sb.append(SEP + prop.getName() + " = " +
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

    public String getSeperator() {
        return SEP;
    }

    public void setSeperator(String SEP) {
        this.SEP = SEP;
    }

    public void addProperty(MediatorProperty p) {
        properties.add(p);
    }

    public void addAllProperties(List list) {
        properties.addAll(list);
    }

}
