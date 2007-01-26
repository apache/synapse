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

	private int serverPort = DEFAULT_SERVER_TEST_PORT;
	private String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
	
	private String repoPath = "target" + File.separator + "repos" + File.separator + "server";
	private String axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";

	private String repoPathClient = "target" + File.separator + "repos" + File.separator + "client";
	private String axis2_xmlClient = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
	
	public RMScenariosTest () {
		super ("RMScenariosTest");
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
		runEcho(clientOptions, true, false);
		
		// Test async echo with async acks
		clientOptions = new Options();
		runEcho(clientOptions, true, true);
		
		// Test async echo with async acks and offer
		clientOptions = new Options();
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,SandeshaUtil.getUUID());
		runEcho(clientOptions, true, true);
	}
		
	public void testSyncEcho() throws Exception {
		// Test sync echo with an offer, and the 1.1 spec
		Options clientOptions = new Options();
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,SandeshaUtil.getUUID());
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_1);
		runEcho(clientOptions, false, false);
		
		// Test sync echo with an offer, and the 1.0 spec
		clientOptions = new Options();
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,SandeshaUtil.getUUID());
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_0);
		runEcho(clientOptions, false, false);
		
		// Test sync echo with no offer, and the 1.1 spec
		clientOptions = new Options();
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_1);
		runEcho(clientOptions, false, false);
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

	public void runEcho(Options clientOptions, boolean asyncReply, boolean asyncAcks) throws Exception {
		
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

		// Establish a baseline count for inbound sequences
		ArrayList oldIncomingReports = SandeshaClient.getIncomingSequenceReports(configContext);
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1",sequenceKey),callback1);
		
		TestCallback callback2 = new TestCallback ("Callback 2");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo2",sequenceKey),callback2);
		
		TestCallback callback3 = new TestCallback ("Callback 3");
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo3",sequenceKey),callback3);
		
		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
		        //assertions for the out sequence.
				SequenceReport outgoingSequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				System.out.println("Checking Outbound Sequence: " + outgoingSequenceReport.getSequenceID());
				assertEquals (outgoingSequenceReport.getCompletedMessages().size(),3);
				assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(1)));
				assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(2)));
				assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(3)));
				assertEquals(outgoingSequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
				assertEquals(outgoingSequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
				
				//assertions for the inbound sequence. The one we care about is a new sequence,
				//so it will not exist in the oldSequences list.
				ArrayList incomingSequences = SandeshaClient.getIncomingSequenceReports(configContext);
				SequenceReport incomingSequenceReport = getNewReport(incomingSequences, oldIncomingReports);
				System.out.println("Checking Inbound Sequence: " + incomingSequenceReport.getSequenceID());
				String offer = (String) clientOptions.getProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID);
				if(offer != null) assertEquals(offer, incomingSequenceReport.getSequenceID());
				assertEquals (incomingSequenceReport.getCompletedMessages().size(),3);
				assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(1)));
				assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(2)));
				assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(3)));
				assertEquals(incomingSequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_IN);
				assertEquals(SequenceReport.SEQUENCE_STATUS_TERMINATED, incomingSequenceReport.getSequenceStatus());
				
				assertTrue(callback1.isComplete());
				assertEquals ("echo1", callback1.getResult());
				
				assertTrue(callback2.isComplete());
				assertEquals ("echo1echo2", callback2.getResult());
				
				assertTrue(callback3.isComplete());
				assertEquals ("echo1echo2echo3", callback3.getResult());
				
				lastError = null;
				break;
			} catch(Error e) {
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
