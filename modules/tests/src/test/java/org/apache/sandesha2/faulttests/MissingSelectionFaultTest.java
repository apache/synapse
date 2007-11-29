/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.MakeConnection;

public class MissingSelectionFaultTest extends SandeshaTestCase {

	private static final String server_repoPath = "target" + File.separator
	        + "repos" + File.separator + "server";

	private static final String server_axis2_xml = "target" + File.separator
	        + "repos" + File.separator + "server" + File.separator
	        + "server_axis2.xml";

	private static ConfigurationContext serverConfigContext;

	public MissingSelectionFaultTest() {
		super("MissingSelectionFaultTest");
	}

	public void setUp() throws Exception {
		super.setUp();
		serverConfigContext = startServer(server_repoPath, server_axis2_xml);
	}

	/**
	 * Sends a MakeConnection message to an RM Destination without a Address or Identifier element.
	 * refused.
	 * 
	 * @throws Exception
	 */
	public void testMissingSelectionFault() throws Exception {

		// Open a connection to the endpoint
		HttpURLConnection connection = FaultTestUtils.getHttpURLConnection(
		        "http://127.0.0.1:" + serverPort
		                + "/axis2/services/RMSampleService",
		        "http://docs.oasis-open.org/ws-rx/wsmc/200702/MakeConnection");

		OutputStream tmpOut2 = connection.getOutputStream();

		byte ar[] = getMessageAsBytes();

		// Send the message to the socket.
		tmpOut2.write(ar);
		tmpOut2.flush();

		// Get the response message from the connection
		String message = FaultTestUtils.retrieveResponseMessage(connection);

		// Check that the fault message isn't null
		assertNotNull(message);

		// Check that the response contains the wsrm:CreateSequenceRefused tag
		assertTrue(message.indexOf("MissingSelection") > -1);

		// Disconnect at the end of the test
		connection.disconnect();
	}

	/**
	 * Get a MakeConnection message as bytes
	 * 
	 * This generates a MakeConnection that has missing values
	 * which will generate a Fault from the service.
	 * 
	 * @return
	 */
	private byte[] getMessageAsBytes() throws Exception {
		String to = "http://127.0.0.1:" + 9999 //serverPort
		        + "/axis2/services/RMSampleService";

		SOAPFactory factory = new SOAP11Factory();
		SOAPEnvelope dummyEnvelope = factory.getDefaultEnvelope();

		// Create a "new" application message
		MessageContext messageContext = new MessageContext();
		messageContext.setConfigurationContext(serverConfigContext);
		messageContext.setAxisService(serverConfigContext
		        .getAxisConfiguration().getService("RMSampleService"));
		messageContext.setEnvelope(dummyEnvelope);

		RMMsgContext applicationRMMsg = new RMMsgContext(messageContext);
		EndpointReference toEPR = new EndpointReference(to);
		applicationRMMsg.setTo(toEPR);

		// Create an RMSBean so the create sequence message can be created
		RMSBean rmsBean = new RMSBean();
		rmsBean.setRMVersion(Sandesha2Constants.SPEC_VERSIONS.v1_1);
		rmsBean.setToEndpointReference(toEPR);
		rmsBean.setAcksToEndpointReference(new EndpointReference(
		        AddressingConstants.Final.WSA_NONE_URI));

		// Create a Create Sequence message
		// generating a new create sequeuce message.
		RMMsgContext makeConnectionRMMessage = RMMsgCreator.createMakeConnectionMessage(applicationRMMsg, rmsBean, "TempSeqId", "ANON URI");
		messageContext = makeConnectionRMMessage.getMessageContext();
		messageContext
		        .setWSAAction(SpecSpecificConstants
		                .getCreateSequenceAction(Sandesha2Constants.SPEC_VERSIONS.v1_1));

		MakeConnection makeConnectionPart = makeConnectionRMMessage.getMakeConnection();

		makeConnectionPart.setIdentifier(null);
		makeConnectionPart.setAddress(null);

		// Update the SOAP Envelope of the message
		makeConnectionRMMessage.addSOAPEnvelope();

		SOAPEnvelope envelope = makeConnectionRMMessage.getMessageContext()
		        .getEnvelope();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		envelope.serialize(outputStream);

		return outputStream.toByteArray();
	}
}
