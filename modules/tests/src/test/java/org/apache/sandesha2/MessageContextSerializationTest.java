/*
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.xml.namespace.QName;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisOperationFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.RMMsgCreator;
import org.apache.sandesha2.util.RangeString;

/**
 * This class tests the Axis2 Message Context Serialization code.
 * Since sandesha can, in certain modes, serializae message contexts, it is
 * important that changes to message context serialization does not break anything.
 * 
 * These tests use some pre-produced binary data ".dat" resource files.
 * These files contain the binary data for certain sandesha data structures.
 * This test ensures that sandesha is still capable of deserializing previously 
 * serialized sandesha data.
 * 
 */
public class MessageContextSerializationTest extends SandeshaTestCase{
	
	private static final String rmsDatFileName = "RMSBean.dat";
	private static final String rmdDatFileName = "RMDBean.dat";
	private static final String msgCtxDatFileName = "MessageContext.dat";
	
	private static RMSBean expectedRMSBean;
	private static RMDBean expectedRMDBean;
	private static MessageContext expectedMessageContext;
	
	private static EndpointReference epr = new EndpointReference("someEPR");
	
	
	
	public MessageContextSerializationTest(String s)throws Exception
	{
		super(s);
	}
	
	public void testMain()throws Exception{
		
		//ensure we can deserialize the rms bean
		{
			FileInputStream fis = new FileInputStream(resourceDir + File.separator + rmsDatFileName);
			ObjectInputStream rmsBeanData = new ObjectInputStream(fis);
			RMSBean bean = (RMSBean)rmsBeanData.readObject();	
			assertTrue(bean.match(expectedRMSBean));
		}
		
		//ensure we can deserialize the rmd bean
		{
			FileInputStream fis = new FileInputStream(resourceDir + File.separator + rmdDatFileName);
			ObjectInputStream rmdBeanData = new ObjectInputStream(fis);
			RMDBean bean = (RMDBean)rmdBeanData.readObject();	
			assertTrue(bean.match(expectedRMDBean));
		}

		//ensure we can deserialize the msg ctx
		{
			FileInputStream fis = new FileInputStream(resourceDir + File.separator + msgCtxDatFileName);
			ObjectInputStream msgCtxData = new ObjectInputStream(fis);
			MessageContext msg = (MessageContext)msgCtxData.readObject();	
			assertEquals(expectedMessageContext.getFrom().getAddress(), msg.getFrom().getAddress());
			assertEquals(expectedMessageContext.getMessageID(), msg.getMessageID());
			assertEquals(expectedMessageContext.getReplyTo().getAddress(), msg.getReplyTo().getAddress());
		}
	}
	
	public void setUp()throws Exception
	{
		//setup the RMSBean
		expectedRMSBean = new RMSBean();
		expectedRMSBean.setAnonymousUUID("someAnonymousUUID");
		expectedRMSBean.setAvoidAutoTermination(true);
		expectedRMSBean.setClientCompletedMessages(new RangeString());
		expectedRMSBean.setClosed(true);
		expectedRMSBean.setCreateSeqMsgID("someCSeqMsgId");
		expectedRMSBean.setCreateSequenceMsgStoreKey("someMsgStoreKey");
		expectedRMSBean.setExpectedReplies(1);
		expectedRMSBean.setHighestOutMessageNumber(1);
		expectedRMSBean.setHighestOutRelatesTo("someRelatesTo");
		expectedRMSBean.setInternalSequenceID("someInternalSequenceID");
		expectedRMSBean.setLastActivatedTime(1);
		expectedRMSBean.setLastOutMessage(1);
		expectedRMSBean.setLastSendError(new Exception());
		expectedRMSBean.setLastSendErrorTimestamp(1);
		expectedRMSBean.setNextMessageNumber(1);
		expectedRMSBean.setOfferedEndPoint("someEndPoint");
		expectedRMSBean.setOfferedSequence("offeredSequence");
		expectedRMSBean.setPollingMode(true);
		expectedRMSBean.setReferenceMessageStoreKey("someRefMsgStoreKey");
		expectedRMSBean.setRMVersion(Sandesha2Constants.SPEC_VERSIONS.v1_1);
		expectedRMSBean.setSecurityTokenData("someSecurityToken");
		expectedRMSBean.setSequenceClosedClient(true);
		expectedRMSBean.setSequenceID("someSequenceID");
		expectedRMSBean.setServiceName("someService");
		expectedRMSBean.setSoapVersion(1);
		expectedRMSBean.setTerminateAdded(true);
		expectedRMSBean.setTerminationPauserForCS(true);
		expectedRMSBean.setTimedOut(true);
		expectedRMSBean.setTransaction(null);
		expectedRMSBean.setTransportTo("transportTo");
		expectedRMSBean.setToEndpointReference(epr);
		expectedRMSBean.setReplyToEndpointReference(epr);
		expectedRMSBean.setAcksToEndpointReference(epr);
		
		//setup the RMDBean
		expectedRMDBean = new RMDBean();
		expectedRMDBean.setClosed(true);
		expectedRMDBean.setHighestInMessageId("someMsgId");
		expectedRMDBean.setLastActivatedTime(1);
		expectedRMDBean.setNextMsgNoToProcess(1);
		expectedRMDBean.setOutboundInternalSequence("someSequenceID");
		expectedRMDBean.setOutOfOrderRanges(new RangeString());
		expectedRMDBean.setPollingMode(true);
		expectedRMDBean.setReferenceMessageKey("someRefMsgStoreKey");
		expectedRMDBean.setRMVersion("someVersion");
		expectedRMDBean.setSecurityTokenData("someTokenData");
		expectedRMDBean.setSequenceID("someSequenceID");
		expectedRMDBean.setServerCompletedMessages(new RangeString());
		expectedRMDBean.setServiceName("someService");
		expectedRMDBean.setTerminated(true);
		expectedRMDBean.setToEndpointReference(epr);
		expectedRMDBean.setReplyToEndpointReference(epr);
		expectedRMDBean.setAcksToEndpointReference(epr);
		
		//setup a typical message context - for this we will also need a config context
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
		expectedMessageContext = configContext.createMessageContext();
		expectedMessageContext.setFrom(epr);
		expectedMessageContext.setMessageID("someMessageID");
		expectedMessageContext.setReplyTo(epr);
	}
	
	/**
	 * If called, this method produces/updates the set of .dat files in the resources directory
	 * Since the data set is complete, this method should not be called (and the data set should
	 * not be changed) unless it is due to a considered requirement.
	 */
	private void produceDataSet()throws IOException{
		
		String resourceDir = new File("test-resources").getAbsolutePath();
		
		//write the RMSBean
		{
			FileOutputStream fos = new FileOutputStream(resourceDir + File.separator + rmsDatFileName);
			ObjectOutputStream rmsBeanData = new ObjectOutputStream(fos);
			rmsBeanData.writeObject(expectedRMSBean);
			rmsBeanData.flush();
			fos.flush();
			rmsBeanData.close();
			fos.close();			
		}

		
		//write the RMDBean
		{
			FileOutputStream fos = new FileOutputStream(resourceDir + File.separator + rmdDatFileName);
			ObjectOutputStream rmdBeanData = new ObjectOutputStream(fos);
			rmdBeanData.writeObject(expectedRMDBean);
			rmdBeanData.flush();
			fos.flush();
			rmdBeanData.close();
			fos.close();			
		}

		//write the msg ctx
		{
			FileOutputStream fos = new FileOutputStream(resourceDir + File.separator + msgCtxDatFileName);
			ObjectOutputStream msgCtxData = new ObjectOutputStream(fos);
			msgCtxData.writeObject(expectedMessageContext);
			msgCtxData.flush();
			fos.flush();
			msgCtxData.close();
			fos.close();			
		}
	}
	
	
	/**
	 * This main method, when run, will setup the .dat files that this test expects.
	 * These files should be checked into SVN when generated/modified
	 */
	public static void main(String[] args)throws Exception{
		MessageContextSerializationTest test = new MessageContextSerializationTest("");
		test.setUp();
		test.produceDataSet();
	}

}
