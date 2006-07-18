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

package org.apache.sandesha2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.namespace.QName;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMText;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisOperationFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.receivers.RawXMLINOnlyMessageReceiver;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.axis2.wsdl.WSDLConstants.WSDL20_2004Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;

public class MTOMRMTest extends SandeshaTestCase {

	SimpleHTTPServer httpServer = null;
	private final String applicationNamespaceName = "http://tempuri.org/"; 
	private final String MTOMping = "MTOMping";
	private final String Text = "Text";
	private final String Attachment = "Attachment";
	private final String TEST_STRING = "Text to be sent using MTOM";
	
	private final String PING_OPERATION_NAME = "ping";

	private Log log = LogFactory.getLog(getClass());
	int serverPort = DEFAULT_SERVER_TEST_PORT;
	
	public MTOMRMTest () {
        super ("MTOMRMTest");
	}
	
	public void setUp () throws AxisFault {
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "server";
		String axis2_xml = "test-resources" + File.separator + "server_mtom_axis2.xml";

		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
		
		AxisConfiguration axisConfiguration = configContext.getAxisConfiguration();
		AxisService axisService = axisConfiguration.getService("RMSampleService");
		AxisOperation operation = AxisOperationFactory.getAxisOperation(WSDL20_2004Constants.MEP_CONSTANT_IN_ONLY);
		operation.setMessageReceiver(new MTOMTestMessageReceiver ()); 
		operation.setName(new QName (MTOMping));
		axisService.addOperation(operation);
		
		AxisOperation pingOperation = axisService.getOperation(new QName (PING_OPERATION_NAME));
		if (pingOperation==null)
			throw new AxisFault ("Cant find the ping operation");
		
		//setting the operation specific phase chain
		operation.setRemainingPhasesInFlow(pingOperation.getRemainingPhasesInFlow());
		
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
	
	public void testMTOMPing () throws AxisFault,InterruptedException  {
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		String transportTo = "http://127.0.0.1:" + "8070" + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "test-resources" + File.separator + "client_mtom_axis2.xml";

		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();
		clientOptions.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportTo);
		
		String sequenceKey = "sequence1";
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		clientOptions.setAction("http://testAction");

		serviceClient.setOptions(clientOptions);
		
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		serviceClient.fireAndForget(getMTOMPingOMBlock());
		
		Thread.sleep(10000);
				
		SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		assertTrue(sequenceReport.getCompletedMessages().contains(new Long(1)));
		assertEquals(sequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
		assertEquals(sequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
	
		serviceClient.finalizeInvoke();
	}
	
	private OMElement getMTOMPingOMBlock() throws AxisFault {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace namespace = fac.createOMNamespace(applicationNamespaceName,"ns1");
		OMElement pingElem = fac.createOMElement(MTOMping, namespace);
		
		OMElement attachElem = fac.createOMElement(Attachment,namespace);
		
		String text = TEST_STRING;
	 	byte[] bytes =  text.getBytes();
	 	
	 	DataSource dataSource = new ByteArrayDataSource (bytes);
	    DataHandler dataHandler = new DataHandler(dataSource);

	    OMText textData = fac.createOMText(dataHandler, true);
	    attachElem.addChild(textData);
		pingElem.addChild(attachElem);

		return pingElem;
	}
	
	private class MTOMTestMessageReceiver extends RawXMLINOnlyMessageReceiver  {
		
		public void invokeBusinessLogic(MessageContext msgContext) throws AxisFault {
			doInvocation (msgContext);
		}
		
	}
	
	private void doInvocation (MessageContext mc) throws AxisFault {
		System.out.println("Invoked...");
		SOAPEnvelope envelope = mc.getEnvelope();
		
		assertNotNull(envelope);
		
		SOAPBody body = envelope.getBody();
		
		OMElement payload = body.getFirstElement();
		OMElement attachmentElem = payload.getFirstChildWithName(new QName (applicationNamespaceName,Attachment));
		if (attachmentElem==null)
			throw new AxisFault ("'Attachment' element is not present as a child of the 'Ping' element");
		
		OMText binaryElem = (OMText) attachmentElem.getFirstOMChild();
		
		binaryElem.setOptimize(true);
        DataHandler dataHandler = (DataHandler) binaryElem.getDataHandler();
        
        try {
			InputStream stream = dataHandler.getInputStream();
			byte[] bytes = new byte [100];
			int size = stream.read(bytes);
			
			
			if (bytes==null)
				throw new AxisFault ("No data was present");
			
			String str = new String (bytes,0,size);
			assertEquals(str,TEST_STRING);
			
		} catch (IOException e) {
			throw new AxisFault (e);
		}
	}
		
}
