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
		
		public interface Actions {

			String ACTION_CREATE_SEQUENCE = "http://schemas.xmlsoap.org/ws/2005/02/rm/CreateSequence";

			String ACTION_CREATE_SEQUENCE_RESPONSE = "http://schemas.xmlsoap.org/ws/2005/02/rm/CreateSequenceResponse";

			String ACTION_SEQUENCE_ACKNOWLEDGEMENT = "http://schemas.xmlsoap.org/ws/2005/02/rm/SequenceAcknowledgement";

			String ACTION_TERMINATE_SEQUENCE = "http://schemas.xmlsoap.org/ws/2005/02/rm/TerminateSequence";

			String SOAP_ACTION_CREATE_SEQUENCE = "http://schemas.xmlsoap.org/ws/2005/02/rm/CreateSequence";

			String SOAP_ACTION_CREATE_SEQUENCE_RESPONSE = "http://schemas.xmlsoap.org/ws/2005/02/rm/CreateSequenceResponse";

			String SOAP_ACTION_SEQUENCE_ACKNOWLEDGEMENT = "http://schemas.xmlsoap.org/ws/2005/02/rm/SequenceAcknowledgement";

			String SOAP_ACTION_TERMINATE_SEQUENCE = "http://schemas.xmlsoap.org/ws/2005/02/rm/TerminateSequence";
		}
	}
	
	public interface SPEC_2006_08 {
		
		String NS_URI               = "http://docs.oasis-open.org/ws-rx/wsrm/200608";
		String ANONYMOUS_URI_PREFIX = "http://docs.oasis-open.org/ws-rx/wsrm/200608/anonymous?id=";
		String SEC_NS_URI           = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

		public interface Actions {
			
			String ACTION_CREATE_SEQUENCE = "http://docs.oasis-open.org/ws-rx/wsrm/200608/CreateSequence";

			String ACTION_CREATE_SEQUENCE_RESPONSE = "http://docs.oasis-open.org/ws-rx/wsrm/200608/CreateSequenceResponse";

			String ACTION_SEQUENCE_ACKNOWLEDGEMENT = "http://docs.oasis-open.org/ws-rx/wsrm/200608/SequenceAcknowledgement";

			String ACTION_TERMINATE_SEQUENCE = "http://docs.oasis-open.org/ws-rx/wsrm/200608/TerminateSequence";
			
			String ACTION_TERMINATE_SEQUENCE_RESPONSE = "http://docs.oasis-open.org/ws-rx/wsrm/200608/TerminateSequenceResponse";
			
			String ACTION_ACK_REQUEST = "http://docs.oasis-open.org/ws-rx/wsrm/200608/AckRequested";
			
			String ACTION_CLOSE_SEQUENCE = "http://docs.oasis-open.org/ws-rx/wsrm/200608/CloseSequence";
			
			String ACTION_CLOSE_SEQUENCE_RESPONSE = "http://docs.oasis-open.org/ws-rx/wsrm/200608/CloseSequenceResponse";
			
			String ACTION_MAKE_CONNECTION = "http://docs.oasis-open.org/ws-rx/wsrm/200608/MakeConnection";
			
			String SOAP_ACTION_CREATE_SEQUENCE = "http://docs.oasis-open.org/ws-rx/wsrm/200608/CreateSequence";

			String SOAP_ACTION_CREATE_SEQUENCE_RESPONSE = "http://docs.oasis-open.org/ws-rx/wsrm/200608/CreateSequenceResponse";

			String SOAP_ACTION_SEQUENCE_ACKNOWLEDGEMENT = "http://docs.oasis-open.org/ws-rx/wsrm/200608/SequenceAcknowledgement";

			String SOAP_ACTION_TERMINATE_SEQUENCE = "http://docs.oasis-open.org/ws-rx/wsrm/200608/TerminateSequence";
			
			String SOAP_ACTION_TERMINATE_SEQUENCE_RESPONSE = "http://docs.oasis-open.org/ws-rx/wsrm/200608/TerminateSequenceResponse";
			
			String SOAP_ACTION_ACK_REQUEST = "http://docs.oasis-open.org/ws-rx/wsrm/200608/AckRequested";
			
			String SOAP_ACTION_CLOSE_SEQUENCE = "http://docs.oasis-open.org/ws-rx/wsrm/200608/CloseSequence";
			
			String SOAP_ACTION_MAKE_CONNECTION = "http://docs.oasis-open.org/ws-rx/wsrm/200608/MakeConnection";
			                                      
		}
	}
	
	public interface WSRM_COMMON {
		
		String NS_PREFIX_RM = "wsrm";

		String MSG_NUMBER = "MessageNumber";

		String LAST_MSG = "LastMessage";

		String SEQUENCE = "Sequence";

		String SEQUENCE_OFFER = "Offer";

		String TERMINATE_SEQUENCE = "TerminateSequence";

		String CLOSE_SEQUENCE = "CloseSequence";
		
		String CLOSE_SEQUENCE_RESPONSE = "CloseSequenceResponse";
		
		String TERMINATE_SEQUENCE_RESPONSE = "TerminateSequenceResponse";
		
		String FAULT_CODE = "FaultCode";

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

		int MAX_MSG_PART_ID = 18;
	}

	public interface SequenceProperties {
		
		String RM_SPEC_VERSION = "WSRMSpecVersion";

		String ALL_SEQUENCES = "AllSequences"; //this is not a sequence
											   // property. This is used as the
											   // sequenceId to share data b/w
											   // sequences
		
		//Addressing version of a sequence. All messages of a sequence should have this addressing versio
		String ADDRESSING_NAMESPACE_VALUE = "AddressingNamespaceValue";

		//For incoming sequences this gives the msg no's of the messages that were
		//received (may be an ack was sent - depending on the policy)
		//For out going sequences this gives the messages that were sent and that were successfully
		//acked by the other end point.
		String CLIENT_COMPLETED_MESSAGES = "ClientCompletedMessages";
		String SERVER_COMPLETED_MESSAGES = "ServerCompletedMessages";
		
		//For IN_ORDER sequences, we can have finite ranges of messages that can be
		//delivered out of order. These are maintained as a String that is consistent
		//with the form described in  org.apache.sandesha2.util.RangeString
		String OUT_OF_ORDER_RANGES = "OutOfOrderRanges";
		
		String TO_EPR = "ToEPR";

		String ACKS_TO_EPR = "acksToEPR";

		String INTERNAL_SEQUENCE_ID = "TempSequenceId";

		String REPLY_TO_EPR = "ReplyToEPR";

		String APP_MSG_PROCESSOR_LIST = "AppMsgProcessorList";

		String OUT_CREATE_SEQUENCE_SENT = "OutCreateSeqSent";

		String NEXT_MESSAGE_NUMBER = "NextMsgNo";

		String INCOMING_SEQUENCE_LIST = "IncomingSequenceList";

		String CHECK_RESPONSE = "CheckResponse";

		String OFFERED_SEQUENCE = "OfferedSequence";

		String TERMINATE_ADDED = "TerminateAdded";
		
		String TERMINATE_RECEIVED = "TerminateReceived";
		
		String LAST_ACTIVATED_TIME = "LastActivatedTime";
		
		String NO_OF_OUTGOING_MSGS_ACKED = "NoOfOutGoingMessagesAcked";
		
		String TRANSPORT_TO = "TransportTo";
		
		String OUT_SEQ_ACKSTO = "OutSequenceAcksTo";
		
		String SEQUENCE_CLOSED = "SequenceClosed";
		
		String SEQUENCE_CLOSED_CLIENT = "SequenceClosedClient"; //indicates the client has sent a close sequence
		
		String SEQUENCE_TERMINATED = "SequenceTerminated";
		
		String SEQUENCE_TIMED_OUT = "SequenceTimedOut";
		
//		String LAST_MESSAGE = "LastMessage";

		String REQUEST_SIDE_SEQUENCE_ID = "RequestSideSequenceID"; 		//used only at the server side
		
		// A pair of properties to keep track of the highest received message
		// for an inbound sequence.
		String HIGHEST_IN_MSG_NUMBER = "HighestInMsgNumber";
		String HIGHEST_IN_MSG_ID = "HighestInMsgId";
		
		// Once an inbound sequence is closed, or we receive a message with the
		// 'LastMessage' marker, we record the message id of the highest message
		// in the sequence.
		String LAST_IN_MSG_ID = "LastInMessageId";
		
		// A pair pf properties to keep track of the highest transmitted message
		// for an outbound sequence.
		String HIGHEST_OUT_MSG_NUMBER = "HighestOutMsgNumber";
		String HIGHEST_OUT_RELATES_TO = "HighestOutRelatesTo";

		// Once the last message for any outbound sequence has been acknowledged,
		// we can safely close the sequence.
		String LAST_OUT_MESSAGE_NO = "LastOutMessage";
		
		String SECURITY_TOKEN = "SecurityToken";
		
		String SOAP_VERSION = "SOAPVersion";
		
		String OFFERED_ENDPOINT = "OfferedEndpoint";
		
		static final String LAST_FAILED_TO_SEND_ERROR = "LastFailToSendError";
		
		static final String LAST_FAILED_TO_SEND_ERROR_TIMESTAMP = "LastFailToSendErrorTimeStamp";

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
		}
	}
	
	public interface DatabaseParams {
		
	}
	
	String IN_HANDLER_NAME = "SandeshaInHandler";

	String OUT_HANDLER_NAME = "SandeshaOutHandler";

	String GLOBAL_IN_HANDLER_NAME = "GlobalInHandler";

	String APPLICATION_PROCESSING_DONE = "Sandesha2AppProcessingDone";

	String ACK_WRITTEN = "AckWritten";

	int INVOKER_SLEEP_TIME = 1000;

	int SENDER_SLEEP_TIME = 500;

	int CLIENT_SLEEP_TIME = 10000;

	int TERMINATE_DELAY = 100;

	String TEMP_SEQUENCE_ID = "uuid:tempID";

	String ACK_PROCSSED = "AckProcessed";

	String RM_ENABLE_KEY = "RMEnabled";

	int MAXIMUM_RETRANSMISSION_ATTEMPTS = 5;
	
	String PROPERTY_FILE = "sandesha2.properties";
	
	String VALUE_NONE = "none";
	
	String VALUE_EMPTY = "empty";
	
	String VALUE_TRUE = "true";
	
	String VALUE_FALSE = "false";
	
	String MESSAGE_STORE_KEY = "Sandesha2MessageStoreKey";

	String ORIGINAL_TRANSPORT_OUT_DESC = "Sandesha2OriginalTransportSender";
	
	String SET_SEND_TO_TRUE = "Sandesha2SetSendToTrue";
	
	String MESSAGE_TYPE = "Sandesha2MessageType";
	
	String QUALIFIED_FOR_SENDING = "Sandesha2QualifiedForSending";  //Sender will send messages only if this property is null (not set) or true.

	String QNAME_SEPERATOR = ",";
	
	String EXECUTIN_CHAIN_SEPERATOR = ".";
	
	String INTERNAL_SEQUENCE_PREFIX = "Sandesha2InternalSequence";
	
	String SANDESHA_PROPERTY_BEAN = "Sandesha2PropertyBean";
	
	String LIST_SEPERATOR = ",";
	
	String LIST_PART_SEPERATOR = "-";
	
	String INMEMORY_STORAGE_MANAGER = "inmemory";
	
	String PERMANENT_STORAGE_MANAGER = "persistent";
	
	String DEFAULT_STORAGE_MANAGER = INMEMORY_STORAGE_MANAGER;
	
	String POLLING_MANAGER = "PollingManager";
	
	String WITHIN_TRANSACTION = "WithinTransaction";
	
	String STORAGE_MANAGER_PARAMETER  = "Sandesha2StorageManager";
	
	String POST_FAILURE_MESSAGE = "PostFailureMessage";
	
	String REINJECTED_MESSAGE = "ReinjectedMessage";
	
	String MODULE_CLASS_LOADER = "Sandesha2ModuleClassLoader";
	
	String SECURITY_MANAGER = "Sandesha2SecurityManager";
	
	String RM_IN_OUT_OPERATION_NAME = "RMInOutOperation";
	
	String RM_IN_ONLY_OPERATION = "RMInOnlyOperation";
	
	String RETRANSMITTABLE_PHASES = "RMRetransmittablePhases";
	
	String propertiesToCopyFromReferenceMessage = "propertiesToCopyFromReferenceMessage";
	
	String propertiesToCopyFromReferenceRequestMessage = "propertiesToCopyFromReferenceRequestMessage";
	
	String [] SPEC_NS_URIS = {
			SPEC_2005_02.NS_URI,
			SPEC_2006_08.NS_URI
	};
	
	public interface MessageContextProperties{
		String INTERNAL_SEQUENCE_ID = "Sandesha2InternalSequenceId";
		String SEQUENCE_ID = "WSRMSequenceId";
		String MESSAGE_NUMBER = "WSRMMessageNumber";
		String SEQUENCE_PROPERTY_KEY = "SequencePropertyKey";
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
    }
}
