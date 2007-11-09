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

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.AcknowledgementRange;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;


public class InvalidAcknowledgementTest extends SandeshaTestCase {

	private static final String server_repoPath = "target" + File.separator
	    + "repos" + File.separator + "server";

	private static final String server_axis2_xml = "target" + File.separator
	    + "repos" + File.separator + "server" + File.separator
	    + "server_axis2.xml";
	
	private ConfigurationContext serverConfigContext;
	
	public InvalidAcknowledgementTest() {
		super("InvalidAcknowledgementTest");
	}

	public void setUp() throws Exception {
		super.setUp();
		serverConfigContext = startServer(server_repoPath, server_axis2_xml);
	}

	/**
	 * Sends an ACK message to an RM Source that will be refused and should be
	 * rejected with an InvalidAck fault
	 * 
	 * We mock up a RMS sequence on the server, this is for us to then use for the fault.
	 * 
	 * @throws Exception
	 */
	public void testInvalidAcknowledgementSOAPFault() throws Exception {		
		// Create an RMS on the service.
		StorageManager storageManager = 
			SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
		
		RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();
		
		String seqID = SandeshaUtil.getUUID();
		
		// Mockup an RMSBean
		RMSBean rmsBean = new RMSBean();
		rmsBean.setCreateSeqMsgID(SandeshaUtil.getUUID());
		rmsBean.setSequenceID(seqID);
		rmsBean.setInternalSequenceID(SandeshaUtil.getInternalSequenceID(seqID, null));
		rmsBean.setToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmsBean.setAcksToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmsBean.setReplyToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmsBean.setRMVersion(Sandesha2Constants.SPEC_VERSIONS.v1_1);
		rmsBean.setClientCompletedMessages(new RangeString());
		rmsBean.setNextMessageNumber(1);
		
		// Create a transaction and insert the RMSBean
		Transaction tran = storageManager.getTransaction();
		
		rmsBeanMgr.insert(rmsBean);
		
		tran.commit();
		
    // Open a connection to the endpoint, using the sequence ack as the action
		HttpURLConnection connection = 
			FaultTestUtils.getHttpURLConnection("http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService",
					"http://docs.oasis-open.org/ws-rx/wsrm/200702/SequenceAcknowledgement");

		OutputStream tmpOut2 = connection.getOutputStream();
		byte ar[] = getAppMessageAsBytes(seqID);
		
		// Send the message to the socket.
		tmpOut2.write(ar);
		tmpOut2.flush();

		// Get the response message from the connection
		String message = FaultTestUtils.retrieveResponseMessage(connection);
    
    // Check that the fault message isn't null
    assertNotNull(message);
    
    // Check that the response contains the InvalidAcknowledgement tag    
    assertTrue(message.indexOf("InvalidAcknowledgement") > -1);
    
    // Check that the <wsrm:Identifier>seqID</wsrm:Identifier> matches the sequence ID specified
    String faultID = message.substring(message.indexOf("<wsrm:Identifier>") + 17, message.indexOf("</wsrm:Identifier>"));
    assertEquals(seqID, faultID);
    
    // Disconnect at the end of the test
    connection.disconnect();
	}
	
	/**
	 * Sends an ACK message to an RM Source that will be refused and should be
	 * rejected with an InvalidAck fault
	 * 
	 * We mock up a RMS sequence on the server, this is for us to then use for the fault.
	 * Mock up a couple of SenderBeans which match the 1, 2, 3 message numbers
	 * Set the highest out message number to be 3
	 * 
	 * Send an ack range in for 1 - 3, Indicate that message 1 has been sent, but no more.
	 * 
	 * @throws Exception
	 */
	public void testInvalidAcknowledgementFromBeanNotSentSOAPFault() throws Exception {
		
		// Create an RMS on the service.
		StorageManager storageManager = 
			SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
		
		RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();
		SenderBeanMgr senderMgr = storageManager.getSenderBeanMgr();
		
		String seqID = SandeshaUtil.getUUID();
		
		// Mockup an RMSBean
		RMSBean rmsBean = new RMSBean();
		rmsBean.setCreateSeqMsgID(SandeshaUtil.getUUID());
		rmsBean.setSequenceID(seqID);
		rmsBean.setInternalSequenceID(SandeshaUtil.getInternalSequenceID(seqID, null));
		rmsBean.setToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmsBean.setAcksToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmsBean.setReplyToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmsBean.setRMVersion(Sandesha2Constants.SPEC_VERSIONS.v1_1);
		rmsBean.setClientCompletedMessages(new RangeString());
		rmsBean.setNextMessageNumber(4);
		rmsBean.setHighestOutMessageNumber(3);
		
		SenderBean bean1 = getSenderBean(seqID, 1, 1);
		SenderBean bean2 = getSenderBean(seqID, 0, 2);
		SenderBean bean3 = getSenderBean(seqID, 1, 3);		

		// Create a transaction and insert the RMSBean
		Transaction tran = storageManager.getTransaction();
		
		rmsBeanMgr.insert(rmsBean);
		senderMgr.insert(bean1);
		senderMgr.insert(bean2);
		senderMgr.insert(bean3);		
		
		tran.commit();
		
    // Open a connection to the endpoint, using the sequence ack as the action
		HttpURLConnection connection = 
			FaultTestUtils.getHttpURLConnection("http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService",
					"http://docs.oasis-open.org/ws-rx/wsrm/200702/SequenceAcknowledgement");

		OutputStream tmpOut2 = connection.getOutputStream();
		byte ar[] = getAppMessageAsBytes(seqID);
		
		// Send the message to the socket.
		tmpOut2.write(ar);
		tmpOut2.flush();

		// Get the response message from the connection
		String message = FaultTestUtils.retrieveResponseMessage(connection);
    
    // Check that the fault message isn't null
    assertNotNull(message);
    
    // Check that the response contains the InvalidAcknowledgement tag    
    assertTrue(message.indexOf("wsrm:InvalidAcknowledgement") > -1);
    
    // Check that the <wsrm:Identifier>seqID</wsrm:Identifier> matches the sequence ID specified
    String faultID = message.substring(message.indexOf("<wsrm:Identifier>") + 17, message.indexOf("</wsrm:Identifier>"));
    assertEquals(seqID, faultID);
    
    // Disconnect at the end of the test
    connection.disconnect();
	}

	/**
	 * Get a SequenceAck message as bytes
	 * 
	 * @return
	 */
	private byte[] getAppMessageAsBytes(String sequenceId) throws Exception
	{
		SOAPFactory factory = new SOAP11Factory();
		SOAPEnvelope dummyEnvelope = factory.getDefaultEnvelope();
		
		// Create a "new" application message
		MessageContext messageContext = new MessageContext();
		messageContext.setConfigurationContext(serverConfigContext);
		messageContext.setAxisService(serverConfigContext.getAxisConfiguration().getService("RMSampleService"));		
		messageContext.setEnvelope(dummyEnvelope);
		
		RMMsgContext applicationRMMsg = new RMMsgContext(messageContext);
		
		// Generate the SequenceAck field.
		// -------------------------------
		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(Sandesha2Constants.SPEC_VERSIONS.v1_1);

		SequenceAcknowledgement sequenceAck = new SequenceAcknowledgement(rmNamespaceValue);
		// Set the sequenceId
		Identifier id = new Identifier(rmNamespaceValue);
		id.setIndentifer(sequenceId);
		sequenceAck.setIdentifier(id);
		
		// Set the Invalid range!
		AcknowledgementRange ackRange = new AcknowledgementRange(rmNamespaceValue);
		ackRange.setLowerValue(1);
		ackRange.setUpperValue(3);
		sequenceAck.addAcknowledgementRanges(ackRange);

		// Set the SequenceAcknowledgement part in the message
		applicationRMMsg.setMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT, sequenceAck);
		applicationRMMsg.addSOAPEnvelope();

		// --------------------------------------------
		// Finished generating SequenceAck part
		
		// Create an RMSBean so the create sequence message can be created
		messageContext.setWSAAction("http://docs.oasis-open.org/ws-rx/wsrm/200702/SequenceAcknowledgement");

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		// Serialize the application message
		applicationRMMsg.getMessageContext().getEnvelope().serialize(outputStream);
		
		return outputStream.toByteArray();
	}
	
	private static SenderBean getSenderBean(String seqID, int sendCount, int messageNo) {
		SenderBean bean = new SenderBean();
		bean.setInternalSequenceID(SandeshaUtil.getInternalSequenceID(seqID, null));	
		bean.setSequenceID(seqID);
		bean.setMessageID(SandeshaUtil.getUUID());		
		bean.setSentCount(sendCount);
		bean.setSend(true);
		bean.setReSend(true);
		bean.setMessageType(Sandesha2Constants.MessageTypes.APPLICATION);
		bean.setMessageNumber(messageNo);
		
		return bean;
	}
}




