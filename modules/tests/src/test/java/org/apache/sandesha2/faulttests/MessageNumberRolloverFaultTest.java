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
import org.apache.axis2.description.AxisOperation;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.msgreceivers.RMMessageReceiver;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.util.Range;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.Sequence;


public class MessageNumberRolloverFaultTest extends SandeshaTestCase {

	private static final String server_repoPath = "target" + File.separator
	    + "repos" + File.separator + "server";

	private static final String server_axis2_xml = "target" + File.separator
	    + "repos" + File.separator + "server" + File.separator
	    + "server_axis2.xml";
	
	private ConfigurationContext serverConfigContext;
	
	public MessageNumberRolloverFaultTest() {
		super("MessageNumberRolloverFaultTest");
	}

	public void setUp() throws Exception {
		super.setUp();
		serverConfigContext = startServer(server_repoPath, server_axis2_xml);
	}

	/**
	 * Sends an application message to the RMD.
	 * The RMD should reject the message with a MessageRolloverFault
	 * 
	 * @throws Exception
	 */
	public void testMessageNumberRolloverFault() throws Exception {
    // Open a connection to the endpoint
		HttpURLConnection connection = 
			FaultTestUtils.getHttpURLConnection("http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService",
					pingAction);

		StorageManager storageManager = 
			SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
		
		RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
		
		String seqID = SandeshaUtil.getUUID();
		
		// Mockup an RMSBean
		RMDBean rmdBean = new RMDBean();
		rmdBean.setSequenceID(seqID);
		rmdBean.setToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setAcksToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setReplyToEndpointReference(new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
		rmdBean.setRMVersion(Sandesha2Constants.SPEC_VERSIONS.v1_1);
		rmdBean.setServerCompletedMessages(new RangeString());
		rmdBean.getServerCompletedMessages().addRange(new Range(1, Long.MAX_VALUE -1));
		rmdBean.getServerCompletedMessages().addRange(new Range(Long.MAX_VALUE, Long.MAX_VALUE));
		
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
    
    // Check that the response contains the MessageNumberRollover tag    
    assertTrue(message.indexOf("wsrm:MessageNumberRollover") > -1);
    
    // Disconnect at the end of the test
    connection.disconnect();
	}
	
	/**
	 * Get an application message as bytes
	 * 
	 * This sets the message number to "Long.maxValue"
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
		sequence.setMessageNumber(Long.MAX_VALUE);
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
}


