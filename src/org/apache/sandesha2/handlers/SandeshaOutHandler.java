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
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.msgprocessors.AckRequestedProcessor;
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

	public InvocationResponse invoke(MessageContext msgCtx) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaOutHandler::invoke, " + msgCtx.getEnvelope().getHeader());

		InvocationResponse returnValue = InvocationResponse.CONTINUE;
		
		ConfigurationContext context = msgCtx.getConfigurationContext();
		if (context == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.configContextNotSet);
			log.debug(message);
			throw new AxisFault(message);
		}

		AxisService axisService = msgCtx.getAxisService();
		if (axisService == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.axisServiceIsNull);
			log.debug(message);
			throw new AxisFault(message);
		}

		//see if this message is unreliable i.e. WSRM not requried
		//look at the msg ctx first
		{
			String unreliable = (String) msgCtx.getProperty(SandeshaClientConstants.UNRELIABLE_MESSAGE);
			if (null != unreliable && "true".equals(unreliable)) {
				if (log.isDebugEnabled())
					log.debug("Exit: SandeshaOutHandler::invoke, Skipping sandesha processing for unreliable message " + returnValue);
				return returnValue;
			}			
		}
		//look at the operation ctx
		{
			Parameter unreliable = msgCtx.getAxisOperation().getParameter(SandeshaClientConstants.UNRELIABLE_MESSAGE);
			if (null != unreliable && "true".equals(unreliable.getValue())) {
				if (log.isDebugEnabled())
					log.debug("Exit: SandeshaOutHandler::invoke, Skipping sandesha processing for unreliable message " + returnValue);
				return returnValue;
			}				
		}
		// Also do not apply RM to fault messages
		{
			if(msgCtx.isProcessingFault()) {
				if(log.isDebugEnabled())
					log.debug("Exit: SandeshaOutHandler::invoke, Skipping sandesha processing for fault message " + returnValue);
				return returnValue ;
			}
		}
		
		//this will change the execution chain of this message to work correctly in retransmissions.
		//For e.g. Phases like security will be removed to be called in each retransmission.
		SandeshaUtil.modifyExecutionChainForStoring(msgCtx);

		String DONE = (String) msgCtx.getProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE);
		if (null != DONE && "true".equals(DONE)) {
			if (log.isDebugEnabled())
				log.debug("Exit: SandeshaOutHandler::invoke, Application processing done " + returnValue);
			return returnValue;
		}
		
		msgCtx.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(context, context.getAxisConfiguration());

		Transaction transaction = null;

		try {
			transaction = storageManager.getTransaction();
			
			// getting rm message
			RMMsgContext rmMsgCtx = MsgInitializer.initializeMessage(msgCtx);

			MsgProcessor msgProcessor = null;
			int messageType = rmMsgCtx.getMessageType();
			if(log.isDebugEnabled()) log.debug("Message Type: " + messageType);
			if (messageType == Sandesha2Constants.MessageTypes.UNKNOWN) {
				MessageContext requestMsgCtx = msgCtx.getOperationContext().getMessageContext(
						OperationContextFactory.MESSAGE_LABEL_IN_VALUE);

                if (msgCtx.isServerSide()) { // for the server side
                    RMMsgContext reqRMMsgCtx = MsgInitializer.initializeMessage(requestMsgCtx);
                    Sequence sequencePart = (Sequence) reqRMMsgCtx
                                    .getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
                    if (sequencePart != null) {
            			String incomingSeqId = sequencePart.getIdentifier().getIdentifier();
            			msgProcessor = new ApplicationMsgProcessor(incomingSeqId);// a rm intended message
                    }
                } else // if client side.
                    msgProcessor = new ApplicationMsgProcessor();
                
			} else {
				msgProcessor = MsgProcessorFactory.getMessageProcessor(rmMsgCtx);
			}

			if (msgProcessor != null){
				if(msgProcessor.processOutMessage(rmMsgCtx)){
					//the msg was paused
					returnValue = InvocationResponse.SUSPEND;
				}
			} else if (messageType==Sandesha2Constants.MessageTypes.ACK_REQUEST) {
				AckRequestedProcessor ackRequestedProcessor = new AckRequestedProcessor ();
				if(ackRequestedProcessor.processOutgoingAckRequestMessage (rmMsgCtx)){
					//the msg was paused
					returnValue = InvocationResponse.SUSPEND;
				}
			}
				

		} catch (Exception e) {
			// message should not be sent in a exception situation.
			msgCtx.pause();
			returnValue = InvocationResponse.SUSPEND;

			// rolling back the transaction
			if (transaction != null) {
				try {
					transaction.rollback();
					transaction = null;
				} catch (Exception e1) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.rollbackError, e1.toString());
					log.debug(message, e);
				}
			}

			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.outMsgError, e.toString());
			throw new AxisFault(message, e);
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
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaOutHandler::invoke " + returnValue);
		return returnValue;
	}

	public String getName() {
		return Sandesha2Constants.OUT_HANDLER_NAME;
	}
}
