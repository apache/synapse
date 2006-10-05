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

import java.net.UnknownServiceException;

import org.apache.axis2.addressing.AddressingConstants;
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
		else if (Sandesha2Constants.SPEC_2006_08.NS_URI.equals(namespaceValue))
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
			return Sandesha2Constants.SPEC_2006_08.NS_URI;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getCreateSequenceAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.ACTION_CREATE_SEQUENCE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.ACTION_CREATE_SEQUENCE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getCreateSequenceResponseAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.ACTION_CREATE_SEQUENCE_RESPONSE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.ACTION_CREATE_SEQUENCE_RESPONSE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getTerminateSequenceAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.ACTION_TERMINATE_SEQUENCE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.ACTION_TERMINATE_SEQUENCE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getTerminateSequenceResponseAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.ACTION_TERMINATE_SEQUENCE_RESPONSE;
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
			return Sandesha2Constants.SPEC_2006_08.Actions.ACTION_CLOSE_SEQUENCE;
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
			return Sandesha2Constants.SPEC_2006_08.Actions.ACTION_CLOSE_SEQUENCE_RESPONSE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getAckRequestAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return null;  //No action defined for ackRequests
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.ACTION_ACK_REQUEST;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getSequenceAcknowledgementAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.ACTION_SEQUENCE_ACKNOWLEDGEMENT;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.ACTION_SEQUENCE_ACKNOWLEDGEMENT;
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
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.ACTION_MAKE_CONNECTION;
		else {
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
		}
	}
	
	public static String getCreateSequenceSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.SOAP_ACTION_CREATE_SEQUENCE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.SOAP_ACTION_CREATE_SEQUENCE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getCreateSequenceResponseSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.SOAP_ACTION_CREATE_SEQUENCE_RESPONSE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.SOAP_ACTION_CREATE_SEQUENCE_RESPONSE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getTerminateSequenceSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.SOAP_ACTION_TERMINATE_SEQUENCE;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.SOAP_ACTION_TERMINATE_SEQUENCE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getTerminateSequenceResponseSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.SOAP_ACTION_TERMINATE_SEQUENCE_RESPONSE;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getAckRequestSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			throw new SandeshaException ("this spec version does not define a ackRequest SOAP action");
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.SOAP_ACTION_ACK_REQUEST;
		else 
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					specVersion));
	}
	
	public static String getSequenceAcknowledgementSOAPAction (String specVersion) throws SandeshaException {
		if (Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2005_02.Actions.SOAP_ACTION_SEQUENCE_ACKNOWLEDGEMENT;
		else if (Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(specVersion)) 
			return Sandesha2Constants.SPEC_2006_08.Actions.SOAP_ACTION_SEQUENCE_ACKNOWLEDGEMENT;
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
	
	public static String getAddressingFaultAction (String addressingNSURI) throws SandeshaException {
		if (AddressingConstants.Submission.WSA_NAMESPACE.equals(addressingNSURI))
			return "http://schemas.xmlsoap.org/ws/2004/08/addressing/fault";  //this is not available in addressing constants )-:
		else if (AddressingConstants.Final.WSA_NAMESPACE.equals(addressingNSURI))
			return AddressingConstants.Final.WSA_FAULT_ACTION;
		else
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownWSAVersion,
					addressingNSURI));
	}
	
	public static String getSecurityNamespace (String rmNamespace) {
		if(rmNamespace.equals(Sandesha2Constants.SPEC_2005_02.NS_URI)) {
			return Sandesha2Constants.SPEC_2005_02.SEC_NS_URI;
		}

		if(rmNamespace.equals(Sandesha2Constants.SPEC_2006_08.NS_URI)) {
			return Sandesha2Constants.SPEC_2006_08.SEC_NS_URI;
		}

		return null;
	}

	public static String getFaultAction (String addressingNamespace) {
		if (AddressingConstants.Final.WSA_NAMESPACE.equals(addressingNamespace))
			return AddressingConstants.Final.WSA_FAULT_ACTION;
		else if (AddressingConstants.Submission.WSA_NAMESPACE.equals(addressingNamespace))
			return AddressingConstants.Submission.WSA_FAULT_ACTION;
		
		return null;
	}

}
