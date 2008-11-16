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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.sandesha2.client.SandeshaClientConstants;

public class UserguideEchoClient {
	
	private final static String applicationNamespaceName = "http://tempuri.org/"; 
	private final static String echoString = "echoString";
	private final static String Text = "Text";
	private final static String Sequence = "Sequence";
	private static String toEPR = "http://127.0.0.1:8070/axis2/services/RMSampleService";

	private static String CLIENT_REPO_PATH = "Full path to the Client Repo folder";
	
	public static void main(String[] args) throws Exception {
		
		String axis2_xml = CLIENT_REPO_PATH + File.separator +"client_axis2.xml";
        ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(CLIENT_REPO_PATH,axis2_xml);
		ServiceClient serviceClient = new ServiceClient (configContext,null);	
		
		Options clientOptions = new Options ();
		clientOptions.setTo(new EndpointReference (toEPR));
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);
		serviceClient.setOptions(clientOptions);

		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1","sequence1"),callback1);
		TestCallback callback2 = new TestCallback ("Callback 2");
		serviceClient.sendReceiveNonBlocking(getEchoOMBlock("echo2","sequence1"),callback2);

		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		TestCallback callback3 = new TestCallback ("Callback 3");
		serviceClient.sendReceiveNonBlocking(getEchoOMBlock("echo3","sequence1"),callback3);
		
        while (!callback3.isComplete()) {
            Thread.sleep(1000);
        }
        
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
		
		return echoStringElement;
	}
}
