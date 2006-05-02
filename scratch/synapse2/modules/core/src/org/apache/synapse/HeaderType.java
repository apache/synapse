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

/**
 * <p> This class encapsulates access to headers. It has a set of "logical names" (e.g. strings)
 * Once you have set the logical name you can read and write that header on a SynapseMessage
 * It is used by the RegexProcessor and the HeaderProcessor classes.
 */
public class HeaderType {

    private final static int TO = 1, FROM = 2, FAULT = 3, ACTION = 4,
        REPLYTO = 5;

    public final static String STRTO = "to", STRFROM = "from",
        STRFAULT = "faultto", STRACTION = "action", STRREPLYTO = "replyto";

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
            throw new SynapseException("unknown header type");
    }

    public String getHeaderType() {
        switch (headerType) {

            case TO:
                return STRTO;
            case FROM:
                return STRFROM;
            case FAULT:
                return STRFAULT;
            case ACTION:
                return STRACTION;
            case REPLYTO:
                return STRREPLYTO;

        }
        return null;
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

    public void setHeader(SynapseMessage sm, String value) {
        switch (headerType) {
            case 0: {
                throw new SynapseException(
                    "headerType=0 in setHeader. Assume called setHeader before setHeaderType");
            }

            case TO: {

                sm.setTo(new EndpointReference(value));
                break;
            }
            case FROM: {
                sm.setFrom(new EndpointReference(value));
                break;
            }
            case REPLYTO: {
                sm.setReplyTo(new EndpointReference(value));
                break;
            }
            case ACTION: {
                sm.setWSAAction(value);
                break;
            }

        }
    }
}
