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

		ServiceContext serviceContext = serviceClient.getServiceContext();
		if (serviceContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		ConfigurationContext configurationContext = serviceContext.getConfigurationContext();

		String internalSequenceID = getInternalSequenceID(to, sequenceKey);

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
		if (!withinTransaction)
			reportTransaction = storageManager.getTransaction();

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
		if (!withinTransaction)
			reportTransaction = storageManager.getTransaction();

		boolean rolebacked = false;

		try {

			internalSequenceFindBean.setName(Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID);
			Collection collection = seqPropMgr.find(internalSequenceFindBean);
			Iterator iterator = collection.iterator();
			while (iterator.hasNext()) {
				SequencePropertyBean bean = (SequencePropertyBean) iterator.next();
				String sequenceID = bean.getSequenceID();
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
				String sequenceID = serverCompletedMsgsBean.getSequenceID();
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

	public static void createSequence(ServiceClient serviceClient, boolean offer) throws SandeshaException {
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
		String sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		if (sequenceKey == null) {
			sequenceKey = SandeshaUtil.getUUID();
			options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, sequenceKey);
		}

		options.setProperty(SandeshaClientConstants.DUMMY_MESSAGE, Sandesha2Constants.VALUE_TRUE);

		try {
			serviceClient.fireAndForget(null);
		} catch (AxisFault e) {
			throw new SandeshaException(e);
		}

		options.setProperty(SandeshaClientConstants.DUMMY_MESSAGE, Sandesha2Constants.VALUE_FALSE);

	}

	public static void createSequence(ServiceClient serviceClient, boolean offer, String sequenceKey)
			throws SandeshaException {

		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		String oldSequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, sequenceKey);

		createSequence(serviceClient, offer);

		options.setProperty(SandeshaClientConstants.SEQUENCE_KEY, oldSequenceKey);
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
			serviceClient.fireAndForget(terminateBody);
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
			serviceClient.fireAndForget(closeSequenceBody);
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
		// TODO test

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

		ServiceContext serviceContext = serviceClient.getServiceContext();
		if (serviceContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		ConfigurationContext configurationContext = serviceContext.getConfigurationContext();

		String internalSequenceID = generateInternalSequenceIDForTheClientSide(to, sequenceKey);

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

		SequencePropertyBean sequenceIDBean = seqPropMgr.retrieve(internalSequenceID,
				Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID);
		if (sequenceIDBean == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.sequenceIdBeanNotSet));

		String sequenceID = sequenceIDBean.getValue();
		return sequenceID;
	}

	public static void sendAckRequest(ServiceClient serviceClient) throws SandeshaException {

		Options options = serviceClient.getOptions();
		if (options == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.optionsObjectNotSet));

		ServiceContext serviceContext = serviceClient.getServiceContext();
		if (serviceContext == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.serviceContextNotSet));

		ConfigurationContext configContext = serviceContext.getConfigurationContext();

		EndpointReference toEPR = options.getTo();
		if (toEPR == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.toEPRNotValid, null));

		String to = toEPR.getAddress();

		String sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);

		String rmSpecVersion = (String) options.getProperty(SandeshaClientConstants.RM_SPEC_VERSION);
		if (rmSpecVersion == null)
			rmSpecVersion = Sandesha2Constants.SPEC_VERSIONS.v1_0;

		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(rmSpecVersion)) {
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.emptyAckRequestSpecLevel, rmSpecVersion));
		}

		String internalSequenceID = getInternalSequenceID(to, sequenceKey);

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

		AckRequested ackRequested = new AckRequested(factory, rmNamespaceValue);
		Identifier identifier = new Identifier(factory, rmNamespaceValue);
		identifier.setIndentifer(outSequenceID);
		ackRequested.setIdentifier(identifier);

		ackRequested.toSOAPEnvelope(dummyEnvelope);

		OMElement ackRequestedHeaderBlock = dummyEnvelope.getHeader().getFirstChildWithName(
				new QName(rmNamespaceValue, Sandesha2Constants.WSRM_COMMON.ACK_REQUESTED));

		String oldAction = options.getAction();

		options.setAction(SpecSpecificConstants.getAckRequestAction(rmSpecVersion));

		serviceClient.addHeader(ackRequestedHeaderBlock);

		try {
			serviceClient.fireAndForget(null);
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
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
		SequencePropertyBean sequenceIDBean = seqPropMgr.retrieve(internalSequenceID,
				Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID);
		if (sequenceIDBean == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.sequenceIdBeanNotSet));

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

		CloseSequence closeSequence = new CloseSequence(factory, rmNamespaceValue);
		Identifier identifier = new Identifier(factory, rmNamespaceValue);
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

		String outSequenceID = internalSequenceBean.getSequenceID();

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

		String outSequenceID = internalSequenceBean.getSequenceID();
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

		String outSequenceID = internalSequenceBean.getSequenceID();
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
		String outSequenceID = internalSequenceBean.getSequenceID();
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

	private static String generateInternalSequenceIDForTheClientSide(String toEPR, String sequenceKey) {
		return SandeshaUtil.getInternalSequenceID(toEPR, sequenceKey);
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
		if (!withinTransaction)
			reportTransaction = storageManager.getTransaction();

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
		SequencePropertyBeanMgr seqPropMgr = storageManager.getSequencePropertyBeanMgr();
		SequencePropertyBean sequenceIDBean = seqPropMgr.retrieve(internalSequenceID,
				Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID);
		if (sequenceIDBean == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.sequenceIdBeanNotSet));

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
		TerminateSequence terminateSequence = new TerminateSequence(factory, rmNamespaceValue);
		Identifier identifier = new Identifier(factory, rmNamespaceValue);
		identifier.setIndentifer(sequenceID);
		terminateSequence.setIdentifier(identifier);
		terminateSequence.toSOAPEnvelope(dummyEnvelope);

		return dummyEnvelope;
	}

}
