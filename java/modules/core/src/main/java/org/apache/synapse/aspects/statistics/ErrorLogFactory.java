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
package org.apache.synapse.aspects.statistics;

import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;

/**
 * Creates ErrorLogs
 */
public class ErrorLogFactory {

    private static boolean enabledErrorInfo;

    static {
        enabledErrorInfo = Boolean.parseBoolean(SynapsePropertiesLoader.getPropertyValue(
                "synapse.detailederrorlogging.enable", "false"));
    }

    /**
     * Create an ErrorLog from the information in the synapse MessageContext
     *
     * @param synCtx MessageContext instance
     * @return <code>ErrorLog</code> instance
     */
    public static ErrorLog createErrorLog(org.apache.synapse.MessageContext synCtx) {

        String errorCode = String.valueOf(synCtx.getProperty(SynapseConstants.ERROR_CODE));
        ErrorLog errorLog = new ErrorLog(errorCode);
        if (enabledErrorInfo) {
            errorLog.setErrorMessage((String) synCtx.getProperty(SynapseConstants.ERROR_MESSAGE));
            errorLog.setErrorDetail((String) synCtx.getProperty(SynapseConstants.ERROR_DETAIL));
            errorLog.setException((Exception) synCtx.getProperty(SynapseConstants.ERROR_EXCEPTION));
        }
        return errorLog;
    }

    /**
     * Create an ErrorLog from the information in the Axis2 MessageContext
     *
     * @param axisCtx Axis2 MessageContext instance
     * @return <code>ErrorLog</code> instance
     */
    public static ErrorLog createErrorLog(org.apache.axis2.context.MessageContext axisCtx) {
        String errorCode = String.valueOf(axisCtx.getProperty(SynapseConstants.ERROR_CODE));
        ErrorLog errorLog = new ErrorLog(errorCode);
        if (enabledErrorInfo) {
            errorLog.setErrorMessage((String) axisCtx.getProperty(SynapseConstants.ERROR_MESSAGE));
            errorLog.setErrorDetail((String) axisCtx.getProperty(SynapseConstants.ERROR_DETAIL));
            errorLog.setException((Exception) axisCtx.getProperty(SynapseConstants.ERROR_EXCEPTION));
        }
        return errorLog;
    }
}
