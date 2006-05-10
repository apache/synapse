/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
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

package org.apache.synapse.mediators.transform;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.*;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import javax.xml.namespace.QName;
import java.net.URI;

/**
 * This transforms the current message instance into a SOAP Fault message. The
 * SOAP version for the fault message could be explicitly specified. Else if the
 * original message was SOAP 1.1 the fault will also be SOAP 1.1 else, SOAP 1.2
 *
 * This class exposes methods to set SOAP 1.1 and 1.2 fault elements and uses
 * these as required.
 */
public class FaultMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(FaultMediator.class);

    /** Make a SOAP 1.1 fault */
    public static final int SOAP11 = 1;
    /** Make a SOAP 1.2 fault */
    public static final int SOAP12 = 2;
    /** Holds the SOAP version to be used to make the fault, if specified */
    private int soapVersion;

    //--------- SOAP 1.1 elements ---------------
    /** SOAP 1.1 faultcode (Required) */
    private QName faultcode = null;
    /** SOAP 1.1 faultstring (Required) */
    private String faultstring = null;
    /** SOAP 1.1 faultactor */
    private URI faultactor = null;
    /** SOAP 1.1 detail (Kept as String) */
    private String detail = null;

    //--------- SOAP 1.2 elements ---------------
    /**
     * Hold the Code/Value of a SOAP 1.2 fault. (Required) Must be one of the following
     * DataEncodingUnknown, MustUnderstand, Receiver, Sender, VersionMismatch
     */
    private QName codeValue = null;
    /** Hold the Code/Subcode/Value of the SOAP 1.2 fault */
    private QName codeSubcodeValue = null;
    /** Holds the Reason of the SOAP 1.2 fault */
    private String Reason = null;
    /** The language attribute for the Reason text */
    private String ReasonLang = null;
    /** Hold the node for the SOAP 1.2 fault */
    private URI Node = null;
    /** Hold the role for the SOAP 1.2 fault */
    private URI Role = null;
    /** SOAP 1.2 detail (Kept as a String) */
    private String Detail = null;

    public boolean mediate(SynapseMessageContext synCtx) {
        log.debug(getType() + " mediate()");
        SOAPEnvelope envelop = synCtx.getEnvelope();

        switch (soapVersion) {
            case SOAP11:
                return makeSOAPFault(synCtx, SOAP11);
            case SOAP12:
                return makeSOAPFault(synCtx, SOAP12);
            default : {
                if (envelop != null) {
                    if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(envelop.getNamespace().getName())) {
                        return makeSOAPFault(synCtx, SOAP12);
                    } else {
                        return makeSOAPFault(synCtx, SOAP11);
                    }
                } else {
                    return makeSOAPFault(synCtx, SOAP11);
                }
            }
        }
    }

    private boolean makeSOAPFault(SynapseMessageContext synCtx, int soapVersion) {

        // get the correct SOAP factory to be used
        SOAPFactory factory = (
            soapVersion == SOAP11 ? OMAbstractFactory.getSOAP11Factory() : OMAbstractFactory.getSOAP12Factory());

        // create the SOAP fault document and envelope
        OMDocument soapFaultDocument = factory.createOMDocument();
        SOAPEnvelope faultEnvelope = factory.getDefaultFaultEnvelope();
        soapFaultDocument.addChild(faultEnvelope);

        // create the fault element
        SOAPFault fault = factory.createSOAPFault();

        // populate it
        setFaultCode(factory, fault, soapVersion);
        setFaultResaon(factory, fault, soapVersion);
        setFaultNode(factory, fault, soapVersion);
        setFaultRole(factory, fault, soapVersion);
        setFaultDetail(factory, fault, soapVersion);

        // set the fault element
        faultEnvelope.getBody().setFirstChild(fault);

        // set the fault message "to" header to the "faultTo" of the original message if
        // such a header existed on the original message, else set it to the "replyTo" of the original

        EndpointReference toEPR = synCtx.getTo();
        EndpointReference faultToEPR = synCtx.getFaultTo();
        if (faultToEPR != null) {
            synCtx.setTo(faultToEPR);
            synCtx.setReplyTo(toEPR);
        } else {
            EndpointReference replyToEPR = synCtx.getReplyTo();
            synCtx.setTo(replyToEPR);
            synCtx.setReplyTo(toEPR);
        }
        synCtx.setResponse(true);

        // overwrite current message envelope with new fault envelope
        try {
            synCtx.setEnvelope(faultEnvelope);
        } catch (AxisFault af) {
            String msg = "Error replacing SOAP envelope with a fault envelope " + af.getMessage();
            log.error(msg);
            throw new SynapseException(af);
        }

        return true;
    }

    private void setFaultCode(SOAPFactory factory, SOAPFault fault, int soapVersion) {
        QName fault_code = (soapVersion == SOAP11 ? faultcode : codeValue);
        if (fault_code != null) {
            SOAPFaultCode code = factory.createSOAPFaultCode();
            SOAPFaultValue value = factory.createSOAPFaultValue(code);
            value.setText(fault_code);
            fault.setCode(code);

        } else {
            handleException(soapVersion == SOAP11 ?
                "faultcode is required for a SOAP 1.1 fault message" :
                "Code/Value is required for a SOAP 1.2 fault message"
            );
        }
    }

    private void setFaultResaon(SOAPFactory factory, SOAPFault fault, int soapVersion) {
        String reasonString = (soapVersion == SOAP11 ? faultstring : Reason);
        if (faultstring != null) {
            SOAPFaultReason reason = factory.createSOAPFaultReason();
            SOAPFaultText text = factory.createSOAPFaultText();
            text.setText(reasonString);
            if (soapVersion == SOAP12 && ReasonLang != null) {
                text.setLang(ReasonLang);
            }
            reason.addSOAPText(text);
            fault.setReason(reason);

        } else {
            handleException(soapVersion == SOAP11 ?
                "faultstring is required for a SOAP 1.1 fault message" :
                "Reason is required for a SOAP 1.2 fault message"
            );
        }
    }

    private void setFaultNode(SOAPFactory factory, SOAPFault fault, int soapVersion) {
        URI fActor = (soapVersion == SOAP11 ? faultactor : Node);
        if (fActor != null) {
            SOAPFaultNode faultNode = factory.createSOAPFaultNode();
            faultNode.setNodeValue(fActor.toString());
            fault.setNode(faultNode);
        }
    }

    private void setFaultRole(SOAPFactory factory, SOAPFault fault, int soapVersion) {
        if (soapVersion == SOAP12) {
            if (Role != null) {
                SOAPFaultRole faultRole = factory.createSOAPFaultRole();
                faultRole.setRoleValue(Role.toString());
                fault.setRole(faultRole);
            }
        }
    }

    private void setFaultDetail(SOAPFactory factory, SOAPFault fault, int soapVersion) {
        String detailText = (soapVersion == SOAP11 ? detail : Detail);
        if (detailText != null) {
            SOAPFaultDetail faultDetail = factory.createSOAPFaultDetail();
            faultDetail.setText(detailText);
            fault.setDetail(faultDetail);
        }
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public int getSoapVersion() {
        return soapVersion;
    }

    public void setSoapVersion(int soapVersion) {
        this.soapVersion = soapVersion;
    }

    // -- SOAP 1.1 getters and setters --

    public QName getFaultcode() {
        return faultcode;
    }

    public void setFaultcode(QName faultcode) {
        this.faultcode = faultcode;
    }

    public String getFaultstring() {
        return faultstring;
    }

    public void setFaultstring(String faultstring) {
        this.faultstring = faultstring;
    }

    public URI getFaultactor() {
        return faultactor;
    }

    public void setFaultactor(URI faultactor) {
        this.faultactor = faultactor;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    // -- SOAP 1.2 getters and setters --

    public QName getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(QName codeValue) {
        if (
            SOAP12Constants.SOAP_DEFAULT_NAMESPACE_PREFIX.equals(codeValue.getNamespaceURI()) &&

            (SOAP12Constants.FAULT_CODE_DATA_ENCODING_UNKNOWN.equals(codeValue.getLocalPart()) ||
            SOAP12Constants.FAULT_CODE_MUST_UNDERSTAND.equals(codeValue.getLocalPart()) ||
            SOAP12Constants.FAULT_CODE_RECEIVER.equals(codeValue.getLocalPart()) ||
            SOAP12Constants.FAULT_CODE_SENDER.equals(codeValue.getLocalPart()) ||
            SOAP12Constants.FAULT_CODE_VERSION_MISMATCH.equals(codeValue.getLocalPart())) ){

            this.codeValue = codeValue;

        } else {
            String msg = "Invalid Fault code value for a SOAP 1.2 fault : " + codeValue;
            log.error(msg);
            throw new SynapseException(msg);
        }
    }

    public QName getCodeSubcodeValue() {
        return codeSubcodeValue;
    }

    public void setCodeSubcodeValue(QName codeSubcodeValue) {
        this.codeSubcodeValue = codeSubcodeValue;
    }

    public String getReason() {
        return Reason;
    }

    public void setReason(String reason) {
        this.Reason = reason;
    }

    public String getReasonLang() {
        return ReasonLang;
    }

    public void setReasonLang(String reasonLang) {
        this.ReasonLang = reasonLang;
    }

    public URI getNode() {
        return Node;
    }

    public void setNode(URI node) {
        Node = node;
    }

    public URI getRole() {
        return Role;
    }

    public void setRole(URI role) {
        Role = role;
    }
}
