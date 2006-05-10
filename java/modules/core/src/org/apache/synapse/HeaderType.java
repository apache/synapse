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

package org.apache.synapse;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a convenience class used to manipulate common headers. The convenience string names this defines could be
 * used to configure the header mediator. Once an instance of this class exists, it could be given a value and
 * a synapse message instance, and asked to set given value as the header value to the given message. i.e. setHeader()
 *
 * Support for additional headers may be added later
 */
public class HeaderType {

    private static final Log log = LogFactory.getLog(HeaderType.class);

    private final static int TO = 1, FROM = 2, FAULT = 3, ACTION = 4, REPLYTO = 5;

    /** Refers the To header */
    public final static String STR_TO = "To";
    /** Refers the From header */
    public final static String STR_FROM = "From";
    /** Refers the FaultTo header */
    public final static String STR_FAULT = "FaultTo";
    /** Refers the Action header */
    public final static String STR_ACTION = "Action";
    /** Refers the ReplyTo header */
    public final static String STR_REPLY_TO = "ReplyTo";

    /** The header type being internally reffered to */
    private int headerType = 0;

    /**
     * Sets the internal header type depending on the header identification string passed in
     * @param header a string denoting a SOAP header
     */
    public void setHeaderType(String header) {
        if (header.equalsIgnoreCase(STR_TO))
            headerType = TO;
        else if (header.equalsIgnoreCase(STR_FROM))
            headerType = FROM;
        else if (header.equalsIgnoreCase(STR_FAULT))
            headerType = FAULT;
        else if (header.equalsIgnoreCase(STR_ACTION))
            headerType = ACTION;
        else if (header.equalsIgnoreCase(STR_REPLY_TO))
            headerType = REPLYTO;
        else {
            String msg = "Unknown header type : " + header;
            log.error(msg);
            throw new SynapseException(msg);
        }
    }

    /**
     * Gets the value of this header from the given Synapse message
     * @param synCtx the message where lookup should be performed
     * @return the string value of the header
     */
    public String getHeader(SynapseMessageContext synCtx) {
        switch (headerType) {
            case TO: {
                if (synCtx.getTo() != null)
                    return synCtx.getTo().getAddress();
            }
            case FROM: {
                if (synCtx.getFrom() != null)
                    return synCtx.getFrom().getAddress();
                break;
            }
            case FAULT: {
                if (synCtx.getFaultTo() != null)
                    return synCtx.getFaultTo().getAddress();
                break;
            }
            case ACTION: {
                if (synCtx.getWSAAction() != null)
                    return synCtx.getWSAAction();
                break;
            }
            case REPLYTO: {
                if (synCtx.getReplyTo() != null)
                    return synCtx.getReplyTo().getAddress();
                break;
            }
        }

        return null;
    }

    /**
     * Removed the header indicated by this header type from the given message. i.e. sets it as null
     * @param synCtx the current message from which the header should be removed (set null)
     */
    public void removeHeader(SynapseMessageContext synCtx) {
        switch (headerType) {
            case TO: {
                synCtx.setTo(null);
                break;
            }
            case FROM: {
                synCtx.setFrom(null);
                break;
            }
            case REPLYTO: {
                synCtx.setReplyTo(null);
                break;
            }
            case ACTION: {
                synCtx.setWSAAction(null);
                break;
            }
            default: {
                String msg = "Unknown header type : " + headerType;
                log.error(msg);
                throw new SynapseException("Invalid header type");
            }
        }
    }

    /**
     * Sets the given value into the message's indicated header
     * @param synCtx the current message on which to set the header
     * @param value the value to be set
     */
    public void setHeader(SynapseMessageContext synCtx, String value) {
        switch (headerType) {
            case TO: {
                synCtx.setTo(new EndpointReference(value));
                break;
            }
            case FROM: {
                synCtx.setFrom(new EndpointReference(value));
                break;
            }
            case REPLYTO: {
                synCtx.setReplyTo(new EndpointReference(value));
                break;
            }
            case ACTION: {
                synCtx.setWSAAction(value);
                break;
            }
            default: {
                String msg = "Unknown header type : " + headerType;
                log.error(msg);
                throw new SynapseException(msg);
            }
        }
    }
}
