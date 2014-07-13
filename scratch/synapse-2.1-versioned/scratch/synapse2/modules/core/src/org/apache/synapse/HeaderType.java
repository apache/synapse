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
import org.apache.axiom.soap.SOAPHeaderBlock;

/**
 * This is a convenience class used to manipulate common headers. The convenience string names this defines could be
 * used to configure the header mediator. Once an instance of this class exists, it could be given a value and
 * a synapse message instance, and asked to set given value as the header value to the given message. i.e. setHeader()
 *
 * Support for additional headers may be added later
 */
public class HeaderType {

    private final static int TO = 1, FROM = 2, FAULT = 3, ACTION = 4, REPLYTO = 5;

    /** Refers the To header */
    public final static String STRTO = "To";
    /** Refers the From header */
    public final static String STRFROM = "From";
    /** Refers the FaultTo header */
    public final static String STRFAULT = "FaultTo";
    /** Refers the Action header */
    public final static String STRACTION = "Action";
    /** Refers the ReplyTo header */
    public final static String STRREPLYTO = "ReplyTo";

    private int headerType = 0;

    public void setHeaderType(String header) {
        if (header.equalsIgnoreCase(STRTO))
            headerType = TO;
        else if (header.equalsIgnoreCase(STRFROM))
            headerType = FROM;
        else if (header.equalsIgnoreCase(STRFAULT))
            headerType = FAULT;
        else if (header.equalsIgnoreCase(STRACTION))
            headerType = ACTION;
        else if (header.equalsIgnoreCase(STRREPLYTO))
            headerType = REPLYTO;
        else
            throw new SynapseException("Unknown header type : " + header);
    }

    public String getHeader(SynapseMessage sm) {
        switch (headerType) {
            case TO: {
                if (sm.getTo() != null)
                    return sm.getTo().getAddress();

            }
            case FROM: {
                if (sm.getFrom() != null)
                    return sm.getFrom().getAddress();
                break;
            }
            case FAULT: {
                if (sm.getFaultTo() != null)
                    return sm.getFaultTo().getAddress();
                break;
            }
            case ACTION: {
                if (sm.getWSAAction() != null)
                    return sm.getWSAAction();
                break;
            }
            case REPLYTO: {
                if (sm.getReplyTo() != null)
                    return sm.getReplyTo().getAddress();
                break;
            }
        }

        return null;
    }

    /**
     * Removed the header indicated by this header type from the given message
     * @param synMsg the current message from which the header should be removed
     */
    public void removeHeader(SynapseMessage synMsg) {
        //TODO This is not yet implemented - revisit later
        System.err.println("Unimplemented functionality - Needs to be fixed");
    }

    /**
     * Sets the given value into the message's indicated header
     * @param synMsg the current message on which to set the header
     * @param value the value to be set
     */
    public void setHeader(SynapseMessage synMsg, String value) {
        switch (headerType) {
            case 0: {
                throw new SynapseException(
                    "headerType=0 in setHeader. Assume called setHeader before setHeaderType");
            }

            case TO: {

                synMsg.setTo(new EndpointReference(value));
                break;
            }
            case FROM: {
                synMsg.setFrom(new EndpointReference(value));
                break;
            }
            case REPLYTO: {
                synMsg.setReplyTo(new EndpointReference(value));
                break;
            }
            case ACTION: {
                synMsg.setWSAAction(value);
                break;
            }
        }
    }
}
