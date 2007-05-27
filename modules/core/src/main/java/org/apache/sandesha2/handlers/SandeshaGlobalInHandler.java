/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *  
 */

package org.apache.sandesha2.handlers;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.Range;
import org.apache.sandesha2.util.RangeString;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.Sequence;

/**
 * The Global handler of Sandesha2. This is only used to check for WSRM 1.0 messages
 * that have a particular way of signalling the last message in a sequence. These
 * checks have to be done before dispatch.
 */

public class SandeshaGlobalInHandler extends AbstractHandler {

	private static final long serialVersionUID = -7187928423123306156L;

	private static final Log log = LogFactory.getLog(SandeshaGlobalInHandler.class);
	
	public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {

		if (log.isDebugEnabled())
			log.debug("Enter: SandeshaGlobalInHandler::invoke, " + msgContext.getEnvelope().getHeader());

		// look at the service to see if RM is totally disabled. This allows the user to disable RM using
		// a property on the service, even when Sandesha is engaged.
		if (msgContext.getAxisService() != null) {
			Parameter unreliableParam = msgContext.getAxisService().getParameter(SandeshaClientConstants.UNRELIABLE_MESSAGE);
			if (null != unreliableParam && "true".equals(unreliableParam.getValue())) {
				log.debug("Exit: SandeshaGlobalInHandler::invoke, Service has disabled RM " + InvocationResponse.CONTINUE);
				return InvocationResponse.CONTINUE;
			}
		}

		// The only work that this handler needs to do is identify messages which
		// follow the WSRM 1.0 convention for sending 'LastMessage' when the sender
		// doesn't have a reliable message to piggyback the last message marker onto.
		// Normally they will identify this scenario with an action marker, but if
		// there is no action at all then we have to check the soap body.
		// Either way, all that this handler need do is set the action back onto
		// the message, so that the dispatchers can allow it to continue. The real
		// processing will be done in the SequenceProcessor.
		String soapAction = msgContext.getSoapAction();
		String wsaAction = msgContext.getWSAAction();
		if(soapAction == null && wsaAction == null) {
			// Look for a WSRM 1.0 sequence header with the lastMessage marker
			SOAPEnvelope env = msgContext.getEnvelope();
			if(env != null) {
				boolean lastMessageHeader = false;
				try {
					SOAPHeader header = env.getHeader();
					if(header != null) {
						Sequence sequence = new Sequence(Sandesha2Constants.SPEC_2005_02.NS_URI);
						sequence.fromOMElement(header);
						if(sequence.getLastMessage() != null) {
							lastMessageHeader = true;
						}
					}
				} catch(Exception e) {
					// Do nothing, we failed to find a Sequence header
				}
				if(lastMessageHeader) {
					SOAPBody body = env.getBody();
					if(body != null && body.getFirstElement() == null) {
						// There is an empty body so we know this is the kind of message
						// that we are looking for.
						if(log.isDebugEnabled()) log.debug("Setting SOAP Action for a WSRM 1.0 last message");
						msgContext.setSoapAction(Sandesha2Constants.SPEC_2005_02.Actions.SOAP_ACTION_LAST_MESSAGE);
					}
				}
			}
		}
    
    // Check if this is an application message and if it is a duplicate
    RMMsgContext rmMsgCtx = MsgInitializer.initializeMessage(msgContext);

    // Set the RMMMessageContext as a property on the message so we can retrieve it later
    msgContext.setProperty(Sandesha2Constants.MessageContextProperties.RM_MESSAGE_CONTEXT, rmMsgCtx);

    
    StorageManager storageManager = 
      SandeshaUtil.getSandeshaStorageManager(rmMsgCtx.getConfigurationContext(), 
          rmMsgCtx.getConfigurationContext().getAxisConfiguration());
    
		//processing any incoming faults.     
    //This is responsible for Sandesha2 specific 
    InvocationResponse response = FaultManager.processMessagesForFaults(rmMsgCtx, storageManager);

    //both application msgs and lastMsg msgs will be processed in the same way here.
    if (rmMsgCtx.getMessageType() == Sandesha2Constants.MessageTypes.APPLICATION ||
    		rmMsgCtx.getMessageType() == Sandesha2Constants.MessageTypes.LAST_MESSAGE) {
      processApplicationMessage(rmMsgCtx);
    }
    
		if (log.isDebugEnabled())
			log.debug("Exit: SandeshaGlobalInHandler::invoke " + response);
		return response;
	}
	
  private static void processApplicationMessage(RMMsgContext rmMsgCtx) throws AxisFault {
    if (log.isDebugEnabled())
      log.debug("Enter: SandeshaGlobalInHandler::processApplicationMessage");
    // Check if this is a duplicate message
    Sequence sequence = (Sequence) rmMsgCtx.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
    String sequenceId = sequence.getIdentifier().getIdentifier();
    long msgNo = sequence.getMessageNumber().getMessageNumber();

    StorageManager storageManager = 
      SandeshaUtil.getSandeshaStorageManager(rmMsgCtx.getConfigurationContext(), 
          rmMsgCtx.getConfigurationContext().getAxisConfiguration());
    
    Transaction transaction = null;
    
    try {
      transaction = storageManager.getTransaction();
    
      // Check that both the Sequence header and message body have been secured properly
      RMDBeanMgr mgr = storageManager.getRMDBeanMgr();
      RMDBean bean = mgr.retrieve(sequenceId);
      
      if(bean != null && bean.getSecurityTokenData() != null) {
        SecurityManager secManager = SandeshaUtil.getSecurityManager(rmMsgCtx.getConfigurationContext());
        
        QName seqName = new QName(rmMsgCtx.getRMNamespaceValue(), Sandesha2Constants.WSRM_COMMON.SEQUENCE);
        
        SOAPEnvelope envelope = rmMsgCtx.getSOAPEnvelope();
        OMElement body = envelope.getBody();
        OMElement seqHeader = envelope.getHeader().getFirstChildWithName(seqName);
        
        SecurityToken token = secManager.recoverSecurityToken(bean.getSecurityTokenData());
        
        secManager.checkProofOfPossession(token, seqHeader, rmMsgCtx.getMessageContext());
        secManager.checkProofOfPossession(token, body, rmMsgCtx.getMessageContext());
      }
    
      if (bean != null) {
        
        if (msgNo == 0) {
          String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.invalidMsgNumber, Long
              .toString(msgNo));
          log.debug(message);
          throw new SandeshaException(message);
        }
    
        // Get the server completed message ranges list
        RangeString serverCompletedMessageRanges = bean.getServerCompletedMessages();
    
        // See if the message is in the list of completed ranges
        boolean msgNoPresentInList = 
          serverCompletedMessageRanges.isMessageNumberInRanges(msgNo);
          
        if (!msgNoPresentInList) {
          serverCompletedMessageRanges.addRange(new Range(msgNo));
          
          storageManager.getRMDBeanMgr().update(bean);
        }
        else {
          if (log.isDebugEnabled())
            log.debug("Detected duplicate message " + msgNo);
          rmMsgCtx.getMessageContext().setRelationships(null);
          // Add the duplicate RM AxisOperation to the message
          AxisOperation duplicateMessageOperation = SpecSpecificConstants.getWSRMOperation(
              Sandesha2Constants.MessageTypes.DUPLICATE_MESSAGE,
              Sandesha2Constants.SPEC_VERSIONS.v1_0,
              rmMsgCtx.getMessageContext().getAxisService());
          rmMsgCtx.getMessageContext().setAxisOperation(duplicateMessageOperation);
        }
              
      } else {
        if (log.isDebugEnabled())
          log.debug("Detected message for no sequence " + msgNo);
        rmMsgCtx.getMessageContext().setRelationships(null);
        // Add the duplicate RM AxisOperation to the message
        AxisOperation duplicateMessageOperation = SpecSpecificConstants.getWSRMOperation(
            Sandesha2Constants.MessageTypes.DUPLICATE_MESSAGE,
            Sandesha2Constants.SPEC_VERSIONS.v1_0,
            rmMsgCtx.getMessageContext().getAxisService());
        rmMsgCtx.getMessageContext().setAxisOperation(duplicateMessageOperation);
      }
      
      if(transaction != null && transaction.isActive()) transaction.commit();
      transaction = null;
    }
    finally {
      if (transaction != null && transaction.isActive())
        transaction.rollback();
    }
    if (log.isDebugEnabled())
      log.debug("Exit: SandeshaGlobalInHandler::processApplicationMessage");
  }
}
