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
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants.Configuration;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SandeshaListener;
import org.apache.sandesha2.client.SequenceReport;
import org.apache.sandesha2.util.SandeshaUtil;


public class SyncPingClient {

	private static final String applicationNamespaceName = "http://tempuri.org/"; 
	private static final String ping = "ping";
	private static final String Text = "Text";
	
	private String toIP = "127.0.0.1";
	
	private String toPort = "8080";
	private String transportToPort = "8070";
	
	private String toEPR = "http://" + toIP +  ":" + toPort + "/axis2/services/RMSampleService";
	private String transportToEPR = "http://" + toIP +  ":" + transportToPort + "/axis2/services/RMSampleService";
	
	private static String SANDESHA2_HOME = "<SANDESHA2_HOME>"; //Change this to ur path.
	
	private static String AXIS2_CLIENT_PATH = SANDESHA2_HOME + File.separator + "target" + File.separator +"repos" + File.separator + "client" + File.separator;   //this will be available after a maven build
	
	public static void main(String[] args) throws AxisFault {
		
		String axisClientRepo = null;
		if (args!=null && args.length>0)
			axisClientRepo = args[0];
		
		if (axisClientRepo!=null && !"".equals(axisClientRepo)) {
			AXIS2_CLIENT_PATH = axisClientRepo;
			SANDESHA2_HOME = "";
		}

		
		new SyncPingClient ().run();
	}
	
	private void run () throws AxisFault {
		
		if ("<SANDESHA2_HOME>".equals(SANDESHA2_HOME)){
			System.out.println("ERROR: Please change <SANDESHA2_HOME> to your Sandesha2 installation directory.");
			return;
		}
		
		String axis2_xml = AXIS2_CLIENT_PATH + "client_axis2.xml";
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(AXIS2_CLIENT_PATH,axis2_xml);
		
		Options clientOptions = new Options ();	
		clientOptions.setProperty(Configuration.TRANSPORT_URL,transportToEPR);
		clientOptions.setTo(new EndpointReference (toEPR));
		
		String sequenceKey = SandeshaUtil.getUUID();// "sequence2";
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
	    
//		clientOptions.setProperty(MessageContextConstants.CHUNKED,Constants.VALUE_FALSE);   //uncomment this to send messages without chunking.
		
//		clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);   //uncomment this to send messages in SOAP 1.2
		
//		clientOptions.setProperty(SandeshaClient.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_1);  //uncomment this to send the messages according to the v1_1 spec.
		
//		clientOptions.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,AddressingConstants.Submission.WSA_NAMESPACE);
		
		clientOptions.setProperty(SandeshaClientConstants.SANDESHA_LISTENER, new SandeshaListenerImpl ());
		ServiceClient serviceClient = new ServiceClient (configContext,null);
//		serviceClient.engageModule(new QName ("sandesha2"));
		
		clientOptions.setAction("urn:wsrm:Ping");
		serviceClient.setOptions(clientOptions);
		
		serviceClient.fireAndForget(getPingOMBlock("ping1"));
		serviceClient.fireAndForget(getPingOMBlock("ping2"));
		
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");

		serviceClient.fireAndForget(getPingOMBlock("ping3"));
		
		SequenceReport sequenceReport = null;
			
		boolean complete = false;
		while (!complete) {
			sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
			if (sequenceReport!=null && sequenceReport.getCompletedMessages().size()==3)
				complete = true;
			else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
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
	
	private class SandeshaListenerImpl implements SandeshaListener {

		public void onError(AxisFault fault) {
			System.out.println("*********** RM fault callbak called");
		}

		public void onTimeOut(SequenceReport report) {
			System.out.println("Sequence timed out");
		} 	
	}
	
}
