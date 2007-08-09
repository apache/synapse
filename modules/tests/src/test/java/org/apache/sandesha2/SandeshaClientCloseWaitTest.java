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
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;

public class SandeshaClientCloseWaitTest extends SandeshaTestCase {

	String server_repoPath = "target" + File.separator + "repos" + File.separator + "server";
	String server_axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";

	public SandeshaClientCloseWaitTest () {
		super ("SandeshaClientTest");
	}

		/**
		 * Checks the following scenario
		 * 
		 * Don't start the server
		 * 1) send an application message (will generate the create sequence)
		 * 2) close the sequence
		 * 3) send another application message (this should fail)
		 * 4) start the server
		 * 5) wait a bit then terminate sequence
		 * 6) Issue wait until sequence completed (with a wait time)
		 * 7) Ensure that the sequence was terminated
		 * 
		 */
		public void testCloseSequenceWithWait () throws Exception {
			String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
			
			String repoPath = "target" + File.separator + "repos" + File.separator + "client";
			String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
			
			ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
			
			Options clientOptions = new Options ();
			clientOptions.setAction(pingAction);
			clientOptions.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		   clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION, 
		       Sandesha2Constants.SPEC_VERSIONS.v1_1);
			clientOptions.setTo(new EndpointReference (to));
			
			ServiceClient serviceClient = new ServiceClient (configContext,null);
			
			String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
			clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
			clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
				//serviceClient.
			serviceClient.setOptions(clientOptions);
				
			try{
				// 1) Send the application message
				serviceClient.fireAndForget(getPingOMBlock("ping1"));
				
				// 2) Close the sequence
				SandeshaClient.closeSequence(serviceClient);
				
				// 3) Send the second application message (this should fail)
				try{
					serviceClient.fireAndForget(getPingOMBlock("ping2"));
					fail(); //this should have failed
				}
				catch(Exception e){
					//good
				}
				
				// 4) Start the server			
				startServer(server_repoPath, server_axis2_xml);

				// 5) Wait a bit then terminate
				long limit = System.currentTimeMillis() + waitTime;
				Error lastError = null;
				while(System.currentTimeMillis() < limit) {
					Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
					
					try {
						//now check the sequence is running
						SequenceReport report = SandeshaClient.getOutgoingSequenceReport(serviceClient);
						assertEquals(report.getSequenceStatus(), SequenceReport.SEQUENCE_STATUS_ESTABLISHED);

						lastError = null;
						break;
					} catch(Error e) {
						lastError = e;
					}
				}
				if(lastError != null) throw lastError;
				SandeshaClient.terminateSequence(serviceClient);
				
				// 6) wait for the sequence completion (30 second wait)
				SandeshaClient.waitUntilSequenceCompleted(serviceClient, 30000);
				
				// 7) Check that the sequence has terminated
				SequenceReport report = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertNotNull(report);
				assertEquals(SequenceReport.SEQUENCE_STATUS_TERMINATED, report.getSequenceStatus());

			}
			finally {
				configContext.getListenerManager().stop();
				serviceClient.cleanup();			
			}
			
		}
}
