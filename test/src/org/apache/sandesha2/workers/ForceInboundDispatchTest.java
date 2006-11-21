package org.apache.sandesha2.workers;

import java.io.File;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beans.NextMsgBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.workers.Invoker;

public class ForceInboundDispatchTest extends SandeshaTestCase  {

	SimpleHTTPServer httpServer = null;
	private final String applicationNamespaceName = "http://tempuri.org/"; 
	private final String ping = "ping";
	private final String Text = "Text";

	private Log log = LogFactory.getLog(getClass());
	int serverPort = DEFAULT_SERVER_TEST_PORT;
	
	ConfigurationContext serverConfigCtx = null;
	
	public ForceInboundDispatchTest () {
        super ("ForceDispatchTest");
	}
	
	public void setUp () throws AxisFault {
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "server";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";

		serverConfigCtx = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		String serverPortStr = getTestProperty("test.server.port");
		if (serverPortStr!=null) {
			try {
				serverPort = Integer.parseInt(serverPortStr);
			} catch (NumberFormatException e) {
				log.error(e);
			}
		}
		
		httpServer = new SimpleHTTPServer (serverConfigCtx,serverPort);
		httpServer.start();
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			throw new SandeshaException ("sleep interupted");
		}
	}
	
	public void tearDown () throws SandeshaException {
		if (httpServer!=null)
			httpServer.stop();
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			throw new SandeshaException ("sleep interupted");
		}
	}
	
	public void testForceInvoke () throws AxisFault,InterruptedException  {
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";

		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();
		clientOptions.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportTo);
		
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
			
			String inboundSequenceID = SandeshaUtil.getSequenceIDFromInternalSequenceID(SandeshaUtil.getInternalSequenceID(to, sequenceKey),
					SandeshaUtil.getInMemoryStorageManager(configContext));
			
			SandeshaClient.forceDispatchOfInboundMessages(serverConfigCtx, 
					inboundSequenceID, 
					true); //allow later msgs to be delivered 
			
			//check that the server is now expecting msg 4
			StorageManager serverStore = SandeshaUtil.getInMemoryStorageManager(serverConfigCtx);
			NextMsgBean nextMsgBean = 
				serverStore.getNextMsgBeanMgr().retrieve(inboundSequenceID);
			assertNotNull(nextMsgBean);
			assertEquals(nextMsgBean.getNextMsgNoToProcess(), 4);
			
			//also check that the sequence has an out of order gap that contains msg 2
			SequencePropertyBean outOfOrderRanges = 
				serverStore.getSequencePropertyBeanMgr().retrieve(
							inboundSequenceID, 
							Sandesha2Constants.SequenceProperties.OUT_OF_ORDER_RANGES);
			
			assertNotNull(outOfOrderRanges);
			RangeString rangeString = new RangeString(outOfOrderRanges.getValue());
			assertTrue(rangeString.isMessageNumberInRanges(2));
			
			//we deliver msg 2
			//set highest out msg number to 1
			SequencePropertyBean nextMsgNoBean = 
					SandeshaUtil.getInMemoryStorageManager(configContext).getSequencePropertyBeanMgr().
					retrieve(SandeshaUtil.getInternalSequenceID(to, sequenceKey),
					Sandesha2Constants.SequenceProperties.NEXT_MESSAGE_NUMBER);
			nextMsgNoBean.setValue("1");
			
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
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";

		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();
		clientOptions.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportTo);
		
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
			
			String inboundSequenceID = SandeshaUtil.getSequenceIDFromInternalSequenceID(SandeshaUtil.getInternalSequenceID(to, sequenceKey),
					SandeshaUtil.getInMemoryStorageManager(configContext));
			
			SandeshaClient.forceDispatchOfInboundMessages(serverConfigCtx, inboundSequenceID, false);
			
			//check that the server is now expecting msg 4
			NextMsgBean nextMsgBean = 
				SandeshaUtil.getInMemoryStorageManager(serverConfigCtx).getNextMsgBeanMgr().
					retrieve(inboundSequenceID);
			assertNotNull(nextMsgBean);
			assertEquals(nextMsgBean.getNextMsgNoToProcess(), 4);
	  }
		finally{
			configContext.getListenerManager().stop();
			serviceClient.cleanup();			
		}

	}
	
	private OMElement getPingOMBlock(String text) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace namespace = fac.createOMNamespace(applicationNamespaceName,"ns1");
		OMElement pingElem = fac.createOMElement(ping, namespace);
		OMElement textElem = fac.createOMElement(Text, namespace);
		
		textElem.setText(text);
		pingElem.addChild(textElem);

		return pingElem;
	}

}
