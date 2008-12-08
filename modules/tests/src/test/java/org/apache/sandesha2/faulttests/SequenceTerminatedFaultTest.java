/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sandesha2.faulttests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;

import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;
import org.apache.sandesha2.msgreceivers.RMMessageReceiver;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.AckRequested;
import org.apache.sandesha2.wsrm.CloseSequence;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.LastMessageNumber;
import org.apache.sandesha2.wsrm.Sequence;


public class SequenceTerminatedFaultTest extends SandeshaTestCase {

	private static final String server_repoPath = "target" + File.separator
	    + "repos" + File.separator + "server";

	private static final String server_axis2_xml = "target" + File.separator
	    + "repos" + File.separator + "server" + File.separator
	    + "server_axis2.xml";
	
	private static ConfigurationContext serverConfigContext;
	
	private boolean serverStarted = false;
	
	public SequenceTerminatedFaultTest() {
		super("SequenceTerminatedFaultTest");
	}

	public void setUp() throws Exception {
		super.setUp();
		if (!serverStarted) {
			serverConfigContext = startServer(server_repoPath, server_axis2_xml);
		}
		serverStarted = true;
	}

	public void tearDown () throws Exception {
		super.tearDown();
	}

	/**
	 * Sends an application message to the RMD.
	 * The RMD should reject the message with a sequence terminated fault
	 * 
	 * @throws Exception
	 */
	public void testSequenceTerminatedFault() throws Exception {
    // Open a connection to the endpoint
		HttpURLConnection connection = 
			FaultTestUtils.getHttpURLConnection("http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService",
					pingAction);

		StorageManager storageManager = 
			SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
		
		RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
		
		String seqID = SandeshaUtil.getUUID();
		
		// Mockup an RMDBean
		RMDBean rmdBean = new RMDBean();
		rmdBean.setSequenceID(seqID);
		rmdBean.setToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setAcksToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setReplyToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setRMVersion(Sandesha2Constants.SPEC_VERSIONS.v1_1);
		rmdBean.setServerCompletedMessages(new RangeString());
		// Flag that the sequence is terminated.
		rmdBean.setTerminated(true);
		
		// Create a transaction and insert the RMSBean
		Transaction tran = storageManager.getTransaction();
		
		rmdBeanMgr.insert(rmdBean);
		
		tran.commit();

		
		OutputStream tmpOut2 = connection.getOutputStream();

		byte ar[] = getAppMessageAsBytes(seqID);
		// Send the message to the socket.
		tmpOut2.write(ar);
		tmpOut2.flush();

		// Get the response message from the connection
		String message = FaultTestUtils.retrieveResponseMessage(connection);
    
    // Check that the fault message isn't null
    assertNotNull(message);
    
    // Check that the response contains the Sequence Terminated tag    
    assertTrue(message.indexOf("wsrm:SequenceTerminated") > -1);
    
    // Check that the <wsrm:Identifier>seqID</wsrm:Identifier> matches the sequence ID specified
    String faultID = message.substring(message.indexOf("<wsrm:Identifier>") + 17, message.indexOf("</wsrm:Identifier>"));
    assertEquals(seqID, faultID);
    
    // Disconnect at the end of the test
    connection.disconnect();
	}
	
	/**
	 * Sends a Close message to the RMD.
	 * The RMD should reject the message with a sequence terminated fault
	 * 
	 * @throws Exception
	 */
	public void testSequenceTerminatedOnCloseFault() throws Exception {
    // Open a connection to the endpoint
		HttpURLConnection connection = 
			FaultTestUtils.getHttpURLConnection("http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService",
					Sandesha2Constants.SPEC_2007_02.Actions.ACTION_CLOSE_SEQUENCE);

		StorageManager storageManager = 
			SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
		
		RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
		
		String seqID = SandeshaUtil.getUUID();
		
		// Mockup an RMDBean
		RMDBean rmdBean = new RMDBean();
		rmdBean.setSequenceID(seqID);
		rmdBean.setToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setAcksToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setReplyToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setRMVersion(Sandesha2Constants.SPEC_VERSIONS.v1_1);
		rmdBean.setServerCompletedMessages(new RangeString());
		// Flag that the sequence is terminated.
		rmdBean.setTerminated(true);
		
		// Create a transaction and insert the RMSBean
		Transaction tran = storageManager.getTransaction();
		
		rmdBeanMgr.insert(rmdBean);
		
		tran.commit();

		
		OutputStream tmpOut2 = connection.getOutputStream();

		byte ar[] = getCloseMessageAsBytes(seqID);
		// Send the message to the socket.
		tmpOut2.write(ar);
		tmpOut2.flush();

		// Get the response message from the connection
		String message = FaultTestUtils.retrieveResponseMessage(connection);
    
    // Check that the fault message isn't null
    assertNotNull(message);
    
    // Check that the response contains the Sequence Terminated tag    
    assertTrue(message.indexOf("wsrm:SequenceTerminated") > -1);
    
    // Check that the <wsrm:Identifier>seqID</wsrm:Identifier> matches the sequence ID specified
    String faultID = message.substring(message.indexOf("<wsrm:Identifier>") + 17, message.indexOf("</wsrm:Identifier>"));
    assertEquals(seqID, faultID);
    
    // Disconnect at the end of the test
    connection.disconnect();
	}

	/**
	 * Sends a AckRequest message to the RMD.
	 * The RMD should reject the message with a sequence terminated fault
	 * 
	 * @throws Exception
	 */
	public void testSequenceTerminatedOnAckRequestFault() throws Exception {
    // Open a connection to the endpoint
		HttpURLConnection connection = 
			FaultTestUtils.getHttpURLConnection("http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService",
					Sandesha2Constants.SPEC_2007_02.Actions.ACTION_ACK_REQUEST);

		StorageManager storageManager = 
			SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
		
		RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
		
		String seqID = SandeshaUtil.getUUID();
		
		// Mockup an RMDBean
		RMDBean rmdBean = new RMDBean();
		rmdBean.setSequenceID(seqID);
		rmdBean.setToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setAcksToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setReplyToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setRMVersion(Sandesha2Constants.SPEC_VERSIONS.v1_1);
		rmdBean.setServerCompletedMessages(new RangeString());
		// Flag that the sequence is terminated.
		rmdBean.setTerminated(true);
		
		// Create a transaction and insert the RMSBean
		Transaction tran = storageManager.getTransaction();
		
		rmdBeanMgr.insert(rmdBean);
		
		tran.commit();

		
		OutputStream tmpOut2 = connection.getOutputStream();

		byte ar[] = getAckRequestMessageAsBytes(seqID);
		// Send the message to the socket.
		tmpOut2.write(ar);
		tmpOut2.flush();

		// Get the response message from the connection
		String message = FaultTestUtils.retrieveResponseMessage(connection);
    
    // Check that the fault message isn't null
    assertNotNull(message);
    
    // Check that the response contains the Sequence Terminated tag    
    assertTrue(message.indexOf("wsrm:SequenceTerminated") > -1);
    
    // Check that the <wsrm:Identifier>seqID</wsrm:Identifier> matches the sequence ID specified
    String faultID = message.substring(message.indexOf("<wsrm:Identifier>") + 17, message.indexOf("</wsrm:Identifier>"));
    assertEquals(seqID, faultID);
    
    // Disconnect at the end of the test
    connection.disconnect();
	}

	/**
	 * When sending application messages, if the RMD sequence returns a sequence terminated
	 * fault, then the RMS Sequence should be terminated	 
	 */
	public void testRMSSequenceTerminatedOnSequenceTerminatedFault() throws Exception {
		runSequenceTerminated(false, false);
	}

	/**
	 * When sending application messages, if the RMD sequence returns a sequence terminated
	 * fault, then the RMS Sequence should be terminated.
	 * 
	 * Runs at SOAP12 level	 
	 */
	public void testRMSSequenceTerminatedOnSequenceTerminatedFaultSOAP12() throws Exception {
		runSequenceTerminated(false, true);
	}

	/**
	 * When sending application messages, if the RMD sequence returns a sequence terminated
	 * fault, then the RMS Sequence should be terminated	 
	 */
	public void testRMSSequenceTerminatedOnSequenceUnknownFault() throws Exception {		
		runSequenceTerminated(true, false);
	}

	/**
	 * When sending application messages, if the RMD sequence returns a sequence terminated
	 * fault, then the RMS Sequence should be terminated	 
	 * 
	 * Runs at SOAP12 level	 
	 */
	public void testRMSSequenceTerminatedOnSequenceUnknownFaultSOAP12() throws Exception {	
		runSequenceTerminated(true, true);
	}

	private void runSequenceTerminated(boolean deleteRMSBean, boolean soap12) throws Exception {
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);		
		
		Options clientOptions = new Options ();
		clientOptions.setAction(pingAction);
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setFaultTo(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));

		String sequenceKey = SandeshaUtil.getUUID();
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_1);
		
		if (soap12)
			clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		serviceClient.setOptions(clientOptions);

    // Send a single ping message
		serviceClient.fireAndForget(getPingOMBlock("ping1"));
	
		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			// Check that the sequence has been deleted.
			StorageManager storageManager = 
				SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
			
			Transaction tran = storageManager.getTransaction();
			
			RMDBean finderBean = new RMDBean();
			finderBean.setTerminated(false);
			List<RMDBean> rmdBeans = storageManager.getRMDBeanMgr().find(finderBean);
			
			tran.commit();
			
			lastError = null;
			
			if (rmdBeans.isEmpty())
				lastError = new Error("rmdBeans empty " + rmdBeans);
			else {
				RMDBean bean = (RMDBean)rmdBeans.get(0);
				if (!bean.getServerCompletedMessages().getContainedElementsAsNumbersList().contains(new Integer(1))) {
					tran = storageManager.getTransaction();
					if (deleteRMSBean) {
						storageManager.getRMDBeanMgr().delete(bean.getSequenceID());
					} else {
						bean.setTerminated(true);
						storageManager.getRMDBeanMgr().update(bean);
					}
					tran.commit();
					break;				
				}
				
				lastError = new Error("App message has not arrived");
			}
		}

		if(lastError != null) throw lastError;

		// Send a second application message.
		serviceClient.fireAndForget(getPingOMBlock("ping2"));
		
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
        //Check that the outgoing sequence is terminated
				SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertEquals(sequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
				assertEquals(sequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
				
				lastError = null;
				break;
			} catch(Error e) {
				lastError = e;
			}
		}

		if(lastError != null) throw lastError;
		
		configContext.getListenerManager().stop();
		serviceClient.cleanup();

	}
	
	/**
	 * Get an application message as bytes
	 * 
	 * @return
	 */
	private byte[] getAppMessageAsBytes(String uuid) throws Exception
	{
		SOAPFactory factory = new SOAP11Factory();
		SOAPEnvelope dummyEnvelope = factory.getDefaultEnvelope();
		
		// Create a "new" application message
		MessageContext messageContext = new MessageContext();
		messageContext.setConfigurationContext(serverConfigContext);
		messageContext.setAxisService(serverConfigContext.getAxisConfiguration().getService("RMSampleService"));		
		messageContext.setEnvelope(dummyEnvelope);
		
		RMMsgContext applicationRMMsg = new RMMsgContext(messageContext);
		
		// Generate the Sequence field.
		// -------------------------------
		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(Sandesha2Constants.SPEC_VERSIONS.v1_1);

		Sequence sequence = new Sequence(rmNamespaceValue);
		sequence.setMessageNumber(1);
		Identifier id1 = new Identifier(rmNamespaceValue);
		id1.setIndentifer(uuid);
		sequence.setIdentifier(id1);
		applicationRMMsg.setSequence(sequence);
		applicationRMMsg.addSOAPEnvelope();

		// --------------------------------------------
		// Finished generating Sequence part
		
		// Create an RMSBean so the create sequence message can be created
		messageContext.setWSAAction(pingAction);

		// Set the AxisOperation to be InOut
		AxisOperation operation = messageContext.getAxisService().getOperation(Sandesha2Constants.RM_IN_OUT_OPERATION);
		operation.setMessageReceiver(new RMMessageReceiver());
		messageContext.setAxisOperation(operation);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		// Serialize the application message
		applicationRMMsg.getMessageContext().getEnvelope().serialize(outputStream);
		
		return outputStream.toByteArray();
	}
	
	/**
	 * Get a close message as bytes
	 * 
	 * @return
	 */
	private byte[] getCloseMessageAsBytes(String uuid) throws Exception
	{
		SOAPFactory factory = new SOAP11Factory();
		SOAPEnvelope dummyEnvelope = factory.getDefaultEnvelope();
		
		// Create a "new" application message
		MessageContext messageContext = new MessageContext();
		messageContext.setConfigurationContext(serverConfigContext);
		messageContext.setAxisService(serverConfigContext.getAxisConfiguration().getService("RMSampleService"));		
		messageContext.setEnvelope(dummyEnvelope);
		
		RMMsgContext applicationRMMsg = new RMMsgContext(messageContext);
		
		// Generate the Close field.
		// -------------------------------
		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(Sandesha2Constants.SPEC_VERSIONS.v1_1);

		CloseSequence sequence = new CloseSequence(rmNamespaceValue);
		Identifier id1 = new Identifier(rmNamespaceValue);
		id1.setIndentifer(uuid);
		sequence.setIdentifier(id1);
		applicationRMMsg.setCloseSequence(sequence);
		LastMessageNumber lastMsgNumber = new LastMessageNumber(rmNamespaceValue);
		lastMsgNumber.setMessageNumber(1);
		sequence.setLastMessageNumber(lastMsgNumber);
		applicationRMMsg.addSOAPEnvelope();

		// --------------------------------------------
		// Finished generating Close part
		
		// Create an RMSBean so the create sequence message can be created
		messageContext.setWSAAction(pingAction);

		// Set the AxisOperation to be InOut
		AxisOperation operation = messageContext.getAxisService().getOperation(Sandesha2Constants.RM_IN_OUT_OPERATION);
		operation.setMessageReceiver(new RMMessageReceiver());
		messageContext.setAxisOperation(operation);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		// Serialize the application message
		applicationRMMsg.getMessageContext().getEnvelope().serialize(outputStream);
		
		return outputStream.toByteArray();
	}

	/**
	 * Get a close message as bytes
	 * 
	 * @return
	 */
	private byte[] getAckRequestMessageAsBytes(String uuid) throws Exception
	{
		SOAPFactory factory = new SOAP11Factory();
		SOAPEnvelope dummyEnvelope = factory.getDefaultEnvelope();
		
		// Create a "new" application message
		MessageContext messageContext = new MessageContext();
		messageContext.setConfigurationContext(serverConfigContext);
		messageContext.setAxisService(serverConfigContext.getAxisConfiguration().getService("RMSampleService"));		
		messageContext.setEnvelope(dummyEnvelope);
		
		RMMsgContext applicationRMMsg = new RMMsgContext(messageContext);
		
		// Generate the Close field.
		// -------------------------------
		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(Sandesha2Constants.SPEC_VERSIONS.v1_1);

		AckRequested sequence = new AckRequested(rmNamespaceValue);
		Identifier id1 = new Identifier(rmNamespaceValue);
		id1.setIndentifer(uuid);
		sequence.setIdentifier(id1);
		applicationRMMsg.addAckRequested(sequence);
		applicationRMMsg.addSOAPEnvelope();

		// --------------------------------------------
		// Finished generating Close part
		
		// Create an RMSBean so the create sequence message can be created
		messageContext.setWSAAction(pingAction);

		// Set the AxisOperation to be InOut
		AxisOperation operation = messageContext.getAxisService().getOperation(Sandesha2Constants.RM_IN_OUT_OPERATION);
		operation.setMessageReceiver(new RMMessageReceiver());
		messageContext.setAxisOperation(operation);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		// Serialize the application message
		applicationRMMsg.getMessageContext().getEnvelope().serialize(outputStream);
		
		return outputStream.toByteArray();
	}

}




