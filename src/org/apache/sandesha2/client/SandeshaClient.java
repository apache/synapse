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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axiom.soap.impl.llom.soap12.SOAP12Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.async.AsyncResult;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisOperationFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.wsdl.WSDLConstants.WSDL20_2004Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.CreateSeqBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.NextMsgBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.CreateSeqBean;
import org.apache.sandesha2.storage.beans.NextMsgBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.AcknowledgementManager;
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

		SequenceReport sequenceReport = new SequenceReport();
		sequenceReport.setSequenceDirection(SequenceReport.SEQUENCE_DIRECTION_OUT);

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
		CreateSeqBeanMgr createSeqMgr = storageManager.getCreateSeqBeanMgr();

		String withinTransactionStr = (String) configurationContext.getProperty(Sandesha2Constants.WITHIN_TRANSACTION);
		boolean withinTransaction = false;
		if (withinTransactionStr != null && Sandesha2Constants.VALUE_TRUE.equals(withinTransactionStr))
			withinTransaction = true;

		Transaction reportTransaction = null;
		if (!withinTransaction) {
			reportTransaction = storageManager.getTransaction();
			configurationContext.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_TRUE);
		}

		boolean rolebacked = false;

		try {

			sequenceReport.setInternalSequenceID(internalSequenceID);

			CreateSeqBean createSeqFindBean = new CreateSeqBean();
			createSeqFindBean.setInternalSequenceID(internalSequenceID);

			CreateSeqBean createSeqBean = createSeqMgr.findUnique(createSeqFindBean);

			// if data not is available sequence has to be terminated or
			// timedOut.
			if (createSeqBean == null) {

				// check weather this is an terminated sequence.
				if (isSequenceTerminated(internalSequenceID, seqPropMgr)) {
					fillTerminatedOutgoingSequenceInfo(sequenceReport, internalSequenceID, seqPropMgr);

					return sequenceReport;
				}

				if (isSequenceTimedout(internalSequenceID, seqPropMgr)) {
					fillTimedoutOutgoingSequenceInfo(sequenceReport, internalSequenceID, seqPropMgr);

					return sequenceReport;
				}

				// sequence must hv been timed out before establiching. No other
				// posibility I can think of.
				// this does not get recorded since there is no key (which is
				// normally the sequenceID) to store it.
				// (properties with key as the internalSequenceID get deleted in
				// timing out)

				// so, setting the sequence status to INITIAL
				sequenceReport.setSequenceStatus(SequenceReport.SEQUENCE_STATUS_INITIAL);

				// returning the current sequence report.
				return sequenceReport;
			}

			String outSequenceID = createSeqBean.getSequenceID();
			if (outSequenceID == null) {
				sequenceReport.setInternalSequenceID(internalSequenceID);
				sequenceReport.setSequenceStatus(SequenceReport.SEQUENCE_STATUS_INITIAL);
				sequenceReport.setSequenceDirection(SequenceReport.SEQUENCE_DIRECTION_OUT);
				if(createSeqBean.getSecurityTokenData() != null) sequenceReport.setSecureSequence(true);

				return sequenceReport;
			}

			sequenceReport.setSequenceStatus(SequenceReport.SEQUENCE_STATUS_ESTABLISHED);
			fillOutgoingSequenceInfo(sequenceReport, outSequenceID, seqPropMgr);

		} catch (Exception e) {
			if (!withinTransaction && reportTransaction!=null) {
				reportTransaction.rollback();
				configurationContext.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);
				rolebacked = true;
			}
		} finally {
			if (!withinTransaction && !rolebacked && reportTransaction!=null) {
				reportTransaction.commit();
				configurationContext.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);
			}
		}

		return sequenceReport;
	}

	/**
	 * Users can get a list of sequenceReports each describing a incoming
	 * sequence, which are the sequences the client work as a RMD.
	 * 
	 * @param configCtx
	 * @return
	 * @throws SandeshaException
	 */
	public static ArrayList getIncomingSequenceReports(ConfigurationContext configCtx) throws SandeshaException {

		SandeshaReport report = getSandeshaReport(configCtx);
		ArrayList incomingSequenceIDs = report.getIncomingSequenceList();
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
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
		SandeshaReport sandeshaReport = new SandeshaReport();
		SequencePropertyBean internalSequenceFindBean = new SequencePropertyBean();

		String withinTransactionStr = (String) configurationContext.getProperty(Sandesha2Constants.WITHIN_TRANSACTION);
		boolean withinTransaction = false;
		if (withinTransactionStr != null && Sandesha2Constants.VALUE_TRUE.equals(withinTransactionStr))
			withinTransaction = true;

		Transaction reportTransaction = null;
		if (!withinTransaction) {
			reportTransaction = storageManager.getTransaction();
			configurationContext.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_TRUE);
		}

		boolean rolebacked = false;

		try {

			internalSequenceFindBean.setName(Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID);
			Collection collection = seqPropMgr.find(internalSequenceFindBean);
			Iterator iterator = collection.iterator();
			while (iterator.hasNext()) {
				SequencePropertyBean bean = (SequencePropertyBean) iterator.next();
				String sequenceID = bean.getSequencePropertyKey();
				sandeshaReport.addToOutgoingSequenceList(sequenceID);
				sandeshaReport.addToOutgoingInternalSequenceMap(sequenceID, bean.getValue());

				SequenceReport report = getOutgoingSequenceReport(bean.getValue(), configurationContext);

				sandeshaReport.addToNoOfCompletedMessagesMap(sequenceID, report.getCompletedMessages().size());
				sandeshaReport.addToSequenceStatusMap(sequenceID, report.getSequenceStatus());
			}

			// incoming sequences
			SequencePropertyBean serverCompletedMsgsFindBean = new SequencePropertyBean();
			serverCompletedMsgsFindBean.setName(Sandesha2Constants.SequenceProperties.SERVER_COMPLETED_MESSAGES);

			Collection serverCompletedMsgsBeans = seqPropMgr.find(serverCompletedMsgsFindBean);
			Iterator iter = serverCompletedMsgsBeans.iterator();
			while (iter.hasNext()) {
				SequencePropertyBean serverCompletedMsgsBean = (SequencePropertyBean) iter.next();
				String sequenceID = serverCompletedMsgsBean.getSequencePropertyKey();
				sandeshaReport.addToIncomingSequenceList(sequenceID);

				SequenceReport sequenceReport = getIncomingSequenceReport(sequenceID, configurationContext);

				sandeshaReport.addToNoOfCompletedMessagesMap(sequenceID, sequenceReport.getCompletedMessages().size());
				sandeshaReport.addToSequenceStatusMap(sequenceID, sequenceReport.getSequenceStatus());
			}

		} catch (Exception e) {
			if (!withinTransaction && reportTransaction!=null) {
				reportTransaction.rollback();
				rolebacked = true;
			}
		} finally {
			if (!withinTransaction && !rolebacked && reportTransaction!=null) {
				reportTransaction.commit();
			}
		}

		return sandeshaReport;
	}

	/**
	 * Clients can use this to create a sequence sequence.
	 * 
	 * @param serviceClient - A configured ServiceClient to be used to invoke RM messages. This need to have Sandesha2 engaged.
	 * @param offer - Weather a sequence should be offered for obtaining response messages.
	 * @return The sequenceKey of the newly generated sequence.
	 * @throws SandeshaException
	 */
	public static String createSequence(ServiceClient serviceClient, boolean offer) throws SandeshaException {
		
		// Generate a new sequence key to be used for creating the sequence
		String	newSequenceKey = SandeshaUtil.getUUID();
		
		// Create the sequence
		createSequence(serviceClient, offer, newSequenceKey);
		
		// Return the new sequence key
		return newSequenceKey;
	}

	/**
	 * Creates a sequence with the specified sequenceKey.
	 * 
	 * @param serviceClient
	 * @param offer
	 * @param sequenceKey
	 * @throws SandeshaException
	 */
	public static void createSequence(ServiceClient serviceClient, boolean offer, String sequenceKey)
			throws SandeshaException {
		
		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaClient::createSequence " + offer + ", " + sequenceKey);
		
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
		
		try {			
			//just to inform the sender.
			serviceClient.fireAndForget (null);
		} catch (AxisFault e) {
			throw new SandeshaException(e);
		}

		options.setAction(oldAction);
		
		options.setProperty(SandeshaClientConstants.DUMMY_MESSAGE, Sandesha2Constants.VALUE_FALSE);
		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, oldSequenceKey);
		
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaClient::createSequence");
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

		SOAPEnvelope terminateEnvelope = configureTerminateSequence(options, serviceContext.getConfigurationContext());
		OMElement terminateBody = terminateEnvelope.getBody().getFirstChildWithName(
				new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.TERMINATE_SEQUENCE));

		String oldAction = options.getAction();
		options.setAction(SpecSpecificConstants.getTerminateSequenceAction(rmSpecVersion));

		try {
			//to inform the Sandesha2 out handler.
			serviceClient.fireAndForget (terminateBody);				
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

			if (maxWaitingTime >= 0) {
				long timeNow = System.currentTimeMillis();
				if (timeNow > (startTime + maxWaitingTime))
					done = true;
			}
		}
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
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		// Get a transaction to retrieve the properties
		Transaction transaction = storageManager.getTransaction();
		SequencePropertyBean sequenceIDBean = null;
		
		try
		{			
			sequenceIDBean = seqPropMgr.retrieve(internalSequenceID,
					Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID);		
		}
		finally
		{
			// Commit the transaction as it was only a retrieve
			transaction.commit();
		}
		
		if (sequenceIDBean == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.sequenceIdBeanNotSet));

		String sequenceID = sequenceIDBean.getValue();
		return sequenceID;
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

		String internalSequenceID = getInternalSequenceIdFromServiceClient(serviceClient);

		SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(internalSequenceID, configContext);
		if (sequenceReport == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotGenerateReport, internalSequenceID));
		if (sequenceReport.getSequenceStatus() != SequenceReport.SEQUENCE_STATUS_ESTABLISHED)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotSendAckRequestNotActive, internalSequenceID));

		String outSequenceID = getSequenceID(serviceClient);

		String soapNamespaceURI = options.getSoapVersionURI();
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
		identifier.setIndentifer(outSequenceID);
		ackRequested.setIdentifier(identifier);

		ackRequested.toSOAPEnvelope(dummyEnvelope);

		OMElement ackRequestedHeaderBlock = dummyEnvelope.getHeader().getFirstChildWithName(
				new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.ACK_REQUESTED));

		String oldAction = options.getAction();

		options.setAction(SpecSpecificConstants.getAckRequestAction(rmSpecVersion));

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
		//only do this if we are running inOrder
		if(SandeshaUtil.getPropertyBean(configContext.getAxisConfiguration()).isInOrder()){
			Invoker invoker = (Invoker) configContext.getProperty(Sandesha2Constants.INVOKER);
			if (invoker==null){
				throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.invokerNotFound, sequenceID));
			}
			
			invoker.forceInvokeOfAllMessagesCurrentlyOnSequence(configContext, sequenceID, allowLaterDeliveryOfMissingMessages);			
		}
	}
	
	private static String getInternalSequenceID(String to, String sequenceKey) {
		return SandeshaUtil.getInternalSequenceID(to, sequenceKey);
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

		String to = epr.getAddress();
		String sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);

		String internalSequenceID = SandeshaUtil.getInternalSequenceID(to, sequenceKey);

		SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(internalSequenceID,
				configurationContext);
		if (sequenceReport == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotGenerateReport, internalSequenceID));
		if (sequenceReport.getSequenceStatus() != SequenceReport.SEQUENCE_STATUS_ESTABLISHED)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotCloseSequenceNotActive, internalSequenceID));

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());
		
		// Get a transaction for getting the sequence properties
		Transaction transaction = storageManager.getTransaction();

		SequencePropertyBean sequenceIDBean = null;
		
		try
		{
			SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
			sequenceIDBean = seqPropMgr.retrieve(internalSequenceID,
					Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID);
			if (sequenceIDBean == null)
				throw new SandeshaException(SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.sequenceIdBeanNotSet));
		}
		finally
		{
			transaction.commit();
		}
		
		String sequenceID = sequenceIDBean.getValue();

		if (sequenceID == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotFindSequenceID, sequenceIDBean.toString()));

		String rmSpecVersion = (String) options.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);

		if (rmSpecVersion == null)
			rmSpecVersion = SpecSpecificConstants.getDefaultSpecVersion();

		if (!SpecSpecificConstants.isSequenceClosingAllowed(rmSpecVersion))
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.closeSequenceSpecLevel, rmSpecVersion));

		SOAPEnvelope dummyEnvelope = null;
		SOAPFactory factory = null;
		String soapNamespaceURI = options.getSoapVersionURI();
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

	private static boolean isSequenceTerminated(String internalSequenceID, SequencePropertyBeanMgr seqPropMgr)
			throws SandeshaException {
		SequencePropertyBean internalSequenceFindBean = new SequencePropertyBean();
		internalSequenceFindBean.setValue(internalSequenceID);
		internalSequenceFindBean.setName(Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID);

		SequencePropertyBean internalSequenceBean = seqPropMgr.findUnique(internalSequenceFindBean);
		if (internalSequenceBean == null) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.internalSeqBeanNotAvailableOnSequence, internalSequenceID);
			log.debug(message);

			return false;
		}

		String outSequenceID = internalSequenceBean.getSequencePropertyKey();

		SequencePropertyBean sequenceTerminatedBean = seqPropMgr.retrieve(outSequenceID,
				Sandesha2Constants.SequenceProperties.SEQUENCE_TERMINATED);
		if (sequenceTerminatedBean != null && Sandesha2Constants.VALUE_TRUE.equals(sequenceTerminatedBean.getValue())) {
			return true;
		}

		return false;
	}

	private static boolean isSequenceTimedout(String internalSequenceID, SequencePropertyBeanMgr seqPropMgr)
			throws SandeshaException {
		SequencePropertyBean internalSequenceFindBean = new SequencePropertyBean();
		internalSequenceFindBean.setValue(internalSequenceID);
		internalSequenceFindBean.setName(Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID);

		SequencePropertyBean internalSequenceBean = seqPropMgr.findUnique(internalSequenceFindBean);
		if (internalSequenceBean == null) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.internalSeqBeanNotAvailableOnSequence, internalSequenceID);
			log.debug(message);

			return false;
		}

		String outSequenceID = internalSequenceBean.getSequencePropertyKey();
		SequencePropertyBean sequenceTerminatedBean = seqPropMgr.retrieve(outSequenceID,
				Sandesha2Constants.SequenceProperties.SEQUENCE_TIMED_OUT);
		if (sequenceTerminatedBean != null && Sandesha2Constants.VALUE_TRUE.equals(sequenceTerminatedBean.getValue())) {
			return true;
		}

		return false;
	}

	private static void fillTerminatedOutgoingSequenceInfo(SequenceReport report, String internalSequenceID,
			SequencePropertyBeanMgr seqPropMgr) throws SandeshaException {
		SequencePropertyBean internalSequenceFindBean = new SequencePropertyBean();
		internalSequenceFindBean.setValue(internalSequenceID);
		internalSequenceFindBean.setName(Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID);

		SequencePropertyBean internalSequenceBean = seqPropMgr.findUnique(internalSequenceFindBean);
		if (internalSequenceBean == null) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.notValidTerminate, internalSequenceID);
			log.debug(message);

			throw new SandeshaException(message);
		}

		report.setSequenceStatus(SequenceReport.SEQUENCE_STATUS_TERMINATED);

		String outSequenceID = internalSequenceBean.getSequencePropertyKey();
		fillOutgoingSequenceInfo(report, outSequenceID, seqPropMgr);
	}

	private static void fillTimedoutOutgoingSequenceInfo(SequenceReport report, String internalSequenceID,
			SequencePropertyBeanMgr seqPropMgr) throws SandeshaException {
		SequencePropertyBean internalSequenceFindBean = new SequencePropertyBean();
		internalSequenceFindBean.setValue(internalSequenceID);
		internalSequenceFindBean.setName(Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID);

		SequencePropertyBean internalSequenceBean = seqPropMgr.findUnique(internalSequenceFindBean);
		if (internalSequenceBean == null) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.notValidTimeOut, internalSequenceID);
			log.debug(message);

			throw new SandeshaException(message);
		}

		report.setSequenceStatus(SequenceReport.SEQUENCE_STATUS_TIMED_OUT);
		String outSequenceID = internalSequenceBean.getSequencePropertyKey();
		fillOutgoingSequenceInfo(report, outSequenceID, seqPropMgr);
	}

	private static void fillOutgoingSequenceInfo(SequenceReport report, String outSequenceID,
			SequencePropertyBeanMgr seqPropMgr) throws SandeshaException {
		report.setSequenceID(outSequenceID);

		ArrayList completedMessageList = AcknowledgementManager.getClientCompletedMessagesList(outSequenceID,
				seqPropMgr);

		Iterator iter = completedMessageList.iterator();
		while (iter.hasNext()) {
			Long lng = new Long(Long.parseLong((String) iter.next()));
			report.addCompletedMessage(lng);
		}
		
		SequencePropertyBean tokenBean = seqPropMgr.retrieve(outSequenceID, Sandesha2Constants.SequenceProperties.SECURITY_TOKEN);
		if(tokenBean != null) report.setSecureSequence(true);
	}

	private static byte getServerSequenceStatus(String sequenceID, StorageManager storageManager)
			throws SandeshaException {

		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		SequencePropertyBean terminatedBean = seqPropMgr.retrieve(sequenceID,
				Sandesha2Constants.SequenceProperties.SEQUENCE_TERMINATED);
		if (terminatedBean != null) {
			return SequenceReport.SEQUENCE_STATUS_TERMINATED;
		}

		SequencePropertyBean timedOutBean = seqPropMgr.retrieve(sequenceID,
				Sandesha2Constants.SequenceProperties.SEQUENCE_TIMED_OUT);
		if (timedOutBean != null) {
			return SequenceReport.SEQUENCE_STATUS_TIMED_OUT;
		}

		NextMsgBeanMgr nextMsgMgr = storageManager.getNextMsgBeanMgr();
		NextMsgBean nextMsgBean = nextMsgMgr.retrieve(sequenceID);

		if (nextMsgBean != null) {
			return SequenceReport.SEQUENCE_STATUS_ESTABLISHED;
		}

		throw new SandeshaException(SandeshaMessageHelper.getMessage(
				SandeshaMessageKeys.cannotFindSequence, sequenceID
				));
	}

	private class DummyCallback extends Callback {

		public void onComplete(AsyncResult result) {
			// TODO Auto-generated method stub
			System.out.println(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.dummyCallback));
		}

		public void onError(Exception e) {
			// TODO Auto-generated method stub
			System.out.println(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.dummyCallbackError));

		}

	}

	public static SequenceReport getIncomingSequenceReport(String sequenceID, ConfigurationContext configCtx)
			throws SandeshaException {

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx,configCtx.getAxisConfiguration());
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();

		String withinTransactionStr = (String) configCtx.getProperty(Sandesha2Constants.WITHIN_TRANSACTION);
		boolean withinTransaction = false;
		if (withinTransactionStr != null && Sandesha2Constants.VALUE_TRUE.equals(withinTransactionStr))
			withinTransaction = true;

		Transaction reportTransaction = null;
		if (!withinTransaction) {
			reportTransaction = storageManager.getTransaction();
			configCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_TRUE);
		}
		
		boolean rolebacked = false;

		try {

			SequenceReport sequenceReport = new SequenceReport();

			ArrayList completedMessageList = AcknowledgementManager.getServerCompletedMessagesList(sequenceID,
					seqPropMgr);

			Iterator iter = completedMessageList.iterator();
			while (iter.hasNext()) {
				;
				sequenceReport.addCompletedMessage((Long) iter.next());
			}

			sequenceReport.setSequenceID(sequenceID);
			sequenceReport.setInternalSequenceID(sequenceID); // for the
																// incoming side
																// internalSequenceID=sequenceID
			sequenceReport.setSequenceDirection(SequenceReport.SEQUENCE_DIRECTION_IN);

			sequenceReport.setSequenceStatus(getServerSequenceStatus(sequenceID, storageManager));

			SequencePropertyBean tokenBean = seqPropMgr.retrieve(sequenceID, Sandesha2Constants.SequenceProperties.SECURITY_TOKEN);
			if(tokenBean != null) sequenceReport.setSecureSequence(true);
			
			return sequenceReport;

		} catch (Exception e) {
			if (!withinTransaction && reportTransaction!=null) {
				reportTransaction.rollback();
				configCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);
				rolebacked = true;
			}
		} finally {
			if (!withinTransaction && !rolebacked && reportTransaction!=null) {
				reportTransaction.commit();
				configCtx.setProperty(Sandesha2Constants.WITHIN_TRANSACTION, Sandesha2Constants.VALUE_FALSE);
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

		String to = epr.getAddress();
		String sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		String internalSequenceID = SandeshaUtil.getInternalSequenceID(to, sequenceKey);
		SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(internalSequenceID,
				configurationContext);
		if (sequenceReport == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotGenerateReport, internalSequenceID));
		if (sequenceReport.getSequenceStatus() != SequenceReport.SEQUENCE_STATUS_ESTABLISHED)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noSequenceEstablished, internalSequenceID));
		
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,configurationContext.getAxisConfiguration());

		// Get a transaction to obtain sequence information
		Transaction transaction = storageManager.getTransaction();
		
		SequencePropertyBean sequenceIDBean = null;
		
		try
		{
			SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
			sequenceIDBean = seqPropMgr.retrieve(internalSequenceID,
					Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID);
			if (sequenceIDBean == null)
				throw new SandeshaException(SandeshaMessageHelper.getMessage(
						SandeshaMessageKeys.sequenceIdBeanNotSet));

		}
		finally
		{
			transaction.commit();
		}
		
		String sequenceID = sequenceIDBean.getValue();

		
		if (sequenceID == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotFindSequenceID, sequenceIDBean.toString()));

		String rmSpecVersion = (String) options.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);
		if (rmSpecVersion == null)
			rmSpecVersion = SpecSpecificConstants.getDefaultSpecVersion();

		options.setAction(SpecSpecificConstants.getTerminateSequenceAction(rmSpecVersion));
		SOAPEnvelope dummyEnvelope = null;
		SOAPFactory factory = null;
		String soapNamespaceURI = options.getSoapVersionURI();
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
				anonOutOnlyOperation = AxisOperationFactory.getAxisOperation(WSDL20_2004Constants.MEP_CONSTANT_OUT_ONLY);
				anonOutOnlyOperation.setName(ServiceClient.ANON_OUT_ONLY_OP);
				
				AxisOperation referenceOperation = service.getOperation(new QName (Sandesha2Constants.RM_IN_ONLY_OPERATION));
				
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
				anonOutInOperation = AxisOperationFactory.getAxisOperation(WSDL20_2004Constants.MEP_CONSTANT_OUT_IN);
				anonOutInOperation.setName(ServiceClient.ANON_OUT_IN_OP);
				
				AxisOperation referenceOperation = service.getOperation(new QName (Sandesha2Constants.RM_IN_OUT_OPERATION_NAME));
				
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
	public static String getLastSendError(ServiceClient serviceClient) 
	
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
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
		
		Transaction transaction = storageManager.getTransaction();

		String resultString = null;
    
		try 
		{		
			// Lookup the last failed to send error
			SequencePropertyBean errorBean = seqPropMgr.retrieve(internalSequenceId, Sandesha2Constants.SequenceProperties.LAST_FAILED_TO_SEND_ERROR);
		
			
			// Get the value from the 
			if (errorBean != null)
				resultString = errorBean.getValue();
		}
		finally
		{
			transaction.commit();
		}
		
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaClient::getLastSendError, " + resultString);
		
		return resultString;
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
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
		
		// Create a transaction for the retrieve operation
		Transaction transaction = storageManager.getTransaction();
	
		long resultTime = -1;

		try
		{		
			// Lookup the last failed to send error
			SequencePropertyBean errorTSBean = 
				seqPropMgr.retrieve(internalSequenceId, Sandesha2Constants.SequenceProperties.LAST_FAILED_TO_SEND_ERROR_TIMESTAMP);
				
			// Get the value from the 
			if (errorTSBean != null)
				resultTime = Long.valueOf(errorTSBean.getValue()).longValue();
		}
		finally
		{
			// commit the transaction as it was only a retrieve
			transaction.commit();
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
	private static String getInternalSequenceIdFromServiceClient(ServiceClient serviceClient) 
	
	throws SandeshaException
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

		String internalSequenceID = getInternalSequenceID(to, sequenceKey);

		return internalSequenceID;
	}
}
