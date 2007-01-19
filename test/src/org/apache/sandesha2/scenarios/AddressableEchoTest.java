package org.apache.sandesha2.scenarios;

import java.io.File;
import java.util.ArrayList;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
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

public class AddressableEchoTest extends SandeshaTestCase {

	int serverPort = DEFAULT_SERVER_TEST_PORT;
	private static boolean serverStarted = false; 
	private static ConfigurationContext configContext = null;
	
	public AddressableEchoTest () {
		super ("AddressableEchoTest");
	}
	
	public void setUp () throws Exception {
		super.setUp();

		String repoPath = "target" + File.separator + "repos" + File.separator + "server";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";

		if (!serverStarted) {
			startServer(repoPath, axis2_xml);
			String repoPathClient = "target" + File.separator + "repos" + File.separator + "client";
			String axis2_xmlClient = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
			configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPathClient,axis2_xmlClient);

		}
		serverStarted = true;
	}
	
	/**
	 * Override the teardown processing
	 */
	public void tearDown () {
	
	}

	public void testAsyncEcho () throws Exception {
	
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";		

		Options clientOptions = new Options ();
		clientOptions.setAction(echoAction);
		clientOptions.setTo(new EndpointReference (to));
		
		String sequenceKey = SandeshaUtil.getUUID();
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		
		String acksTo = null;
		clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		
		serviceClient.setOptions(clientOptions);
		
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);
		
		clientOptions.setAction("urn:wsrm:EchoString");
		
		serviceClient.setOptions(clientOptions);
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1",sequenceKey),callback1);
		
		TestCallback callback2 = new TestCallback ("Callback 2");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo2",sequenceKey),callback2);
		
		TestCallback callback3 = new TestCallback ("Callback 3");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo3",sequenceKey),callback3);
		
		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
		        //assertions for the out sequence.
				SequenceReport outgoingSequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertEquals (outgoingSequenceReport.getCompletedMessages().size(),3);
				assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(1)));
				assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(2)));
				assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(3)));
				assertEquals(outgoingSequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_ESTABLISHED);
				assertEquals(outgoingSequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
				
				ArrayList incomingReports = SandeshaClient.getIncomingSequenceReports(configContext);
				assertEquals(incomingReports.size(),1);
				SequenceReport incomingSequenceReport = (SequenceReport) incomingReports.get(0);
				assertEquals (incomingSequenceReport.getCompletedMessages().size(),3);
				assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(1)));
				assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(2)));
				assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(3)));
				assertEquals(incomingSequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_IN);

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
		
		SandeshaClient.terminateSequence(serviceClient, sequenceKey);
		
		SandeshaClient.waitUntilSequenceCompleted(serviceClient, sequenceKey);
		
    //assertion that the out sequence is terminated.
		SequenceReport outgoingSequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		assertEquals(outgoingSequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
	}
	
	public void testAsyncEchoWithOffer () throws AxisFault, InterruptedException {
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		String sequenceKey = SandeshaUtil.getUUID();
		String offeredSequenceId = SandeshaUtil.getUUID();
		Options clientOptions = new Options ();
		clientOptions.setAction(echoAction);
		clientOptions.setTo(new EndpointReference (to));		
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		clientOptions.setProperty(SandeshaClientConstants.AcksTo,null);
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,offeredSequenceId);		
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);	
		clientOptions.setAction("urn:wsrm:EchoString");
		
		serviceClient.setOptions(clientOptions);
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1",sequenceKey),callback1);
		
		TestCallback callback2 = new TestCallback ("Callback 2");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo2",sequenceKey),callback2);
		
		TestCallback callback3 = new TestCallback ("Callback 3");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo3",sequenceKey),callback3);

		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
		        //assertions for the out sequence.
				SequenceReport outgoingSequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertEquals (outgoingSequenceReport.getCompletedMessages().size(),3);
				assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(1)));
				assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(2)));
				assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(3)));
				assertEquals(outgoingSequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
				
				SequenceReport incomingSequenceReport = SandeshaClient.getIncomingSequenceReport(offeredSequenceId,configContext);
				assertEquals (incomingSequenceReport.getCompletedMessages().size(),3);
				assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(1)));
				assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(2)));
				assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(3)));
				assertEquals(incomingSequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_IN);
				
				assertTrue(callback1.isComplete());
				assertNotNull (callback1.getResult(),"echo1");
				
				assertTrue(callback2.isComplete());
				assertNotNull (callback2.getResult(),"echo1echo2");
				
				assertTrue(callback3.isComplete());
				assertNotNull (callback3.getResult(),"echo1echo2echo3");
		
				lastError = null;
				break;
			} catch(Error e) {
				lastError = e;
			}
		}

		if(lastError != null) throw lastError;

		SandeshaClient.terminateSequence(serviceClient, sequenceKey);
		
		SandeshaClient.waitUntilSequenceCompleted(serviceClient, sequenceKey);
		
    //assertion that the out sequence is terminated.
		SequenceReport outgoingSequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		assertEquals(outgoingSequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
				
		configContext.getListenerManager().stop();
		serviceClient.cleanup();
	}

}
