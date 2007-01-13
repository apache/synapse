/*
 * Created on Sep 5, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.apache.sandesha2.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.ListenerManager;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.wsrm.CreateSequence;

/**
 * This is used to set up a new sequence, both at the sending side and the
 * receiving side.
 */

public class SequenceManager {

	private static Log log = LogFactory.getLog(SequenceManager.class);

	/**
	 * Set up a new inbound sequence, triggered by the arrival of a create sequence message. As this
	 * is an inbound sequence, the sequencePropertyKey is the sequenceId.
	 */
	public static RMDBean setupNewSequence(RMMsgContext createSequenceMsg, StorageManager storageManager, SecurityManager securityManager, SecurityToken token)
			throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SequenceManager::setupNewSequence");
		
		String sequenceId = SandeshaUtil.getUUID();

		// Generate the new RMD Bean
		RMDBean rmdBean = new RMDBean();

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

		EndpointReference acksTo = createSequence.getAcksTo().getEPR();

		if (acksTo == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noAcksToPartInCreateSequence);
			log.debug(message);
			throw new AxisFault(message);
		}

		MessageContext createSeqContext = createSequenceMsg.getMessageContext();
		ConfigurationContext configurationContext = createSeqContext.getConfigurationContext();

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		rmdBean.setServerCompletedMessages(new ArrayList());
		
		rmdBean.setReplyToEPR(to.getAddress());
		rmdBean.setAcksToEPR(acksTo.getAddress());

		// If no replyTo value. Send responses as sync.
		if (replyTo != null)
			rmdBean.setToEPR(replyTo.getAddress());

		// Store the security token alongside the sequence
		if(token != null) {
			String tokenData = securityManager.getTokenRecoveryData(token);
			SequencePropertyBean tokenBean = new SequencePropertyBean(sequenceId,
					Sandesha2Constants.SequenceProperties.SECURITY_TOKEN, tokenData);
			seqPropMgr.insert(tokenBean);
		}		

		RMDBeanMgr nextMsgMgr = storageManager.getRMDBeanMgr();
		
		rmdBean.setSequenceID(sequenceId);
		rmdBean.setNextMsgNoToProcess(1);
		
		// If this sequence has a 'To' address that is anonymous then we must have got the
		// message as a response to a poll. We need to make sure that we keep polling until
		// the sequence is closed.
		if(to.hasAnonymousAddress()) {
			String newKey = SandeshaUtil.getUUID();
			rmdBean.setPollingMode(true);
			rmdBean.setReferenceMessageKey(newKey);
			storageManager.storeMessageContext(newKey, createSeqContext);
		}

		nextMsgMgr.insert(rmdBean);

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
		} else if (Sandesha2Constants.SPEC_2006_08.NS_URI.equals(messageRMNamespace)) {
			specVersion = Sandesha2Constants.SPEC_VERSIONS.v1_1;
		} else {
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDecideRMVersion));
		}

		SequencePropertyBean specVerionBean = new SequencePropertyBean();
		specVerionBean.setSequencePropertyKey(sequenceId);
		specVerionBean.setName(Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION);
		specVerionBean.setValue(specVersion);

		seqPropMgr.insert(specVerionBean);

		// TODO get the SOAP version from the create seq message.

		if (log.isDebugEnabled())
			log.debug("Exit: SequenceManager::setupNewSequence, " + rmdBean);
		return rmdBean;
	}

	public void removeSequence(String sequence) {

	}

	public static RMSBean setupNewClientSequence(MessageContext firstAplicationMsgCtx, String sequencePropertyKey,
			String specVersion, StorageManager storageManager) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: SequenceManager::setupNewClientSequence " + sequencePropertyKey);
		
		RMSBean rmsBean = new RMSBean();
		ConfigurationContext configurationContext = firstAplicationMsgCtx.getConfigurationContext();

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		EndpointReference toEPR = firstAplicationMsgCtx.getTo();
		String acksTo = (String) firstAplicationMsgCtx.getProperty(SandeshaClientConstants.AcksTo);

		if (toEPR == null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
			log.debug(message);
			throw new SandeshaException(message);
		}

		rmsBean.setToEPR(toEPR.getAddress());

		if (firstAplicationMsgCtx.isServerSide()) {
			// setting replyTo value, if this is the server side.
			OperationContext opContext = firstAplicationMsgCtx.getOperationContext();
			try {
				MessageContext requestMessage = opContext
						.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
				if (requestMessage == null) {
					String message = SandeshaMessageHelper
							.getMessage(SandeshaMessageKeys.cannotFindReqMsgFromOpContext);
					log.error(message);
					throw new SandeshaException(message);
				}

				// replyTo of the response msg is the 'to' value of the req msg
				EndpointReference replyToEPR = requestMessage.getTo(); 
				
				if (replyToEPR != null) {
					rmsBean.setReplyToEPR(replyToEPR.getAddress());
					rmsBean.setAcksToEPR(replyToEPR.getAddress());
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
		} else {
			EndpointReference replyToEPR = firstAplicationMsgCtx.getReplyTo();
			if (replyToEPR!=null) {
				rmsBean.setReplyToEPR(replyToEPR.getAddress());
			}

		}
		
		// Default value for acksTo is anonymous (this happens only for the client side)
		boolean anonAcks = true;
		if (acksTo != null) {
			rmsBean.setAcksToEPR(acksTo);
			EndpointReference epr = new EndpointReference(acksTo);
			anonAcks = epr.hasAnonymousAddress();
		}

		// start the in listner for the client side, if acksTo is not anonymous.
		if (!firstAplicationMsgCtx.isServerSide() && !anonAcks) {

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

		// New up the client completed messages list
		rmsBean.setClientCompletedMessages(new ArrayList());

		// saving transportTo value;
		String transportTo = (String) firstAplicationMsgCtx.getProperty(Constants.Configuration.TRANSPORT_URL);
		if (transportTo != null) {
			rmsBean.setTransportTo(transportTo);
		}

		// setting the spec version for the client side.
		SequencePropertyBean specVerionBean = new SequencePropertyBean();
		specVerionBean.setSequencePropertyKey(sequencePropertyKey);
		specVerionBean.setName(Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION);
		specVerionBean.setValue(specVersion);
		seqPropMgr.insert(specVerionBean);

		// updating the last activated time.
		rmsBean.setLastActivatedTime(System.currentTimeMillis());
		
		SandeshaUtil.startSenderForTheSequence(configurationContext, sequencePropertyKey);

		updateClientSideListnerIfNeeded(firstAplicationMsgCtx, anonAcks);
		if (log.isDebugEnabled())
			log.debug("Exit: SequenceManager::setupNewClientSequence " + rmsBean);
		return rmsBean;
	}

	private static void updateClientSideListnerIfNeeded(MessageContext messageContext, boolean anonAcks)
			throws SandeshaException {
		if (messageContext.isServerSide())
			return; // listners are updated only for the client side.

		String transportInProtocol = messageContext.getOptions().getTransportInProtocol();

		boolean startListnerForAsyncAcks = false;
		boolean startListnerForAsyncControlMsgs = false; // For async
															// createSerRes &
															// terminateSeq.

		if (!anonAcks) {
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

	public static boolean hasSequenceTimedOut(String internalSequenceId, RMMsgContext rmMsgCtx, StorageManager storageManager)
			throws SandeshaException {

		// operation is the lowest level, Sandesha2 could be engaged.
		SandeshaPolicyBean propertyBean = SandeshaUtil.getPropertyBean(rmMsgCtx.getMessageContext()
				.getAxisOperation());

		if (propertyBean.getInactivityTimeoutInterval() <= 0)
			return false;

		boolean sequenceTimedOut = false;

		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceId);
		
		if (rmsBean != null) {
			long lastActivatedTime = rmsBean.getLastActivatedTime();
			long timeNow = System.currentTimeMillis();
			if (lastActivatedTime > 0 && (lastActivatedTime + propertyBean.getInactivityTimeoutInterval() < timeNow))
				sequenceTimedOut = true;
		}
		return sequenceTimedOut;
	}
}
