/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.msgprocessors;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.polling.PollingManager;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.workers.SequenceEntry;
import org.apache.sandesha2.wsrm.MessagePending;

public class MessagePendingProcessor {

	private static final Log log = LogFactory.getLog(MessagePendingProcessor.class);
	
	public void processMessagePendingHeaders(RMMsgContext message) throws AxisFault {
		
		if (log.isDebugEnabled())
			log.debug("Enter: MessagePendingProcessor::processMessagePendingHeaders");

		MessagePending messagePending = message.getMessagePending();
		if (messagePending!=null) {
			boolean pending = messagePending.isPending();
			if (pending) {
				SequenceEntry entry = (SequenceEntry) message.getProperty(Sandesha2Constants.MessageContextProperties.MAKECONNECTION_ENTRY);
				if(entry != null) {
					ConfigurationContext context = message.getConfigurationContext();
					StorageManager storage = SandeshaUtil.getSandeshaStorageManager(context, context.getAxisConfiguration());
					PollingManager pollingManager = storage.getPollingManager();
					if(pollingManager != null) pollingManager.schedulePollingRequest(entry.getSequenceId(), entry.isRmSource());
				}
			}
		}
			
		
		
		if (log.isDebugEnabled())
			log.debug("Exit: MessagePendingProcessor::processMessagePendingHeaders");
	}

}
