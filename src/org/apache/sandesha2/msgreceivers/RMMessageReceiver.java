/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2.msgreceivers;


import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.receivers.AbstractMessageReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;

/**
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Jaliya Ekanayaka <jaliya@opensource.lk>
 */

//Currently this is a dummy Msg Receiver.
//All the necessary RM logic happens at MessageProcessors.
//This only ensures that the defaults Messsage Receiver does not get called for RM control messages.

public class RMMessageReceiver extends AbstractMessageReceiver {

	private static final Log log = LogFactory.getLog(RMMessageReceiver.class.getName());
	
	public final void receive(MessageContext messgeCtx) throws AxisFault {
		log.debug("RM MESSSAGE RECEIVER WAS CALLED");
		
		RMMsgContext rmMsgCtx = MsgInitializer.initializeMessage(messgeCtx);
		log.debug("MsgReceiver got type:" + SandeshaUtil.getMessageTypeString(rmMsgCtx.getMessageType()));	
	}
	
}