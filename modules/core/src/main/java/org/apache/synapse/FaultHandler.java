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

package org.apache.synapse;

import java.util.Stack;
import java.io.StringWriter;
import java.io.Writer;
import java.io.PrintWriter;

/**
 * This is an abstract class that handles an unexpected error during Synapse mediation, but looking
 * at the stack of registered FaultHanders and invoking on them as appropriate. Sequences and
 * Endpoints would be Synapse entities that handles faults. If such an entity is unable to handle
 * an error condition, then a SynapseException should be thrown, which triggers this fault
 * handling logic.
 */
public abstract class FaultHandler {

    public void handleFault(MessageContext synCtx) {

        try {
            onFault(synCtx);

        } catch (SynapseException e) {

            Stack faultStack = synCtx.getFaultStack();
            if (faultStack != null && !faultStack.isEmpty()) {
                ((FaultHandler) faultStack.pop()).handleFault(synCtx);
            }
        }
    }

    public void handleFault(MessageContext synCtx, Exception e) {

        if (synCtx.getProperty(SynapseConstants.ERROR_CODE) == null) {
            synCtx.setProperty(SynapseConstants.ERROR_CODE, "00000");
        }
        if (synCtx.getProperty(SynapseConstants.ERROR_MESSAGE) == null) {
            // use only the first line as the message for multiline exception messages (Axis2 has these)
            synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, e.getMessage().split("\n")[0]);
        }
        synCtx.setProperty(SynapseConstants.ERROR_DETAIL, getStackTrace(e));

        try {
            onFault(synCtx);

        } catch (SynapseException se) {

            Stack faultStack = synCtx.getFaultStack();
            if (faultStack != null && !faultStack.isEmpty()) {
                ((FaultHandler) faultStack.pop()).handleFault(synCtx, se);
            }
        }
    }

    /**
     * This will be executed to handle any Exceptions occured within the Synapse environment.
     * @param synCtx SynapseMessageContext of which the fault occured message comprises
     * @throws SynapseException in case there is a failure in the fault execution
     */
    public abstract void onFault(MessageContext synCtx);

    private static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
}
