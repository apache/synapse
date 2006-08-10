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

import javax.xml.namespace.QName;

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
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.msgprocessors.AcknowledgementProcessor;
import org.apache.sandesha2.msgprocessors.MsgProcessor;
import org.apache.sandesha2.msgprocessors.MsgProcessorFactory;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;

/**
 * This is invoked in the inFlow of an RM endpoint. This is responsible for
 * selecting an suitable message processor and letting it process the message.
 */

public class SandeshaInHandler extends AbstractHandler {

	private static final long serialVersionUID = 733210926016820857L;

	private static final Log log = LogFactory.getLog(SandeshaInHandler.class.getName());

	public QName getName() {
		return new QName(Sandesha2Constants.IN_HANDLER_NAME);
	}

	public void invoke(MessageContext msgCtx) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaInHandler::invoke, " + msgCtx.getEnvelope().getHeader());

		ConfigurationContext context = msgCtx.getConfigurationContext();
		if (context == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.configContextNotSet);
			log.debug(message);
			throw new AxisFault(message);
		}

		String DONE = (String) msgCtx.getProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE);
		if (null != DONE && "true".equals(DONE))
			return;

		String reinjectedMessage = (String) msgCtx.getProperty(Sandesha2Constants.REINJECTED_MESSAGE);
		if (reinjectedMessage != null && Sandesha2Constants.VALUE_TRUE.equals(reinjectedMessage)) {
			if (log.isDebugEnabled())
				log.debug("Exit: SandeshaInHandler::invoke, reinjectedMessage");
			return; // Reinjected messages are not processed by Sandesha2 inflow
					// handlers
		}
		
		// Process Ack headers in the message
		AcknowledgementProcessor ackProcessor = new AcknowledgementProcessor();
		ackProcessor.processAckHeaders(msgCtx);

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context, context.getAxisConfiguration());

		boolean withinTransaction = false;
		String withinTransactionStr = (String) msgCtx.getProperty(Sandesha2Constants.WITHIN_TRANSACTION);
		if (withinTransactionStr != null && Sandesha2Constants.VALUE_TRUE.equals(withinTransactionStr)) {
			withinTransaction = true;
		}

		Transaction transaction = null;
		if (!withinTransaction) {
			transaction = storageManager.getTransaction();
			msgCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_TRUE);
		}
		boolean rolebacked = false;

		try {

			AxisService axisService = msgCtx.getAxisService();
			if (axisService == null) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.axisServiceIsNull);
				log.debug(message);
				throw new AxisFault(message);
			}

			RMMsgContext rmMsgCtx = null;
			try {
				rmMsgCtx = MsgInitializer.initializeMessage(msgCtx);
			} catch (SandeshaException ex) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotInnitMessage);
				log.debug(message);
				throw new AxisFault(message);
			}

			// validating the message
			MessageValidator.validateMessage(rmMsgCtx, storageManager);

			MsgProcessor msgProcessor = MsgProcessorFactory.getMessageProcessor(rmMsgCtx);

			try {
				if (msgProcessor != null)
					msgProcessor.processInMessage(rmMsgCtx);
			} catch (SandeshaException se) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.msgError, se.toString());
				log.debug(message, se);
				throw new AxisFault(message, se);
			}

		} catch (Exception e) {
			// message should not be sent in a exception situation.
			msgCtx.pause();

			if (!withinTransaction) {
				try {
					transaction.rollback();
					msgCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);
					rolebacked = true;
				} catch (Exception e1) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.rollbackError, e1.toString());
					log.debug(message, e);
				}
			}

			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.inMsgError, e.toString());
			throw new AxisFault(message, e);
		} finally {
			if (!withinTransaction && !rolebacked) {
				try {
					transaction.commit();
					msgCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);
				} catch (Exception e) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.commitError, e.toString());
					log.debug(message, e);
				}
			}
		}
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaInHandler::invoke");
	}

}
