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

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;

public class MessageRetransmissionTest extends SandeshaTestCase {

	private static final String server_repoPath = "target" + File.separator + "repos" + File.separator + "server";
	private static final String server_axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";
	
	public MessageRetransmissionTest () {
		super ("MessageRetransmissionTest");
	}
	
	public void testMessageRetransmission () throws Exception  {
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
	
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";

		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		//clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		Options clientOptions = new Options ();
		clientOptions.setAction(pingAction);
		clientOptions.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		
		clientOptions.setTo(new EndpointReference (to));
		
		String sequenceKey = "sequence1";
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		//serviceClient.
		
		serviceClient.setOptions(clientOptions);
		serviceClient.fireAndForget(getPingOMBlock("ping1"));
//		serviceClient.fireAndForget(getPingOMBlock("ping2"));
		
		//starting the server after a wait
		Thread.sleep(3000);
		startServer(server_repoPath, server_axis2_xml);

		serviceClient.fireAndForget(getPingOMBlock("ping2"));
		
		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
				SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertTrue(sequenceReport.getCompletedMessages().contains(new Long(1)));
				assertTrue(sequenceReport.getCompletedMessages().contains(new Long(2)));
				assertEquals(sequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);

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
