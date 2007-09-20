/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
 */

package org.apache.sandesha2.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axiom.soap.impl.llom.soap12.SOAP12Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisOperationFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.msgreceivers.RMMessageReceiver;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.workers.Invoker;
import org.apache.sandesha2.wsrm.AckRequested;
import org.apache.sandesha2.wsrm.CloseSequence;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.TerminateSequence;

/**
 * Contains all the Sandesha2Constants of Sandesha2. Please see sub-interfaces
 * to see grouped data.
 */

public class SandeshaClient {

	private static final Log log = LogFactory.getLog(SandeshaClient.class);

	/**
	 * Users can get a SequenceReport of the sequence defined by the information
	 * given from the passed serviceClient object.
	 * 
	 * @param serviceClient
	 * @return
	 * @throws SandeshaException
	 */
	public static SequenceReport getOutgoingSequenceReport(ServiceClient serviceClient) throws SandeshaException {

		ServiceContext serviceContext = serviceClient.getServiceContext();
		if (serviceContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		ConfigurationContext configurationContext = serviceContext.getConfigurationContext();

		String internalSequenceID = getInternalSequenceIdFromServiceClient(serviceClient);

		return getOutgoingSequenceReport(internalSequenceID, configurationContext);
	}

	public static SequenceReport getOutgoingSequenceReport(String to, String sequenceKey,
			ConfigurationContext configurationContext) throws SandeshaException {

		String internalSequenceID = SandeshaUtil.getInternalSequenceID(to, sequenceKey);
		return getOutgoingSequenceReport(internalSequenceID, configurationContext);
	}

	public static SequenceReport getOutgoingSequenceReport(String internalSequenceID,	
			ConfigurationContext configurationContext) throws SandeshaException {
	  return getOutgoingSequenceReport(internalSequenceID, configurationContext, true);
	}
	
	public static SequenceReport getOutgoingSequenceReport(String internalSequenceID,
			ConfigurationContext configurationContext, boolean createTransaction) throws SandeshaException {

		SequenceReport sequenceReport = new SequenceReport();
		sequenceReport.setSequenceDirection(SequenceReport.SEQUENCE_DIRECTION_OUT);

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
		RMSBeanMgr createSeqMgr = storageManager.getRMSBeanMgr();

		Transaction reportTransaction = null;

		try {
			if (createTransaction)
				reportTransaction = storageManager.getTransaction();

			sequenceReport.setInternalSequenceID(internalSequenceID);

			RMSBean createSeqFindBean = new RMSBean();
			createSeqFindBean.setInternalSequenceID(internalSequenceID);

			RMSBean rMSBean = createSeqMgr.findUnique(createSeqFindBean);

			// if data not is available sequence has to be terminated or
			// timedOut.
			if (rMSBean != null && rMSBean.isTerminated()) {

				// check weather this is an terminated sequence.
				sequenceReport.setSequenceStatus(SequenceReport.SEQUENCE_STATUS_TERMINATED);

				fillOutgoingSequenceInfo(sequenceReport, rMSBean, storageManager);

				return sequenceReport;

			} else if (rMSBean != null && rMSBean.isTimedOut()) {

				sequenceReport.setSequenceStatus(SequenceReport.SEQUENCE_STATUS_TIMED_OUT);
				
				fillOutgoingSequenceInfo(sequenceReport, rMSBean, storageManager);

				return sequenceReport;
				
			} else if (rMSBean == null) {

				// sequence must hv been timed out before establishing. No other
				// posibility I can think of.
				// this does not get recorded since there is no key (which is
				// normally the sequenceID) to store it.

				// so, setting the sequence status to INITIAL
				sequenceReport.setSequenceStatus(SequenceReport.SEQUENCE_STATUS_INITIAL);

				// returning the current sequence report.
				return sequenceReport;
			}

			String outSequenceID = rMSBean.getSequenceID();
			if (outSequenceID == null) {
				sequenceReport.setInternalSequenceID(internalSequenceID);
				sequenceReport.setSequenceStatus(SequenceReport.SEQUENCE_STATUS_INITIAL);
				sequenceReport.setSequenceDirection(SequenceReport.SEQUENCE_DIRECTION_OUT);
				if(rMSBean.getSecurityTokenData() != null) sequenceReport.setSecureSequence(true);

				return sequenceReport;
			}

			sequenceReport.setSequenceStatus(SequenceReport.SEQUENCE_STATUS_ESTABLISHED);
			fillOutgoingSequenceInfo(sequenceReport, rMSBean, storageManager);
			
			if(reportTransaction != null && reportTransaction.isActive()) reportTransaction.commit();
			reportTransaction = null;

		} catch (Exception e) {
			// Just log the exception
			if(log.isDebugEnabled()) log.debug("Exception", e);
		} finally {
			if (reportTransaction!=null && reportTransaction.isActive()) reportTransaction.rollback();
		}

		return sequenceReport;
	}

	private static void fillOutgoingSequenceInfo(SequenceReport report, RMSBean rmsBean,
			StorageManager storageManager) {
		report.setSequenceID(rmsBean.getSequenceID());

		List completedMessageList = rmsBean.getClientCompletedMessages().getContainedElementsAsNumbersList();

		Iterator iter = completedMessageList.iterator();
		while (iter.hasNext()) {
			report.addCompletedMessage((Long)iter.next());
		}
		
		if(rmsBean.getSecurityTokenData() != null) report.setSecureSequence(true);
	}

	/**
	 * Users can get a list of sequenceReports each describing a incoming
	 * sequence, which are the sequences the client work as a RMD.
	 * 
	 * @param configCtx
	 * @return
	 * @throws SandeshaException
	 */
	public static List getIncomingSequenceReports(ConfigurationContext configCtx) throws SandeshaException {

		SandeshaReport report = getSandeshaReport(configCtx);
		List incomingSequenceIDs = report.getIncomingSequenceList();
		Iterator incomingSequenceIDIter = incomingSequenceIDs.iterator();

		ArrayList incomingSequenceReports = new ArrayList();

		while (incomingSequenceIDIter.hasNext()) {
			String sequenceID = (String) incomingSequenceIDIter.next();
			SequenceReport incomingSequenceReport = getIncomingSequenceReport(sequenceID, configCtx);
			if (incomingSequenceReport == null) {
				throw new SandeshaException(SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.incommingSequenceReportNotFound, sequenceID));
			}
			incomingSequenceReports.add(incomingSequenceReport);
		}

		return incomingSequenceReports;
	}

	/**
	 * SandeshaReport gives the details of all incoming and outgoing sequences.
	 * The outgoing sequence have to pass the initial state (CS/CSR exchange) to
	 * be included in a SandeshaReport
	 * 
	 * @param configurationContext
	 * @return
	 * @throws SandeshaException
	 */
	public static SandeshaReport getSandeshaReport(ConfigurationContext configurationContext) throws SandeshaException {

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
		SandeshaReport sandeshaReport = new SandeshaReport();

		Transaction reportTransaction = null;

		try {
			reportTransaction = storageManager.getTransaction();

			List rmsBeans = storageManager.getRMSBeanMgr().find(null);
			Iterator iterator = rmsBeans.iterator();
			while (iterator.hasNext()) {
				RMSBean bean = (RMSBean) iterator.next();
				String sequenceID = bean.getSequenceID();
				sandeshaReport.addToOutgoingSequenceList(sequenceID);
				sandeshaReport.addToOutgoingInternalSequenceMap(sequenceID, bean.getInternalSequenceID());

				SequenceReport report = getOutgoingSequenceReport(bean.getInternalSequenceID(), configurationContext);

				sandeshaReport.addToNoOfCompletedMessagesMap(sequenceID, report.getCompletedMessages().size());
				sandeshaReport.addToSequenceStatusMap(sequenceID, report.getSequenceStatus());
			}

			// incoming sequences
			Collection rmdBeans = storageManager.getRMDBeanMgr().find(null);

			Iterator iter = rmdBeans.iterator();
			while (iter.hasNext()) {
				RMDBean serverCompletedMsgsBean = (RMDBean) iter.next();
				String sequenceID = serverCompletedMsgsBean.getSequenceID();
				sandeshaReport.addToIncomingSequenceList(sequenceID);

				SequenceReport sequenceReport = getIncomingSequenceReport(sequenceID, configurationContext);

				sandeshaReport.addToNoOfCompletedMessagesMap(sequenceID, sequenceReport.getCompletedMessages().size());
				sandeshaReport.addToSequenceStatusMap(sequenceID, sequenceReport.getSequenceStatus());
			}
			
			if(reportTransaction != null && reportTransaction.isActive()) reportTransaction.commit();
			reportTransaction = null;

		} catch (Exception e) {
			// just log the error
			if(log.isDebugEnabled()) log.debug("Exception", e);
		} finally {
			if (reportTransaction!=null && reportTransaction.isActive()) reportTransaction.rollback();
		}

		return sandeshaReport;
	}

	/**
	 * This could be used to create sequences with a given sequence key.
	 * 
	 * @param serviceClient - A configured ServiceClient to be used to invoke RM messages. This need to have Sandesha2 engaged.
	 * @param offer - Weather a sequence should be offered for obtaining response messages.
	 * @param sequenceKey The sequenceKey of the newly generated sequence.
	 * @throws SandeshaException
	 */
	public static void createSequence(ServiceClient serviceClient, boolean offer, String sequenceKey) throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaClient::createSequence , " + offer + ", " + sequenceKey);
		
		setUpServiceClientAnonymousOperations (serviceClient);
		
		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		EndpointReference toEPR = serviceClient.getOptions().getTo();
		if (toEPR == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.toEPRNotValid, null));

		String to = toEPR.getAddress();
		if (to == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.toEPRNotValid, null));

		if (offer) {
			String offeredSequenceID = SandeshaUtil.getUUID();
			options.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID, offeredSequenceID);
		}

		// setting a new squenceKey if not already set.
		String oldSequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);

	  options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, sequenceKey);

		String rmSpecVersion = (String) options.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);

		if (rmSpecVersion == null)
			rmSpecVersion = SpecSpecificConstants.getDefaultSpecVersion();

		//When the message is marked as Dummy the application processor will not actually try to send it. 
		//But still the create Sequence will be added.

		options.setProperty(SandeshaClientConstants.DUMMY_MESSAGE, Sandesha2Constants.VALUE_TRUE);

		String oldAction = options.getAction();
		options.setAction(SpecSpecificConstants.getCreateSequenceAction(rmSpecVersion));
		
		ServiceContext serviceContext = serviceClient.getServiceContext();
		if (serviceContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		ConfigurationContext configurationContext = serviceContext.getConfigurationContext();

		// cleanup previous sequence
		cleanupTerminatedSequence(to, oldSequenceKey, SandeshaUtil.getSandeshaStorageManager(configurationContext, configurationContext.getAxisConfiguration()));
		
		// If the client requested async operations, mint a replyTo EPR
		boolean resetReply = false;
		if(options.isUseSeparateListener() && options.getReplyTo() == null) {
			try {
				if(log.isDebugEnabled()) log.debug("Creating replyTo EPR");
				
				// Try to work out which transport to use. If the user didn't choose one
				// then we use the To address to take a guess.
				TransportOutDescription senderTransport = options.getTransportOut();
				String transportName = null;
				if(senderTransport != null) {
					transportName = senderTransport.getName();
				}

				if(transportName == null) {
					int index = to.indexOf(':');
					if(index > 0) transportName = to.substring(0, index);
				}

				EndpointReference replyTo = serviceContext.getMyEPR(transportName);
				if(replyTo != null) {
					options.setReplyTo(replyTo);
					resetReply = true;
				}
				
				if(log.isDebugEnabled()) log.debug("Created replyTo EPR: " + replyTo);

			} catch(AxisFault e) {
				if(log.isDebugEnabled()) log.debug("Caught exception", e);
				throw new SandeshaException(e);
			}
		}
		try {			
			//just to inform the sender.
			serviceClient.fireAndForget (null);
		} catch (AxisFault e) {
			throw new SandeshaException(e);
		}
		finally {
			options.setAction(oldAction);
			if(resetReply) options.setReplyTo(null);
			
			options.setProperty(SandeshaClientConstants.DUMMY_MESSAGE, Sandesha2Constants.VALUE_FALSE);
			options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, oldSequenceKey);
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaClient::createSequence");
	}

	/**
	 * If a user has requested to create a new sequence which was previously terminated, we need to clean up
	 * any previous properties that might have been stored.
	 * @param to
	 * @param sequenceKey
	 * @throws SandeshaStorageException 
	 */
	private static final void cleanupTerminatedSequence(String to, String sequenceKey, StorageManager storageManager) throws SandeshaException {
		String internalSequenceId = SandeshaUtil.getInternalSequenceID(to, sequenceKey);
		
		if (log.isTraceEnabled())
			log.trace("Checking if sequence " + internalSequenceId + " previously terminated");
		
		Transaction tran = null;
		
		try {
			tran = storageManager.getTransaction();
			
			RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceId);
			//see if the sequence is terminated
			boolean terminatedSequence = false;
			if (rmsBean != null && rmsBean.isTerminated())
				terminatedSequence = true;
	
			//see if the sequence is timed out
			if(rmsBean != null && rmsBean.isTimedOut()){
				terminatedSequence = true;
			}
	
			if (terminatedSequence) {		
				// Delete the rmsBean
				storageManager.getRMSBeanMgr().delete(rmsBean.getCreateSeqMsgID());
			}
			
			if(tran != null && tran.isActive()) tran.commit();
			tran = null;
		
		} finally {
			if(tran!=null && tran.isActive())
				tran.rollback();
		}
	}
	
	/**
	 * Clients can use this to create a sequence sequence.
	 * 
	 * @param serviceClient - A configured ServiceClient to be used to invoke RM messages. This need to have Sandesha2 engaged.
	 * @param offer - Weather a sequence should be offered for obtaining response messages.
	 * @return The sequenceKey of the newly generated sequence.
	 * @throws SandeshaException
	 */
	public static String createSequence(ServiceClient serviceClient, boolean offer)
			throws SandeshaException {

		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));
		
		String newSequenceKey = SandeshaUtil.getUUID();
		createSequence(serviceClient, offer, newSequenceKey);
		
		return newSequenceKey;
	}
	
	/**
	 * User can terminate the sequence defined by the passed serviceClient.
	 * 
	 * @deprecated
	 */
	public static void createSequnce(ServiceClient serviceClient, boolean offer, String sequenceKey)
		throws SandeshaException {
		createSequence(serviceClient,offer,sequenceKey);
	}

	/**
	 * User can terminate the sequence defined by the passed serviceClient.
	 * 
	 * @param serviceClient
	 * @throws SandeshaException
	 */
	public static void terminateSequence(ServiceClient serviceClient) throws SandeshaException {
		
		setUpServiceClientAnonymousOperations (serviceClient);
		
		ServiceContext serviceContext = serviceClient.getServiceContext();
		if (serviceContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		String rmSpecVersion = (String) options.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);

		if (rmSpecVersion == null)
			rmSpecVersion = SpecSpecificConstants.getDefaultSpecVersion();

		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(rmSpecVersion);

		String oldAction = options.getAction();

		//in WSRM 1.0 we are adding another application msg with the LastMessage tag, instead of sending a terminate here.
		//Actual terminate will be sent once all the messages upto this get acked
		
		try {
			if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(rmSpecVersion)) {
				SOAPEnvelope terminateEnvelope = configureTerminateSequence(options, serviceContext
						.getConfigurationContext());
				OMElement terminateBody = terminateEnvelope.getBody().getFirstChildWithName(
						new QName(rmNamespaceValue,
								Sandesha2Constants.WSRM_COMMON.TERMINATE_SEQUENCE));

				// To inform the Sandesha2 out handler. The response to this call needs
				// be routed to the Sandesha Message receiver
				AxisCallback callback = new AxisCallback() {

					public void onMessage(MessageContext msg) {
						try {
							RMMessageReceiver rm = new RMMessageReceiver();
							rm.receive(msg);
						} catch(Exception e) {
							if(log.isErrorEnabled()) log.error("Exception on callback", e);
						}
					}

					public void onFault(MessageContext msg) {
						try {
							RMMessageReceiver rm = new RMMessageReceiver();
							rm.receive(msg);
						} catch(Exception e) {
							if(log.isErrorEnabled()) log.error("Exception on callback", e);
						}
					}

					public void onError(Exception e) {
						if(log.isErrorEnabled()) log.error("Exception on callback", e);
					}

					public void onComplete() {
						// Nothing to do here
					}
					
				};
				serviceClient.sendReceiveNonBlocking(terminateBody, callback);

			} else {
				options.setAction(Sandesha2Constants.SPEC_2005_02.Actions.ACTION_LAST_MESSAGE);
				options.setProperty(SandeshaClientConstants.LAST_MESSAGE, Constants.VALUE_TRUE);
				serviceClient.fireAndForget(null);
			}
			
		} catch (AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.couldNotSendTerminate,
					e.toString());
			throw new SandeshaException(message, e);
		} finally {
			options.setAction(oldAction);
		}
	}

	public static void terminateSequence(ServiceClient serviceClient, String sequenceKey) throws SandeshaException {
		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		String oldSequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, sequenceKey);
		terminateSequence(serviceClient);

		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, oldSequenceKey);
	}

	/**
	 * User can close the sequence defined by the passed serviceClient.
	 * 
	 * @param serviceClient
	 * @throws SandeshaException
	 */
	public static void closeSequence(ServiceClient serviceClient) throws SandeshaException {
		
		setUpServiceClientAnonymousOperations (serviceClient);
		
		ServiceContext serviceContext = serviceClient.getServiceContext();
		if (serviceContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		String rmSpecVersion = (String) options.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);

		if (rmSpecVersion == null)
			rmSpecVersion = SpecSpecificConstants.getDefaultSpecVersion();

		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(rmSpecVersion);

		SOAPEnvelope closeSequnceEnvelope = configureCloseSequence(options, serviceContext.getConfigurationContext());
		OMElement closeSequenceBody = closeSequnceEnvelope.getBody().getFirstChildWithName(
				new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.CLOSE_SEQUENCE));

		String oldAction = options.getAction();
		options.setAction(SpecSpecificConstants.getCloseSequenceAction(rmSpecVersion));
		try {
			//to inform the sandesha2 out handler
			serviceClient.fireAndForget (closeSequenceBody);
		} catch (AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.couldNotSendClose,
					e.toString());
			throw new SandeshaException(message, e);
		} finally {
			options.setAction(oldAction);
		}
	}

	public static void closeSequence(ServiceClient serviceClient, String sequenceKey) throws SandeshaException {

		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		String specVersion = (String) options.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);
		if (!Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.closeSequenceSpecLevel,
					specVersion);
			throw new SandeshaException (message);
		}
		
		String oldSequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, sequenceKey);
		closeSequence(serviceClient);

		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, oldSequenceKey);
	}

	/**
	 * This blocks the system until the messages u have sent hv been completed.
	 * 
	 * @param serviceClient
	 */
	public static void waitUntilSequenceCompleted(ServiceClient serviceClient) throws SandeshaException {
		waitUntilSequenceCompleted(serviceClient, -1);
	}

	public static void waitUntilSequenceCompleted(ServiceClient serviceClient, String sequenceKey)
			throws SandeshaException {
		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		String oldSequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, sequenceKey);
		waitUntilSequenceCompleted(serviceClient);

		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, oldSequenceKey);
	}

	/**
	 * This blocks the system until the messages u have sent hv been completed
	 * or until the given time interval exceeds. (the time is taken in seconds)
	 * 
	 * @param serviceClient
	 * @param maxWaitingTime
	 */
	public static void waitUntilSequenceCompleted(ServiceClient serviceClient, long maxWaitingTime)
			throws SandeshaException {
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaClient::waitUntilSequenceCompleted , " + maxWaitingTime);

		long startTime = System.currentTimeMillis();

		SequenceReport sequenceReport = getOutgoingSequenceReport(serviceClient);
		if (sequenceReport == null) {
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotFindReportForGivenData,
					serviceClient.toString()));
		}

		boolean done = false;
		while (!done) {
			sequenceReport = getOutgoingSequenceReport(serviceClient);
			int status = sequenceReport.getSequenceStatus();
			if (status == SequenceReport.SEQUENCE_STATUS_TERMINATED)
				done = true;
			if (status == SequenceReport.SEQUENCE_STATUS_TIMED_OUT)
				done = true;

			if (!done) {
				long timeNow = System.currentTimeMillis();
				if ((timeNow > (startTime + maxWaitingTime)) && maxWaitingTime != -1)
					done = true;
				else
				{
					// Wait for half a second to stop 100 CPU
					try {
	          Thread.sleep(500);
          } catch (InterruptedException e) {
          	// Ignore the exception
          }
				}
			}
		}
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaClient::waitUntilSequenceCompleted , " + maxWaitingTime);
	}

	public static void waitUntilSequenceCompleted(ServiceClient serviceClient, long maxWaitingTime, String sequenceKey)
			throws SandeshaException {
		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		String oldSequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, sequenceKey);
		waitUntilSequenceCompleted(serviceClient, maxWaitingTime);

		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, oldSequenceKey);
	}

	// gives the out sequenceID if CS/CSR exchange is done. Otherwise a
	// SandeshaException
	public static String getSequenceID(ServiceClient serviceClient) throws SandeshaException {

		String internalSequenceID = getInternalSequenceIdFromServiceClient(serviceClient); 

		ServiceContext serviceContext = serviceClient.getServiceContext();
		if (serviceContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		ConfigurationContext configurationContext = serviceContext.getConfigurationContext();

		SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		if (sequenceReport == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotFindReportForGivenData, serviceClient.toString()));

		if (sequenceReport.getSequenceStatus() != SequenceReport.SEQUENCE_STATUS_ESTABLISHED) {
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noSequenceEstablished,
					internalSequenceID));
		}

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());

		// Get a transaction to retrieve the properties
		Transaction transaction = null;
		String sequenceID = null;
		
		try
		{
			transaction = storageManager.getTransaction();
			sequenceID = SandeshaUtil.getSequenceIDFromInternalSequenceID(internalSequenceID, storageManager);		
		}
		finally
		{
			// Commit the transaction as it was only a retrieve
			if(transaction != null) transaction.commit();
		}
		
		if (sequenceID == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.sequenceIdBeanNotSet));

		return sequenceID;
	}
	
	private static SOAPEnvelope configureAckRequest(Options options, ConfigurationContext configurationContext) 
	
	throws SandeshaException, MissingResourceException {
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		EndpointReference epr = options.getTo();
		if (epr == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.toEPRNotValid, null));

		//first see if the cliet has told us which sequence to terminate
		String internalSequenceID = 
			(String)options.getProperty(SandeshaClientConstants.INTERNAL_SEQUENCE_ID);
		
		if(internalSequenceID==null){
			//lookup the internal seq id based on to EPR and sequenceKey
			String to = epr.getAddress();
			String sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
			internalSequenceID = SandeshaUtil.getInternalSequenceID(to, sequenceKey);
		}
		
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());

		// Get a transaction to obtain sequence information
		Transaction transaction = null;
		String sequenceID = null;
		
		try
		{
			transaction = storageManager.getTransaction();
			sequenceID = SandeshaUtil.getSequenceIDFromInternalSequenceID(internalSequenceID, storageManager);
		}
		finally
		{
			// Commit the tran whatever happened
			if(transaction != null) transaction.commit();
		}
		
		if (sequenceID == null)
			sequenceID = Sandesha2Constants.TEMP_SEQUENCE_ID;	
		
		String rmSpecVersion = (String) options.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);
		if (rmSpecVersion == null)
			rmSpecVersion = SpecSpecificConstants.getDefaultSpecVersion();

		options.setAction(SpecSpecificConstants.getAckRequestAction(rmSpecVersion));
		
		String soapNamespaceURI = options.getSoapVersionURI();
		if (soapNamespaceURI == null) 
			soapNamespaceURI = getSOAPNamespaceURI(storageManager, internalSequenceID);
		
		SOAPFactory factory = null;
		SOAPEnvelope dummyEnvelope = null;
		if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(soapNamespaceURI)) {
			factory = new SOAP12Factory();
			dummyEnvelope = factory.getDefaultEnvelope();
		} else {
			factory = new SOAP11Factory();
			dummyEnvelope = factory.getDefaultEnvelope();
		}

		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(rmSpecVersion);

		AckRequested ackRequested = new AckRequested(rmNamespaceValue);
		Identifier identifier = new Identifier(rmNamespaceValue);
		identifier.setIndentifer(sequenceID);
		ackRequested.setIdentifier(identifier);

		ackRequested.toSOAPEnvelope(dummyEnvelope);

		return dummyEnvelope;
	}

	public static void sendAckRequest(ServiceClient serviceClient) throws SandeshaException {

		setUpServiceClientAnonymousOperations (serviceClient);
		
		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		ServiceContext serviceContext = serviceClient.getServiceContext();
		if (serviceContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		ConfigurationContext configContext = serviceContext.getConfigurationContext();

		String rmSpecVersion = (String) options.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);
		if (rmSpecVersion == null)
			rmSpecVersion = Sandesha2Constants.SPEC_VERSIONS.v1_0;

		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(rmSpecVersion)) {
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.emptyAckRequestSpecLevel, rmSpecVersion));
		}
		
		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(rmSpecVersion);

		SOAPEnvelope dummyEnvelope = configureAckRequest(options, configContext);
		
		OMElement ackRequestedHeaderBlock = dummyEnvelope.getHeader().getFirstChildWithName(
				new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.ACK_REQUESTED));

		String oldAction = options.getAction();

		serviceClient.addHeader(ackRequestedHeaderBlock);

		try {
			//to inform the sandesha2 out handler
			serviceClient.fireAndForget (null);
		} catch (AxisFault e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotSendAckRequestException, e.toString());
			throw new SandeshaException(message, e);
		}

		serviceClient.removeHeaders();
		options.setAction(oldAction);
	}

	public static void sendAckRequest(ServiceClient serviceClient, String sequenceKey) throws SandeshaException {
		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		String oldSequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, sequenceKey);
		sendAckRequest(serviceClient);

		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, oldSequenceKey);
	}

	/**
	 * Forces any inbound messages currently on the specified inOrder inbound sequence to be dispatched out of order.
	 * @param configContext
	 * @param sequenceID
	 * @param allowLaterDeliveryOfMissingMessages if true, messages skipped over during this
	 * action will be invoked if they arrive on the system at a later time. 
	 * Otherwise messages skipped over will be ignored
	 * @throws SandeshaException
	 */
	public static void forceDispatchOfInboundMessages(ConfigurationContext configContext, 
			String sequenceID,
			boolean allowLaterDeliveryOfMissingMessages)throws SandeshaException{

		Transaction reportTransaction = null;

		try {
			StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configContext, configContext.getAxisConfiguration());
			reportTransaction = storageManager.getTransaction();

			//only do this if we are running inOrder
			if(SandeshaUtil.getPropertyBean(configContext.getAxisConfiguration()).isInOrder()){
				Invoker invoker = (Invoker)SandeshaUtil.getSandeshaStorageManager(configContext, configContext.getAxisConfiguration()).getInvoker();
				if (invoker==null){
					throw new SandeshaException(SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.invokerNotFound, sequenceID));
				}
				
				invoker.forceInvokeOfAllMessagesCurrentlyOnSequence(configContext, sequenceID, allowLaterDeliveryOfMissingMessages);			
			}
			
			if(reportTransaction != null && reportTransaction.isActive()) reportTransaction.commit();
			reportTransaction = null;

		} catch (Exception e) {
			// Just log the exception
			if(log.isDebugEnabled()) log.debug("Exception", e);
		} finally {
			if(reportTransaction != null && reportTransaction.isActive()) reportTransaction.rollback();
		}
	}

	private static SOAPEnvelope configureCloseSequence(Options options, ConfigurationContext configurationContext)
			throws SandeshaException {

		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		EndpointReference epr = options.getTo();
		if (epr == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.toEPRNotValid, null));

		//first see if the cliet has told us which sequence to close
		String internalSequenceID = 
			(String)options.getProperty(SandeshaClientConstants.INTERNAL_SEQUENCE_ID);
		
		if(internalSequenceID==null){
			//lookup the internal seq id based on to EPR and sequenceKey
			String to = epr.getAddress();
			String sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
			internalSequenceID = SandeshaUtil.getInternalSequenceID(to, sequenceKey);
		}

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
		
		// Get a transaction for getting the sequence properties
		Transaction transaction = null;
		String sequenceID = null;
		
		try
		{
			transaction = storageManager.getTransaction();
			sequenceID = SandeshaUtil.getSequenceIDFromInternalSequenceID(internalSequenceID, storageManager);
		}
		finally
		{
			// Commit the tran whatever happened
			if(transaction != null) transaction.commit();
		}
		
		if (sequenceID == null)
			sequenceID = Sandesha2Constants.TEMP_SEQUENCE_ID;	

		String rmSpecVersion = (String) options.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);

		if (rmSpecVersion == null)
			rmSpecVersion = SpecSpecificConstants.getDefaultSpecVersion();

		if (!SpecSpecificConstants.isSequenceClosingAllowed(rmSpecVersion))
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.closeSequenceSpecLevel, rmSpecVersion));

		SOAPEnvelope dummyEnvelope = null;
		SOAPFactory factory = null;
		String soapNamespaceURI = options.getSoapVersionURI();
		if (soapNamespaceURI == null) 
			soapNamespaceURI = getSOAPNamespaceURI(storageManager, internalSequenceID);

		if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(soapNamespaceURI)) {
			factory = new SOAP12Factory();
			dummyEnvelope = factory.getDefaultEnvelope();
		} else {
			factory = new SOAP11Factory();
			dummyEnvelope = factory.getDefaultEnvelope();
		}

		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(rmSpecVersion);

		CloseSequence closeSequence = new CloseSequence(rmNamespaceValue);
		Identifier identifier = new Identifier(rmNamespaceValue);
		identifier.setIndentifer(sequenceID);
		closeSequence.setIdentifier(identifier);

		closeSequence.toSOAPEnvelope(dummyEnvelope);

		return dummyEnvelope;
	}

	private static byte getServerSequenceStatus(String sequenceID, StorageManager storageManager)
			throws SandeshaException {

		RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceID);
		if (rmdBean != null && rmdBean.isTerminated()) {
			return SequenceReport.SEQUENCE_STATUS_TERMINATED;
		}

/*	Only outbound sequences time out	
    SequencePropertyBean timedOutBean = seqPropMgr.retrieve(sequenceID,
				Sandesha2Constants.SequenceProperties.SEQUENCE_TIMED_OUT);
		if (timedOutBean != null) {
			return SequenceReport.SEQUENCE_STATUS_TIMED_OUT;
		}
*/
		if (rmdBean != null) {
			return SequenceReport.SEQUENCE_STATUS_ESTABLISHED;
		}

		throw new SandeshaException(SandeshaMessageHelper.getMessage(
				SandeshaMessageKeys.cannotFindSequence, sequenceID
				));
	}

	public static SequenceReport getIncomingSequenceReport(String sequenceID, ConfigurationContext configCtx)
			throws SandeshaException {

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx,configCtx.getAxisConfiguration());

		Transaction reportTransaction = null;

		try {
			reportTransaction = storageManager.getTransaction();

			SequenceReport sequenceReport = new SequenceReport();

			RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceID);

			List completedMessageList = rmdBean.getServerCompletedMessages().getContainedElementsAsNumbersList();
			
			Iterator iter = completedMessageList.iterator();
			while (iter.hasNext()) {
				sequenceReport.addCompletedMessage((Long) iter.next());
			}

			sequenceReport.setSequenceID(sequenceID);
			sequenceReport.setInternalSequenceID(sequenceID); // for the
																// incoming side
																// internalSequenceID=sequenceID
			sequenceReport.setSequenceDirection(SequenceReport.SEQUENCE_DIRECTION_IN);

			sequenceReport.setSequenceStatus(getServerSequenceStatus(sequenceID, storageManager));

			if(rmdBean.getSecurityTokenData() != null) sequenceReport.setSecureSequence(true);
			
			if (reportTransaction!=null && reportTransaction.isActive()) reportTransaction.commit();
			reportTransaction = null;

			return sequenceReport;

		} catch (Exception e) {
			// Just log the exception
			if(log.isDebugEnabled()) log.debug("Exception", e);
		} finally {
			if(reportTransaction != null && reportTransaction.isActive()) {
				reportTransaction.rollback();
			}
		}

		return null;
	}

	private static SOAPEnvelope configureTerminateSequence(Options options, ConfigurationContext configurationContext)
			throws SandeshaException {

		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		EndpointReference epr = options.getTo();
		if (epr == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.toEPRNotValid, null));

		//first see if the cliet has told us which sequence to terminate
		String internalSequenceID = 
			(String)options.getProperty(SandeshaClientConstants.INTERNAL_SEQUENCE_ID);
		
		if(internalSequenceID==null){
			//lookup the internal seq id based on to EPR and sequenceKey
			String to = epr.getAddress();
			String sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
			internalSequenceID = SandeshaUtil.getInternalSequenceID(to, sequenceKey);
		}
		
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());

		// Get a transaction to obtain sequence information
		Transaction transaction = null;
		String sequenceID = null;
		
		try
		{
			transaction = storageManager.getTransaction();
			sequenceID = SandeshaUtil.getSequenceIDFromInternalSequenceID(internalSequenceID, storageManager);
		}
		finally
		{
			// Commit the tran whatever happened
			if(transaction != null) transaction.commit();
		}
		
		if (sequenceID == null)
			sequenceID = Sandesha2Constants.TEMP_SEQUENCE_ID;	
		
		String rmSpecVersion = (String) options.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);
		if (rmSpecVersion == null)
			rmSpecVersion = SpecSpecificConstants.getDefaultSpecVersion();

		options.setAction(SpecSpecificConstants.getTerminateSequenceAction(rmSpecVersion));
		SOAPEnvelope dummyEnvelope = null;
		SOAPFactory factory = null;
		String soapNamespaceURI = options.getSoapVersionURI();
		if (soapNamespaceURI == null) 
			soapNamespaceURI = getSOAPNamespaceURI(storageManager, internalSequenceID);
		if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(soapNamespaceURI)) {
			factory = new SOAP12Factory();
			dummyEnvelope = factory.getDefaultEnvelope();
		} else {
			factory = new SOAP11Factory();
			dummyEnvelope = factory.getDefaultEnvelope();
		}

		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(rmSpecVersion);
		TerminateSequence terminateSequence = new TerminateSequence(rmNamespaceValue);
		Identifier identifier = new Identifier(rmNamespaceValue);
		identifier.setIndentifer(sequenceID);
		terminateSequence.setIdentifier(identifier);
		terminateSequence.toSOAPEnvelope(dummyEnvelope);

		return dummyEnvelope;
	}
	
	
//	private static SOAPEnvelope configureCreateSequence(Options options,
//			ConfigurationContext configurationContext) throws AxisFault {
//
//		if (options == null)
//			throw new SandeshaException(SandeshaMessageHelper
//					.getMessage(SandeshaMessageKeys.optionsObjectNotSet));
//
//		EndpointReference epr = options.getTo();
//		if (epr == null)
//			throw new SandeshaException(SandeshaMessageHelper.getMessage(
//					SandeshaMessageKeys.toEPRNotValid, null));
//
//
//		String rmSpecVersion = (String) options
//				.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);
//		if (rmSpecVersion == null)
//			rmSpecVersion = SpecSpecificConstants.getDefaultSpecVersion();
//
//		options.setAction(SpecSpecificConstants
//				.getCreateSequenceAction (rmSpecVersion));
//
//		SOAPEnvelope dummyEnvelope = null;
//		SOAPFactory factory = null;
//		String soapNamespaceURI = options.getSoapVersionURI();
//		if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI
//				.equals(soapNamespaceURI)) {
//			factory = new SOAP12Factory();
//			dummyEnvelope = factory.getDefaultEnvelope();
//		} else {
//			factory = new SOAP11Factory();
//			dummyEnvelope = factory.getDefaultEnvelope();
//		}
//
//		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(rmSpecVersion);
//
//		String addressingNamespaceValue = (String) options.getProperty(AddressingConstants.WS_ADDRESSING_VERSION);
//		if (addressingNamespaceValue==null)
//			addressingNamespaceValue = SpecSpecificConstants.getDefaultAddressingNamespace ();
//		
//
//		CreateSequence createSequence = new CreateSequence (rmNamespaceValue,addressingNamespaceValue);
//		AcksTo acksTo = new AcksTo (rmNamespaceValue,addressingNamespaceValue);
//		createSequence.setAcksTo(acksTo);
//		EndpointReference endpointReference = new EndpointReference (null);
//		acksTo.setAddress(endpointReference);
//		
//		createSequence.toSOAPEnvelope(dummyEnvelope);
//
//		return dummyEnvelope;
//	}
	
	
	/**
	 * Sandesha uses default 'fireAndForget' and 'sendReceive' methods to send control messages.
	 * But these can only be called when Anonymous operations are present within the passed ServiceClient.
	 * But these could be situations where these Anonymous operations are not present. In such cases Sandesha2
	 * will try to add them into the serviceClient. 
	 */
	private static void setUpServiceClientAnonymousOperations (ServiceClient serviceClient) throws SandeshaException {
		try {
			
			AxisService service = serviceClient.getAxisService();

			AxisOperation anonOutOnlyOperation = service.getOperation(ServiceClient.ANON_OUT_ONLY_OP);
			
			if (anonOutOnlyOperation==null) {
				anonOutOnlyOperation = AxisOperationFactory.getAxisOperation(WSDLConstants.MEP_CONSTANT_OUT_ONLY);
				anonOutOnlyOperation.setName(ServiceClient.ANON_OUT_ONLY_OP);
				
				AxisOperation referenceOperation = service.getOperation(Sandesha2Constants.RM_IN_ONLY_OPERATION);
				
				if (referenceOperation!=null) {
					anonOutOnlyOperation.setPhasesOutFlow(referenceOperation.getPhasesOutFlow());
					anonOutOnlyOperation.setPhasesOutFaultFlow(referenceOperation.getPhasesOutFaultFlow());
					anonOutOnlyOperation.setPhasesInFaultFlow(referenceOperation.getPhasesInFaultFlow());
					anonOutOnlyOperation.setPhasesInFaultFlow(referenceOperation.getRemainingPhasesInFlow());

					service.addOperation(anonOutOnlyOperation);
				} else {
					String message = "Cant find RM Operations. Please engage the Sandesha2 module before doing the invocation.";
					throw new SandeshaException (message);
				}
			}

			AxisOperation anonOutInOperation = service.getOperation(ServiceClient.ANON_OUT_IN_OP);
			
			if (anonOutInOperation==null) {
				anonOutInOperation = AxisOperationFactory.getAxisOperation(WSDLConstants.MEP_CONSTANT_OUT_IN);
				anonOutInOperation.setName(ServiceClient.ANON_OUT_IN_OP);
				
				AxisOperation referenceOperation = service.getOperation(Sandesha2Constants.RM_IN_OUT_OPERATION);
				
				if (referenceOperation!=null) {
					anonOutInOperation.setPhasesOutFlow(referenceOperation.getPhasesOutFlow());
					anonOutInOperation.setPhasesOutFaultFlow(referenceOperation.getPhasesOutFaultFlow());
					anonOutInOperation.setPhasesInFaultFlow(referenceOperation.getPhasesInFaultFlow());
					anonOutInOperation.setPhasesInFaultFlow(referenceOperation.getRemainingPhasesInFlow());
					
					//operation will be added to the service only if a valid referenceOperation was found.
					service.addOperation(anonOutInOperation);
				}
			}
		} catch (AxisFault e) {
			throw new SandeshaException (e);
		}
	}

	/**
	 * Gets the last error that occured on a send to an endpoint.
	 * 
	 * The method will return null if no exception has been encountered.
	 * Errors may be transient and maybe out of date.  To check the validity of the
	 * error, check the timestamp from which the error was encountered 
	 * @see getLastSendTimestamp .
	 * 
	 * @param serviceClient
	 * @return
	 * @throws SandeshaException
	 */
	public static Exception getLastSendError(ServiceClient serviceClient) 
	
	throws SandeshaException
	{
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaClient::getLastSendError");

		// Get the internal sequence id for this client
		String internalSequenceId = getInternalSequenceIdFromServiceClient(serviceClient);
		
		if (log.isTraceEnabled())
			log.trace("Looking up sequence with identifier " + internalSequenceId);
		
		ServiceContext serviceContext = serviceClient.getServiceContext();
		if (serviceContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		ConfigurationContext configurationContext = serviceContext.getConfigurationContext();

		// Get the in use storage manager and the sequence property bean manager
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
		
		Transaction transaction = null;
		Exception resultException = null;
    
		try 
		{
			transaction = storageManager.getTransaction();
			RMSBean bean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceId);
			
			if (bean != null) {						
				resultException = bean.getLastSendError();
			}
		}
		finally
		{
			// Commit the tran whatever happened
			if(transaction != null) transaction.commit();
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaClient::getLastSendError, " + resultException);
		
		return resultException;
	}
	
	/**
	 * Gets the timestamp from which the last error that occured on a send to an endpoint.
	 * 
	 * The method will return -1 if no errors have been encountered.
	 * 
	 * @param serviceClient
	 * @return
	 * @throws SandeshaException
	 */
	public static long getLastSendErrorTimestamp(ServiceClient serviceClient)
	
	throws SandeshaException
	{
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaClient::getLastSendErrorTimestamp");

		// Get the internal sequence id for this client
		String internalSequenceId = getInternalSequenceIdFromServiceClient(serviceClient);
		
		if (log.isTraceEnabled())
			log.trace("Looking up sequence with identifier " + internalSequenceId);
		
		ServiceContext serviceContext = serviceClient.getServiceContext();
		if (serviceContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		ConfigurationContext configurationContext = serviceContext.getConfigurationContext();

		// Get the in use storage manager and the sequence property bean manager
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
		
		// Create a transaction for the retrieve operation
		Transaction transaction = null;
		long resultTime = -1;

		try
		{
			transaction = storageManager.getTransaction();
			
			RMSBean bean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceId);
			
			if (bean != null) {						
				resultTime = bean.getLastSendErrorTimestamp();
			}
		}
		finally
		{
			// commit the transaction as it was only a retrieve
			if(transaction != null) transaction.commit();
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaClient::getLastSendErrorTimestamp, " + resultTime);
		
		return resultTime;

	}

	/**
	 * Gets the internal sequence id from the service client instance.
	 * 
	 * @param serviceClient
	 * @return
	 * @throws SandeshaException
	 */
	private static String getInternalSequenceIdFromServiceClient(ServiceClient serviceClient) throws SandeshaException
	{
		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		EndpointReference toEPR = options.getTo();
		if (toEPR == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.toEPRNotValid, null));

		String to = toEPR.getAddress();
		String sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);

		String internalSequenceID = SandeshaUtil.getInternalSequenceID(to, sequenceKey);

		return internalSequenceID;
	}

	private static final String getSOAPNamespaceURI(StorageManager storageManager, String internalSequenceID) throws SandeshaException {
		String soapNamespaceURI = null;
		
		// Get the RMSBean for this sequence.
		Transaction transaction = null;
		
		try {
			transaction = storageManager.getTransaction();
			RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceID);
			if (rmsBean.getSoapVersion() == Sandesha2Constants.SOAPVersion.v1_2)
				soapNamespaceURI = SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI;
		} finally {
			if(transaction != null) transaction.commit();
		}
		
		return soapNamespaceURI;
	}
	
	public static void setPolicyBean (ServiceClient serviceClient, SandeshaPolicyBean policyBean) throws SandeshaException {
		try {
			AxisService axisService = serviceClient.getAxisService();
			if (axisService!=null) {
				Parameter parameter = axisService.getParameter(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
				SandeshaPolicyBean parent = null;
				if (parameter==null) {
					parameter = new Parameter ();
					parameter.setName(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
				} else {
					parameter.setEditable(true); //if we don't do it here, Axis2 will not allow us to override the parameter value.
					parent = (SandeshaPolicyBean) parameter.getValue();
					policyBean.setParent(parent);
				}
				
				parameter.setValue(policyBean);
				axisService.addParameter(parameter);
			} else {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotSetPolicyBeanServiceNull);
				throw new SandeshaException (message);
			}
		} catch (AxisFault e) {
			throw new SandeshaException (e);
		}
	}
	
}
