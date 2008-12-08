/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.sandesha2.interop.rm1_1_clients;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;
import org.apache.sandesha2.interop.RMInteropServiceStub;
import org.tempuri.Ping;

public class Scenario_1_3 {

	private static final String applicationNamespaceName = "http://tempuri.org/"; 
	private static final String ping = "ping";
	private static final String Text = "Text";
	
	private static String toIP = "127.0.0.1";
	private static String toPort = "9762";
	private static String transportToIP = "127.0.0.1";
	private static String transportToPort = "8070";
	private static String servicePart = "/axis2/services/RMInteropService";
	private static String toEPR = "http://" + toIP +  ":" + toPort + servicePart;
	private static String transportToEPR = "http://" + transportToIP +  ":" + transportToPort + servicePart;
	
	private static String SANDESHA2_HOME = "<SANDESHA2_HOME>"; //Change this to ur path.
	
	private static String AXIS2_CLIENT_PATH = SANDESHA2_HOME + File.separator + "target" + File.separator +"repos" + File.separator + "client" + File.separator;   //this will be available after a maven build
	
	public static void main(String[] args) throws Exception {
		
		String axisClientRepo = null;
		if (args!=null && args.length>0)
			axisClientRepo = args[0];
		
		if (axisClientRepo!=null && !"".equals(axisClientRepo)) {
			AXIS2_CLIENT_PATH = axisClientRepo;
			SANDESHA2_HOME = "";
		}
		
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("sandesha2_interop.properties");

		Properties properties = new Properties();
		if (in != null) {
			properties.load(in);
			
			toEPR = properties.getProperty("to");
			transportToEPR = properties.getProperty("transportTo");
		}
		
//		new Scenario_1_3 ().run();
		new Scenario_1_3 ().runStubBased ();
		
		Thread.sleep(60000);
	}
	
	private void run () throws Exception {
		

		ConfigurationContext configurationContext = generateConfigContext();
		
		Options clientOptions = new Options ();
		setUpOptions(clientOptions);
		
		ServiceClient serviceClient = new ServiceClient (configurationContext,null);		
		serviceClient.setOptions(clientOptions);
		
		clientOptions.setProperty(SandeshaClientConstants.MESSAGE_NUMBER,new Long(1));
		serviceClient.fireAndForget(getPingOMBlock("ping1"));
		
		clientOptions.setProperty(SandeshaClientConstants.MESSAGE_NUMBER,new Long(3));
		serviceClient.fireAndForget(getPingOMBlock("ping3"));
		
		boolean complete = false;
		while (!complete) {
			SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
			if (sequenceReport!=null && sequenceReport.getCompletedMessages().size()==2) 
				complete = true;
			else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} 
		}

		SandeshaClient.closeSequence(serviceClient);
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		SandeshaClient.terminateSequence(serviceClient);
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	
		configurationContext.getListenerManager().stop();
		serviceClient.cleanup();
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
	
	private void runStubBased () throws Exception {
		
		ConfigurationContext configurationContext = generateConfigContext();
		RMInteropServiceStub stub = new RMInteropServiceStub (configurationContext,toEPR);
		ServiceClient stubServiceClient = stub._getServiceClient();
		
		Options options = stubServiceClient.getOptions();
		setUpOptions(options);
		
		options.setProperty(SandeshaClientConstants.MESSAGE_NUMBER,new Long(1));
		Ping ping = new Ping ();
		ping.setText("ping1");
		stub.Ping(ping);
		
		options.setProperty(SandeshaClientConstants.MESSAGE_NUMBER,new Long(3));
		ping = new Ping ();
		ping.setText("ping2");
		stub.Ping(ping);
		
		boolean complete = false;
		while (!complete) {
			SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(stubServiceClient);
			if (sequenceReport!=null && sequenceReport.getCompletedMessages().size()==2) 
				complete = true;
			else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} 
		}

		SandeshaClient.closeSequence(stubServiceClient);
		
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		SandeshaClient.terminateSequence(stubServiceClient);
		
		try {
			Thread.sleep(3000000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		stubServiceClient.cleanup();
		
		
	}
	
	private ConfigurationContext generateConfigContext () throws Exception {
		if ("<SANDESHA2_HOME>".equals(SANDESHA2_HOME)){
			System.out.println("ERROR: Please change <SANDESHA2_HOME> to your Sandesha2 installation directory.");
			throw new Exception ("Client not set up correctly");
		}
		
		String axis2_xml = AXIS2_CLIENT_PATH + "client_axis2.xml";
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(AXIS2_CLIENT_PATH,axis2_xml);

		return configContext;
	}
	
	private void setUpOptions (Options clientOptions) {
		clientOptions.setProperty(Constants.Configuration.TRANSPORT_URL,transportToEPR);
//		clientOptions.setProperty(Options.COPY_PROPERTIES, new Boolean (true));
		clientOptions.setTo(new EndpointReference (toEPR));
		
		String sequenceKey = "sequence1";
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
	    
//		clientOptions.setProperty(MessageContextConstants.CHUNKED,Constants.VALUE_FALSE);   //uncomment this to send messages without chunking.
		clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);   //uncomment this to send messages in SOAP 1.2
//		clientOptions.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,AddressingConstants.Submission.WSA_NAMESPACE);
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_1);  //uncomment this to send the messages according to the v1_1 spec.
		
		clientOptions.setAction("urn:wsrm:Ping");
		
	}
	
}
