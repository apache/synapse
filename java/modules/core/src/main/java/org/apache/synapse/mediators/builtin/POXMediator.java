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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * Halts further processing/mediation of the current message. i.e. returns false
 */
public class POXMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(LogMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);
    private boolean value = false;

    /**
     * Halts further mediation of the current message by returning false.
     *
     * @param synCtx the current message
     * @return false always
     */
    public boolean mediate(MessageContext synCtx) {
        log.debug("Rest mediator :: mediate()");
        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        if (shouldTrace) {
            trace.trace("Start : POX Mediator. setDoingPOX(" + value + ")");
        }
        synCtx.setDoingPOX(value);
        if (shouldTrace) {
            trace.trace("End : POX Mediator");
        }
        return true;
    }

    public void setValue(boolean value) {
		this.value = value;
    }

    public boolean getValue() { return value;}
}
