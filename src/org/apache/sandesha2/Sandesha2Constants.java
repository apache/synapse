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

package org.apache.sandesha2;

import javax.xml.namespace.QName;

import org.apache.axis2.addressing.AddressingConstants;

/**
 * Contains all the Sandesha2Constants of Sandesha2.
 * Please see sub-interfaces to see grouped data.
 */

public interface Sandesha2Constants {

	
	public interface SPEC_VERSIONS {
		String v1_0 = "Spec_2005_02";
		String v1_1 = "Spec_2006_08";
	}
	
	public interface SPEC_2005_02 {
		
		String NS_URI = "http://schemas.xmlsoap.org/ws/2005/02/rm";
		
		String SEC_NS_URI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
		
		String ADDRESSING_NS_URI = AddressingConstants.Submission.WSA_NAMESPACE;
		
		public interface Actions {

			String ACTION_CREATE_SEQUENCE = "http://schemas.xmlsoap.org/ws/2005/02/rm/CreateSequence";

			String ACTION_CREATE_SEQUENCE_RESPONSE = "http://schemas.xmlsoap.org/ws/2005/02/rm/CreateSequenceResponse";

			String ACTION_SEQUENCE_ACKNOWLEDGEMENT = "http://schemas.xmlsoap.org/ws/2005/02/rm/SequenceAcknowledgement";

			String ACTION_TERMINATE_SEQUENCE = "http://schemas.xmlsoap.org/ws/2005/02/rm/TerminateSequence";

			String ACTION_LAST_MESSAGE = "http://schemas.xmlsoap.org/ws/2005/02/rm/LastMessage";

			String SOAP_ACTION_CREATE_SEQUENCE = "http://schemas.xmlsoap.org/ws/2005/02/rm/CreateSequence";

			String SOAP_ACTION_CREATE_SEQUENCE_RESPONSE = "http://schemas.xmlsoap.org/ws/2005/02/rm/CreateSequenceResponse";

			String SOAP_ACTION_SEQUENCE_ACKNOWLEDGEMENT = "http://schemas.xmlsoap.org/ws/2005/02/rm/SequenceAcknowledgement";

			String SOAP_ACTION_TERMINATE_SEQUENCE = "http://schemas.xmlsoap.org/ws/2005/02/rm/TerminateSequence";
			
			String SOAP_ACTION_LAST_MESSAGE = "http://schemas.xmlsoap.org/ws/2005/02/rm/LastMessage";
		}
		
		public interface QNames {
			// Headers
			QName Sequence = new QName(NS_URI, WSRM_COMMON.SEQUENCE);
			QName SequenceAck = new QName(NS_URI, WSRM_COMMON.SEQUENCE_ACK);
			QName AckRequest = new QName(NS_URI, WSRM_COMMON.ACK_REQUESTED);
			QName SequenceFault = new QName(NS_URI, WSRM_COMMON.SEQUENCE_FAULT);
			
			// Body elements
			QName CreateSequence = new QName(NS_URI, WSRM_COMMON.CREATE_SEQUENCE);
			QName CreateSequenceResponse = new QName(NS_URI, WSRM_COMMON.CREATE_SEQUENCE_RESPONSE);
			QName CloseSequence = new QName(NS_URI, WSRM_COMMON.CLOSE_SEQUENCE);
			QName CloseSequenceResponse = new QName(NS_URI, WSRM_COMMON.CLOSE_SEQUENCE_RESPONSE);
			QName TerminateSequence = new QName(NS_URI, WSRM_COMMON.TERMINATE_SEQUENCE);
			QName TerminateSequenceResponse = new QName(NS_URI, WSRM_COMMON.TERMINATE_SEQUENCE_RESPONSE);
			
			// Other elements
			QName Identifier = new QName(NS_URI, WSRM_COMMON.IDENTIFIER);
		}
	}
	
	public interface SPEC_2007_02 {
		
		String NS_URI               = "http://docs.oasis-open.org/ws-rx/wsrm/200702";
		String MC_NS_URI            = "http://docs.oasis-open.org/ws-rx/wsmc/200702";
		String ANONYMOUS_URI_PREFIX = "http://docs.oasis-open.org/ws-rx/wsmc/200702/anonymous?id=";
		String SEC_NS_URI           = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
		String ADDRESSING_NS_URI    = AddressingConstants.Final.WSA_NAMESPACE;

		public interface Actions {
			
			// WS-RM actions
			String ACTION_CREATE_SEQUENCE             = SPEC_2007_02.NS_URI + "/CreateSequence";
			String ACTION_CREATE_SEQUENCE_RESPONSE    = SPEC_2007_02.NS_URI + "/CreateSequenceResponse";
			String ACTION_CLOSE_SEQUENCE              = SPEC_2007_02.NS_URI + "/CloseSequence";
			String ACTION_CLOSE_SEQUENCE_RESPONSE     = SPEC_2007_02.NS_URI + "/CloseSequenceResponse";
			String ACTION_TERMINATE_SEQUENCE          = SPEC_2007_02.NS_URI + "/TerminateSequence";
			String ACTION_TERMINATE_SEQUENCE_RESPONSE = SPEC_2007_02.NS_URI + "/TerminateSequenceResponse";
			String ACTION_SEQUENCE_ACKNOWLEDGEMENT    = SPEC_2007_02.NS_URI + "/SequenceAcknowledgement";
			String ACTION_ACK_REQUEST                 = SPEC_2007_02.NS_URI + "/AckRequested";
			
			// WS-MC actions
			String ACTION_MAKE_CONNECTION             = SPEC_2007_02.MC_NS_URI + "/MakeConnection";

			String SOAP_ACTION_CREATE_SEQUENCE             = ACTION_CREATE_SEQUENCE;
			String SOAP_ACTION_CREATE_SEQUENCE_RESPONSE    = ACTION_CREATE_SEQUENCE_RESPONSE;
			String SOAP_ACTION_CLOSE_SEQUENCE              = ACTION_CLOSE_SEQUENCE;
			String SOAP_ACTION_CLOSE_SEQUENCE_RESPONSE     = ACTION_CLOSE_SEQUENCE_RESPONSE;
			String SOAP_ACTION_TERMINATE_SEQUENCE          = ACTION_TERMINATE_SEQUENCE;
			String SOAP_ACTION_TERMINATE_SEQUENCE_RESPONSE = ACTION_TERMINATE_SEQUENCE_RESPONSE;
			String SOAP_ACTION_SEQUENCE_ACKNOWLEDGEMENT    = ACTION_SEQUENCE_ACKNOWLEDGEMENT;
			String SOAP_ACTION_ACK_REQUEST                 = ACTION_ACK_REQUEST;
			String SOAP_ACTION_MAKE_CONNECTION             = ACTION_MAKE_CONNECTION;
		}
		
		public interface QNames {
			// Headers
			QName Sequence = new QName(NS_URI, WSRM_COMMON.SEQUENCE);
			QName SequenceAck = new QName(NS_URI, WSRM_COMMON.SEQUENCE_ACK);
			QName AckRequest = new QName(NS_URI, WSRM_COMMON.ACK_REQUESTED);
			QName SequenceFault = new QName(NS_URI, WSRM_COMMON.SEQUENCE_FAULT);
			QName UsesSequenceSTR = new QName(NS_URI, WSRM_COMMON.USES_SEQUENCE_STR);
			QName MessagePending = new QName(MC_NS_URI, WSRM_COMMON.MESSAGE_PENDING);
			
			// Body elements
			QName CreateSequence = new QName(NS_URI, WSRM_COMMON.CREATE_SEQUENCE);
			QName CreateSequenceResponse = new QName(NS_URI, WSRM_COMMON.CREATE_SEQUENCE_RESPONSE);
			QName CloseSequence = new QName(NS_URI, WSRM_COMMON.CLOSE_SEQUENCE);
			QName CloseSequenceResponse = new QName(NS_URI, WSRM_COMMON.CLOSE_SEQUENCE_RESPONSE);
			QName TerminateSequence = new QName(NS_URI, WSRM_COMMON.TERMINATE_SEQUENCE);
			QName TerminateSequenceResponse = new QName(NS_URI, WSRM_COMMON.TERMINATE_SEQUENCE_RESPONSE);
			QName MakeConnection = new QName(MC_NS_URI, WSRM_COMMON.MAKE_CONNECTION);
			
			// Other elements
			QName Identifier = new QName(NS_URI, WSRM_COMMON.IDENTIFIER);
		}
	}
	
	public interface WSRM_COMMON {
		
		String NS_PREFIX_RM = "wsrm";

		String NS_PREFIX_MC = "wsmc";

		String MSG_NUMBER = "MessageNumber";

		String LAST_MSG = "LastMessage";

		String SEQUENCE = "Sequence";

		String SEQUENCE_OFFER = "Offer";

		String TERMINATE_SEQUENCE = "TerminateSequence";

		String CLOSE_SEQUENCE = "CloseSequence";
		
		String CLOSE_SEQUENCE_RESPONSE = "CloseSequenceResponse";
		
		String TERMINATE_SEQUENCE_RESPONSE = "TerminateSequenceResponse";
		
		String FAULT_CODE = "FaultCode";

		String DETAIL = "Detail";

		String SEQUENCE_FAULT = "SequenceFault";

		String ACKS_TO = "AcksTo";

		String EXPIRES = "Expires";

		String CREATE_SEQUENCE = "CreateSequence";

		String CREATE_SEQUENCE_RESPONSE = "CreateSequenceResponse";

		String ACK_REQUESTED = "AckRequested";

		String ACK_RANGE = "AcknowledgementRange";

		String UPPER = "Upper";

		String LOWER = "Lower";

		String NACK = "Nack";

		String SEQUENCE_ACK = "SequenceAcknowledgement";

		String IDENTIFIER = "Identifier";

		String MAX_MSG_NUMBER = "MaxMessageNumber";

		String ACCEPT = "Accept";
		
		String NONE = "None";
		
		String FINAL = "Final";
		
		String MAKE_CONNECTION = "MakeConnection";
		
		String ADDRESS = "Address";
		
		String MESSAGE_PENDING = "MessagePending";
		
		String PENDING = "pending";
		
		String USES_SEQUENCE_STR = "UsesSequenceSTR";
		
		String ENDPOINT = "Endpoint";
	}

	public interface WSA {
		
		String NS_PREFIX_ADDRESSING = "wsa";

		String ADDRESS = "Address";

//		String SOAP_FAULT_ACTION = "http://schemas.xmlsoap.org/ws/2004/08/addressing/fault";
		
	}

	public interface MessageTypes {
		int UNKNOWN = 0;

		int CREATE_SEQ = 1;

		int CREATE_SEQ_RESPONSE = 2;

		int APPLICATION = 3;

		int ACK = 4;
		
		int CLOSE_SEQUENCE = 5;

		int CLOSE_SEQUENCE_RESPONSE = 6;
		
		int TERMINATE_SEQ = 7;
		
		int ACK_REQUEST = 8;
		
		int TERMINATE_SEQ_RESPONSE = 9;

		int FAULT_MSG = 10;
		
		int MAKE_CONNECTION_MSG = 11;
		
		int LAST_MESSAGE = 12;
		
		int MAX_MESSAGE_TYPE = 12;
	}

	public interface MessageParts {
		int UNKNOWN = 0;

		int SEQUENCE = 6;

		int SEQ_ACKNOWLEDGEMENT = 7;

		int ADDR_HEADERS = 8;

		int CREATE_SEQ = 9;

		int CREATE_SEQ_RESPONSE = 10;

		int TERMINATE_SEQ = 11;
		
		int CLOSE_SEQUENCE = 12;
		
		int CLOSE_SEQUENCE_RESPONSE = 13;

		int TERMINATE_SEQ_RESPONSE = 14;
		
		int ACK_REQUEST = 15;

		int USES_SEQUENCE_STR = 16;
		
		int MAKE_CONNECTION = 17;
		
		int MESSAGE_PENDING = 18;
		
		int SEQUENCE_FAULT = 19;

		int MAX_MSG_PART_ID = 19;
	}

	public interface SOAPVersion {
		int v1_1 = 1;

		int v1_2 = 2;
	}

	public interface QOS {

		public interface DeliveryAssurance {

			String IN_ORDER = "InOrder";

			String NOT_IN_ORDER = "NotInOrder";

			String DEFAULT_DELIVERY_ASSURANCE = IN_ORDER;
		}

		public interface InvocationType {

			//invocation types
			String EXACTLY_ONCE = "ExactlyOnce";

			String MORE_THAN_ONCE = "MoreThanOnce";

			String DEFAULT_INVOCATION_TYPE = EXACTLY_ONCE;
		}

	}

	public interface BeanMAPs {
		String CREATE_SEQUECE = "CreateSequenceBeanMap";

		String RETRANSMITTER = "RetransmitterBeanMap";

		String SEQUENCE_PROPERTY = "SequencePropertyBeanMap";

		String STORAGE_MAP = "StorageMapBeanMap";

		String NEXT_MESSAGE = "NextMsgBeanMap";
	}

	public interface SOAPFaults {

		public interface Subcodes {

			String SEQUENCE_TERMINATED = "wsrm:SequenceTerminated";
			
			String SEQUENCE_CLOSED = "wsrm:SequenceClosed";

			String UNKNOWN_SEQUENCE = "wsrm:UnknownSequence";

			String INVALID_ACKNOWLEDGEMENT = "wsrm:InvalidAcknowledgement";

			String MESSAGE_NUMBER_ROLEOVER = "wsrm:MessageNumberRollover";

			String LAST_MESSAGE_NO_EXCEEDED = "wsrm:LastMessageNumberExceeded";

			String CREATE_SEQUENCE_REFUSED = "wsrm:CreateSequenceRefused";
			

		}

		public interface FaultType {

			public static final int UNKNOWN_SEQUENCE = 1;

			public static final int MESSAGE_NUMBER_ROLLOVER = 2;

			public static final int INVALID_ACKNOWLEDGEMENT = 3;

			public static final int CREATE_SEQUENCE_REFUSED = 4;
			
			public static final int LAST_MESSAGE_NO_EXCEEDED = 5;

			public static final int SEQUENCE_CLOSED = 6;

			public static final int SEQUENCE_TERMINATED = 7;
		}
	}

	public interface Properties {
		
		String RetransmissionInterval = "RetransmissionInterval";
		
		String AcknowledgementInterval = "AcknowledgementInterval";
		
		String ExponentialBackoff = "ExponentialBackoff";
		
		String InactivityTimeout = "InactivityTimeout";
		
		String InactivityTimeoutMeasure = "InactivityTimeoutMeasure";
		
//		String StorageManager = "StorageManager";
		
		String InMemoryStorageManager = "InMemoryStorageManager";
		
		String PermanentStorageManager = "PermanentStorageManager";
		
		String InOrderInvocation = "InvokeInOrder";
		
		String MessageTypesToDrop = "MessageTypesToDrop";
		
		String RetransmissionCount = "RetransmissionCount";

		String SecurityManager = "SecurityManager";

		String EnableMakeConnection = "EnableMakeConnection";
		
		String EnableRMAnonURI = "EnableRMAnonURI";
		
		String UseMessageSerialization = "UseMessageSerialization";
		
		public interface DefaultValues {
			
			int RetransmissionInterval = 6000;
			
			int AcknowledgementInterval = 3000;
			
			boolean ExponentialBackoff = true;
			
			int InactivityTimeout = -1;
			
			String InactivityTimeoutMeasure = "seconds";   //this can be - seconds,minutes,hours,days
			
//			String StorageManager = "org.apache.sandesha2.storage.inmemory.InMemoryStorageManager";
		
			String InMemoryStorageManager = "org.apache.sandesha2.storage.inmemory.InMemoryStorageManager";
			
			String PermanentStorageManager = "org.apache.sandesha2.storage.inmemory.InMemoryStorageManager";
			
			boolean InvokeInOrder = true;
			
			String MessageTypesToDrop=VALUE_NONE;
			
			int RetransmissionCount = 8;
			
			int MaximumRetransmissionCount = 10;
			
			String SecurityManager = "org.apache.sandesha2.security.dummy.DummySecurityManager";

			boolean EnableMakeConnection = true;
			
			boolean EnableRMAnonURI = true;
			
			boolean UseMessageSerialization = false;
			
			boolean enforceRM = false;
		}
	}
	
	public interface DatabaseParams {
		
	}
	
	static final String IN_HANDLER_NAME = "SandeshaInHandler";

	static final String OUT_HANDLER_NAME = "SandeshaOutHandler";

	static final String GLOBAL_IN_HANDLER_NAME = "GlobalInHandler";

	static final String APPLICATION_PROCESSING_DONE = "Sandesha2AppProcessingDone";

	static final String ACK_WRITTEN = "AckWritten";

	int INVOKER_SLEEP_TIME = 1000;

	int SENDER_SLEEP_TIME = 500;

	int CLIENT_SLEEP_TIME = 10000;

	int TERMINATE_DELAY = 100;

	static final String TEMP_SEQUENCE_ID = "uuid:tempID";

	static final String PROPERTY_FILE = "sandesha2.properties";
	
	static final String VALUE_NONE = "none";
	
	static final String VALUE_EMPTY = "empty";
	
	static final String VALUE_TRUE = "true";
	
	static final String VALUE_FALSE = "false";
	
	static final String MESSAGE_STORE_KEY = "Sandesha2MessageStoreKey";

	static final String ORIGINAL_TRANSPORT_OUT_DESC = "Sandesha2OriginalTransportSender";
	
	static final String SET_SEND_TO_TRUE = "Sandesha2SetSendToTrue";
	
	static final String MESSAGE_TYPE = "Sandesha2MessageType";
	
	static final String QUALIFIED_FOR_SENDING = "Sandesha2QualifiedForSending";  //Sender will send messages only if this property is null (not set) or true.

	static final String EXECUTIN_CHAIN_SEPERATOR = ".";
	
	static final String INTERNAL_SEQUENCE_PREFIX = "Sandesha2InternalSequence";
	
	static final String SANDESHA_PROPERTY_BEAN = "Sandesha2PropertyBean";
	
	static final String LIST_SEPERATOR = ",";
	
	static final String INMEMORY_STORAGE_MANAGER = "inmemory";
	
	static final String PERMANENT_STORAGE_MANAGER = "persistent";
	
	static final String DEFAULT_STORAGE_MANAGER = INMEMORY_STORAGE_MANAGER;
	
	static final String POLLING_MANAGER = "PollingManager";
	
	static final String STORAGE_MANAGER_PARAMETER  = "Sandesha2StorageManager";
	
	static final String POST_FAILURE_MESSAGE = "PostFailureMessage";
	
	static final String MODULE_CLASS_LOADER = "Sandesha2ModuleClassLoader";
	
	static final String SECURITY_MANAGER = "Sandesha2SecurityManager";
	
	static final String RM_IN_OUT_OPERATION_NAME = "RMInOutOperation";
	
	static final String RM_IN_ONLY_OPERATION = "RMInOnlyOperation";
	
	static final String RETRANSMITTABLE_PHASES = "RMRetransmittablePhases";
	
	static final String RM_ANON_UUID = "RMAnonymousUUID";
	
	static final String propertiesToCopyFromReferenceMessage = "propertiesToCopyFromReferenceMessage";
	
	static final String propertiesToCopyFromReferenceRequestMessage = "propertiesToCopyFromReferenceRequestMessage";
	
	static final String MSG_NO_OF_IN_MSG = "MsgNoOfInMsg";
	
	static final String MAKE_CONNECTION_RESPONSE = "MakeConnectionResponse";
	
	static final String [] SPEC_NS_URIS = {
			SPEC_2005_02.NS_URI,
			SPEC_2007_02.NS_URI
	};
	
	public interface MessageContextProperties{
		static final String INTERNAL_SEQUENCE_ID = "Sandesha2InternalSequenceId";
		static final String SEQUENCE_ID = "WSRMSequenceId";
		static final String MESSAGE_NUMBER = "WSRMMessageNumber";
		static final String SECURITY_TOKEN = "SecurityToken";
		static final String INBOUND_SEQUENCE_ID    = "Sandesha2InboundSequenceId";
		static final String INBOUND_MESSAGE_NUMBER = "Sandesha2InboundMessageNumber";
		static final String INBOUND_LAST_MESSAGE   = "Sandesha2InboundLastMessage";
		static final String MAKECONNECTION_ENTRY   = "Sandesha2MakeConnectionEntry";
	}
    
    public interface Assertions {
        
        public static final String URI_POLICY_NS = "http://schemas.xmlsoap.org/ws/2004/09/policy";
        public static final String URI_RM_POLICY_NS = "http://ws.apache.org/sandesha2/policy";
        
        public static final String ATTR_WSRM = "wsrm";
        public static final String ATTR_WSP = "wsp";
        
        public static final String ELEM_POLICY = "Policy";
        public static final String ELEM_RMASSERTION = "RMAssertion";
        public static final String ELEM_ACK_INTERVAL = "AcknowledgementInterval";
        public static final String ELEM_RETRANS_INTERVAL = "RetransmissionInterval";
        public static final String ELEM_MAX_RETRANS_COUNT = "MaximumRetransmissionCount";
        public static final String ELEM_EXP_BACKOFF = "ExponentialBackoff";
        public static final String ELEM_INACTIVITY_TIMEOUT = "InactivityTimeout";
        public static final String ELEM_INACTIVITY_TIMEOUT_MEASURES = "InactivityTimeoutMeasure";
        public static final String ELEM_INVOKE_INORDER = "InvokeInOrder";
        public static final String ELEM_MSG_TYPES_TO_DROP = "MessageTypesToDrop";
        public static final String ELEM_STORAGE_MGR = "StorageManagers";
        public static final String ELEM_SEC_MGR = "SecurityManager";
        public static final String ELEM_INMEMORY_STORAGE_MGR = "InMemoryStorageManager";
        public static final String ELEM_PERMANENT_STORAGE_MGR = "PermanentStorageManager";
        public static final String ELEM_MAKE_CONNECTION = "MakeConnection";
        public static final String ELEM_ENABLED = "Enabled";
        public static final String ELEM_USE_RM_ANON_URI = "UseRMAnonURI";
        public static final String ELEM_USE_SERIALIZATION = "UseMessageSerialization";
        public static final String ELEM_ENFORCE_RM = "EnforceRM";
        
        public static final QName Q_ELEM_POLICY = new QName(URI_POLICY_NS, ELEM_POLICY, ATTR_WSP);
        public static final QName Q_ELEM_RMASSERTION = new QName(URI_RM_POLICY_NS, ELEM_RMASSERTION, ATTR_WSRM);
        public static final QName Q_ELEM__RMBEAN = new QName(URI_RM_POLICY_NS, "RMBean", ATTR_WSRM);
        public static final QName Q_ELEM_ACK_INTERVAL = new QName(URI_RM_POLICY_NS, ELEM_ACK_INTERVAL, ATTR_WSRM);
        public static final QName Q_ELEM_RETRANS_INTERVAL = new QName(URI_RM_POLICY_NS, ELEM_RETRANS_INTERVAL, ATTR_WSRM);
        public static final QName Q_ELEM_MAX_RETRANS_COUNT = new QName(URI_RM_POLICY_NS, ELEM_MAX_RETRANS_COUNT, ATTR_WSRM);
        public static final QName Q_ELEM_EXP_BACKOFF = new QName(URI_RM_POLICY_NS, ELEM_EXP_BACKOFF, ATTR_WSRM);
        public static final QName Q_ELEM_INACTIVITY_TIMEOUT = new QName(URI_RM_POLICY_NS, ELEM_INACTIVITY_TIMEOUT, ATTR_WSRM);
        public static final QName Q_ELEM_INACTIVITY_TIMEOUT_MEASURES = new QName(URI_RM_POLICY_NS, ELEM_INACTIVITY_TIMEOUT_MEASURES, ATTR_WSRM);
        public static final QName Q_ELEM_INVOKE_INORDER = new QName(URI_RM_POLICY_NS, ELEM_INVOKE_INORDER, ATTR_WSRM);
        public static final QName Q_ELEM_MSG_TYPES_TO_DROP = new QName(URI_RM_POLICY_NS, ELEM_MSG_TYPES_TO_DROP, ATTR_WSRM);
        public static final QName Q_ELEM_STORAGE_MGR =new QName(URI_RM_POLICY_NS, ELEM_STORAGE_MGR, ATTR_WSRM);
        public static final QName Q_ELEM_SEC_MGR = new QName(URI_RM_POLICY_NS, ELEM_SEC_MGR, ATTR_WSRM);
        public static final QName Q_ELEM_INMEMORY_STORAGE_MGR =new QName(URI_RM_POLICY_NS, ELEM_INMEMORY_STORAGE_MGR, ATTR_WSRM);
        public static final QName Q_ELEM_PERMANENT_STORAGE_MGR =new QName(URI_RM_POLICY_NS, ELEM_PERMANENT_STORAGE_MGR, ATTR_WSRM);
        public static final QName Q_ELEM_MAKE_CONNECTION = new QName(URI_RM_POLICY_NS, ELEM_MAKE_CONNECTION, ATTR_WSRM);
        public static final QName Q_ELEM_ENABLED = new QName(URI_RM_POLICY_NS, ELEM_ENABLED, ATTR_WSRM);
        public static final QName Q_ELEM_USE_RM_ANON_URI = new QName(URI_RM_POLICY_NS, ELEM_USE_RM_ANON_URI, ATTR_WSRM);
        public static final QName Q_ELEM_USE_SERIALIZATION = new QName(URI_RM_POLICY_NS, ELEM_USE_SERIALIZATION, ATTR_WSRM);
        public static final QName Q_ELEM_ENFORCE_RM = new QName(URI_RM_POLICY_NS, ELEM_ENFORCE_RM, ATTR_WSRM);
        
    }
}
