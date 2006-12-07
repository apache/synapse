package org.apache.sandesha2.scenarios;

import java.io.File;
import java.util.List;

import org.apache.axiom.om.OMElement;
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

public class OptionalReliabilityTest extends SandeshaTestCase {

	public OptionalReliabilityTest() {
		super ("OptionalReliabilityTest");
	}
	
	public void setUp () throws Exception {
		super.setUp();

		String repoPath = "target" + File.separator + "repos" + File.separator + "server";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";

		startServer(repoPath, axis2_xml);
	}
	
	public void testPing () throws Exception {
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
		ServiceClient serviceClient = new ServiceClient (configContext,null);

		Options clientOptions = new Options ();
		clientOptions.setAction(pingAction);
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(SandeshaClientConstants.UNRELIABLE_MESSAGE, "true");
		serviceClient.setOptions(clientOptions);
		
		serviceClient.fireAndForget(getPingOMBlock("echo1"));
		
		//assertions for the out sequence.
		SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		assertTrue(sequenceReport.getCompletedMessages().isEmpty());
		
		//assertions for the in sequence
		List inboundReports = SandeshaClient.getIncomingSequenceReports(configContext);
		assertTrue(inboundReports.isEmpty());
		
		configContext.getListenerManager().stop();
		serviceClient.cleanup();
	}

	public void testSyncEcho () throws AxisFault, InterruptedException {
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
		ServiceClient serviceClient = new ServiceClient (configContext,null);

		Options clientOptions = new Options ();
		clientOptions.setAction(echoAction);
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(SandeshaClientConstants.UNRELIABLE_MESSAGE,"true");
		serviceClient.setOptions(clientOptions);
		
		OMElement result = serviceClient.sendReceive(getEchoOMBlock("echo1", "sync"));
		
		// Check the response
		String echoStr = checkEchoOMBlock(result);
		assertEquals(echoStr, "echo1");
		
		//assertions for the out sequence.
		SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		assertTrue(sequenceReport.getCompletedMessages().isEmpty());
		
		//assertions for the in sequence
		List inboundReports = SandeshaClient.getIncomingSequenceReports(configContext);
		assertTrue(inboundReports.isEmpty());
		
		configContext.getListenerManager().stop();
		serviceClient.cleanup();
	}

	public void testAsyncEcho () throws AxisFault, InterruptedException {
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
		ServiceClient serviceClient = new ServiceClient (configContext,null);

		Options clientOptions = new Options ();
		clientOptions.setAction(echoAction);
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(SandeshaClientConstants.UNRELIABLE_MESSAGE,"true");
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);
		serviceClient.setOptions(clientOptions);
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1", "async"),callback1);

		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
				//assertions for the out sequence.
				SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertTrue(sequenceReport.getCompletedMessages().isEmpty());
				
				assertTrue(callback1.isComplete());
				assertEquals(callback1.getResult(),"echo1");
				
				//assertions for the in sequence
				List inboundReports = SandeshaClient.getIncomingSequenceReports(configContext);
				assertTrue(inboundReports.isEmpty());
				
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
