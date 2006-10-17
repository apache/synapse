package org.apache.sandesha2.scenarios;

import java.io.File;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.async.AsyncResult;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;

public class OptionalReliabilityTest extends SandeshaTestCase {

	SimpleHTTPServer httpServer = null;
	
	private final static String applicationNamespaceName = "http://tempuri.org/"; 
	private final static String echoString = "echoString";
	private final static String ping = "ping";
	private final static String Text = "Text";
	private final static String Sequence = "Sequence";
	private final static String echoStringResponse = "echoStringResponse";
	private final static String EchoStringReturn = "EchoStringReturn";
	int serverPort = DEFAULT_SERVER_TEST_PORT;
	private Log log = LogFactory.getLog(getClass());
	
	public OptionalReliabilityTest() {
		super ("OptionalReliabilityTest");
	}
	
	public void setUp () throws AxisFault {
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "server";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";


		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		String serverPortStr = getTestProperty("test.server.port");
		if (serverPortStr!=null) {
		
			try {
				serverPort = Integer.parseInt(serverPortStr);
			} catch (NumberFormatException e) {
				log.error(e);
			}
		}
		
		httpServer = new SimpleHTTPServer (configContext,serverPort);
		httpServer.start();
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			throw new SandeshaException ("sleep interupted");
		}
	}
	
	public void tearDown () throws SandeshaException {
		if (httpServer!=null)
			httpServer.stop();
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			throw new SandeshaException ("sleep interupted");
		}
	}

	public void testPing () throws AxisFault, InterruptedException {
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
		ServiceClient serviceClient = new ServiceClient (configContext,null);

		Options clientOptions = new Options ();
		clientOptions.setAction("ping");
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportTo);
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
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
		ServiceClient serviceClient = new ServiceClient (configContext,null);

		Options clientOptions = new Options ();
		clientOptions.setAction("echo");
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportTo);
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
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
		ServiceClient serviceClient = new ServiceClient (configContext,null);

		Options clientOptions = new Options ();
		clientOptions.setAction("echo");
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportTo);
		clientOptions.setProperty(SandeshaClientConstants.UNRELIABLE_MESSAGE,"true");
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);
		serviceClient.setOptions(clientOptions);
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1", "async"),callback1);

		Thread.sleep(4000);
		
    //assertions for the out sequence.
		SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		assertTrue(sequenceReport.getCompletedMessages().isEmpty());
		
		assertTrue(callback1.isComplete());
		assertEquals(callback1.getResult(),"echo1");
		
		//assertions for the in sequence
		List inboundReports = SandeshaClient.getIncomingSequenceReports(configContext);
		assertTrue(inboundReports.isEmpty());
		
		configContext.getListenerManager().stop();
		serviceClient.cleanup();
	}
	
	private static OMElement getEchoOMBlock(String text, String appSeq) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace applicationNamespace = fac.createOMNamespace(applicationNamespaceName,"ns1");
		OMElement echoStringElement = fac.createOMElement(echoString, applicationNamespace);
		OMElement textElem = fac.createOMElement(Text,applicationNamespace);
		OMElement sequenceElem = fac.createOMElement(Sequence,applicationNamespace);
		
		textElem.setText(text);
		sequenceElem.setText(appSeq);
		echoStringElement.addChild(textElem);
		echoStringElement.addChild(sequenceElem);
		
		return echoStringElement;
	}

	private static OMElement getPingOMBlock(String text) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace namespace = fac.createOMNamespace(applicationNamespaceName,"ns1");
		OMElement pingElem = fac.createOMElement(ping, namespace);
		OMElement textElem = fac.createOMElement(Text, namespace);
		
		textElem.setText(text);
		pingElem.addChild(textElem);

		return pingElem;
	}

	private static String checkEchoOMBlock(OMElement response) {
		assertEquals(response.getNamespace().getName(), applicationNamespaceName);
		assertEquals(response.getLocalName(), echoStringResponse);
		
		OMElement echoStringReturnElem = response.getFirstChildWithName(new QName (applicationNamespaceName,EchoStringReturn));
		assertNotNull(echoStringReturnElem);
		
		String resultStr = echoStringReturnElem.getText();

		return resultStr;
	}
	
	class TestCallback extends Callback {

		String name = null;
		boolean completed = false;
		boolean errorRported = false;
		String resultStr;
		
		public boolean isCompleted() {
			return completed;
		}

		public boolean isErrorRported() {
			return errorRported;
		}

		public String getResult () {
			return resultStr;
		}
		
		public TestCallback (String name) {
			this.name = name;
		}
		
		public void onComplete(AsyncResult result) {
			//System.out.println("On Complete Called for " + text);
			SOAPBody body = result.getResponseEnvelope().getBody();
			this.resultStr = checkEchoOMBlock(body.getFirstElement());
			completed = true;
		}

		public void onError (Exception e) {
			e.printStackTrace();
			errorRported = true;
		}
	}

}
