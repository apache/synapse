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
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.receivers.AbstractMessageReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.msgprocessors.MsgProcessor;
import org.apache.sandesha2.msgprocessors.MsgProcessorFactory;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;

/**
*Currently this is a dummy Msg Receiver.
*All the necessary RM logic happens at MessageProcessors.
*This only ensures that the defaults Messsage Receiver does not get called for RM control messages.
*/


public class RMMessageReceiver extends AbstractMessageReceiver {

	private static final Log log = LogFactory.getLog(RMMessageReceiver.class);
	
	public final void receive(MessageContext msgCtx) throws AxisFault {
		if(log.isDebugEnabled()) log.debug("Entry: RMMessageReceiver::receive");
		
		RMMsgContext rmMsgCtx = MsgInitializer.initializeMessage(msgCtx);
		if(log.isDebugEnabled()) log.debug("MsgReceiver got type: " + SandeshaUtil.getMessageTypeString(rmMsgCtx.getMessageType()));	

		// Note that some messages (such as stand-alone acks) will be routed here, but
		// the headers will already have been processed. Therefore we should not assume
		// that we will have a MsgProcessor every time.
		MsgProcessor msgProcessor = MsgProcessorFactory.getMessageProcessor(rmMsgCtx);
		if(msgProcessor != null) {
			Transaction transaction = null;
			
			try {
				ConfigurationContext context = msgCtx.getConfigurationContext();
				StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context, context.getAxisConfiguration());				
				transaction = storageManager.getTransaction();

				msgProcessor.processInMessage(rmMsgCtx);

			} catch (Exception e) {
				if (log.isDebugEnabled())
					log.debug("Exception caught during processInMessage", e);
				// message should not be sent in a exception situation.
				msgCtx.pause();
	
				if (transaction != null) {
					try {
						transaction.rollback();
						transaction = null;
					} catch (Exception e1) {
						String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.rollbackError, e1.toString());
						log.debug(message, e);
					}
				}
	
				if (!(e instanceof AxisFault)) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.inMsgError, e.toString());
					throw new AxisFault(message, e);
				}
				
				throw (AxisFault)e;
			} finally {
				if (transaction != null) {
					try {
						transaction.commit();
					} catch (Exception e) {
						String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.commitError, e.toString());
						log.debug(message, e);
					}
				}
			}
		}	

		if(log.isDebugEnabled()) log.debug("Exit: RMMessageReceiver::receive");
	}
}
