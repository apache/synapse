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
import org.apache.axiom.soap.impl.llom.soap12.SOAP12Factory;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.Range;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.Sequence;
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

	private RMDBean setupRMDBean(String sequenceID)throws Exception
	{
		// Create an RMS on the service.
		StorageManager storageManager = 
			SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
	
		RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
	
		// Mockup an RMSBean
		RMDBean rmdBean = new RMDBean();
		rmdBean.setSequenceID(sequenceID);
		rmdBean.setToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setAcksToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setReplyToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setRMVersion(Sandesha2Constants.SPEC_VERSIONS.v1_1);
		rmdBean.setServerCompletedMessages(new RangeString());
		rmdBean.setHighestInMessageNumber(0);
		rmdBean.setNextMsgNoToProcess(1);
	
		// Create a transaction and insert the RMSBean
		Transaction tran = storageManager.getTransaction();
		
		rmdBeanMgr.insert(rmdBean);
		
		tran.commit();
		return rmdBean;
	}
	
	private RMSBean setupRMSBean(String sequenceID)throws Exception
	{
		// Create an RMS on the service.
		StorageManager storageManager = 
			SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
	
		RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();
	
		// Mockup an RMSBean
		RMSBean rmsBean = new RMSBean();
		rmsBean.setCreateSeqMsgID(SandeshaUtil.getUUID());
		rmsBean.setSequenceID(sequenceID);
		rmsBean.setInternalSequenceID(SandeshaUtil.getInternalSequenceID(sequenceID, null));
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
		return rmsBean;
	}
	
	public void testSoap11_PiggyBackInvalidAcknowledgementSOAPFault() throws Exception{
		runInvalidAcknowledgementSOAPFault(true, false);
	}

	public void testSoap12_PiggyBackInvalidAcknowledgementSOAPFault() throws Exception{
		runInvalidAcknowledgementSOAPFault(true, true);
	}

	public void testSoap11_Standalone_InvalidAcknowledgementSOAPFault() throws Exception{
		runInvalidAcknowledgementSOAPFault(false, false);
	}
	
	public void testSoap12_Standalone_InvalidAcknowledgementSOAPFault() throws Exception{
		runInvalidAcknowledgementSOAPFault(false, true);
	}

	public void testSoap11_PiggyBack_InvalidAcknowledgementFromBeanNotSentSOAPFault()throws Exception{
		runInvalidAcknowledgementFromBeanNotSentSOAPFault(true, false);
	}

	public void testSoap12_PiggyBack_InvalidAcknowledgementFromBeanNotSentSOAPFault()throws Exception{
		runInvalidAcknowledgementFromBeanNotSentSOAPFault(true, true);
	}

	public void testSoap11_Standalone_InvalidAcknowledgementFromBeanNotSentSOAPFault()throws Exception{
		runInvalidAcknowledgementFromBeanNotSentSOAPFault(false, false);
	}

	public void testSoap12_Standalone_InvalidAcknowledgementFromBeanNotSentSOAPFault()throws Exception{
		runInvalidAcknowledgementFromBeanNotSentSOAPFault(false, true);
	}
	
	/**
	 * Sends an ACK message to an RM Source that will be refused.
	 * 
	 *  If the ack msg has an application msg on it too then it should be ignored. 
	 *  If not then should be rejected with an InvalidAck fault
	 * 
	 * We mock up a RMS sequence on the server, this is for us to then use for the fault.
	 * 
	 * @throws Exception
	 */
	public void runInvalidAcknowledgementSOAPFault(boolean piggyBack, boolean soap12) throws Exception {		
		
	   String ackMsgSeqID = SandeshaUtil.getUUID();
	   String applicationMsgSeqID = SandeshaUtil.getUUID();
	   
       RMSBean rmsBean = setupRMSBean(ackMsgSeqID);
       RMDBean rmdBean = setupRMDBean(applicationMsgSeqID);
		
    // Open a connection to the endpoint, using the sequence ack as the action
		HttpURLConnection connection = 
			FaultTestUtils.getHttpURLConnection("http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService",
					"http://docs.oasis-open.org/ws-rx/wsrm/200702/SequenceAcknowledgement", soap12);

		
		SOAPFactory factory = null;
		if(soap12){
			factory = new SOAP12Factory();
		}
		else{
			factory = new SOAP11Factory();
		}
		
		OutputStream tmpOut2 = connection.getOutputStream();
		byte ar[] = null;
		if(piggyBack){
			ar = getPiggybackedAckMessageAsBytes(ackMsgSeqID, applicationMsgSeqID, factory);
		}
		else{
			ar = getAckMessageAsBytes(ackMsgSeqID, factory);
		}
		
		// Send the message to the socket.
		tmpOut2.write(ar);
		tmpOut2.flush();

		// Get the response message from the connection
		String message = FaultTestUtils.retrieveResponseMessage(connection);
    
	    // Check that the fault message isn't null
	    assertNotNull(message);
	    
	    if(!piggyBack){
		    // Check that the response contains the InvalidAcknowledgement tag    
		    assertTrue(message.indexOf("InvalidAcknowledgement") > -1);
		    
		    // Check that the <wsrm:Identifier>seqID</wsrm:Identifier> matches the sequence ID specified
		    String faultID = message.substring(message.indexOf("<wsrm:Identifier>") + 17, message.indexOf("</wsrm:Identifier>"));
		    assertEquals(ackMsgSeqID, faultID);	    	
	    }
	    else{
		    //check the inbound application msg has been processed too
		    assertEquals(rmdBean.getHighestInMessageNumber(), 1);	    	
	    }

	    
	    // Disconnect at the end of the test
	    connection.disconnect();
	}
	
	/**
	 * Sends an ACK message to an RM Source that will be refused since the request has not been sent.
	 * 
	 *  If the ack msg has an application msg on it too then it should be ignored.
	 *  
	 *  If not then should be rejected with an InvalidAck fault
	 * 
	 * We mock up a RMS sequence on the server, this is for us to then use for the fault.
	 * Mock up a couple of SenderBeans which match the 1, 2, 3 message numbers
	 * Set the highest out message number to be 3
	 * 
	 * Send an ack range in for 1 - 3, Indicate that message 1 has been sent, but no more.
	 * 
	 * @throws Exception
	 */
	public void runInvalidAcknowledgementFromBeanNotSentSOAPFault(boolean piggyBack, boolean soap12) throws Exception {
		
		// Create an RMS on the service.
		StorageManager storageManager = 
			SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
		
		SenderBeanMgr senderMgr = storageManager.getSenderBeanMgr();
		
		// Create an RMS on the service.
		String ackMsgSeqID = SandeshaUtil.getUUID();
		String applicationMsgSeqID = SandeshaUtil.getUUID();
		   
		setupRMSBean(ackMsgSeqID);
		RMDBean rmdBean = setupRMDBean(applicationMsgSeqID);
		
		SenderBean bean1 = getSenderBean(ackMsgSeqID, 1, 1);
		SenderBean bean2 = getSenderBean(ackMsgSeqID, 0, 2);
		SenderBean bean3 = getSenderBean(ackMsgSeqID, 1, 3);		

		// Create a transaction and insert the RMSBean
		Transaction tran = storageManager.getTransaction();
		
		senderMgr.insert(bean1);
		senderMgr.insert(bean2);
		senderMgr.insert(bean3);		
		
		tran.commit();
		
    // Open a connection to the endpoint, using the sequence ack as the action
		HttpURLConnection connection = 
			FaultTestUtils.getHttpURLConnection("http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService",
					"http://docs.oasis-open.org/ws-rx/wsrm/200702/SequenceAcknowledgement", soap12);

		OutputStream tmpOut2 = connection.getOutputStream();
		SOAPFactory factory = null;
		if(soap12){
			factory = new SOAP12Factory();
		}
		else{
			factory = new SOAP11Factory();
		}
		
		byte ar[] = null;
		if(piggyBack){
			ar = getPiggybackedAckMessageAsBytes(ackMsgSeqID, applicationMsgSeqID, factory);
		}
		else{
			ar = getAckMessageAsBytes(ackMsgSeqID, factory);
		}
		
		// Send the message to the socket.
		tmpOut2.write(ar);
		tmpOut2.flush();

		// Get the response message from the connection
		String message = FaultTestUtils.retrieveResponseMessage(connection);
    
	    // Check that the fault message isn't null
	    assertNotNull(message);
	    
	    if(!piggyBack){	
	    	// Check that the response contains the InvalidAcknowledgement tag    
    		assertTrue(message.indexOf("InvalidAcknowledgement") > -1); //note name space prefix is not always the same so ignore		    		

		    // Check that the <wsrm:Identifier>seqID</wsrm:Identifier> matches the sequence ID specified
		    String faultID = message.substring(message.indexOf("<wsrm:Identifier>") + 17, message.indexOf("</wsrm:Identifier>"));
		    assertEquals(ackMsgSeqID, faultID);
	    }
	    else{
		    //check the inbound application msg has been processed too
		    assertEquals(rmdBean.getHighestInMessageNumber(), 1);
		    
		    //TODO check for response msg
	    }
	    
	    // Disconnect at the end of the test
	    connection.disconnect();
	}

	private byte[] getAckMessageAsBytes(String sequenceIDAck, SOAPFactory soapFactory)throws Exception
	{
		SOAPEnvelope dummyEnvelope = soapFactory.getDefaultEnvelope();
		
		// Create a "new" application message
		MessageContext messageContext = new MessageContext();
		messageContext.setConfigurationContext(serverConfigContext);
		messageContext.setAxisService(serverConfigContext.getAxisConfiguration().getService("RMSampleService"));		
		messageContext.setEnvelope(dummyEnvelope);
		
		RMMsgContext applicationRMMsg = new RMMsgContext(messageContext);
		
		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(Sandesha2Constants.SPEC_VERSIONS.v1_1);
				
		//setup the sequenceAck portion of the msg
		SequenceAcknowledgement sequenceAck = new SequenceAcknowledgement(rmNamespaceValue);
		// Set the sequenceId
		Identifier id = new Identifier(rmNamespaceValue);
		id.setIndentifer(sequenceIDAck);
		sequenceAck.setIdentifier(id);
		
		// Set the Invalid range!
		Range ackRange = new Range(1,3);
		sequenceAck.addAcknowledgementRanges(ackRange);

		// Set the SequenceAcknowledgement part in the message
		applicationRMMsg.addSequenceAcknowledgement(sequenceAck);
		
		applicationRMMsg.addSOAPEnvelope();

		// --------------------------------------------
		// Finished generating SequenceAck part
		
		messageContext.setWSAAction("http://docs.oasis-open.org/ws-rx/wsrm/200702/SequenceAcknowledgement");
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		// Serialize the application message
		applicationRMMsg.getMessageContext().getEnvelope().serialize(outputStream);
		
		return outputStream.toByteArray();
	}
	
	/**
	 * Get a SequenceAck piggy backed onto an application msg as bytes
	 * 
	 * @return
	 */
	private byte[] getPiggybackedAckMessageAsBytes(String sequenceIDAck, String sequenceIDApplicationMessage, SOAPFactory factory) throws Exception
	{
		SOAPEnvelope dummyEnvelope = factory.getDefaultEnvelope();
	
		// Create a "new" application message
		MessageContext messageContext = new MessageContext();
		messageContext.setConfigurationContext(serverConfigContext);
		messageContext.setAxisService(serverConfigContext.getAxisConfiguration().getService("RMSampleService"));		
		messageContext.setEnvelope(dummyEnvelope);
		
		RMMsgContext applicationRMMsg = new RMMsgContext(messageContext);
		
		String rmNamespaceValue = SpecSpecificConstants.getRMNamespaceValue(Sandesha2Constants.SPEC_VERSIONS.v1_1);
		
		//now add a sequence part to simulate a piggy backed application msg
		Sequence sequence = new Sequence(rmNamespaceValue);
		Identifier sequenceID = new Identifier(rmNamespaceValue);
		sequenceID.setIndentifer(sequenceIDApplicationMessage); //some new uuid ID
		sequence.setIdentifier(sequenceID);
		sequence.setMessageNumber(1);
		applicationRMMsg.setSequence(sequence);
		
		//setup the sequenceAck portion of the msg
		SequenceAcknowledgement sequenceAck = new SequenceAcknowledgement(rmNamespaceValue);
		// Set the sequenceId
		Identifier id = new Identifier(rmNamespaceValue);
		id.setIndentifer(sequenceIDAck);
		sequenceAck.setIdentifier(id);
		
		// Set the Invalid range!
		Range ackRange = new Range(1,3);
		sequenceAck.addAcknowledgementRanges(ackRange);

		// Set the SequenceAcknowledgement part in the message
		applicationRMMsg.addSequenceAcknowledgement(sequenceAck);
		
		applicationRMMsg.addSOAPEnvelope();

		// --------------------------------------------
		// Finished generating SequenceAck part
		
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
		bean.setMessageContextRefKey("fakeKey");
		return bean;
	}
}





