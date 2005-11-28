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

package org.apache.synapse.processors.builtin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

import org.apache.synapse.processors.AbstractProcessor;

/**
 * @author Paul Fremantle
 * <p>
 * Logs the message using commons logging
 * 
 * <p>
 * TODO make configurable - short or detailed logs
 *
 */
public class LogProcessor extends AbstractProcessor {
	
	private Log log = LogFactory.getLog(getClass());

	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
		if (smc.getTo() != null)
			log.info("To: " + smc.getTo().getAddress());
		if (smc.getFrom() != null)
			log.info("From: " + smc.getFrom().getAddress());
		if (smc.getReplyTo() != null)
			log.info("ReplyTo: " + smc.getReplyTo().getAddress());
		if (smc.getEnvelope() != null)
			log.info("Envelope: " + smc.getEnvelope().toString());
		return true;
	}


}
