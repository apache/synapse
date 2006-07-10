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
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.msgprocessors.ApplicationMsgProcessor;
import org.apache.sandesha2.msgprocessors.MsgProcessor;
import org.apache.sandesha2.msgprocessors.MsgProcessorFactory;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.wsrm.Sequence;

/**
 * This is invoked in the outFlow of an RM endpoint
 */

public class SandeshaOutHandler extends AbstractHandler {

	private static final long serialVersionUID = 8261092322051924103L;
  
  private static final Log log = LogFactory.getLog(SandeshaOutHandler.class.getName());

	public void invoke(MessageContext msgCtx) throws AxisFault {
    if (log.isDebugEnabled())
      log.debug("Enter: SandeshaOutHandler::invoke, " + msgCtx.getEnvelope().getHeader() );

		ConfigurationContext context = msgCtx.getConfigurationContext();
		if (context == null) {
			String message = "ConfigurationContext is null";
			log.debug(message);
			throw new AxisFault(message);
		}

		AxisService axisService = msgCtx.getAxisService();
		if (axisService == null) {
			String message = "AxisService is null";
			log.debug(message);
			throw new AxisFault(message);
		}

		String DONE = (String) msgCtx.getProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE);
		if (null != DONE && "true".equals(DONE))
    {
      if (log.isDebugEnabled())
        log.debug("Exit: SandeshaOutHandler::invoke, Application processing done");
			return;
    }

		msgCtx.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context,context.getAxisConfiguration());

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
			// getting rm message
			RMMsgContext rmMsgCtx = MsgInitializer.initializeMessage(msgCtx);

			String dummyMessageString = (String) msgCtx.getOptions().getProperty(SandeshaClientConstants.DUMMY_MESSAGE);
			boolean dummyMessage = false;
			if (dummyMessageString != null && Sandesha2Constants.VALUE_TRUE.equals(dummyMessageString))
				dummyMessage = true;

			MsgProcessor msgProcessor = null;
			int messageType = rmMsgCtx.getMessageType();
			if (messageType == Sandesha2Constants.MessageTypes.UNKNOWN) {
				MessageContext requestMsgCtx = msgCtx.getOperationContext().getMessageContext(
						OperationContextFactory.MESSAGE_LABEL_IN_VALUE);
				if (requestMsgCtx != null) { // for the server side
					RMMsgContext reqRMMsgCtx = MsgInitializer.initializeMessage(requestMsgCtx);
					Sequence sequencePart = (Sequence) reqRMMsgCtx
							.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
					if (sequencePart != null)
						msgProcessor = new ApplicationMsgProcessor();// a rm
																		// intended
																		// message.
				} else if (!msgCtx.isServerSide()) // if client side.
					msgProcessor = new ApplicationMsgProcessor();
			} else {
				msgProcessor = MsgProcessorFactory.getMessageProcessor(rmMsgCtx);
			}

			if (msgProcessor != null)
				msgProcessor.processOutMessage(rmMsgCtx);

		} catch (Exception e) {
			//message should not be sent in a exception situation.
			msgCtx.pause();
			
			//rolling back the transaction
			if (!withinTransaction) {
				try {
					transaction.rollback();
					msgCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);
					rolebacked = true;
				} catch (Exception e1) {
					String message = "Exception thrown when trying to roleback the transaction.";
					log.debug(message,e);
				}
			}
			
			String message = "Sandesha2 got an exception when processing the out message";
			throw new AxisFault (message,e);
		} finally {
			if (!withinTransaction && !rolebacked) {
				try {
					transaction.commit();
					msgCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);
				} catch (Exception e) {
					String message = "Exception thrown when trying to commit the transaction.";
					log.debug(message,e);
				}
			}
		}
    if (log.isDebugEnabled())
      log.debug("Exit: SandeshaOutHandler::invoke");
	}

	public QName getName() {
		return new QName(Sandesha2Constants.OUT_HANDLER_NAME);
	}
}
