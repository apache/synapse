/*
 * Copyright  1999-2004 The Apache Software Foundation.
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

package org.apache.sandesha2.util;

import java.util.MissingResourceException;

import javax.xml.namespace.QName;

import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * To get values which are different in the RM specs in a convenient manner.
 */

public class SpecSpecificConstants {

	
	public static String getSpecVersionString (String namespaceValue) throws SandeshaException {
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceValue))
			return Sandesha2Constants.SPEC_VERSIONS.v1_0;
		else if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(namespaceValue))
			return Sandesha2Constants.SPEC_VERSIONS.v1_1;
		else
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
	}
	
	public static String getRMNamespaceValue (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.NS_URI;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.NS_URI;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getCreateSequenceAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.ACTION_CREATE_SEQUENCE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.ACTION_CREATE_SEQUENCE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getCreateSequenceResponseAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.ACTION_CREATE_SEQUENCE_RESPONSE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.ACTION_CREATE_SEQUENCE_RESPONSE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getTerminateSequenceAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.ACTION_TERMINATE_SEQUENCE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.ACTION_TERMINATE_SEQUENCE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getTerminateSequenceResponseAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.ACTION_TERMINATE_SEQUENCE_RESPONSE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec));
	}
	
	public static String getCloseSequenceAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.closeSequenceSpecLevel,
					specVersion));
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.ACTION_CLOSE_SEQUENCE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}

	public static String getCloseSequenceResponseAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.closeSequenceSpecLevel,
					specVersion));
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.ACTION_CLOSE_SEQUENCE_RESPONSE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getAckRequestAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return null;  //No action defined for ackRequests
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.ACTION_ACK_REQUEST;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getSequenceAcknowledgementAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.ACTION_SEQUENCE_ACKNOWLEDGEMENT;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.ACTION_SEQUENCE_ACKNOWLEDGEMENT;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getMakeConnectionAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) {
			String message = "MakeConnection is not supported in this RM version";
			throw new SandeshaException (message);
		}
		return Sandesha2Constants.SPEC_2007_02.Actions.ACTION_MAKE_CONNECTION;
	}
	
	public static String getCreateSequenceSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.SOAP_ACTION_CREATE_SEQUENCE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.SOAP_ACTION_CREATE_SEQUENCE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getCreateSequenceResponseSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.SOAP_ACTION_CREATE_SEQUENCE_RESPONSE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.SOAP_ACTION_CREATE_SEQUENCE_RESPONSE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getTerminateSequenceSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.SOAP_ACTION_TERMINATE_SEQUENCE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.SOAP_ACTION_TERMINATE_SEQUENCE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getTerminateSequenceResponseSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.SOAP_ACTION_TERMINATE_SEQUENCE_RESPONSE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getAckRequestSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			throw new SandeshaException ("this spec version does not define a ackRequest SOAP action");
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.SOAP_ACTION_ACK_REQUEST;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getSequenceAcknowledgementSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.SOAP_ACTION_SEQUENCE_ACKNOWLEDGEMENT;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.SOAP_ACTION_SEQUENCE_ACKNOWLEDGEMENT;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static boolean isTerminateSequenceResponseRequired (String specVersion)  throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return false;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return true;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static boolean isLastMessageIndicatorRequired (String specVersion)  throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return true;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return false;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static boolean isAckFinalAllowed (String specVersion)  throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return false;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return true;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static boolean isAckNoneAllowed (String specVersion)  throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return false;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return true;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static boolean isSequenceClosingAllowed (String specVersion)  throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return false;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return true;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getDefaultSpecVersion () {
		return Sandesha2Constants.SPEC_VERSIONS.v1_0;
	}
	
	public static String getAddressingAnonymousURI (String addressingNSURI) throws SandeshaException {
		if (AddressingConstants.Submission.WSA_NAMESPACE.equals(addressingNSURI))
			return AddressingConstants.Submission.WSA_ANONYMOUS_URL;
		else if (AddressingConstants.Final.WSA_NAMESPACE.equals(addressingNSURI))
			return AddressingConstants.Final.WSA_ANONYMOUS_URL;
		else
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownWSAVersion,
					addressingNSURI));
	}
	
	public static String getAddressingFaultAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return AddressingConstants.Final.WSA_FAULT_ACTION;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2007_02.Actions.SOAP_ACTION_FAULT;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getAddressingNamespace(String rmNamespace) throws SandeshaException {
		if(Sandesha2Constants.SPEC_2007_02.NS_URI.equals(rmNamespace)) {
			return Sandesha2Constants.SPEC_2007_02.ADDRESSING_NS_URI;
		} else if(Sandesha2Constants.SPEC_2005_02.NS_URI.equals(rmNamespace)) {
			return Sandesha2Constants.SPEC_2005_02.ADDRESSING_NS_URI;
		}

		throw new SandeshaException (SandeshaMessageHelper.getMessage(
				SandeshaMessageKeys.unknownRMNamespace,	rmNamespace));
	}
	
	public static String getSecurityNamespace (String rmNamespace) {
		if(rmNamespace.equals(Sandesha2Constants.SPEC_2005_02.NS_URI)) {
			return Sandesha2Constants.SPEC_2005_02.SEC_NS_URI;
		}

		if(rmNamespace.equals(Sandesha2Constants.SPEC_2007_02.NS_URI)) {
			return Sandesha2Constants.SPEC_2007_02.SEC_NS_URI;
		}

		return null;
	}
	
	public static AxisOperation getWSRMOperation(int messageType, String rmSpecLevel, AxisService service)
	throws SandeshaException
	{
		// This table needs to be kept in sync with the operations defined in the
		// sandesha module.xml. The module.xml defintions are used to identify
		// messages as they come into the server, and this table us used to pick
		// the correct operation as we make invocations. Because of this, the
		// tables are opposites of one another.
		AxisOperation result = null;
		if(Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(rmSpecLevel)) {
			switch(messageType) {
			case Sandesha2Constants.MessageTypes.CREATE_SEQ:
				result = service.getOperation(Sandesha2Constants.RM_OUT_IN_OPERATION);
				break;
			case Sandesha2Constants.MessageTypes.TERMINATE_SEQ:
			case Sandesha2Constants.MessageTypes.ACK:
			case Sandesha2Constants.MessageTypes.ACK_REQUEST:
			case Sandesha2Constants.MessageTypes.LAST_MESSAGE:
				result = service.getOperation(Sandesha2Constants.RM_OUT_ONLY_OPERATION);
				break;			
			case Sandesha2Constants.MessageTypes.DUPLICATE_MESSAGE_IN_ONLY:
			  result = service.getOperation(Sandesha2Constants.RM_DUPLICATE_IN_ONLY_OPERATION);
			  break;
			case Sandesha2Constants.MessageTypes.DUPLICATE_MESSAGE_IN_OUT:
				result = service.getOperation(Sandesha2Constants.RM_DUPLICATE_IN_OUT_OPERATION);
				break;
			case Sandesha2Constants.MessageTypes.POLL_RESPONSE_MESSAGE:
				result = service.getOperation(Sandesha2Constants.RM_OUT_ONLY_OPERATION);
				break;	
			}
		} else if(Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(rmSpecLevel)) {
			switch(messageType) {
			case Sandesha2Constants.MessageTypes.CREATE_SEQ:
			case Sandesha2Constants.MessageTypes.CLOSE_SEQUENCE:
			case Sandesha2Constants.MessageTypes.TERMINATE_SEQ:
				result = service.getOperation(Sandesha2Constants.RM_OUT_IN_OPERATION);
				break;
			case Sandesha2Constants.MessageTypes.ACK:
			case Sandesha2Constants.MessageTypes.ACK_REQUEST:
				result = service.getOperation(Sandesha2Constants.RM_OUT_ONLY_OPERATION);
				break;
			case Sandesha2Constants.MessageTypes.POLL_RESPONSE_MESSAGE:
				result = service.getOperation(Sandesha2Constants.RM_OUT_ONLY_OPERATION);
				break;		
			}
		}
		
		// MakeConnection is defined in its own spec, not the RM spec.
		if(messageType == Sandesha2Constants.MessageTypes.MAKE_CONNECTION_MSG) {
			result = service.getOperation(Sandesha2Constants.RM_OUT_IN_OPERATION);
		}
		
		if(result == null) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.couldNotFindOperation,
					Integer.toString(messageType), rmSpecLevel);
			throw new SandeshaException(message);
		}
		
		return result;
	}

	public static QName getFaultSubcode(String namespaceValue, int faultType) 
	throws SandeshaException, MissingResourceException {
		QName result = null;
		
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceValue)) {
			switch (faultType) {
				case Sandesha2Constants.SOAPFaults.FaultType.UNKNOWN_SEQUENCE:
					result = Sandesha2Constants.SPEC_2005_02.QNames.UnknownSequence;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.MESSAGE_NUMBER_ROLLOVER:
					result = Sandesha2Constants.SPEC_2005_02.QNames.MessageNumberRollover;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.INVALID_ACKNOWLEDGEMENT:
					result = Sandesha2Constants.SPEC_2005_02.QNames.InvalidAcknowledgement;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.CREATE_SEQUENCE_REFUSED:
					result = Sandesha2Constants.SPEC_2005_02.QNames.CreateSequenceRefused;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.LAST_MESSAGE_NO_EXCEEDED:
					result = Sandesha2Constants.SPEC_2005_02.QNames.LastMessageNoExceeded;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.SEQUENCE_CLOSED:
					result = Sandesha2Constants.SPEC_2005_02.QNames.SequenceClosed;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.SEQUENCE_TERMINATED:
					result = Sandesha2Constants.SPEC_2005_02.QNames.SequenceTerminated;
					break;
			}
		}
		else if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(namespaceValue)) {
			switch (faultType) {
				case Sandesha2Constants.SOAPFaults.FaultType.UNKNOWN_SEQUENCE:
					result = Sandesha2Constants.SPEC_2007_02.QNames.UnknownSequence;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.MESSAGE_NUMBER_ROLLOVER:
					result = Sandesha2Constants.SPEC_2007_02.QNames.MessageNumberRollover;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.INVALID_ACKNOWLEDGEMENT:
					result = Sandesha2Constants.SPEC_2007_02.QNames.InvalidAcknowledgement;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.CREATE_SEQUENCE_REFUSED:
					result = Sandesha2Constants.SPEC_2007_02.QNames.CreateSequenceRefused;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.LAST_MESSAGE_NO_EXCEEDED:
					result = Sandesha2Constants.SPEC_2007_02.QNames.LastMessageNoExceeded;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.SEQUENCE_CLOSED:
					result = Sandesha2Constants.SPEC_2007_02.QNames.SequenceClosed;
					break;
				case Sandesha2Constants.SOAPFaults.FaultType.SEQUENCE_TERMINATED:
					result = Sandesha2Constants.SPEC_2007_02.QNames.SequenceTerminated;
					break;
			}
		}
		else
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					namespaceValue));
		
		return result;
  }
	
	public static boolean sendAckInBackChannel (int messageType) {
		boolean result = true;
		
		switch (messageType) {
			case Sandesha2Constants.MessageTypes.LAST_MESSAGE:
				result = false;
				break;
		}
		
		return result;
	}

}
