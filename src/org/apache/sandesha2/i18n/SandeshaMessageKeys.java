/*
 * Copyright 2006 The Apache Software Foundation.
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

package org.apache.sandesha2.i18n;

public class SandeshaMessageKeys {


	public static final String cannotInitInMemoryStorageManager="cannotInitInMemoryStorageManager";
	public static final String cannotInitPersistentStorageManager="cannotInitPersistentStorageManager";
	public static final String cannotProceedDueToStorageManager="cannotProceedDueToStorageManager";
	public static final String cannotGetStorageKey="cannotGetStorageKey";
	public static final String cannotGetStorageManager="cannotGetStorageManager";
	public static final String storageManagerMustImplement="storageManagerMustImplement";
	public static final String cannotInitSecurityManager="cannotInitSecurityManager";
	public static final String securityManagerMustImplement="securityManagerMustImplement";
	public static final String cannotFindModulePolicies="cannotFindModulePolicies";
	public static final String cannotPauseThread = "cannotPauseThread";

	public static final String commitError="commitError";
	public static final String rollbackError="rollbackError";
	public static final String deadlock="deadlock";
	public static final String noTransaction="noTransaction";
	public static final String inMsgError="inMsgError";
	public static final String outMsgError="outMsgError";
	public static final String invokeMsgError="invokeMsgError";
	public static final String msgError="msgError";
	public static final String sendMsgError="sendMsgError";
	public static final String cannotSendMsgAsSequenceClosed="cannotSendMsgAsSequenceClosed";
	public static final String cannotSendMsgAsSequenceTerminated="cannotSendMsgAsSequenceTerminated";
	public static final String cannotSendMsgAsSequenceTimedout="cannotSendMsgAsSequenceTimedout";
	public static final String noValidSyncResponse="noValidSyncResponse";
	public static final String generalError="generalError";
	public static final String classLoaderNotFound="classLoaderNotFound";

	public static final String defaultPropertyBeanNotSet="defaultPropertyBeanNotSet";
	public static final String propertyBeanNotSet="propertyBeanNotSet";
	public static final String optionsObjectNotSet="optionsObjectNotSet";
	public static final String serviceContextNotSet="serviceContextNotSet";
	public static final String sequenceIdBeanNotSet="sequenceIdBeanNotSet";
	public static final String configContextNotSet="configContextNotSet";
	public static final String soapEnvNotSet="soapEnvNotSet";
	public static final String soapBodyNotPresent="soapBodyNotPresent";
	public static final String unknownSoapVersion="unknownSoapVersion";
	public static final String axisServiceIsNull="axisServiceIsNull";
	public static final String msgContextNotSetInbound="msgContextNotSetInbound";
	public static final String msgContextNotSetOutbound="msgContextNotSetOutbound";
	public static final String msgContextNotSet="msgContextNotSet";
	public static final String requestMsgContextNull="requestMsgContextNull";
	public static final String axisOperationError="axisOperationError";
	public static final String axisOperationRegisterError="axisOperationRegisterError";
	public static final String transportOutNotPresent="transportOutNotPresent";
	public static final String storedKeyNotPresent="storedKeyNotPresent";
	public static final String invalidQName="invalidQName";
	public static final String couldNotFindPropertyKey="couldNotFindPropertyKey";
	public static final String couldNotFindOperation="couldNotFindOperation";

	public static final String dummyCallback="dummyCallback";
	public static final String cannotFindSequenceID="cannotFindSequenceID";
	public static final String dummyCallbackError="dummyCallbackError";
	public static final String setAValidMsgNumber="setAValidMsgNumber";
	public final static String processTokenMethodError="processTokenMethodError";
	public final static String policyProcessingException="policyProcessingException"; 
	public static final String errorOnTokenStack="errorOnTokenStack";
	public static final String unknownTokenAbortTran="unknownTokenAbortTran";
	public static final String commitingUnknownToken="commitingUnknownToken";
	public static final String cannotStartTransportListenerDueToError="cannotStartTransportListener";
	public static final String cannotStartListenerForIncommingMsgs="cannotStartListenerForIncommingMsgs";
	public static final String selectRSNotSupported="selectRSNotSupported";
	public static final String nonUniqueResult="nonUniqueResult";
	public static final String invalidStringArray="invalidStringArray";
	public static final String nextMsgNotSetCorrectly="nextMsgNotSetCorrectly";
	public static final String invalidNextMsgNumber="invalidNextMsgNumber";
	public static final String cannotCointinueSender="cannotCointinueSender";
	public static final String msgContextNotPresentInStorage="msgContextNotPresentInStorage";
	public static final String sendHasUnavailableMsgEntry="sendHasUnavailableMsgEntry";
	public static final String cannotInnitMessage="cannotInnitMessage";
	public static final String propertyInvalidValue="propertyInvalidValue";
	public static final String couldNotCopyParameters="couldNotCopyParameters";
	public static final String invalidRange="invalidRange";
	public static final String senderBeanNotFound="senderBeanNotFound";
	public static final String workAlreadyAssigned="workAlreadyAssigned";
	public static final String workNotPresent="workNotPresent";
	public static final String cannotSendToTheAddress="cannotSendToTheAddress";
	


	public static final String rmNamespaceNotMatchSequence="rmNamespaceNotMatchSequence";
	public static final String wsaNamespaceNotMatchSequence="wsaNamespaceNotMatchSequence";
	public static final String unknownWSAVersion="unknownWSAVersion";
	public static final String rmNamespaceMismatch="rmNamespaceMismatch";
	public static final String emptyAckRequestSpecLevel="emptyAckRequestSpecLevel";
	public static final String closeSequenceSpecLevel="closeSequenceSpecLevel";
	public static final String unknownSpec="unknownSpec";
	public static final String unknownRMNamespace="unknownRMNamespace";
	public static final String unknownNamespace="unknownNamespace";
	public static final String cannotDecideRMVersion="cannotDecideRMVersion";
	public static final String specVersionPropertyNotAvailable="specVersionPropertyNotAvailable";
	public static final String specVersionNotSet="specVersionNotSet";
	public static final String specDoesNotSupportElement="specDoesNotSupportElement";
		
	public static final String couldNotSendTerminate="couldNotSendTerminate";
	public static final String couldNotSendClose="couldNotSendClose";
	public static final String couldNotSendAck="couldNotSendAck";
	public static final String couldNotSendTerminateResponse="couldNotSendTerminateResponse";
	public static final String couldNotSendTerminateSeqNotFound="couldNotSendTerminateSeqNotFound";
	public static final String couldNotSendFault="couldNotSendFault";
	public static final String cannotSendAckRequestNotActive="cannotSendAckRequestNotActive";
	public static final String cannotSendAckRequestException="cannotSendAckRequestException";
	public static final String ackRequestMultipleParts="ackRequestMultipleParts";
	public static final String noAckRequestPartFound="noAckRequestPartFound";
	public static final String cannotCloseSequenceNotActive="cannotCloseSequenceNotActive";
	public static final String noSequenceEstablished="noSequenceEstablished";
	public static final String invalidInternalSequenceID="invalidInternalSequenceID";
	public static final String tempNotSetOnReceivedMsg="tempNotSetOnReceivedMsg";
	public static final String completedMsgBeanIsNull="completedMsgBeanIsNull";
	public static final String internalSeqBeanNotAvailableOnSequence="internalSeqBeanNotAvailableOnSequence";
	public static final String createSeqEntryNotFound="createSeqEntryNotFound";

	public static final String toEPRNotValid="toEPRNotValid";
	public static final String noWSAACtionValue="noWSAACtionValue";
	public static final String cannotFindSequence="cannotFindSequence";
	public static final String notValidTerminate="notValidTerminate";
	public static final String notValidTimeOut="notValidTimeOut";
	public static final String cannotFindAcksTo="cannotFindAcksTo";
	public static final String droppingDuplicate="droppingDuplicate";
	public static final String cannotAcceptMsgAsSequenceClosed="cannotAcceptMsgAsSequenceClosed"; 
	public static final String msgNumberMustBeLargerThanZero="msgNumberMustBeLargerThanZero";
	public static final String msgNumberLargerThanLastMsg="msgNumberLargerThanLastMsg";
	public static final String msgNumberNotLargerThanLastMsg="msgNumberNotLargerThanLastMsg";
	public static final String outMsgHasNoEnvelope="outMsgHasNoEnvelope";
	public static final String msgNumberExceededLastMsgNo="msgNumberExceededLastMsgNo";
	public static final String ackInvalid="ackInvalid";
	public static final String ackInvalidHighMsg="ackInvalidHighMsg";
	public static final String ackInvalidNotSent="ackInvalidNotSent";
	public static final String highestMsgIdNotStored="highestMsgIdNotStored";
	public static final String cannotHaveFinalWithNack="cannotHaveFinalWithNack";
	public static final String accptButNoSequenceOffered="accptButNoSequenceOffered";
	public static final String relatesToNotAvailable="relatesToNotAvailable";
	public static final String cannotDerriveAckInterval="cannotDerriveAckInterval";
	public static final String cannotDerriveRetransInterval="cannotDerriveRetransInterval";
	public static final String cannotDerriveInactivityTimeout="cannotDerriveInactivityTimeout";
	public static final String noCreateSeqParts="noCreateSeqParts";
	public static final String noAcceptPart="noAcceptPart";
	public static final String noAcksToPartInCreateSequence="noAcksToPartInCreateSequence";
	public static final String tempSeqIdNotSet="tempSeqIdNotSet";
	public static final String ackRandDoesNotHaveCorrectValues="ackRandDoesNotHaveCorrectValues";
	public static final String cannotSetAckRangeNullElement="cannotSetAckRangeNullElement";
	public static final String completedMessagesNull="completedMessagesNull";
	public static final String emptyLastMsg="emptyLastMsg";
	public static final String acksToStrNotSet="acksToStrNotSet";
	public static final String invalidSequenceID="invalidsequenceID";
	public static final String cantSendMakeConnectionNoTransportOut="cantSendMakeConnectionNoTransportOut";
	public static final String makeConnectionDisabled="makeConnectionDisabled";
	
	public static final String noCreateSeqResponse="noCreateSeqResponse";
	public static final String noTerminateSeqPart="noTerminateSeqPart";
	public static final String noNackInSeqAckPart="noNackInSeqAckPart";
	public static final String nackDoesNotContainValidLongValue="nackDoesNotContainValidLongValue";
	public static final String cannotSetNackElemnt="cannotSetNackElemnt";
	public static final String seqAckPartIsNull="seqAckPartIsNull";
	public static final String cannotAddSequencePartNullMsgNumber="cannotAddSequencePartNullMsgNumber";
	public static final String cannotSetSeqAck="cannotSetSeqAck";
	public static final String noneNotAllowedNamespace="noneNotAllowedNamespace";
	public static final String noneNotAllowedAckRangesPresent="noneNotAllowedAckRangesPresent";
	public static final String noneNotAllowedNackPresent="noneNotAllowedNackPresent";
	public static final String finalNotAllowedNamespace="finalNotAllowedNamespace";
	public static final String noFaultCodeNullElement="noFaultCodeNullElement";
	public static final String noSeqFaultInElement="noSeqFaultInElement";
	public static final String noSeqOfferInElement="noSeqOfferInElement";
	public static final String noCreateSeqInElement="noCreateSeqInElement";
	public static final String noTerminateSeqInElement="noTerminateSeqInElement";
	public static final String noTerminateSeqResponseInElement="noTerminateSeqResponseInElement";
	public static final String noAcceptPartInElement="noAcceptPartInElement";
	public static final String noUpperOrLowerAttributesInElement="noUpperOrLowerAttributesInElement";
	public static final String noSequencePartInElement="noSequencePartInElement";
	public static final String noLastMessagePartInElement="noLastMessagePartInElement";
	public static final String noFinalPartInElement="noFinalPartInElement"; 
	public static final String noNonePartInElement="noNonePartInElement";
	public static final String noCloseSequencePartInElement="noCloseSequencePartInElement";
	public static final String noMessageNumberPartInElement="noMessageNumberPartInElement";
	public static final String noCloseSeqResponsePartInElement="noCloseSeqResponsePartInElement";
	public static final String noExpiresPartInElement="noExpiresPartInElement";
	public static final String noCreateSeqPartInElement="noCreateSeqPartInElement";
	public static final String noAckRequestedPartInElement="noAckRequestedPartInElement";
	public static final String noCreateSeqResponsePartInElement="noCreateSeqResponsePartInElement";
	public static final String noFaultCodePart="noFaultCodePart";
	public static final String cannotFindAddressElement="cannotFindAddressElement";
	public static final String cannotFindAddressText="cannotFindAddressText";
	public static final String nullPassedElement="nullPassedElement";
	public static final String noAckRequestedElement="noAckRequestedElement";
	public static final String invalidAckMessageEntry="invalidAckMessageEntry";
	public static final String seqPartIsNull="seqPartIsNull";
	public static final String incomingSequenceNotValidID="incomingSequenceNotValidID";
	public static final String noMakeConnectionPartInElement="noCloseSequencePartInElement";

	public static final String seqFaultCannotBeExtractedToNonHeader="seqFaultCannotBeExtractedToNonHeader";
	public static final String seqElementCannotBeAddedToNonHeader="seqElementCannotBeAddedToNonHeader";
	public static final String ackRequestedCannotBeAddedToNonHeader="ackRequestedCannotBeAddedToNonHeader";
	public static final String terminateSeqCannotBeAddedToNonBody="terminateSeqCannotBeAddedToNonBody";
	public static final String terminateSeqResponseCannotBeAddedToNonBody="terminateSeqResponseCannotBeAddedToNonBody";
	public static final String closeSeqCannotBeAddedToNonBody="closeSeqCannotBeAddedToNonBody";
	public static final String closeSeqCannotBeExtractedFromNonBody="closeSeqCannotBeExtractedFromNonBody";
	public static final String closeSeqResponseCannotBeAddedToNonBody="closeSeqResponseCannotBeAddedToNonBody";
	public static final String createSeqCannotBeAddedToNonBody="createSeqCannotBeAddedToNonBody";
	public static final String createSeqResponseCannotBeAddedToNonBody="createSeqResponseCannotBeAddedToNonBody";
	public static final String seqOfferNullID="seqOfferNullID";
	public static final String terminateSequence="terminateSequence";
	public static final String terminateSeqResponseNullID="terminateSeqResponseNullID";
	public static final String sequencePartNullID="sequencePartNullID";
	public static final String closeSeqPartNullID="closeSeqPartNullID";
	public static final String invalidIdentifier="invalidIdentifier";
	public static final String closeSeqResponsePartNullID="closeSeqResponsePartNullID";
	public static final String ackRequestNullID="ackRequestNullID";
	public static final String createSeqNullAcksTo="createSeqNullAcksTo";
	public static final String acceptNullAcksTo="acceptNullAcksTo";
	public static final String noAcksToPart="noAcksToPart";
	public static final String noElementPart="noElementPart";
	public static final String cannotProcessExpires="cannotProcessExpires";
	public static final String noFaultCode="noFaultCode";
	public static final String seqAckNonHeader="seqAckNonHeader";
	public static final String makeConnectionCannotBeAddedToNonBody="makeConnectionCannotBeAddedToNonBody";

	public static final String cannotSetAcksTo="cannotSetAcksTo";
	public static final String cannotSetEndpoint="cannotSetEndpoint";
	public static final String invalidMsgNumber="invalidMsgNumber";
	public static final String addressNotValid="addressNotValid";

	public static final String incommingSequenceReportNotFound="incommingSequenceReportNotFound";
	public static final String cannotGenerateReport="cannotGenerateReport";
	public static final String cannotFindReportForGivenData="cannotFindReportForGivenData";
	public static final String cannotGenerateReportNonUniqueSequence="cannotGenerateReportNonUniqueSequence";

	public static final String outSeqIDIsNull="outSeqIDIsNull";
	public static final String requestMsgNotPresent="requestMsgNotPresent";
	public static final String requestSeqIsNull="requestSeqIsNull";
	public static final String newSeqIdIsNull="newSeqIdIsNull";
	public static final String unavailableAppMsg="unavailableAppMsg";
	public static final String terminateAddedPreviously="terminateAddedPreviously";
	public static final String maximumRetransmissionCountProcessor="maximumRetransmissionCountProcessor";
	public static final String nullMsgId="nullMsgId";
	public static final String storageMapNotPresent="storageMapNotPresent";
	public static final String failedToStoreMessage="failedToStoreMessage";
	public static final String failedToLoadMessage="failedToLoadMessage";
	public static final String entryNotPresentForUpdating="entryNotPresentForUpdating";
	public static final String appMsgIsNull="appMsgIsNull";
	public static final String invalidMsgNumberList="invalidMsgNumberList";
	public static final String cannotFindReqMsgFromOpContext="cannotFindReqMsgFromOpContext";

	public static final String secureDummyNoProof="secureDummyNoProof";
	public static final String secureDummyNoToken="secureDummyNoToken";
	public static final String secureDummyNoSTR  ="secureDummyNoSTR";
	
	public static final String cannotFindTransportInDesc = "cannotFindTransportInDesc";
	public static final String toEPRNotSet = "toEPRNotSet";
	public static final String toBeanNotSet = "toBeanNotSet";
	public static final String replyToBeanNotSet = "replyToBeanNotSet";
	    
    
	public final static String errorRetrievingSecurityToken = "errorRetrievingSecurityToken";
	public final static String proofOfPossessionNotVerified = "proofOfPossessionNotVerified";
	public final static String noSecurityResults = "noSecurityResults";
	public final static String noSecConvTokenInPolicy = "noSecConvTokenInPolicy";
	public final static String noServicePolicy = "noServicePolicy";
	    
	public final static String elementMustForSpec = "elementMustForSpec";
	public final static String addressingNamespaceNotSet = "addressingNamespaceNotSet";
	public final static String couldNotSendCreateSeqResponse = "couldNotSendCreateSeqResponse";
	public final static String invalidElementFoundWithinElement = "invalidElementFoundWithinElement";
	public final static String invokerNotFound="invokerNotFound";
	    
	public final static String couldNotSendAckRequestSeqNotFound="couldNotSendAckRequestSeqNotFound";
	public final static String couldNotSendCloseResponse="couldNotSendCloseResponse";
	public final static String couldNotSendCloseSeqNotFound="couldNotSendCloseSeqNotFound";
	
	public final static String couldNotLoadModulePolicies = "couldNotLoadModulePolicies";
	public final static String modulePoliciesLoaded = "modulePoliciesLoaded";
	
	public final static String createSequenceRefused = "createSequenceRefused";
	public final static String referencedMessageNotFound = "referencedMessageNotFound";
	public final static String messageNumberRollover = "messageNumberRollover";
	
	public final static String policyBeanNotFound = "policyBeanNotFound";
	public final static String cloneDoesNotMatchToOriginal = "cloneDoesNotMatchToOriginal";
	public final static String exceptionInFlowCompletion = "exceptionInFlowCompletion";
	public final static String rmdBeanNotFound = "rmdBeanNotFound";

}
