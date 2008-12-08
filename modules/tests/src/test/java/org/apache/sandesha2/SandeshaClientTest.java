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
import java.util.List;

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axis2.Constants;
import org.apache.axis2.Constants.Configuration;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.SandeshaUtil;

public class SandeshaClientTest extends SandeshaTestCase {

	String server_repoPath = "target" + File.separator + "repos" + File.separator + "server";
	String server_axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";
	private boolean startedServer = false;

	public SandeshaClientTest () {
		super ("SandeshaClientTest");
	}

	public void setUp () throws Exception {
		super.setUp();
		String repoPath = "target" + File.separator + "repos" + File.separator + "server";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";
		if (!startedServer)
			startServer(repoPath, axis2_xml);
		startedServer = true;
	}
	
	/**
	 * Override the teardown processing
	 */
	public void tearDown () throws Exception {
		super.tearDown();
	}

	public void testCreateSequenceWithOffer () throws Exception {
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
		Options clientOptions = new Options ();
		
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(Configuration.TRANSPORT_URL,transportTo);
		
//		String sequenceKey = SandeshaUtil.getUUID();
//		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		
		try
		{
			String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
			clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
			clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
			
			String offeredSequenceID = SandeshaUtil.getUUID();
			clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,offeredSequenceID);
			
			serviceClient.setOptions(clientOptions);
			//serviceClient.
			
			clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
			clientOptions.setUseSeparateListener(true);
			
			serviceClient.setOptions(clientOptions);
			
			String sequenceKey = SandeshaClient.createSequence(serviceClient,true);
			clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY, sequenceKey);
			
			long limit = System.currentTimeMillis() + waitTime;
			Error lastError = null;
			while(System.currentTimeMillis() < limit) {
				Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
				
				try {
					SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
					
					assertNotNull(sequenceReport.getSequenceID());
					assertFalse(sequenceReport.isSecureSequence());

					lastError = null;
					break;
				} catch(Error e) {
					lastError = e;
				}
			}
			if(lastError != null) throw lastError;
		}
		finally
		{
			configContext.getListenerManager().stop();
			serviceClient.cleanup();			
		}

	}
	
	public void testSequenceCloseTerminate()throws Exception{
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
			
			String sequenceKey = "some_sequence_key";
			clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
			
			ServiceClient serviceClient = new ServiceClient (configContext,null);
			
			String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
			clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
			clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
				//serviceClient.
			serviceClient.setOptions(clientOptions);
				
			try{
				
				serviceClient.fireAndForget(getPingOMBlock("ping1"));
				
				long limit = System.currentTimeMillis() + waitTime;
				Error lastError = null;
				while(System.currentTimeMillis() < limit) {
					Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
					
					try {
						SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
						assertNotNull(sequenceReport.getSequenceID());
						
						//now close the sequence
						SandeshaClient.closeSequence(serviceClient);
						
						//try and send another msg - this should fail
						try{
							serviceClient.fireAndForget(getPingOMBlock("ping2"));
							fail(); //this should have failed
						}
						catch(Exception e){
							//good
						}
					
						//finally terminate the sequence
						terminateAndCheck(serviceClient);

						lastError = null;
						break;
					} catch(Error e) {
						lastError = e;
					}
				}
				if(lastError != null) throw lastError;
			}
			finally{
				configContext.getListenerManager().stop();
				serviceClient.cleanup();			
			}
			
		}
		
		private void terminateAndCheck(ServiceClient srvcClient)throws Exception{
			SandeshaClient.terminateSequence(srvcClient);

			long limit = System.currentTimeMillis() + waitTime;
			Error lastError = null;
			while(System.currentTimeMillis() < limit) {
				Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
				
				try {
					//now check the sequence is terminated
					SequenceReport report = SandeshaClient.getOutgoingSequenceReport(srvcClient);
					assertNotNull(report);
					assertEquals(report.getSequenceStatus(), SequenceReport.SEQUENCE_STATUS_TERMINATED);

					lastError = null;
					break;
				} catch(Error e) {
					lastError = e;
				}
			}
			if(lastError != null) throw lastError;
		}
	
//	public void testCreateSequenceWithoutOffer () {
////		SandeshaClient.createSequence(serviceClient,true);
//		
//		
//	}
	
//	public void testCreateSequenceWithSequenceKey () {
//		
//	}
//

  /**
	 * Checks the following scenario
	 * 
	 * 1) send an application message (will generate the create sequence)
	 * 2) terminate the sequence
	 * 3) Issue wait until sequence completed (with a wait time)
	 * 4) Create a new sequence
	 * 5) send another application message 
	 * 6) terminate the sequence
	 * 7) Ensure that the sequence was terminated
	 * 
	 */
	public void testTerminateCreateWithWait () throws Exception {

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
		
			// 2) Terminate the sequence
			SandeshaClient.terminateSequence(serviceClient);

			// 3) wait for the sequence completion (30 second wait)
			SandeshaClient.waitUntilSequenceCompleted(serviceClient, 30000);

			// 4) Create a new Sequence to the same endpoint
			SandeshaClient.createSequence(serviceClient, false, null);
			
			// 5) Send the second application message (this should use a new sequence)
			serviceClient.fireAndForget(getPingOMBlock("ping2"));			

			// 6) Terminate the sequence
			SandeshaClient.terminateSequence(serviceClient);

			// 7) wait for the sequence completion (30 second wait)
			SandeshaClient.waitUntilSequenceCompleted(serviceClient, 30000);

			// 8) Check that the sequence has terminated
			SequenceReport report = SandeshaClient.getOutgoingSequenceReport(serviceClient);
			assertNotNull(report);
			assertEquals(SequenceReport.SEQUENCE_STATUS_TERMINATED, report.getSequenceStatus());

		}
		finally {
			configContext.getListenerManager().stop();
			serviceClient.cleanup();			
		}		
	}

//	
//	public void testCloseSequence () {
//		
//	}
//
	/**
	 * Test that sending an ACK request gets transmitted
	 * This doesn't check the content of the Ack Request, only that the
	 * SenderBean no longer exists for it.
	 */
	public void testAckRequest () throws Exception {
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();

		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(Configuration.TRANSPORT_URL,transportTo);
				
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION, Sandesha2Constants.SPEC_VERSIONS.v1_1);
		clientOptions.setUseSeparateListener(true);
		
		serviceClient.setOptions(clientOptions);
				
		// Create a sequence 
		SandeshaClient.createSequence(serviceClient, false, null);
		
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
		
		// Send the ACK request
		SandeshaClient.sendAckRequest(serviceClient);
		
		limit = System.currentTimeMillis() + waitTime;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
				// Get the storage manager from the ConfigurationContext
				StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configContext, configContext.getAxisConfiguration());
				
				// Get the sequence id for this sequence.
				String sequenceId = SandeshaClient.getSequenceID(serviceClient);
				
				// Get the SenderBeanManager
				SenderBeanMgr senderManager = storageManager.getSenderBeanMgr();
						
				// Check that there are no sender beans inside the SenderBeanMgr.
				SenderBean senderBean = new SenderBean();
				senderBean.setSequenceID(sequenceId);
				senderBean.setSend(true);
				senderBean.setReSend(false);
				
				// Find any sender beans for the to address.
				List<SenderBean> beans = senderManager.find(senderBean);
				assertTrue("SenderBeans found when the list should be empty", beans.isEmpty());
				
				SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				
				assertNotNull(sequenceReport.getSequenceID());
				assertFalse(sequenceReport.isSecureSequence());

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
//	
//	public void getSequenceIDTest () {
//		
//	}
	
}
