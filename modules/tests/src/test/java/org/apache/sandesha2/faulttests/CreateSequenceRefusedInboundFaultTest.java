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

import java.io.File;

import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;
import org.apache.sandesha2.util.SandeshaUtil;

public class CreateSequenceRefusedInboundFaultTest extends SandeshaTestCase {
	
	private boolean startedServer = false;
	
	public CreateSequenceRefusedInboundFaultTest() {
		super("CreateSequenceRefusedInboundFaultTest");
	}

	public void setUp() throws Exception {
		super.setUp();
		String repoPath = "target" + File.separator + "repos" + File.separator + "server";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";

		if (!startedServer) {
			startServer(repoPath, axis2_xml);
			startedServer = true;
		}
	}
	
	public void tearDown()throws Exception {
		super.tearDown();
	}
	
	/** Test removed for the moment as RM 1.0 faults are not processed by the chain.
	 * The GlobalInHandler needs to detect if the message is a fault and assign an appropriate
	 * operation.
	 * @throws Exception
	 */
	public void testSOAP11CreateSequenceRefusedInboundFault () throws Exception {
		runTest(false);
	}

	public void testSOAP12CreateSequenceRefusedInboundFault () throws Exception {
		runTest(true);
	}

	private void runTest(boolean runSoap12) throws Exception{
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();
		clientOptions.setAction(echoAction);
		clientOptions.setTo(new EndpointReference (to));

		//setting the SOAP version as 1.2
		if (runSoap12)
			clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);

		String sequenceKey = SandeshaUtil.getUUID();
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		
		// Set a bad acks to so the CreateSequence will be refused.
		String acksTo = AddressingConstants.Final.WSA_NONE_URI;
		clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
		
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);		
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		serviceClient.setOptions(clientOptions);		
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		boolean caughtException = false;
		try {
			serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1",sequenceKey),callback1);
		} catch (AxisFault e) {
			caughtException = true;
		}
        
		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
		        //assertions for the out sequence.
				SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertEquals(sequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
				assertEquals(sequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
				
				if (!caughtException)
					assertTrue(callback1.isErrorReported());
				
				assertEquals(callback1.getResult(),null);
				
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
	
}



