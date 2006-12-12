/*
 * Copyright 2006 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.sandesha2.scenarios;

import java.io.File;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;
import org.apache.sandesha2.util.SandeshaUtil;

/**
 * This testcase is similar to the AnonymousAckEchoTest, but the replyTo EPR
 * is also anonymous, so all the server->client message flows use the HTTP
 * backchannel.
 */
public class AnonymousEchoTest extends SandeshaTestCase {

	public AnonymousEchoTest () {
		super ("AnonymousEchoTest");
	}
	
	public void setUp () throws Exception {
		super.setUp();
		String repoPath = "target" + File.separator + "repos" + File.separator + "server";
		String axis2_xml = repoPath + File.separator + "server_axis2.xml";
		startServer(repoPath, axis2_xml);
	}
	
	public void testSyncEcho () throws Exception {
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = repoPath + File.separator + "client_axis2.xml";
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();
		clientOptions.setAction(echoAction);
		clientOptions.setTo(new EndpointReference (to));
		String sequenceKey = SandeshaUtil.getUUID();
		String offeredSequenceID = SandeshaUtil.getUUID();
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,offeredSequenceID);
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_1);
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		
		// Put in enough config to convince Axis that this is async, without setting up
		// new WS-Adressing replyTo etc.
		clientOptions.setUseSeparateListener(true);
		clientOptions.setProperty(Constants.Configuration.USE_CUSTOM_LISTENER,Boolean.TRUE);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		serviceClient.setOptions(clientOptions);

		OMElement reply = serviceClient.sendReceive(getEchoOMBlock("echo1",sequenceKey));
		String replyText = checkEchoOMBlock(reply);
		assertEquals("echo1", replyText);
		
		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
		        //assertions for the out sequence.
				SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertTrue(sequenceReport.getCompletedMessages().contains(new Long(1)));
				assertEquals(SequenceReport.SEQUENCE_DIRECTION_OUT, sequenceReport.getSequenceDirection());
				assertEquals(SequenceReport.SEQUENCE_STATUS_TERMINATED, sequenceReport.getSequenceStatus());
	
				//assertions for the in sequence
				sequenceReport = SandeshaClient.getIncomingSequenceReport(offeredSequenceID, configContext);
				assertTrue(sequenceReport.getCompletedMessages().contains(new Long(1)));
				assertEquals(SequenceReport.SEQUENCE_DIRECTION_IN, sequenceReport.getSequenceDirection());
				assertEquals(SequenceReport.SEQUENCE_STATUS_TERMINATED, sequenceReport.getSequenceStatus());

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
