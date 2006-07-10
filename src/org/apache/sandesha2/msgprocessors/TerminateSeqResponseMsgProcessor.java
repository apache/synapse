/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *  
 */

package org.apache.sandesha2.msgprocessors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.SandeshaException;

/**
 * To process terminate sequence response messages.
 */
public class TerminateSeqResponseMsgProcessor implements MsgProcessor {

	private static final Log log = LogFactory.getLog(TerminateSeqResponseMsgProcessor.class);
	
	public void processInMessage(RMMsgContext terminateResRMMsg)
			throws SandeshaException { 
		
		
		//TODO add processing logic
		
		terminateResRMMsg.pause();
	}
	
	public void processOutMessage(RMMsgContext rmMsgCtx) throws SandeshaException {
		
	}
}
