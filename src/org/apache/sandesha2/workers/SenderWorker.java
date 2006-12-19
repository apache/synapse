package org.apache.sandesha2.workers;

import java.util.ArrayList;
import java.util.MissingResourceException;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.MessageRetransmissionAdjuster;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.wsrm.TerminateSequence;

public class SenderWorker extends SandeshaWorker implements Runnable {

  private static final Log log = LogFactory.getLog(SenderWorker.class);

	private ConfigurationContext configurationContext = null;
	private SenderBean senderBean = null;
	private TransportOutDescription transportOut = null;
	
	public SenderWorker (ConfigurationContext configurationContext, SenderBean senderBean) {
		this.configurationContext = configurationContext;
		this.senderBean = senderBean;
	}
	
	public void setTransportOut (TransportOutDescription transportOut) {
		this.transportOut = transportOut;
	}
	
	public void run () {
		
		if (log.isDebugEnabled())
			log.debug("Enter: SenderWorker::run");

		Transaction transaction = null;
		
		try {
			StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext, configurationContext.getAxisConfiguration());
			SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();
			
			transaction = storageManager.getTransaction();

			String key = senderBean.getMessageContextRefKey();
			MessageContext msgCtx = storageManager.retrieveMessageContext(key, configurationContext);
      
			if (msgCtx == null) {
				// This sender bean has already been processed
				return;
			}
      
			RMMsgContext rmMsgCtx = MsgInitializer.initializeMessage(msgCtx);

			boolean continueSending = MessageRetransmissionAdjuster.adjustRetransmittion(rmMsgCtx, senderBean, configurationContext,
					storageManager);
			if (!continueSending) {
				if (log.isDebugEnabled())
					log.debug("Exit: SenderWorker::run, !continueSending");
				return;
			}

			// sender will not send the message if following property is
			// set and not true.
			// But it will set if it is not set (null)

			// This is used to make sure that the mesage get passed the
			// Sandesha2TransportSender.

			String qualifiedForSending = (String) msgCtx.getProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING);
			if (qualifiedForSending != null && !qualifiedForSending.equals(Sandesha2Constants.VALUE_TRUE)) {
				if (log.isDebugEnabled())
					log.debug("Exit: SenderWorker::run, !qualified for sending");
				return;
			}

			if (msgCtx == null) {
				if (log.isDebugEnabled())
					log.debug(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.sendHasUnavailableMsgEntry));
				return;			
			}

			// operation is the lowest level Sandesha2 should be attached
			ArrayList msgsNotToSend = SandeshaUtil.getPropertyBean(msgCtx.getAxisOperation()).getMsgTypesToDrop();

			if (msgsNotToSend != null && msgsNotToSend.contains(new Integer(rmMsgCtx.getMessageType()))) {
				if (log.isDebugEnabled())
					log.debug("Exit: SenderWorker::run, message type to be dropped " + rmMsgCtx.getMessageType());
				return;	
			}

			// If we are sending to the anonymous URI then we _must_ have a transport waiting,
			// or the message can't go anywhere. If there is nothing here then we leave the
			// message in the sender queue, and a MakeConnection will hopefully pick it up
			// soon.
			EndpointReference toEPR = msgCtx.getTo();
			if(toEPR.hasAnonymousAddress()) {
				RequestResponseTransport t = null;
				MessageContext inMsg = null;
				OperationContext op = msgCtx.getOperationContext();
				if(op != null) inMsg = op.getMessageContext(OperationContextFactory.MESSAGE_LABEL_IN_VALUE);
				if(inMsg != null) t = (RequestResponseTransport) inMsg.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
				if(t == null) {
					if(log.isDebugEnabled()) log.debug("Exit: SenderWorker::run, no response transport for anonymous message");
					return;
				}
			}

			updateMessage(msgCtx);

			int messageType = senderBean.getMessageType();
			
//			if (messageType == Sandesha2Constants.MessageTypes.APPLICATION) {
//				Sequence sequence = (Sequence) rmMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
//				String sequenceID = sequence.getIdentifier().getIdentifier();
//			}

//			if (AcknowledgementManager.ackRequired (rmMsgCtx)) {
//				RMMsgCreator.addAckMessage(rmMsgCtx);
			
			//} else 
				
			if (isAckPiggybackableMsgType(messageType)) { // checking weather this message can carry piggybacked acks
				// piggybacking if an ack if available for the same
				// sequence.
				// TODO do piggybacking based on wsa:To
					
				AcknowledgementManager.piggybackAcksIfPresent(rmMsgCtx, storageManager);
			}

			// sending the message
			
			//if a different TransportOutDesc hs already been set, it will be used instead
			//of the one from te MessageContext.
			
			if (transportOut!=null)
				msgCtx.setTransportOut(transportOut);

			boolean successfullySent = false;

			// have to commit the transaction before sending. This may
			// get changed when WS-AT is available.
			if(transaction != null) {
				transaction.commit();
				transaction = null;
			}

			try {

				// had to fully build the SOAP envelope to support
				// retransmissions.
				// Otherwise a 'parserAlreadyAccessed' exception could
				// get thrown in retransmissions.
				// But this has a performance reduction.
				msgCtx.getEnvelope().build();

				ArrayList retransmittablePhases = (ArrayList) msgCtx.getProperty(Sandesha2Constants.RETRANSMITTABLE_PHASES);
				if (retransmittablePhases!=null) {
					msgCtx.setExecutionChain(retransmittablePhases);
				} else {
					ArrayList emptyExecutionChain = new ArrayList ();
					msgCtx.setExecutionChain(emptyExecutionChain);
				}
				
				msgCtx.setCurrentHandlerIndex(0);
				msgCtx.setCurrentPhaseIndex(0);
				msgCtx.setPaused(false);
			
				AxisEngine engine = new AxisEngine (msgCtx.getConfigurationContext());
				if (log.isDebugEnabled())
					log.debug("Resuming a send for message : " + msgCtx.getEnvelope().getHeader());
				InvocationResponse response = engine.resumeSend(msgCtx);
				if(log.isDebugEnabled()) log.debug("Engine resume returned " + response);
				if(response != InvocationResponse.SUSPEND) {
					RequestResponseTransport t = null;
					MessageContext inMsg = null;
					OperationContext op = msgCtx.getOperationContext();
					if(op != null) inMsg = op.getMessageContext(OperationContextFactory.MESSAGE_LABEL_IN_VALUE);
					if(inMsg != null) t = (RequestResponseTransport) inMsg.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
					if(t != null) {
						if(log.isDebugEnabled()) log.debug("Signalling transport in " + t);
						if(t != null) t.signalResponseReady();
					}
				}
				
				successfullySent = true;
			} catch (Exception e) {
				String message = SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.sendMsgError, e.toString());
				
				if (log.isErrorEnabled())
				  log.error(message, e);
				// Store the Exception as a sequence property to enable the client to lookup the last 
				// exception time and timestamp.
				
				// Create a new Transaction
				transaction = storageManager.getTransaction();
			
				try
				{
					
					// Get the internal sequence id from the context
					String internalSequenceId = (String)rmMsgCtx.getProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID);
					
					// Get the sequence property bean manager
					SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
					
					// Add the new sequence property beans.
					String exceptionStr = SandeshaUtil.getStackTrace(e);
					SequencePropertyBean eBean = 
						new SequencePropertyBean(internalSequenceId, 
																	   Sandesha2Constants.SequenceProperties.LAST_FAILED_TO_SEND_ERROR, 
																	   exceptionStr);
					
					SequencePropertyBean etsBean = 
						new SequencePropertyBean(internalSequenceId, 
																	   Sandesha2Constants.SequenceProperties.LAST_FAILED_TO_SEND_ERROR_TIMESTAMP, 
																	   String.valueOf(System.currentTimeMillis()));
					
					
					// Insert the exception bean
					seqPropMgr.insert(eBean);
					
					// Insert the timestamp bean
					seqPropMgr.insert(etsBean);
					
					// Commit the properties
					if(transaction != null) {
						transaction.commit();
						transaction = null;
					}
				}
				catch (Exception e1)
				{
					if (log.isErrorEnabled())
						log.error(e1);
					
					if (transaction != null) {
						transaction.rollback();
						transaction = null;
					}
				}
				
			}
			// Establish the transaction for post-send processing
			transaction = storageManager.getTransaction();

			// update or delete only if the object is still present.
			SenderBean bean1 = senderBeanMgr
					.retrieve(senderBean.getMessageID());
			if (bean1 != null) {
				if (senderBean.isReSend()) {
					bean1.setSentCount(senderBean.getSentCount());
					bean1.setTimeToSend(senderBean.getTimeToSend());
					senderBeanMgr.update(bean1);
				} else {
					senderBeanMgr.delete(bean1.getMessageID());

					// removing the message from the storage.
					String messageStoredKey = bean1.getMessageContextRefKey();
					storageManager.removeMessageContext(messageStoredKey);
				}
			}

			if (successfullySent) {
				if (!msgCtx.isServerSide())
				{
					// Commit the transaction to release the SenderBean
					transaction.commit();
					transaction = null;
					transaction = storageManager.getTransaction();
					checkForSyncResponses(msgCtx);
				}
			}

			if ((rmMsgCtx.getMessageType() == Sandesha2Constants.MessageTypes.TERMINATE_SEQ)
					&&
					 (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(rmMsgCtx.getRMNamespaceValue()))) {
				
				//terminate message sent using the SandeshaClient. Since the terminate message will simply get the
				//InFlow of the reference message get called which could be zero sized (OutOnly operations).
				
				// terminate sending side if this is the WSRM 1.0 spec. 
				// If the WSRM versoion is 1.1 termination will happen in the terminate sequence response message.
				
				TerminateSequence terminateSequence = (TerminateSequence) rmMsgCtx
						.getMessagePart(Sandesha2Constants.MessageParts.TERMINATE_SEQ);
				String sequenceID = terminateSequence.getIdentifier().getIdentifier();
				ConfigurationContext configContext = msgCtx.getConfigurationContext();

				String internalSequenceID = SandeshaUtil.getSequenceProperty(sequenceID,
						Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID, storageManager);
				String sequencePropertyKey = internalSequenceID; //property key of the sending side is the internal sequence Id.
				TerminateManager.terminateSendingSide(configContext, sequencePropertyKey ,internalSequenceID, msgCtx.isServerSide(),
						storageManager);
			}

		} catch (Exception e) {
			if (log.isDebugEnabled()) log.debug("Caught exception", e);
			if (transaction!=null) {
				transaction.rollback();
				transaction = null;
			}
		} finally {
			if (transaction!=null) transaction.commit();
			
			if (lock!=null && workId!=null) {
				lock.removeWork(workId);
			}
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: SenderWorker::run");
	}
	
	private void updateMessage(MessageContext msgCtx1) throws SandeshaException {
		// do updates if required.
	}
	
	private boolean isAckPiggybackableMsgType(int messageType) {
		if (log.isDebugEnabled())
			log.debug("Enter: SenderWorker::isAckPiggybackableMsgType, " + messageType);
		boolean piggybackable = true;

		if (messageType == Sandesha2Constants.MessageTypes.ACK)
			piggybackable = false;

		if (log.isDebugEnabled())
			log.debug("Exit: SenderWorker::isAckPiggybackableMsgType, " + piggybackable);
		return piggybackable;
	}
	
	private void checkForSyncResponses(MessageContext msgCtx) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: SenderWorker::checkForSyncResponses, " + msgCtx.getEnvelope().getHeader());

		try {

			boolean responsePresent = (msgCtx.getProperty(MessageContext.TRANSPORT_IN) != null);
			if (!responsePresent) {
				if(log.isDebugEnabled()) log.debug("Exit: SenderWorker::checkForSyncResponses, no response present");
				return;
			}

			// create the responseMessageContext

			MessageContext responseMessageContext = new MessageContext();
			
			//setting the message as serverSide will let it go through the MessageReceiver (may be callback MR).
			responseMessageContext.setServerSide(true);
			
			responseMessageContext.setConfigurationContext(msgCtx.getConfigurationContext());
			responseMessageContext.setTransportIn(msgCtx.getTransportIn());
			responseMessageContext.setTransportOut(msgCtx.getTransportOut());

			responseMessageContext.setProperty(MessageContext.TRANSPORT_IN, msgCtx
					.getProperty(MessageContext.TRANSPORT_IN));
			responseMessageContext.setServiceContext(msgCtx.getServiceContext());
			responseMessageContext.setServiceGroupContext(msgCtx.getServiceGroupContext());

			// copying required properties from op. context to the response msg
			// ctx.
			OperationContext requestMsgOpCtx = msgCtx.getOperationContext();
			if (requestMsgOpCtx != null) {
				responseMessageContext.setOperationContext(requestMsgOpCtx);
				
				if (responseMessageContext.getProperty(HTTPConstants.MTOM_RECEIVED_CONTENT_TYPE) == null) {
					responseMessageContext.setProperty(HTTPConstants.MTOM_RECEIVED_CONTENT_TYPE, requestMsgOpCtx
							.getProperty(HTTPConstants.MTOM_RECEIVED_CONTENT_TYPE));
				}

				if (responseMessageContext.getProperty(HTTPConstants.CHAR_SET_ENCODING) == null) {
					responseMessageContext.setProperty(HTTPConstants.CHAR_SET_ENCODING, requestMsgOpCtx
							.getProperty(HTTPConstants.CHAR_SET_ENCODING));
				}
			}

			// If request is REST we assume the responseMessageContext is REST,
			// so set the variable

			responseMessageContext.setDoingREST(msgCtx.isDoingREST());

			SOAPEnvelope resenvelope = null;
			try {
				// MessageContext is modified in TransportUtils.createSOAPMessage(). It might be used by axis.engine or handler.
				// To catch the modification and pass it to engine or handler, resenvelope is created by responseMessageContext. 
				resenvelope = TransportUtils.createSOAPMessage(responseMessageContext, msgCtx.getEnvelope().getNamespace().getNamespaceURI());
			} catch (AxisFault e) {
				//Cannot find a valid SOAP envelope.
				if (log.isDebugEnabled()) {
					log.debug(SandeshaMessageHelper
							.getMessage(SandeshaMessageKeys.soapEnvNotSet));
					log.debug("Caught exception", e);
				}
				
				return;
			}

			if (resenvelope != null) {
				responseMessageContext.setEnvelope(resenvelope);
				AxisEngine engine = new AxisEngine(msgCtx.getConfigurationContext());

				if (isFaultEnvelope(resenvelope)) {
					engine.receiveFault(responseMessageContext);
				} else {
					engine.receive(responseMessageContext);
				}
			}

		} catch (Exception e) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noValidSyncResponse);
			log.debug(message, e);
			throw new SandeshaException(message, e);
		}
		if (log.isDebugEnabled())
			log.debug("Exit: SenderWorker::checkForSyncResponses");
	}
	
	private boolean isFaultEnvelope(SOAPEnvelope envelope) {
		if (log.isDebugEnabled())
			log.debug("Enter: SenderWorker::isFaultEnvelope, " + envelope.getBody().getFault());
		SOAPFault fault = envelope.getBody().getFault();
		if (fault != null) {
			if (log.isDebugEnabled())
				log.debug("Exit: SenderWorker::isFaultEnvelope, TRUE");
			return true;
		}

		if (log.isDebugEnabled())
			log.debug("Exit: SenderWorker::isFaultEnvelope, FALSE");
		return false;
	}

}
