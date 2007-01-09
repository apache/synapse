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
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SandeshaReport;
import org.apache.sandesha2.client.SequenceReport;
import org.apache.sandesha2.util.SandeshaUtil;

public class SandeshaReportsTest extends SandeshaTestCase {

	public SandeshaReportsTest () {
		super ("SandeshaReportsTest");
	}
	
	public void setUp () throws Exception {
		super.setUp();

		String repoPath = "target" + File.separator + "repos" + File.separator + "server";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";

		startServer(repoPath, axis2_xml);
	}
	
	public void testSequenceReports () throws Exception  {

		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();
		clientOptions.setAction(echoAction);
		clientOptions.setTo(new EndpointReference (to));
		
		String sequenceKey = SandeshaUtil.getUUID();
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
		clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
		
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		

		serviceClient.setOptions(clientOptions);
		//serviceClient.
		
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);
		
		serviceClient.setOptions(clientOptions);
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking(getEchoOMBlock("echo1",sequenceKey),callback1);
		
		TestCallback callback2 = new TestCallback ("Callback 2");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo2",sequenceKey),callback2);

		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
		        //testing outgoing sequence reports
				SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertTrue(sequenceReport.getCompletedMessages().contains(new Long(1)));
				assertTrue(sequenceReport.getCompletedMessages().contains(new Long(2)));
				assertEquals(sequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_ESTABLISHED);
				assertEquals(sequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
				
				//testing incoming sequence reports
				ArrayList incomingSequenceReports = SandeshaClient.getIncomingSequenceReports(configContext);
				assertEquals(incomingSequenceReports.size(),1);
				SequenceReport incomingSequenceReport = (SequenceReport) incomingSequenceReports.get(0);
				assertEquals(incomingSequenceReport.getCompletedMessages().size(),2);
				assertNotNull(incomingSequenceReport.getSequenceID());
				assertEquals(incomingSequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_IN);
				assertNotNull(incomingSequenceReport.getInternalSequenceID());
				
				assertEquals(incomingSequenceReport.getSequenceID(),incomingSequenceReport.getInternalSequenceID());  //for the incoming side, internalSequenceID==sequenceID

				lastError = null;
				break;
			} catch(Error e) {
				lastError = e;
			}
		}
		if(lastError != null) throw lastError;

		SandeshaClient.terminateSequence(serviceClient, sequenceKey);
		SandeshaClient.waitUntilSequenceCompleted(serviceClient, sequenceKey);
		
		SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		assertEquals(sequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
		
		configContext.getListenerManager().stop();
		serviceClient.cleanup();
	}
	
	public void testRMReport () throws AxisFault,InterruptedException {
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";

		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		//clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		Options clientOptions = new Options ();
		clientOptions.setAction(pingAction);
//		clientOptions.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		
		clientOptions.setTo(new EndpointReference (to));
		
		String sequenceKey1 = "sequence3";
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey1);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		
		serviceClient.setOptions(clientOptions);
		
		
		serviceClient.fireAndForget(getPingOMBlock("ping1"));		
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		serviceClient.fireAndForget(getPingOMBlock("ping2"));
		
		String sequenceKey2 = "sequence4";
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey2);
		
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "false");
		serviceClient.fireAndForget(getPingOMBlock("ping1"));		
		
		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
				SandeshaReport rmReport = SandeshaClient.getSandeshaReport(configContext);
				
			 	SequenceReport sequence1Report = null;
			 	SequenceReport sequence2Report = null;
			 	
			 	Iterator iterator = rmReport.getOutgoingSequenceList().iterator();
			 	while (iterator.hasNext()) {
			 		String sequenceID = (String) iterator.next();
			 		
			 		 String internalSequenceID = rmReport.getInternalSequenceIdOfOutSequence(sequenceID);
			 		 
			 		 if (internalSequenceID.indexOf(sequenceKey1)>=0) {
			 			 sequence1Report = SandeshaClient.getOutgoingSequenceReport(to,sequenceKey1,configContext);
			 		 } else if (internalSequenceID.indexOf(sequenceKey2)>=0){
			 			 sequence2Report = SandeshaClient.getOutgoingSequenceReport(to,sequenceKey2,configContext);
			 		 }
			 	}
			 	
			 	assertNotNull(sequence1Report);
			 	assertNotNull(sequence2Report);
			 	
			 	assertEquals(sequence1Report.getCompletedMessages().size(),2);
			 	assertEquals(sequence2Report.getCompletedMessages().size(),1);
			 	
			 	assertEquals(sequence1Report.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
			 	assertEquals(sequence2Report.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_ESTABLISHED);	

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
	
	
//	public void testSequenceTermination () throws AxisFault,InterruptedException {
//		
//		String to = "http://127.0.0.1:8060/axis2/services/RMSampleService";
//		String transportTo = "http://127.0.0.1:8060/axis2/services/RMSampleService";
//		
//		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
//		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
//
//		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
//
//		//clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
//		Options clientOptions = new Options ();
//		clientOptions.setProperty(Options.COPY_PROPERTIES,new Boolean (true));
//		clientOptions.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
//		
//		clientOptions.setTo(new EndpointReference (to));
//		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportTo);
//		
//		String sequenceKey1 = "sequence1";
//		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey1);
//		
//		ServiceClient serviceClient = new ServiceClient (configContext,null);
//		
//		serviceClient.setOptions(clientOptions);
//		
//		serviceClient.fireAndForget(getPingOMBlock("ping11"));		
//		serviceClient.fireAndForget(getPingOMBlock("ping12"));
//		
//		String sequenceKey2 = "sequence2";
//		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey2);
//		
//		serviceClient.fireAndForget(getPingOMBlock("ping21"));	
//		
//		SandeshaClient.terminateSequence(serviceClient,sequenceKey1);
//		
//		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
//		serviceClient.fireAndForget(getPingOMBlock("ping22"));	
//		try {
//			//waiting till the messages exchange finishes.
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			throw new SandeshaException ("sleep interupted");
//		}
//		
//		
//		SandeshaReport rmReport = SandeshaClient.getSandeshaReport(configContext);
//		
//	 	SequenceReport sequence1Report = null;
//	 	SequenceReport sequence2Report = null;
//	 	
//	 	Iterator iterator = rmReport.getOutgoingSequenceList().iterator();
//	 	while (iterator.hasNext()) {
//	 		String sequenceID = (String) iterator.next();
//	 		
//	 		 String internalSequenceID = rmReport.getInternalSequenceIdOfOutSequence(sequenceID);
//	 		 
//	 		 if (internalSequenceID.indexOf(sequenceKey1)>=0) {
//	 			 sequence1Report = SandeshaClient.getOutgoingSequenceReport(to,sequenceKey1,configContext);
//	 		 } else if (internalSequenceID.indexOf(sequenceKey2)>=0){
//	 			 sequence2Report = SandeshaClient.getOutgoingSequenceReport(to,sequenceKey2,configContext);
//	 		 }
//	 	}
//	 	
//	 	assertNotNull(sequence1Report);
//	 	assertNotNull(sequence2Report);
//	 	
//	 	assertEquals(sequence1Report.getCompletedMessages().size(),2);
//	 	assertEquals(sequence2Report.getCompletedMessages().size(),2);
//	 	
//	 	assertEquals(sequence1Report.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
//	 	assertEquals(sequence2Report.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);	
//	}
	
}
