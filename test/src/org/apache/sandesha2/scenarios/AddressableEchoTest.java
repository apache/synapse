package org.apache.sandesha2.scenarios;

import java.io.File;
import java.util.ArrayList;

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
import org.apache.sandesha2.util.SandeshaUtil;

public class AddressableEchoTest extends SandeshaTestCase {

	SimpleHTTPServer httpServer = null;
	
	private final static String applicationNamespaceName = "http://tempuri.org/"; 
	private final static String echoString = "echoString";
	private final static String Text = "Text";
	private final static String Sequence = "Sequence";
	private final static String echoStringResponse = "echoStringResponse";
	private final static String EchoStringReturn = "EchoStringReturn";
	int serverPort = DEFAULT_SERVER_TEST_PORT;
	private Log log = LogFactory.getLog(getClass());
	
	public AddressableEchoTest () {
		super ("AddressableEchoTest");
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
	
	public void testAsyncEcho () throws AxisFault, InterruptedException {
	
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();

		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportTo);
		
		String sequenceKey = SandeshaUtil.getUUID();
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		
		String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
		clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		
		serviceClient.setOptions(clientOptions);
		//serviceClient.
		
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);
		
		serviceClient.setOptions(clientOptions);
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1",sequenceKey),callback1);
		
		TestCallback callback2 = new TestCallback ("Callback 2");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo2",sequenceKey),callback2);
		
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		TestCallback callback3 = new TestCallback ("Callback 3");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo3",sequenceKey),callback3);

        
        Thread.sleep(10000);
		
        //assertions for the out sequence.
		SequenceReport outgoingSequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		assertEquals (outgoingSequenceReport.getCompletedMessages().size(),3);
		assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(1)));
		assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(2)));
		assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(3)));
		assertEquals(outgoingSequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
		assertEquals(outgoingSequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
		
		ArrayList incomingReports = SandeshaClient.getIncomingSequenceReports(configContext);
		assertEquals(incomingReports.size(),1);
		SequenceReport incomingSequenceReport = (SequenceReport) incomingReports.get(0);
		assertEquals (incomingSequenceReport.getCompletedMessages().size(),3);
		assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(1)));
		assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(2)));
		assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(3)));
		assertEquals(incomingSequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
		assertEquals(incomingSequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_IN);
		
		assertTrue(callback1.isComplete());
		assertEquals(callback1.getResult(),"echo1");
		
		assertTrue(callback2.isComplete());
		assertEquals(callback2.getResult(),"echo1echo2");
		
		assertTrue(callback3.isComplete());
		assertEquals(callback3.getResult(),"echo1echo2echo3");
		
		serviceClient.finalizeInvoke();
	}
	
	public void testAsyncEchoWithOffer () throws AxisFault, InterruptedException {
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();

		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportTo);
		
		String sequenceKey = SandeshaUtil.getUUID();
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		
		String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
		clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		
		String offeredSequeiceId = SandeshaUtil.getUUID();
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,offeredSequeiceId);
		
		serviceClient.setOptions(clientOptions);
		//serviceClient.
		
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);
		
		serviceClient.setOptions(clientOptions);
		
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1",sequenceKey),callback1);
		
		TestCallback callback2 = new TestCallback ("Callback 2");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo2",sequenceKey),callback2);
		
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		TestCallback callback3 = new TestCallback ("Callback 3");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo3",sequenceKey),callback3);

        
        Thread.sleep(10000);
		
        //assertions for the out sequence.
        //assertions for the out sequence.
		SequenceReport outgoingSequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		assertEquals (outgoingSequenceReport.getCompletedMessages().size(),3);
		assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(1)));
		assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(2)));
		assertTrue(outgoingSequenceReport.getCompletedMessages().contains(new Long(3)));
		assertEquals(outgoingSequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
		assertEquals(outgoingSequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
		
		SequenceReport incomingSequenceReport = SandeshaClient.getIncomingSequenceReport(offeredSequeiceId,configContext);
		assertEquals (incomingSequenceReport.getCompletedMessages().size(),3);
		assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(1)));
		assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(2)));
		assertTrue(incomingSequenceReport.getCompletedMessages().contains(new Long(3)));
		assertEquals(incomingSequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
		assertEquals(incomingSequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_IN);
		
		assertTrue(callback1.isComplete());
		assertEquals(callback1.getResult(),"echo1");
		
		assertTrue(callback2.isComplete());
		assertEquals(callback2.getResult(),"echo1echo2");
		
		assertTrue(callback3.isComplete());
		assertEquals(callback3.getResult(),"echo1echo2echo3");
		
		serviceClient.finalizeInvoke();
	}
	
	private static OMElement getEchoOMBlock(String text, String sequenceKey) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace applicationNamespace = fac.createOMNamespace(applicationNamespaceName,"ns1");
		OMElement echoStringElement = fac.createOMElement(echoString, applicationNamespace);
		OMElement textElem = fac.createOMElement(Text,applicationNamespace);
		OMElement sequenceElem = fac.createOMElement(Sequence,applicationNamespace);
		
		textElem.setText(text);
		sequenceElem.setText(sequenceKey);
		echoStringElement.addChild(textElem);
		echoStringElement.addChild(sequenceElem);
		
		return echoStringElement;
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
			
			OMElement echoStringResponseElem = body.getFirstChildWithName(new QName (applicationNamespaceName,echoStringResponse));
			if (echoStringResponseElem==null) { 
				System.out.println("Error: SOAPBody does not have a 'echoStringResponse' child");
				return;
			}
			
			OMElement echoStringReturnElem = echoStringResponseElem.getFirstChildWithName(new QName (applicationNamespaceName,EchoStringReturn));
			if (echoStringReturnElem==null) { 
				System.out.println("Error: 'echoStringResponse' element does not have a 'EchoStringReturn' child");
				return;
			}
			
			String resultStr = echoStringReturnElem.getText();
			this.resultStr = resultStr;
			completed = true;
		}

		public void onError (Exception e) {
			e.printStackTrace();
			errorRported = true;
		}
	}

}
