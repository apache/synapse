/*
 * Copyright 2007 The Apache Software Foundation.
 * Copyright 2007 International Business Machines Corp.
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
package org.apache.sandesha2.scenarios;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import junit.framework.AssertionFailedError;

import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;
import org.apache.sandesha2.util.SandeshaUtil;

public class RMScenariosTest extends SandeshaTestCase {

	private static boolean serverStarted = false; 
	private static ConfigurationContext configContext = null;

	protected String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
	
	protected String repoPath = "target" + File.separator + "repos" + File.separator + "server";
	protected String axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";

	protected String repoPathClient = "target" + File.separator + "repos" + File.separator + "client";
	protected String axis2_xmlClient = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
	
	public RMScenariosTest () {
		super ("RMScenariosTest");
	}
	
	public RMScenariosTest (String name) {
		super(name);
	}

	public void setUp () throws Exception {
		super.setUp();

		if (!serverStarted) {
			startServer(repoPath, axis2_xml);
			configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPathClient,axis2_xmlClient);
		}
		serverStarted = true;
	}
	
	/**
	 * Override the teardown processing
	 */
	public void tearDown () {
	
	}

	public void testPing() throws Exception  {
		// Run a ping test with sync acks
		runPing(false);
		
		// Run a ping test with async acks
		runPing(true);
	}

	public void testAsyncEcho() throws Exception {
		// Test async echo with sync acks
		Options clientOptions = new Options();
		runEcho(clientOptions, true, false, false);
		
		// Test async echo with async acks
		clientOptions = new Options();
		runEcho(clientOptions, true, true, false);
		
		// Test async echo with async acks and offer
		clientOptions = new Options();
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,SandeshaUtil.getUUID());
		runEcho(clientOptions, true, true, false);
	}
		
	public void testSyncEcho() throws Exception {
		// Test sync echo with an offer, and the 1.1 spec
		Options clientOptions = new Options();
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,SandeshaUtil.getUUID());
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_1);
		runEcho(clientOptions, false, false, false);
		
		// Test sync echo with an offer, and the 1.0 spec
		clientOptions = new Options();
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,SandeshaUtil.getUUID());
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_0);
		runEcho(clientOptions, false, false, true);
		
		// Test sync echo with no offer, and the 1.1 spec
		clientOptions = new Options();
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_1);
		runEcho(clientOptions, false, false, false);
	}

	public void runPing(boolean asyncAcks) throws Exception {
		
		Options clientOptions = new Options();

		ServiceClient serviceClient = new ServiceClient (configContext,null);
		serviceClient.setOptions(clientOptions);

		String sequenceKey = SandeshaUtil.getUUID();

		clientOptions.setAction(pingAction);
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		
		if(asyncAcks) {
			String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
			clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
			clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
			clientOptions.setUseSeparateListener(true);
		}

		serviceClient.fireAndForget(getPingOMBlock("ping1"));
		
		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
				SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertTrue(sequenceReport.getCompletedMessages().contains(new Long(1)));
				assertEquals(sequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
				assertEquals(sequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);

				lastError = null;
				break;
			} catch(Error e) {
				lastError = e;
			}
		}

		if(lastError != null) throw lastError;

		serviceClient.cleanup();
	}

	public void runEcho(Options clientOptions, boolean asyncReply, boolean asyncAcks, boolean explicitTermination) throws Exception {
		
		String sequenceKey = SandeshaUtil.getUUID();

		ServiceClient serviceClient = new ServiceClient (configContext,null);
		serviceClient.setOptions(clientOptions);

		clientOptions.setAction(echoAction);
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);

		if(asyncReply || asyncAcks) {
			clientOptions.setUseSeparateListener(true);
			
			if(asyncAcks) {
				String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
				clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
			} else {
				String acksTo = AddressingConstants.Final.WSA_ANONYMOUS_URL;
				clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
			}
		}
		
		if(asyncAcks) {
			String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
			clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
		} else {
			String acksTo = AddressingConstants.Final.WSA_ANONYMOUS_URL;
			clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
		}

		if (explicitTermination) {
			clientOptions.setProperty(SandeshaClientConstants.AVOID_AUTO_TERMINATION, Boolean.TRUE);
		} else {
			clientOptions.setProperty(SandeshaClientConstants.AVOID_AUTO_TERMINATION, Boolean.FALSE);
		}
		
		// Establish a baseline count for inbound sequences
		ArrayList oldIncomingReports = SandeshaClient.getIncomingSequenceReports(configContext);
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1",sequenceKey),callback1);
		
		TestCallback callback2 = new TestCallback ("Callback 2");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo2",sequenceKey),callback2);
		
		TestCallback callback3 = new TestCallback ("Callback 3");
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo3",sequenceKey),callback3);
		
		if (explicitTermination) {
			Thread.sleep(6000);
			SandeshaClient.terminateSequence(serviceClient);
		}
		
		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
				
		        //assertions for the out sequence.
				SequenceReport outgoingSequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				System.out.println("Checking Outbound Sequence: " + outgoingSequenceReport.getSequenceID());
				assertEquals ("Outbound message count", 3, outgoingSequenceReport.getCompletedMessages().size());
				assertTrue("Outbound message #1", outgoingSequenceReport.getCompletedMessages().contains(new Long(1)));
				assertTrue("Outbound message #2", outgoingSequenceReport.getCompletedMessages().contains(new Long(2)));
				assertTrue("Outbound message #3", outgoingSequenceReport.getCompletedMessages().contains(new Long(3)));
				assertEquals("Outbound sequence status: TERMINATED", SequenceReport.SEQUENCE_STATUS_TERMINATED, outgoingSequenceReport.getSequenceStatus());
				assertEquals("Outbound sequence direction: OUT", SequenceReport.SEQUENCE_DIRECTION_OUT, outgoingSequenceReport.getSequenceDirection());
				
				//assertions for the inbound sequence. The one we care about is a new sequence,
				//so it will not exist in the oldSequences list.
				ArrayList incomingSequences = SandeshaClient.getIncomingSequenceReports(configContext);
				SequenceReport incomingSequenceReport = getNewReport(incomingSequences, oldIncomingReports);
				System.out.println("Checking Inbound Sequence: " + incomingSequenceReport.getSequenceID());
				String offer = (String) clientOptions.getProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID);
				if(offer != null) assertEquals("Inbound seq id", offer, incomingSequenceReport.getSequenceID());
				assertEquals ("Inbound message count", 3, incomingSequenceReport.getCompletedMessages().size());
				assertTrue("Inbound message #1", incomingSequenceReport.getCompletedMessages().contains(new Long(1)));
				assertTrue("Inbound message #2", incomingSequenceReport.getCompletedMessages().contains(new Long(2)));
				assertTrue("Inbound message #3", incomingSequenceReport.getCompletedMessages().contains(new Long(3)));
				assertEquals("Inbound sequence status: TERMINATED", SequenceReport.SEQUENCE_STATUS_TERMINATED, incomingSequenceReport.getSequenceStatus());
				assertEquals("Inbound sequence direction: IN", SequenceReport.SEQUENCE_DIRECTION_IN, incomingSequenceReport.getSequenceDirection());
				
				assertTrue("Callback #1", callback1.isComplete());
				assertEquals("Callback #1 data", "echo1", callback1.getResult());
				
				assertTrue("Callback #2", callback2.isComplete());
				assertEquals("Callback #2 data", "echo1echo2", callback2.getResult());
				
				assertTrue("Callback #3", callback3.isComplete());
				assertEquals("Callback #3 data", "echo1echo2echo3", callback3.getResult());
				
				lastError = null;
				break;
			} catch(Error e) {
				System.out.println("Possible error:" + e);
				lastError = e;
			}
		}
		if(lastError != null) throw lastError;
		
		serviceClient.cleanup();
	}

	// Scan through lists of old and new incoming sequences, to find the sequence that
	// was established by this test. Note that some of the old sequences may have timed out.
	private SequenceReport getNewReport(ArrayList incomingSequences, ArrayList oldIncomingReports) {
		HashSet sequenceIds = new HashSet();
		for(Iterator oldSequences = oldIncomingReports.iterator(); oldSequences.hasNext(); ) {
			SequenceReport report = (SequenceReport) oldSequences.next();
			sequenceIds.add(report.getSequenceID());
		}
		for(Iterator currentSequences = incomingSequences.iterator(); currentSequences.hasNext(); ) {
			SequenceReport report = (SequenceReport) currentSequences.next();
			if(!sequenceIds.contains(report.getSequenceID())) {
				return report;
			}
		}
		throw new AssertionFailedError("Failed to find a new reply sequence");
	}


}
