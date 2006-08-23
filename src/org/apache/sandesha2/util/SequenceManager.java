/*
 * Created on Sep 5, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.apache.sandesha2.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.OperationContextFactory;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.ListenerManager;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.wsdl.WSDLConstants.WSDL20_2004Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.NextMsgBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.NextMsgBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.wsrm.CreateSequence;

/**
 * This is used to set up a new sequence, both at the sending side and the
 * receiving side.
 */

public class SequenceManager {

	private static Log log = LogFactory.getLog(SequenceManager.class);

	public static String setupNewSequence(RMMsgContext createSequenceMsg, StorageManager storageManager)
			throws AxisFault {

		String sequenceId = SandeshaUtil.getUUID();

		EndpointReference to = createSequenceMsg.getTo();
		if (to == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
			log.debug(message);
			throw new AxisFault(message);
		}

		EndpointReference replyTo = createSequenceMsg.getReplyTo();

		CreateSequence createSequence = (CreateSequence) createSequenceMsg
				.getMessagePart(Sandesha2Constants.MessageParts.CREATE_SEQ);
		if (createSequence == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.createSeqEntryNotFound);
			log.debug(message);
			throw new AxisFault(message);
		}

		EndpointReference acksTo = createSequence.getAcksTo().getAddress().getEpr();

		if (acksTo == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noAcksToPartInCreateSequence);
			log.debug(message);
			throw new AxisFault(message);
		}

		ConfigurationContext configurationContext = createSequenceMsg.getMessageContext().getConfigurationContext();

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean receivedMsgBean = new SequencePropertyBean(sequenceId,
				Sandesha2Constants.SequenceProperties.SERVER_COMPLETED_MESSAGES, "");

		// setting the addressing version
		String addressingNamespaceValue = createSequenceMsg.getAddressingNamespaceValue();
		SequencePropertyBean addressingNamespaceBean = new SequencePropertyBean(sequenceId,
				Sandesha2Constants.SequenceProperties.ADDRESSING_NAMESPACE_VALUE, addressingNamespaceValue);
		seqPropMgr.insert(addressingNamespaceBean);

		String anonymousURI = SpecSpecificConstants.getAddressingAnonymousURI(addressingNamespaceValue);

		// If no replyTo value. Send responses as sync.
		SequencePropertyBean toBean = null;
		if (replyTo != null) {
			toBean = new SequencePropertyBean(sequenceId, Sandesha2Constants.SequenceProperties.TO_EPR, replyTo
					.getAddress());
		} else {
			toBean = new SequencePropertyBean(sequenceId, Sandesha2Constants.SequenceProperties.TO_EPR, anonymousURI);
		}

		SequencePropertyBean replyToBean = new SequencePropertyBean(sequenceId,
				Sandesha2Constants.SequenceProperties.REPLY_TO_EPR, to.getAddress());
		SequencePropertyBean acksToBean = new SequencePropertyBean(sequenceId,
				Sandesha2Constants.SequenceProperties.ACKS_TO_EPR, acksTo.getAddress());

		seqPropMgr.insert(receivedMsgBean);
		seqPropMgr.insert(replyToBean);
		seqPropMgr.insert(acksToBean);

		if (toBean != null)
			seqPropMgr.insert(toBean);

		NextMsgBeanMgr nextMsgMgr = storageManager.getNextMsgBeanMgr();
		nextMsgMgr.insert(new NextMsgBean(sequenceId, 1)); // 1 will be the
															// next

		// message to invoke. This will apply for only in-order invocations.

		SandeshaUtil.startSenderForTheSequence(configurationContext, sequenceId);

		// stting the RM SPEC version for this sequence.
		String createSequenceMsgAction = createSequenceMsg.getWSAAction();
		if (createSequenceMsgAction == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noWSAACtionValue));

		String messageRMNamespace = createSequence.getNamespaceValue();

		String specVersion = null;
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(messageRMNamespace)) {
			specVersion = Sandesha2Constants.SPEC_VERSIONS.v1_0;
		} else if (Sandesha2Constants.SPEC_2005_10.NS_URI.equals(messageRMNamespace)) {
			specVersion = Sandesha2Constants.SPEC_VERSIONS.v1_1;
		} else {
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDecideRMVersion));
		}

		SequencePropertyBean specVerionBean = new SequencePropertyBean();
		specVerionBean.setSequenceID(sequenceId);
		specVerionBean.setName(Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION);
		specVerionBean.setValue(specVersion);

		seqPropMgr.insert(specVerionBean);

		// TODO get the SOAP version from the create seq message.

		return sequenceId;
	}

	public void removeSequence(String sequence) {

	}

	public static void setupNewClientSequence(MessageContext firstAplicationMsgCtx, String internalSequenceId,
			String specVersion, StorageManager storageManager) throws SandeshaException {

		ConfigurationContext configurationContext = firstAplicationMsgCtx.getConfigurationContext();

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		// setting the addressing version
		String addressingNamespace = (String) firstAplicationMsgCtx
				.getProperty(AddressingConstants.WS_ADDRESSING_VERSION);

		if (addressingNamespace == null) {
			OperationContext opCtx = firstAplicationMsgCtx.getOperationContext();
			if (opCtx != null) {
				try {
					MessageContext requestMsg = opCtx.getMessageContext(OperationContextFactory.MESSAGE_LABEL_IN_VALUE);
					if (requestMsg != null)
						addressingNamespace = (String) requestMsg
								.getProperty(AddressingConstants.WS_ADDRESSING_VERSION);
				} catch (AxisFault e) {
					throw new SandeshaException(e);
				}
			}
		}

		if (addressingNamespace == null)
			addressingNamespace = AddressingConstants.Final.WSA_NAMESPACE; // defaults
																			// to
																			// Final.
																			// Make
																			// sure
																			// this
																			// is
																			// synchronized
																			// with
																			// addressing.

		SequencePropertyBean addressingNamespaceBean = new SequencePropertyBean(internalSequenceId,
				Sandesha2Constants.SequenceProperties.ADDRESSING_NAMESPACE_VALUE, addressingNamespace);
		seqPropMgr.insert(addressingNamespaceBean);

		String anonymousURI = SpecSpecificConstants.getAddressingAnonymousURI(addressingNamespace);

		EndpointReference toEPR = firstAplicationMsgCtx.getTo();
		String acksTo = (String) firstAplicationMsgCtx.getProperty(SandeshaClientConstants.AcksTo);

		if (toEPR == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
			log.debug(message);
			throw new SandeshaException(message);
		}

		SequencePropertyBean toBean = new SequencePropertyBean(internalSequenceId,
				Sandesha2Constants.SequenceProperties.TO_EPR, toEPR.getAddress());
		SequencePropertyBean replyToBean = null;
		SequencePropertyBean acksToBean = null;

		if (firstAplicationMsgCtx.isServerSide()) {
			// setting replyTo value, if this is the server side.
			OperationContext opContext = firstAplicationMsgCtx.getOperationContext();
			try {
				MessageContext requestMessage = opContext
						.getMessageContext(OperationContextFactory.MESSAGE_LABEL_IN_VALUE);
				if (requestMessage == null) {
					String message = SandeshaMessageHelper
							.getMessage(SandeshaMessageKeys.cannotFindReqMsgFromOpContext);
					log.error(message);
					throw new SandeshaException(message);
				}

				EndpointReference replyToEPR = requestMessage.getTo(); // 'replyTo'
																		// of
																		// the
																		// response
																		// msg
																		// is
																		// the
																		// 'to'
																		// value
																		// of
																		// the
																		// req
																		// msg.
				if (replyToEPR != null) {
					replyToBean = new SequencePropertyBean(internalSequenceId,
							Sandesha2Constants.SequenceProperties.REPLY_TO_EPR, replyToEPR.getAddress());
					acksToBean = new SequencePropertyBean(internalSequenceId,
							Sandesha2Constants.SequenceProperties.ACKS_TO_EPR, replyToEPR.getAddress());
				} else {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
					log.error(message);
					throw new SandeshaException(message);
				}
			} catch (AxisFault e) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotFindReqMsgFromOpContext);
				log.error(message);
				log.error(e.getStackTrace());
				throw new SandeshaException(message);
			}
		}
		// Default value for acksTo is anonymous (this happens only for the
		// client side)
		if (acksTo == null) {
			acksTo = anonymousURI;
		}

		acksToBean = new SequencePropertyBean(internalSequenceId, Sandesha2Constants.SequenceProperties.ACKS_TO_EPR,
				acksTo);

		// start the in listner for the client side, if acksTo is not anonymous.
		if (!firstAplicationMsgCtx.isServerSide() && !anonymousURI.equals(acksTo)) {

			String transportInProtocol = firstAplicationMsgCtx.getOptions().getTransportInProtocol();
			if (transportInProtocol == null) {
				throw new SandeshaException(SandeshaMessageHelper
						.getMessage(SandeshaMessageKeys.cannotStartListenerForIncommingMsgs));
			}

			try {
				ListenerManager listenerManager = firstAplicationMsgCtx.getConfigurationContext().getListenerManager();
				TransportInDescription transportIn = firstAplicationMsgCtx.getConfigurationContext()
						.getAxisConfiguration().getTransportIn(new QName(transportInProtocol));
				// if acksTo is not anonymous start the in-transport
				if (!listenerManager.isListenerRunning(transportIn.getName().getLocalPart())) {
					listenerManager.addListener(transportIn, false);
				}
			} catch (AxisFault e) {
				throw new SandeshaException(SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.cannotStartTransportListenerDueToError, e.toString()), e);
			}

		}

		SequencePropertyBean msgsBean = new SequencePropertyBean();
		msgsBean.setSequenceID(internalSequenceId);
		msgsBean.setName(Sandesha2Constants.SequenceProperties.CLIENT_COMPLETED_MESSAGES);
		msgsBean.setValue("");

		seqPropMgr.insert(msgsBean);

		seqPropMgr.insert(toBean);
		if (acksToBean != null)
			seqPropMgr.insert(acksToBean);
		if (replyToBean != null)
			seqPropMgr.insert(replyToBean);

		// saving transportTo value;
		String transportTo = (String) firstAplicationMsgCtx.getProperty(MessageContextConstants.TRANSPORT_URL);
		if (transportTo != null) {
			SequencePropertyBean transportToBean = new SequencePropertyBean();
			transportToBean.setSequenceID(internalSequenceId);
			transportToBean.setName(Sandesha2Constants.SequenceProperties.TRANSPORT_TO);
			transportToBean.setValue(transportTo);

			seqPropMgr.insert(transportToBean);
		}

		// setting the spec version for the client side.
		SequencePropertyBean specVerionBean = new SequencePropertyBean();
		specVerionBean.setSequenceID(internalSequenceId);
		specVerionBean.setName(Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION);
		specVerionBean.setValue(specVersion);
		seqPropMgr.insert(specVerionBean);

		// updating the last activated time.
		updateLastActivatedTime(internalSequenceId, storageManager);

		SandeshaUtil.startSenderForTheSequence(configurationContext, internalSequenceId);

		
		updateClientSideListnerIfNeeded(firstAplicationMsgCtx, anonymousURI);

	}

	private static void updateClientSideListnerIfNeeded(MessageContext messageContext, String addressingAnonymousURI)
			throws SandeshaException {
		if (messageContext.isServerSide())
			return; // listners are updated only for the client side.

		String transportInProtocol = messageContext.getOptions().getTransportInProtocol();

		String acksTo = (String) messageContext.getProperty(SandeshaClientConstants.AcksTo);
		String mep = messageContext.getAxisOperation().getMessageExchangePattern();

		boolean startListnerForAsyncAcks = false;
		boolean startListnerForAsyncControlMsgs = false; // For async
															// createSerRes &
															// terminateSeq.

		if (acksTo != null && !addressingAnonymousURI.equals(acksTo)) {
			// starting listner for async acks.
			startListnerForAsyncAcks = true;
		}

		try {
			if ((startListnerForAsyncAcks || startListnerForAsyncControlMsgs) ) {
				
				if (transportInProtocol == null){
					EndpointReference toEPR = messageContext.getOptions().getTo();
					if (toEPR==null) {
						String message = SandeshaMessageHelper.getMessage(
								SandeshaMessageKeys.toEPRNotSet);
						throw new AxisFault (message);
					}
					
					try {
						URI uri = new URI (toEPR.getAddress());
						String scheme = uri.getScheme();
						
						//this is a convention is Axis2. The name of the TransportInDescription has to be the
						//scheme of a URI of that transport.
						//Here we also assume that the Incoming transport will be same as the outgoing one.
						transportInProtocol = scheme;
					} catch (URISyntaxException e) {
						throw new SandeshaException (e);
					}
					
				}
			
				//TODO following code was taken from ServiceContext.gegMyEPR method.
				//	   When a listner-starting method becomes available from Axis2, use that.
				ConfigurationContext configctx = messageContext.getConfigurationContext();
				ListenerManager lm = configctx.getListenerManager();
				if (!lm.isListenerRunning(transportInProtocol)) {
					TransportInDescription trsin = configctx.getAxisConfiguration().
                        	getTransportIn(new QName(transportInProtocol));
					if (trsin != null) {
						lm.addListener(trsin, false);
					} else {
						String message = SandeshaMessageHelper.getMessage(
								SandeshaMessageKeys.cannotFindTransportInDesc,transportInProtocol);
						throw new AxisFault(message);
					}
				}
			}

		} catch (AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotStartTransportListenerDueToError, e.toString());
			log.error(e.getStackTrace());
			throw new SandeshaException(message, e);
		}

	}

	/**
	 * Takes the internalSeqID as the param. Not the sequenceID.
	 * 
	 * @param internalSequenceID
	 * @param configContext
	 * @throws SandeshaException
	 */
	public static void updateLastActivatedTime(String propertyKey, StorageManager storageManager)
			throws SandeshaException {
		// Transaction lastActivatedTransaction =
		// storageManager.getTransaction();
		SequencePropertyBeanMgr sequencePropertyBeanMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean lastActivatedBean = sequencePropertyBeanMgr.retrieve(propertyKey,
				Sandesha2Constants.SequenceProperties.LAST_ACTIVATED_TIME);

		boolean added = false;

		if (lastActivatedBean == null) {
			added = true;
			lastActivatedBean = new SequencePropertyBean();
			lastActivatedBean.setSequenceID(propertyKey);
			lastActivatedBean.setName(Sandesha2Constants.SequenceProperties.LAST_ACTIVATED_TIME);
		}

		long currentTime = System.currentTimeMillis();
		lastActivatedBean.setValue(Long.toString(currentTime));

		if (added)
			sequencePropertyBeanMgr.insert(lastActivatedBean);
		else
			sequencePropertyBeanMgr.update(lastActivatedBean);

	}

	public static long getLastActivatedTime(String propertyKey, StorageManager storageManager) throws SandeshaException {

		SequencePropertyBeanMgr seqPropBeanMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean lastActivatedBean = seqPropBeanMgr.retrieve(propertyKey,
				Sandesha2Constants.SequenceProperties.LAST_ACTIVATED_TIME);

		long lastActivatedTime = -1;

		if (lastActivatedBean != null) {
			lastActivatedTime = Long.parseLong(lastActivatedBean.getValue());
		}

		return lastActivatedTime;
	}

	public static boolean hasSequenceTimedOut(String propertyKey, RMMsgContext rmMsgCtx, StorageManager storageManager)
			throws SandeshaException {

		// operation is the lowest level, Sandesha2 could be engaged.
		SandeshaPropertyBean propertyBean = SandeshaUtil.getPropertyBean(rmMsgCtx.getMessageContext()
				.getAxisOperation());

		if (propertyBean.getInactiveTimeoutInterval() <= 0)
			return false;

		boolean sequenceTimedOut = false;

		long lastActivatedTime = getLastActivatedTime(propertyKey, storageManager);
		long timeNow = System.currentTimeMillis();
		if (lastActivatedTime > 0 && (lastActivatedTime + propertyBean.getInactiveTimeoutInterval() < timeNow))
			sequenceTimedOut = true;

		return sequenceTimedOut;
	}

	public static long getOutGoingSequenceAckedMessageCount(String internalSequenceID, StorageManager storageManager)
			throws SandeshaException {
		// / Transaction transaction = storageManager.getTransaction();
		SequencePropertyBeanMgr seqPropBeanMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean findSeqIDBean = new SequencePropertyBean();
		findSeqIDBean.setValue(internalSequenceID);
		findSeqIDBean.setName(Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID);
		Collection seqIDBeans = seqPropBeanMgr.find(findSeqIDBean);

		if (seqIDBeans.size() == 0) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noSequenceEstablished,
					internalSequenceID);
			log.debug(message);
			throw new SandeshaException(message);
		}

		if (seqIDBeans.size() > 1) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotGenerateReportNonUniqueSequence, internalSequenceID);
			log.debug(message);
			throw new SandeshaException(message);
		}

		SequencePropertyBean seqIDBean = (SequencePropertyBean) seqIDBeans.iterator().next();
		String sequenceID = seqIDBean.getSequenceID();

		SequencePropertyBean ackedMsgBean = seqPropBeanMgr.retrieve(sequenceID,
				Sandesha2Constants.SequenceProperties.NO_OF_OUTGOING_MSGS_ACKED);
		if (ackedMsgBean == null)
			return 0; // No acknowledgement has been received yet.

		long noOfMessagesAcked = Long.parseLong(ackedMsgBean.getValue());
		// / transaction.commit();

		return noOfMessagesAcked;
	}

	public static boolean isOutGoingSequenceCompleted(String internalSequenceID, StorageManager storageManager)
			throws SandeshaException {
		// / Transaction transaction = storageManager.getTransaction();
		SequencePropertyBeanMgr seqPropBeanMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean findSeqIDBean = new SequencePropertyBean();
		findSeqIDBean.setValue(internalSequenceID);
		findSeqIDBean.setName(Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID);
		Collection seqIDBeans = seqPropBeanMgr.find(findSeqIDBean);

		if (seqIDBeans.size() == 0) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noSequenceEstablished);
			log.debug(message);
			throw new SandeshaException(message);
		}

		if (seqIDBeans.size() > 1) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotGenerateReportNonUniqueSequence, internalSequenceID);
			log.debug(message);
			throw new SandeshaException(message);
		}

		SequencePropertyBean seqIDBean = (SequencePropertyBean) seqIDBeans.iterator().next();
		String sequenceID = seqIDBean.getSequenceID();

		SequencePropertyBean terminateAddedBean = seqPropBeanMgr.retrieve(sequenceID,
				Sandesha2Constants.SequenceProperties.TERMINATE_ADDED);
		if (terminateAddedBean == null)
			return false;

		if ("true".equals(terminateAddedBean.getValue()))
			return true;

		// / transaction.commit();
		return false;
	}

	public static boolean isIncomingSequenceCompleted(String sequenceID, StorageManager storageManager)
			throws SandeshaException {

		// / Transaction transaction = storageManager.getTransaction();
		SequencePropertyBeanMgr seqPropBeanMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean terminateReceivedBean = seqPropBeanMgr.retrieve(sequenceID,
				Sandesha2Constants.SequenceProperties.TERMINATE_RECEIVED);
		boolean complete = false;

		if (terminateReceivedBean != null && "true".equals(terminateReceivedBean.getValue()))
			complete = true;

		// / transaction.commit();
		return complete;
	}

}
