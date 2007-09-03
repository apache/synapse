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

package org.apache.synapse.util;

import org.apache.commons.logging.Log;
import org.apache.axis2.context.MessageContext;

/**
 * This is a helper class to get the loggin done in both in and out handlers
 */
public class HandlerUtil {

    /**
     * Helper util method to get the logging done whenever injecting the message into synapse
     * 
     * @param log - Log appender to be used to append logs
     * @param messageContext - MessageContext which will be logged
     */
    public static void doHandlerLogging(Log log, MessageContext messageContext) {
        if (log.isDebugEnabled()) {
            log.debug("Synapse handler received a new message for message mediation...");
            log.debug("Received To: " + (messageContext.getTo() != null ?
                    messageContext.getTo().getAddress() : "null"));
            log.debug("SOAPAction: " + (messageContext.getSoapAction() != null ?
                    messageContext.getSoapAction() : "null"));
            log.debug("WSA-Action: " + (messageContext.getWSAAction() != null ?
                    messageContext.getWSAAction() : "null"));
            String[] cids = messageContext.getAttachmentMap().getAllContentIDs();
            if (cids != null && cids.length > 0) {
                for (int i = 0; i < cids.length; i++) {
                    log.debug("Attachment : " + cids[i]);
                }
            }
            log.debug("Body : \n" + messageContext.getEnvelope());
        }
    }
}
