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
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.async.AsyncResult;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;
import org.apache.sandesha2.interop.RMInteropServiceCallbackHandler;
import org.apache.sandesha2.interop.RMInteropServiceCallbackHandlerImpl;
import org.apache.sandesha2.interop.RMInteropServiceStub;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.inmemory.InMemorySenderBeanMgr;
import org.apache.sandesha2.util.SandeshaUtil;
import org.tempuri.EchoString;
import org.tempuri.EchoStringRequestBodyType;

public class Scenario_3_1 {
	
	private final static String applicationNamespaceName = "http://tempuri.org/"; 
	private final static String echoString = "echoString";
	private final static String Text = "Text";
	private final static String Sequence = "Sequence";
	private final static String echoStringResponse = "echoStringResponse";
	private final static String EchoStringReturn = "EchoStringReturn";
	
	private static String toIP = "127.0.0.1";
	private static String toPort = "8080";
	private static String transportToIP = "127.0.0.1";
	private static String transportToPort = "8070";
	private static String servicePart = "/axis2/services/RMInteropService";
	private static String toAddress = "http://" + toIP +  ":" + toPort + servicePart;
	private static String transportToEPR = "http://" + transportToIP +  ":" + transportToPort + servicePart;
	
	private static String SANDESHA2_HOME = "<SANDESHA2_HOME>"; //Change this to ur path.
	
	private static String AXIS2_CLIENT_PATH = SANDESHA2_HOME + File.separator + "target" + File.separator +"repos" + File.separator + "client" + File.separator;   //this will be available after a maven build
	
	public static void main(String[] args) throws Exception,IOException {
		
		
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
			
			toAddress = properties.getProperty("to");
			transportToEPR = properties.getProperty("transportTo");
		}
		
//		new Scenario_2_1 ().run();
		
		
		new Scenario_3_1 ().runStubBased ();
	}
	
	private void run () throws Exception {
		
		ConfigurationContext configurationContext = getConfigurationContext();
		ServiceClient serviceClient = new ServiceClient (configurationContext,null);	
		
		Options clientOptions = new Options ();

		String sequenceKey = "sequence4";
		String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
		
		setUpOptions(clientOptions, sequenceKey,acksTo);

		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMNamespace namespace = factory.createOMNamespace("urn:wsrm:InteropOptions","rmi");
		OMElement acceptOfferElem = factory.createOMElement("acceptOffer",namespace);
		OMElement useOfferElem = factory.createOMElement("useOffer",namespace);
		acceptOfferElem.setText("true");
		useOfferElem.setText("true");
//		toEPR.addReferenceParameter(acceptOfferElem);
//		toEPR.addReferenceParameter(useOfferElem);
//		clientOptions.setManageSession(true); // without this reference params wont go.
		
		EndpointReference toEPR = new EndpointReference (toAddress);
		serviceClient.setTargetEPR(toEPR);
		
		serviceClient.setOptions(clientOptions);

		Callback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking(getEchoOMBlock("echo1",sequenceKey),callback1);
		
		Callback callback2 = new TestCallback ("Callback 2");
		serviceClient.sendReceiveNonBlocking(getEchoOMBlock("echo2",sequenceKey),callback2);
		
		Callback callback3 = new TestCallback ("Callback 3");
		serviceClient.sendReceiveNonBlocking(getEchoOMBlock("echo3",sequenceKey),callback3);
		
        while (!callback3.isComplete()) {
            Thread.sleep(1000);
        }
        
		Thread.sleep(5000);
		
        SandeshaClient.terminateSequence(serviceClient);
      			
		Thread.sleep(10000);

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
			System.out.println("Callback '" + name +  "' got result:" + resultStr);
		}

		public void onError (Exception e) {
			// TODO Auto-generated method stub
			System.out.println("Error reported for test call back");
			e.printStackTrace();
		}
	}
	
	private ConfigurationContext getConfigurationContext () throws AxisFault {

		if ("<SANDESHA2_HOME>".equals(SANDESHA2_HOME)){
			System.out.println("ERROR: Please set the directory you unzipped Sandesha2 as the first option.");
			throw new AxisFault ("Client not set up correctly");
		}

		String axis2_xml = AXIS2_CLIENT_PATH + "client_axis2.xml";
     
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(AXIS2_CLIENT_PATH,axis2_xml);
		return configContext;
	}
	
	private void setUpOptions (Options clientOptions, String sequenceKey, String acksTo) {

		EndpointReference toEPR = new EndpointReference (toAddress);
		clientOptions.setTo(toEPR);
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportToEPR);
		clientOptions.setAction("urn:wsrm:EchoString");

//		clientOptions.setProperty(MessageContextConstants.CHUNKED,Constants.VALUE_FALSE);   //uncomment this to send messages without chunking.
		clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);   //uncomment this to send messages in SOAP 1.2
//		clientOptions.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,AddressingConstants.Submission.WSA_NAMESPACE);
		
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_1);  //uncomment this to send the messages according to the v1_1 spec.
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,SandeshaUtil.getUUID());  //Uncomment this to offer a sequenceID for the incoming sequence.
		
		//You must set the following two properties in the request-reply case.
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);
		
		clientOptions.setProperty(Constants.Configuration.USE_CUSTOM_LISTNER, new Boolean (true));
				
	}
	
	
	private void runStubBased () throws Exception {

		ConfigurationContext configurationContext = getConfigurationContext();
		
		RMInteropServiceStub stub = new RMInteropServiceStub (configurationContext, toAddress);
		ServiceClient stubServiceClient = stub._getServiceClient();
		
		String sequenceKey = "sequence4";
		String acksTo = stubServiceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
		
		Options options = stubServiceClient.getOptions();
		setUpOptions(options, sequenceKey, acksTo);
		
		EchoString echoString = new EchoString ();
		echoString.setEchoString (new EchoStringRequestBodyType ());
		echoString.getEchoString().setSequence(sequenceKey);
		echoString.getEchoString().setText("echo1");
		
		options.setReplyTo(new EndpointReference (AddressingConstants.Final.WSA_ANONYMOUS_URL));
		
		RMInteropServiceCallbackHandlerImpl callback1 = new RMInteropServiceCallbackHandlerImpl ("callback1");
		stub.startEchoString(echoString, callback1);
		
		echoString = new EchoString ();
		echoString.setEchoString (new EchoStringRequestBodyType ());
		echoString.getEchoString().setSequence(sequenceKey);
		echoString.getEchoString().setText("echo2");
		
		RMInteropServiceCallbackHandlerImpl callback2 = new RMInteropServiceCallbackHandlerImpl ("callback2");
		stub.startEchoString(echoString, callback2);
		
		echoString = new EchoString ();
		echoString.setEchoString (new EchoStringRequestBodyType ());
		echoString.getEchoString().setSequence(sequenceKey);
		echoString.getEchoString().setText("echo3");
		
		RMInteropServiceCallbackHandlerImpl callback3 = new RMInteropServiceCallbackHandlerImpl ("callback3");
		stub.startEchoString(echoString, callback3);
		
		while (!callback3.isCompleted()) {
			Thread.sleep(2000);
		}

		Thread.sleep(5000);
		
        SandeshaClient.terminateSequence(stubServiceClient);
      			
		Thread.sleep(20000);
		
	}
}
