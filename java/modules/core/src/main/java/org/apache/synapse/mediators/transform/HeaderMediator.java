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

package org.apache.synapse.mediators.transform;

import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.*;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.axis2.addressing.EndpointReference;

/**
 * The header mediator is able to set a given value as a SOAP header, or remove a given
 * header from the current message instance. This supports the headers currently
 * supported by the HeaderType class. If an expression is supplied, its runtime value
 * is evaluated using the current message. Unless the action is set to remove, the
 * default behaviour of this mediator is to set a header value.
 *
 * @see HeaderType
 */
public class HeaderMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(HeaderMediator.class);

    public static final int ACTION_SET = 0;
    public static final int ACTION_REMOVE = 1;

    /** The name of the header @see HeaderType */
    private String name = null;
    /** The literal value to be set as the header (if one was specified) */
    private String value = null;
    /** Set the header (ACTION_SET) or remove it (ACTION_REMOVE). Defaults to ACTION_SET */
    private int action = ACTION_SET;
    /** An expression which should be evaluated, and the result set as the header value */
    private AXIOMXPath expression = null;

    /**
     * Sets/Removes a SOAP header on the current message
     *
     * @param synCtx the current message which is altered as necessary
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {
        log.debug("Header mediator <" + (action == ACTION_SET ? "Set" : "Remove") + "> :: mediate()");

        if (action == ACTION_SET) {
            String value = (getValue() != null ? getValue() :
                Axis2MessageContext.getStringValue(getExpression(), synCtx));

            log.debug("Setting header : " + name + " to : " + value);

            if (Constants.HEADER_TO.equals(name)) {
                synCtx.setTo(new EndpointReference(value));
            } else if (Constants.HEADER_FROM.equals(name)){
                synCtx.setFrom(new EndpointReference(value));
            } else if (Constants.HEADER_ACTION.equals(name)) {
                synCtx.setWSAAction(value);
            } else if (Constants.HEADER_FAULT.equals(name)) {
                synCtx.setFaultTo(new EndpointReference(value));
            } else if (Constants.HEADER_REPLY_TO.equals(name)) {
                synCtx.setReplyTo(new EndpointReference(value));
            } else {
                handleException("Unsupported header : " + name);
            }

        } else {
            log.debug("Removing header : " + name + " from current message");

            if (Constants.HEADER_TO.equals(name)) {
                synCtx.setTo(null);
            } else if (Constants.HEADER_FROM.equals(name)){
                synCtx.setFrom(null);
            } else if (Constants.HEADER_ACTION.equals(name)) {
                synCtx.setWSAAction(null);
            } else if (Constants.HEADER_FAULT.equals(name)) {
                synCtx.setFaultTo(null);
            } else if (Constants.HEADER_REPLY_TO.equals(name)) {
                synCtx.setReplyTo(null);
            } else {
                handleException("Unsupported header : " + name);
            }
        }
        return true;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AXIOMXPath getExpression() {
        return expression;
    }

    public void setExpression(AXIOMXPath expression) {
        this.expression = expression;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
