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

package sandesha2.samples.userguide;

import java.io.File;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.util.SandeshaUtil;

public class AsyncEchoBlockingClient {
	
	private final static String applicationNamespaceName = "http://tempuri.org/"; 
	private final static String echoString = "echoString";
	private final static String Text = "Text";
	private final static String Sequence = "Sequence";
	private final static String EchoStringReturn = "EchoStringReturn";
	
	private String toIP = "127.0.0.1";
	
	private String toPort = "8070";
	
	private String transportToPort = "8070";
	
	private String toEPR = "http://" + toIP +  ":" + toPort + "/axis2/services/RMSampleService";
	
	private String transportToEPR = "http://" + toIP +  ":" + transportToPort + "/axis2/services/RMSampleService";
	
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
		
		new AsyncEchoBlockingClient ().run();
	}
	
	private void run () throws Exception {
		
		if ("<SANDESHA2_HOME>".equals(SANDESHA2_HOME)){
			System.out.println("ERROR: Please set the directory you unzipped Sandesha2 as the first option.");
			return;
		}

		String axis2_xml = AXIS2_CLIENT_PATH + "client_axis2.xml";
     
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(AXIS2_CLIENT_PATH,axis2_xml);

		ServiceClient serviceClient = new ServiceClient (configContext,null);	
		
		Options clientOptions = new Options ();
		
		clientOptions.setTo(new EndpointReference (toEPR));
		
		String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress() + "/" + ServiceClient.ANON_OUT_IN_OP;
		clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
		
		String sequenceKey = "sequence4";
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportToEPR);
		
//		clientOptions.setProperty(MessageContextConstants.CHUNKED,Constants.VALUE_FALSE);   //uncomment this to send messages without chunking.
		
		clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);   //uncomment this to send messages in SOAP 1.2
		
//		clientOptions.setProperty(SandeshaClient.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_1);  //uncomment this to send the messages according to the v1_1 spec.
		
		clientOptions.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,AddressingConstants.Submission.WSA_NAMESPACE);
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,SandeshaUtil.getUUID());  //Uncomment this to offer a sequenceID for the incoming sequence.
		clientOptions.setAction("urn:wsrm:EchoString");
		
		//You must set the following two properties in the request-reply case.
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);
		
		serviceClient.setOptions(clientOptions);

		clientOptions.setTimeOutInMilliSeconds(40000);
		
		OMElement result = serviceClient.sendReceive (getEchoOMBlock("echo1",sequenceKey));
		showResult(result);
		
		result = serviceClient.sendReceive (getEchoOMBlock("echo2",sequenceKey));
		showResult(result);
		
		result = serviceClient.sendReceive(getEchoOMBlock("echo3",sequenceKey));
		showResult(result);
		
		result = serviceClient.sendReceive(getEchoOMBlock("echo4",sequenceKey));
		showResult(result);
		
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		OMElement bodyElem = getEchoOMBlock("echo5",sequenceKey);
		
		result = serviceClient.sendReceive(bodyElem);
		showResult(result);
		        
        Thread.sleep(4000);
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
		
		OMNamespace namespace = fac.createOMNamespace(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI,"env");
		OMAttribute attr = fac.createOMAttribute("encodingStyle", namespace,"http://schemas.xmlsoap.org/soap/encoding/");
		
		echoStringElement.addAttribute(attr);
		
		return echoStringElement;
	}

    private void showResult (OMElement echoStringResponseElem) {
		
		OMElement echoStringReturnElem = echoStringResponseElem.getFirstChildWithName(new QName (applicationNamespaceName,EchoStringReturn));
		if (echoStringReturnElem==null) { 
			System.out.println("Error: 'echoStringResponse' element does not have a 'EchoStringReturn' child");
			return;
		}
		
		String resultStr = echoStringReturnElem.getText();
		System.out.println("Got result:" + resultStr);
    }	
}
