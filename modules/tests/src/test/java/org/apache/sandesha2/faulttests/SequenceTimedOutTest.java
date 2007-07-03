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

package org.apache.sandesha2.faulttests;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.SandeshaUtil;

public class SequenceTimedOutTest extends SandeshaTestCase {
	
	public SequenceTimedOutTest() {
		super("SequenceTimedOutTest");
	}

	public void setUp() throws Exception {
		super.setUp();
	}
	
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	/**
	 * Test to check that when a sequence times out - that we alert the client to the
	 * fact that the sequence has now gone.
	 * 
	 * @throws Exception
	 */
	public void testSOAP11CreateSequenceRefusedInboundFault () throws Exception {
		
		String to = "http://127.0.0.1:" + 9999 + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);		
		
		Options clientOptions = new Options ();
		clientOptions.setAction(echoAction);
		clientOptions.setTo(new EndpointReference (to));

		String sequenceKey = SandeshaUtil.getUUID();
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		

		HashMap axisServices = configContext.getAxisConfiguration().getServices();
		
		AxisService service = null;
		Iterator values = axisServices.values().iterator();
		while(values.hasNext())
			service = (AxisService)values.next();

		// Set the Sequence timout property to 1 second.
    Iterator operations = service.getOperations();
    
    while (operations.hasNext())
    {
    	AxisOperation op = (AxisOperation) operations.next();
  		SandeshaPolicyBean propertyBean = 
  			SandeshaUtil.getPropertyBean(op);

  		// Indicate that the sequence should timeout after 1 second
  		if (propertyBean != null)
  			propertyBean.setInactiveTimeoutInterval(1, "seconds");
    }
		
		// Set a bad acks to so the CreateSequence will be refused.
		String acksTo = AddressingConstants.Final.WSA_NONE_URI;
		clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
		
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);		
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		serviceClient.setOptions(clientOptions);		
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1",sequenceKey),callback1);
        
		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
		        //assertions for the out sequence.
				SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertEquals(sequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TIMED_OUT);
				assertEquals(sequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
				
				assertTrue(callback1.isErrorReported());
				assertEquals(callback1.getResult(),null);
				
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
	
	/**
	 * Test to check that when a sequence times out - it gets deleted after the timeout interval.
	 * 
	 * @throws Exception
	 */
	public void testRMSSequenceTimeoutSequenceDeleted () throws Exception {
		
		String to = "http://127.0.0.1:" + 9999 + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);		
		
		Options clientOptions = new Options ();
		clientOptions.setAction(echoAction);
		clientOptions.setTo(new EndpointReference (to));

		String sequenceKey = SandeshaUtil.getUUID();
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);		

		HashMap axisServices = configContext.getAxisConfiguration().getServices();
		
		AxisService service = null;
		Iterator values = axisServices.values().iterator();
		while(values.hasNext())
			service = (AxisService)values.next();

		// Set the Sequence timout property to 1 second.
    Iterator operations = service.getOperations();
    
    while (operations.hasNext())
    {
    	AxisOperation op = (AxisOperation) operations.next();
  		SandeshaPolicyBean propertyBean = 
  			SandeshaUtil.getPropertyBean(op);

  		// Indicate that the sequence should timeout after 1 second
  		// And that it should be deleted after 2 seconds
  		if (propertyBean != null) {
  			propertyBean.setInactiveTimeoutInterval(1, "seconds");
  			propertyBean.setSequenceRemovalTimeoutInterval(2, "seconds");
  		}
    }
		
		// Set a bad acks to so the CreateSequence will be refused.
		String acksTo = AddressingConstants.Final.WSA_NONE_URI;
		clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
		
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setUseSeparateListener(true);		
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		serviceClient.setOptions(clientOptions);		
		
		TestCallback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking (getEchoOMBlock("echo1",sequenceKey),callback1);
        
		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			try {
		        //assertions for the out sequence.
				SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertEquals(sequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TIMED_OUT);
				assertEquals(sequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
				
				assertTrue(callback1.isErrorReported());
				assertEquals(callback1.getResult(),null);
				
				lastError = null;
				break;
			} catch(Error e) {
				lastError = e;
			}
		}

		if(lastError != null) throw lastError;

		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			// Check that the sequence has been deleted.
			StorageManager storageManager = 
				SandeshaUtil.getSandeshaStorageManager(configContext, configContext.getAxisConfiguration());
			
			Transaction tran = storageManager.getTransaction();
			
			RMSBean finderBean = new RMSBean();
			List rmsBeans = storageManager.getRMSBeanMgr().find(finderBean);
			
			tran.commit();
			
			lastError = null;
			
			if (!rmsBeans.isEmpty())
				lastError = new Error("rmsBeans not empty " + rmsBeans);
			else
				break;
			
		}

		if(lastError != null) throw lastError;
		
		configContext.getListenerManager().stop();
		serviceClient.cleanup();

	}
	
	private static final String server_repoPath = "target" + File.separator + "repos" + File.separator + "server";
	private static final String server_axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";

	/**
	 * Checks that an RMDSequence is terminated once the timeout interval arrives.
	 * Also that the RMDBean is deleted once the SequenceRemovalTimeout arrives.
	 */
	public void testRMDSequenceTerminatedDeleted() throws Exception{ 
		ConfigurationContext serverConfigContext = startServer(server_repoPath, server_axis2_xml);
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);		
		
		Options clientOptions = new Options ();
		clientOptions.setAction(pingAction);
		clientOptions.setTo(new EndpointReference (to));

		String sequenceKey = SandeshaUtil.getUUID();
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		serviceClient.setOptions(clientOptions);

		HashMap axisServices = serverConfigContext.getAxisConfiguration().getServices();
		
		AxisService service = null;
		Iterator values = axisServices.values().iterator();
		while(values.hasNext())
			service = (AxisService)values.next();

		// Set the Sequence timout property to 1 second.
    Iterator operations = service.getOperations();
    
    while (operations.hasNext())
    {
    	AxisOperation op = (AxisOperation) operations.next();
  		SandeshaPolicyBean propertyBean = 
  			SandeshaUtil.getPropertyBean(op);

  		// Indicate that the sequence should timeout after 2 second
  		// And that it should be deleted after 2 seconds
  		if (propertyBean != null) {
  			propertyBean.setInactiveTimeoutInterval(2, "seconds");
  			propertyBean.setSequenceRemovalTimeoutInterval(2, "seconds");
  		}
    }
    
    // Send a single ping message
		serviceClient.fireAndForget(getPingOMBlock("ping1"));

		long limit = System.currentTimeMillis() + waitTime;
		Error lastError = null;
		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			// Check that the sequence has been deleted.
			StorageManager storageManager = 
				SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
			
			Transaction tran = storageManager.getTransaction();
			
			RMDBean finderBean = new RMDBean();
			List rmdBeans = storageManager.getRMDBeanMgr().find(finderBean);
			
			tran.commit();
			
			lastError = null;
			
			if (rmdBeans.isEmpty())
				lastError = new Error("rmdBeans empty " + rmdBeans);
			else {
				RMDBean bean = (RMDBean)rmdBeans.get(0);
				if (bean.isTerminated())
					break;
				
				lastError = new Error("RMDBean not deleted " + bean);
			}
		}

		if(lastError != null) throw lastError;

		while(System.currentTimeMillis() < limit) {
			Thread.sleep(tickTime); // Try the assertions each tick interval, until they pass or we time out
			
			// Check that the sequence has been deleted.
			StorageManager storageManager = 
				SandeshaUtil.getSandeshaStorageManager(serverConfigContext, serverConfigContext.getAxisConfiguration());
			
			Transaction tran = storageManager.getTransaction();
			
			RMDBean finderBean = new RMDBean();
			List rmdBeans = storageManager.getRMDBeanMgr().find(finderBean);
			
			tran.commit();
			
			lastError = null;
			
			if (!rmdBeans.isEmpty())
				lastError = new Error("rmdBeans not empty " + rmdBeans);
			else
				break;
		}

		if(lastError != null) throw lastError;
		
		configContext.getListenerManager().stop();
		serviceClient.cleanup();

	}
	
}


