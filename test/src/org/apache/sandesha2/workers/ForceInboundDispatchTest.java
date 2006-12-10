package org.apache.sandesha2.workers;

import java.io.File;

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;

public class ForceInboundDispatchTest extends SandeshaTestCase  {

	ConfigurationContext serverConfigCtx = null;
	
	public ForceInboundDispatchTest () {
        super ("ForceDispatchTest");
	}
	
	public void setUp () throws Exception {
		super.setUp();
		String repoPath = "target" + File.separator + "repos" + File.separator + "server";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";
		serverConfigCtx = startServer(repoPath, axis2_xml);
	}
	
	public void testForceInvoke () throws AxisFault,InterruptedException  {
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";

		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();
		clientOptions.setAction(pingAction);
		clientOptions.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		
		clientOptions.setTo(new EndpointReference (to));
		
		String sequenceKey = "sequence1";
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		serviceClient.setOptions(clientOptions);
		
		try{
			serviceClient.fireAndForget(getPingOMBlock("ping1"));		
			
			//now deliver the next out of order 
			clientOptions.setProperty(SandeshaClientConstants.MESSAGE_NUMBER,new Long(3));
			serviceClient.fireAndForget(getPingOMBlock("ping3"));
	
			Thread.sleep(5000);
			StorageManager mgr = SandeshaUtil.getInMemoryStorageManager(configContext);
			Transaction t = mgr.getTransaction();
			String inboundSequenceID = SandeshaUtil.getSequenceIDFromInternalSequenceID(SandeshaUtil.getInternalSequenceID(to, sequenceKey),
					mgr);
			t.commit();
			
			SandeshaClient.forceDispatchOfInboundMessages(serverConfigCtx, 
					inboundSequenceID, 
					true); //allow later msgs to be delivered 
			
			//check that the server is now expecting msg 4
			StorageManager serverStore = SandeshaUtil.getInMemoryStorageManager(serverConfigCtx);
			t = serverStore.getTransaction();
			RMDBean rMDBean = 
				serverStore.getNextMsgBeanMgr().retrieve(inboundSequenceID);
			assertNotNull(rMDBean);
			assertEquals(rMDBean.getNextMsgNoToProcess(), 4);
			
			//also check that the sequence has an out of order gap that contains msg 2
			SequencePropertyBean outOfOrderRanges = 
				serverStore.getSequencePropertyBeanMgr().retrieve(
							inboundSequenceID, 
							Sandesha2Constants.SequenceProperties.OUT_OF_ORDER_RANGES);
			
			assertNotNull(outOfOrderRanges);
			RangeString rangeString = new RangeString(outOfOrderRanges.getValue());
			assertTrue(rangeString.isMessageNumberInRanges(2));
			t.commit();
			
			//we deliver msg 2
			//set highest out msg number to 1
			t = mgr.getTransaction();
			SequencePropertyBean nextMsgNoBean = 
					mgr.getSequencePropertyBeanMgr().
					retrieve(SandeshaUtil.getInternalSequenceID(to, sequenceKey),
					Sandesha2Constants.SequenceProperties.NEXT_MESSAGE_NUMBER);
			nextMsgNoBean.setValue("1");
			t.commit();
			
			clientOptions.setProperty(SandeshaClientConstants.MESSAGE_NUMBER,new Long(2));
			serviceClient.fireAndForget(getPingOMBlock("ping2"));
		}
		finally{
			configContext.getListenerManager().stop();
			serviceClient.cleanup();			
		}

	}
	
	public void testForceInvokeWithDiscardGaps () throws AxisFault,InterruptedException  {
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";

		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();
		clientOptions.setAction(pingAction);
		clientOptions.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		
		clientOptions.setTo(new EndpointReference (to));
		
		String sequenceKey = "sequence1";
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		serviceClient.setOptions(clientOptions);
		try
		{
			serviceClient.fireAndForget(getPingOMBlock("ping1"));		
			
			//now deliver the next out of order 
			clientOptions.setProperty(SandeshaClientConstants.MESSAGE_NUMBER,new Long(3));
			serviceClient.fireAndForget(getPingOMBlock("ping3"));
	
			Thread.sleep(5000);
			
			StorageManager mgr = SandeshaUtil.getInMemoryStorageManager(configContext);
			Transaction t = mgr.getTransaction();
			String inboundSequenceID = SandeshaUtil.getSequenceIDFromInternalSequenceID(SandeshaUtil.getInternalSequenceID(to, sequenceKey),
					mgr);
			t.commit();
			
			SandeshaClient.forceDispatchOfInboundMessages(serverConfigCtx, inboundSequenceID, false);
			
			//check that the server is now expecting msg 4
			StorageManager serverMgr = SandeshaUtil.getInMemoryStorageManager(serverConfigCtx);
			t = serverMgr.getTransaction();
			RMDBean rMDBean = serverMgr.getNextMsgBeanMgr().retrieve(inboundSequenceID);
			assertNotNull(rMDBean);
			assertEquals(rMDBean.getNextMsgNoToProcess(), 4);
			t.commit();
	  }
		finally{
			configContext.getListenerManager().stop();
			serviceClient.cleanup();			
		}

	}
	
}
