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

package org.apache.synapse.mediators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.Constants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.Navigator;
import org.jaxen.function.StringFunction;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Implements the XPath extension function synapse:get-property(scope,prop-name)
 */
public class GetPropertyFunction implements Function {

    private static final Log log = LogFactory.getLog(GetPropertyFunction.class);

    /** Synapse Message context*/
    private MessageContext synCtx = null;

    public MessageContext getSynCtx() {
        return synCtx;
    }

    public void setSynCtx(MessageContext synCtx) {
        this.synCtx = synCtx;
    }

    /**
     * Returns the string value of the property which is get from the corresponding context to the provided scope .
     * The default scope is used to get property from the synapse message context
     *
     * @param context
     * @param args
     * @return The string value of a property
     * @throws FunctionCallException
     */
    public Object call(Context context, List args) throws FunctionCallException {

        int size = args.size();
        if (size == 0) {
            log.warn("Property key value for lookup was not specified");
            return null;
        } else if (size == 1) {
            return evaluate(Constants.SCOPE_DEFAULT, args.get(0), context.getNavigator());
        } else if (size == 2) {
            return evaluate(args.get(0), args.get(1), context.getNavigator());
        } else {
            String msg = "Invalid arguments for synapse:get-property(prop-name) 0r  " +
                    "synapse:get-property(scope,prop-name) XPath function ";
            log.warn(msg);
            throw new FunctionCallException(msg);
        }
    }

    /**
     * Returns the string value of the property using arg. one as key and arg. two as scope
     *
     * @param scopeObject
     * @param keyObject
     * @param navigator
     * @return The String value of property using arg. one as key and arg. two as scope
     */
    public Object evaluate(Object scopeObject, Object keyObject, Navigator navigator) {
        if (synCtx == null) {
            log.warn("Synapse context has not been set for the XPath extension function" +
                    "'synapse:get-property(prop-name)'");
            return null;

        }
        String scope = StringFunction.evaluate(scopeObject, navigator);
        String key = StringFunction.evaluate(keyObject, navigator);

        if (key == null || "".equals(key)) {
            log.warn("property-name should be provided when executing " +
                    "synapse:get-property(scope,prop-name)" +
                    " or synapse:get-property(prop-name) Xpath function");
            return null;
        }
        if (Constants.SCOPE_DEFAULT.equals(scope)) {

            if (Constants.HEADER_TO.equals(key) && synCtx.getTo() != null) {
                return synCtx.getTo().getAddress();
            } else if (Constants.HEADER_FROM.equals(key) && synCtx.getFrom() != null) {
                return synCtx.getFrom().getAddress();
            } else if (Constants.HEADER_ACTION.equals(key) && synCtx.getWSAAction() != null) {
                return synCtx.getWSAAction();
            } else if (Constants.HEADER_FAULT.equals(key) && synCtx.getFaultTo() != null) {
                return synCtx.getFaultTo().getAddress();
            } else if (Constants.HEADER_REPLY_TO.equals(key) && synCtx.getReplyTo() != null) {
                return synCtx.getReplyTo().getAddress();
            } else if (Constants.HEADER_MESSAGE_ID.equals(key) && synCtx.getMessageID() != null) {
                return synCtx.getMessageID();
            } else {
                Object result = synCtx.getProperty(key);
                if (result != null) {
                    return result;
                } else {
                    return synCtx.getEntry(key);       
                }
            }

        } else if (Constants.SCOPE_AXIS2.equals(scope) && synCtx instanceof Axis2MessageContext) {
            org.apache.axis2.context.MessageContext axis2MessageContext
                    = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            return axis2MessageContext.getConfigurationContext().getProperty(key);
        } else if (Constants.SCOPE_TRANSPORT.equals(scope)
                && synCtx instanceof Axis2MessageContext) {
            org.apache.axis2.context.MessageContext axis2MessageContext
                    = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            Object headers = axis2MessageContext.getProperty(
                    org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers != null && headers instanceof Map) {
                Map headersMap = (HashMap) headers;
                return headersMap.get(key);
            }
        } else {
            log.warn("Invalid scope : '" + scope + "' has been set for the " +
                    "synapse:get-property(scope,prop-name) XPath function");
        }
        return null;
    }
}

