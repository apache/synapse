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

package org.apache.sandesha2.handlers;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.MessageValidator;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.msgprocessors.AckRequestedProcessor;
import org.apache.sandesha2.msgprocessors.AcknowledgementProcessor;
import org.apache.sandesha2.msgprocessors.MessagePendingProcessor;
import org.apache.sandesha2.msgprocessors.SequenceProcessor;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;

/**
 * This is invoked in the inFlow of an RM endpoint. This is responsible for
 * selecting an suitable message processor and letting it process the message.
 */

public class SandeshaInHandler extends AbstractHandler {

	private static final long serialVersionUID = 733210926016820857L;

	private static final Log log = LogFactory.getLog(SandeshaInHandler.class.getName());

	public String getName() {
		return Sandesha2Constants.IN_HANDLER_NAME;
	}
	
	public InvocationResponse invoke(MessageContext msgCtx) throws AxisFault {
		
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaInHandler::invoke, " + msgCtx.getEnvelope().getHeader());

		InvocationResponse returnValue = InvocationResponse.CONTINUE;
		
		ConfigurationContext context = msgCtx.getConfigurationContext();
		if (context == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.configContextNotSet);
			log.debug(message);
			throw new AxisFault(message);
		}

		String DONE = (String) msgCtx.getProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE);
		if (null != DONE && Sandesha2Constants.VALUE_TRUE.equals(DONE)) {
			if (log.isDebugEnabled())
				log.debug("Exit: SandeshaInHandler::invoke, Application processing done " + returnValue);
			return returnValue;
		}
		
		if (log.isDebugEnabled()) log.debug("SandeshaInHandler::invoke Continuing beyond basic checks");

		Transaction transaction = null;

		try {
			StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context, context.getAxisConfiguration());
			transaction = storageManager.getTransaction();

			AxisService axisService = msgCtx.getAxisService();
			if (axisService == null) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.axisServiceIsNull);
				log.debug(message);
				throw new AxisFault(message);
			}

			//processing any incoming faults.			
			RMMsgContext rmMsgCtx = MsgInitializer.initializeMessage(msgCtx);

			//This is responsible for Sandesha2 specific 
			FaultManager.processMessagesForFaults(rmMsgCtx);

			// validating the message
			MessageValidator.validateMessage(rmMsgCtx, storageManager);

			// Process Ack headers in the message
			AcknowledgementProcessor ackProcessor = new AcknowledgementProcessor();
			ackProcessor.processAckHeaders(rmMsgCtx);

			// Process Ack Request headers in the message
			AckRequestedProcessor reqProcessor = new AckRequestedProcessor();
			if(reqProcessor.processAckRequestedHeaders(rmMsgCtx)){
				returnValue = InvocationResponse.SUSPEND;
			}
			
			// Process MessagePending headers
			MessagePendingProcessor pendingProcessor = new MessagePendingProcessor();
			pendingProcessor.processMessagePendingHeaders(rmMsgCtx);

			// Process the Sequence header, if there is one
			SequenceProcessor seqProcessor = new SequenceProcessor();
			returnValue = seqProcessor.processSequenceHeader(rmMsgCtx);

		} catch (Exception e) {
			if (log.isDebugEnabled()) log.debug("SandeshaInHandler::invoke Exception caught during processInMessage", e);
			// message should not be sent in a exception situation.
			msgCtx.pause();
			returnValue = InvocationResponse.SUSPEND;
			
			if (transaction != null) {
				try {
					transaction.rollback();
					transaction = null;
				} catch (Exception e1) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.rollbackError, e1.toString());
					log.debug(message, e);
				}
			}
			
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.inMsgError, e.toString());
			throw new AxisFault(message, e);
		} 
		finally {
			if (log.isDebugEnabled()) log.debug("SandeshaInHandler::invoke Doing final processing");
			if (transaction != null) {
				try {
					transaction.commit();
				} catch (Exception e) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.commitError, e.toString());
					log.debug(message, e);
				}
			}
		}
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaInHandler::invoke " + returnValue);
		return returnValue;
	}
	
}
