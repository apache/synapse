/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.msgprocessors;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.Sequence;

public class LastMessageProcessor  implements MsgProcessor {

	
	
	
	public boolean processInMessage(RMMsgContext rmMsgCtx, Transaction transaction) throws AxisFault {
		processLastMessage(rmMsgCtx);
		return true;
	}

	public boolean processOutMessage(RMMsgContext rmMsgCtx, Transaction transaction) {
		return false;
	}

	public static void processLastMessage(RMMsgContext rmMsgCtx) throws AxisFault {
		
		if (!Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(rmMsgCtx.getRMSpecVersion()))
			return;
		
		Sequence sequence = rmMsgCtx.getSequence();
		String sequenceId = sequence.getIdentifier().getIdentifier();
		
		ConfigurationContext configurationContext = rmMsgCtx.getConfigurationContext();
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(
							configurationContext, configurationContext.getAxisConfiguration());
		
		RMDBeanMgr rmdMgr = storageManager.getRMDBeanMgr();
		RMDBean rmdBean = rmdMgr.retrieve(sequenceId);
		String outBoundInternalSequence = rmdBean.getOutboundInternalSequence();
		
		RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();
		RMSBean findBean = new RMSBean ();
		findBean.setInternalSequenceID(outBoundInternalSequence);
		RMSBean rmsBean = rmsBeanMgr.findUnique (findBean);
		
		if (rmsBean!=null && rmsBean.getLastOutMessage()<=0) {
			//there is a RMS sequence without a LastMsg entry
			
			MessageContext msgContext = rmMsgCtx.getMessageContext();
			
			AxisOperation operation = SpecSpecificConstants.getWSRMOperation(Sandesha2Constants.MessageTypes.LAST_MESSAGE, 
					rmMsgCtx.getRMSpecVersion() , msgContext.getAxisService());
			MessageContext outMessageContext = SandeshaUtil.createNewRelatedMessageContext(rmMsgCtx, operation);
			
			outMessageContext.setServerSide(true);
			
			outMessageContext.setTransportOut(msgContext.getTransportOut());
			outMessageContext.setProperty (Constants.OUT_TRANSPORT_INFO, msgContext.getProperty(Constants.OUT_TRANSPORT_INFO));
			outMessageContext.setProperty (MessageContext.TRANSPORT_OUT, msgContext.getProperty(MessageContext.TRANSPORT_OUT));

			//add the SOAP envelope with body null
			SOAPFactory factory = (SOAPFactory) msgContext.getEnvelope().getOMFactory();
			SOAPEnvelope envelope = factory.getDefaultEnvelope();
			outMessageContext.setEnvelope(envelope);
			
			//set the LastMessageAction and the property
			if (outMessageContext.getOptions()==null)
				outMessageContext.setOptions(new Options ());
			
			OperationContext operationContext = outMessageContext.getOperationContext();
			String inboundSequenceId = (String) msgContext.getProperty(Sandesha2Constants.MessageContextProperties.INBOUND_SEQUENCE_ID);
			operationContext.setProperty(Sandesha2Constants.MessageContextProperties.INBOUND_SEQUENCE_ID, 
					inboundSequenceId);
			
			Long inboundMSgNo = (Long) msgContext.getProperty(Sandesha2Constants.MessageContextProperties.INBOUND_MESSAGE_NUMBER);
			operationContext.setProperty(Sandesha2Constants.MessageContextProperties.INBOUND_MESSAGE_NUMBER, 
					inboundMSgNo);
			
			outMessageContext.getOptions().setAction(Sandesha2Constants.SPEC_2005_02.Actions.ACTION_LAST_MESSAGE);

			//says that the inbound msg of this was a LastMessage - so the new msg will also be a LastMessage
			outMessageContext.setProperty(Sandesha2Constants.MessageContextProperties.INBOUND_LAST_MESSAGE, Boolean.TRUE);
			outMessageContext.setProperty(RequestResponseTransport.TRANSPORT_CONTROL, msgContext.getProperty(RequestResponseTransport.TRANSPORT_CONTROL));
			
			AxisEngine.send(outMessageContext);
			TransportUtils.setResponseWritten(msgContext, true);
		}
		
		
		
	}

}
