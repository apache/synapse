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
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;

/**
 * Concrete implementation of the {@link SynapseLog} interface appropriate
 * for usage in a mediator. Instances of this class should not be created
 * directly but by using the factory method
 * {@link AbstractMediator#getLog(org.apache.synapse.MessageContext)}.
 * <p>
 * Note that this is work in progress.
 * Please refer to https://issues.apache.org/jira/browse/SYNAPSE-374 for
 * more information.
 */
public class MediatorLog implements SynapseLog {
    private static final Log traceLog = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);
    
    private final Log defaultLog;
    private final boolean traceOn;
    
    // The definition of this constructor might change...
    public MediatorLog(Log defaultLog, boolean traceOn) {
        this.defaultLog = defaultLog;
        this.traceOn = traceOn;
    }

    public boolean isTraceOrDebugEnabled() {
        return traceOn || defaultLog.isDebugEnabled();
    }

    /**
     * Log a message to the default log at level DEBUG and and to the trace log
     * at level INFO if trace is enabled for the mediator.
     */
    public void traceOrDebug(Object msg) {
        if (traceOn) {
            traceLog.info(msg);
        }
        defaultLog.debug(msg);
    }

    /**
     * Log a message at level WARN to the default log, if level DEBUG is enabled,
     * and to the trace log, if trace is enabled for the mediator.
     */
    public void traceOrDebugWarn(Object msg) {
        if (traceOn) {
            traceLog.warn(msg);
        }
        if (defaultLog.isDebugEnabled()) {
            defaultLog.warn(msg);
        }
    }
    
    public boolean isTraceTraceEnabled() {
        return traceOn && traceLog.isTraceEnabled();
    }

    /**
     * Log a message to the trace log at level TRACE if trace is enabled for the mediator.
     */
    public void traceTrace(Object msg) {
        if (traceOn) {
            traceLog.trace(msg);
        }
    }

    /**
     * Log a message with level ERROR to the default log and to the trace, if trace is enabled.
     */
    public void error(Object msg) {
        defaultLog.error(msg);
        if (traceOn) {
            traceLog.error(msg);
        }
    }
}
