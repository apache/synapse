/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.transform;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.*;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Iterator;

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
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);
    public static final String WSA_ACTION = "Action";
    /** Make a SOAP 1.1 fault */
    public static final int SOAP11 = 1;
    /** Make a SOAP 1.2 fault */
    public static final int SOAP12 = 2;
    /** Holds the SOAP version to be used to make the fault, if specified */
    private int soapVersion;

    // -- fault elements --
    /** The fault code QName to be used */
    private QName faultCodeValue = null;
    /** An XPath expression that will give the fault code QName at runtime */
    private AXIOMXPath faultCodeExpr = null;
    /** The fault reason to be used */
    private String faultReasonValue = null;
    /** An XPath expression that will give the fault reason string at runtime */
    private AXIOMXPath faultReasonExpr = null;
    /** The fault node URI to be used */
    private URI faultNode = null;
    /** The fault role URI to be used - if applicable */
    private URI faultRole = null;
    /** The fault detail to be used */
    private String faultDetail = null;

    public boolean mediate(MessageContext synCtx) {
        if (log.isDebugEnabled()) {
            log.debug("Fault mediator mediate()");
        }
        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        if(shouldTrace) {
            trace.trace("Start : Fault mediator");
        }
        switch (soapVersion) {
            case SOAP11:
                return makeSOAPFault(synCtx, SOAP11,shouldTrace);
            case SOAP12:
                return makeSOAPFault(synCtx, SOAP12,shouldTrace);
            default : {
                SOAPEnvelope envelop = synCtx.getEnvelope();
                if (envelop != null) {
                    if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(
                        envelop.getNamespace().getNamespaceURI())) {
                        soapVersion = SOAP12;
                        return makeSOAPFault(synCtx, SOAP12,shouldTrace);
                    } else {
                        soapVersion = SOAP11;
                        return makeSOAPFault(synCtx, SOAP11,shouldTrace);
                    }
                } else {
                    return makeSOAPFault(synCtx, SOAP11,shouldTrace);
                }
            }
        }
    }

    private boolean makeSOAPFault(MessageContext synCtx, int soapVersion,boolean shouldTrace) {
        if (log.isDebugEnabled()) {
            log.debug("Creating a SOAP fault using SOAP " + (soapVersion == SOAP11 ? "1.1" : "1.2"));
        }
        // get the correct SOAP factory to be used
        SOAPFactory factory = (
            soapVersion == SOAP11 ? OMAbstractFactory.getSOAP11Factory() : OMAbstractFactory.getSOAP12Factory());

        // create the SOAP fault document and envelope
        OMDocument soapFaultDocument = factory.createOMDocument();
        SOAPEnvelope faultEnvelope = factory.getDefaultFaultEnvelope();
        soapFaultDocument.addChild(faultEnvelope);

        // create the fault element  if it is need
        SOAPFault fault = faultEnvelope.getBody().getFault();
        if(fault == null){
            fault = factory.createSOAPFault();
        }

        // populate it
        setFaultCode(synCtx, factory, fault);
        setFaultResaon(synCtx, factory, fault);
        setFaultNode(factory, fault);
        setFaultRole(factory, fault);
        setFaultDetail(factory, fault);

        // set the all headers of original SOAP Envelope to the Fault Envelope
        if (synCtx.getEnvelope() != null) {
            SOAPHeader soapHeader = synCtx.getEnvelope().getHeader();
            if (soapHeader != null) {
                for (Iterator iter = soapHeader.examineAllHeaderBlocks(); iter.hasNext();) {
                    Object o = iter.next();
                    if (o instanceof SOAPHeaderBlock) {
                        SOAPHeaderBlock header = (SOAPHeaderBlock) o;
                        faultEnvelope.getHeader().addChild(header);
                    } else if (o instanceof OMElement) {
                        faultEnvelope.getHeader().addChild((OMElement) o);
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("The fault message as : " + fault);
        }
        // overwrite current message envelope with new fault envelope
        try {
            if (shouldTrace) {
                trace.trace("Original SOAP Message : " + synCtx.getEnvelope().toString());
                trace.trace("Fault Message created : " + faultEnvelope.toString());
            }
            synCtx.setEnvelope(faultEnvelope);
        } catch (AxisFault af) {
            String msg = "Error replacing SOAP envelope with a fault envelope " + af.getMessage();
            handleException(msg,af);
        }

        if (synCtx.getFaultTo() != null) {
            synCtx.setTo(synCtx.getFaultTo());
        } else if (synCtx.getReplyTo() != null) {
            synCtx.setTo(synCtx.getReplyTo());
        } else {
            synCtx.setTo(null);
        }

        if (shouldTrace) {
            trace.trace("End : Fault mediator");
        }
        return true;
    }

    private void setFaultCode(MessageContext synCtx, SOAPFactory factory, SOAPFault fault) {

        QName fault_code = null;

        if (faultCodeValue == null && faultCodeExpr == null) {
            handleException("A valid fault code QName value or expression is required");
        } else if (faultCodeValue != null) {
            fault_code = faultCodeValue;
        } else {
            fault_code = QName.valueOf(Axis2MessageContext.getStringValue(faultCodeExpr, synCtx));
        }

        SOAPFaultCode code = factory.createSOAPFaultCode();
        switch(soapVersion){
            case SOAP11:
                code.setText(fault_code);
                break;
            case SOAP12:
                SOAPFaultValue value = factory.createSOAPFaultValue(code);
                value.setText(fault_code);
                break;
        }
        fault.setCode(code);
    }

    private void setFaultResaon(MessageContext synCtx, SOAPFactory factory, SOAPFault fault) {
        String reasonString = null;

        if (faultReasonValue == null && faultReasonExpr == null) {
            handleException("A valid fault reason value or expression is required");
        } else if (faultReasonValue != null) {
            reasonString = faultReasonValue;
        } else {
            reasonString = Axis2MessageContext.getStringValue(faultReasonExpr, synCtx);
        }

        SOAPFaultReason reason = factory.createSOAPFaultReason();
        switch(soapVersion){
            case SOAP11:
                reason.setText(reasonString);
                break;
            case SOAP12:
                SOAPFaultText text = factory.createSOAPFaultText();
                text.setText(reasonString);
                reason.addSOAPText(text);
                break;
        }
        fault.setReason(reason);
    }

    private void setFaultNode(SOAPFactory factory, SOAPFault fault) {
        if (faultNode != null) {
            SOAPFaultNode soapfaultNode = factory.createSOAPFaultNode();
            soapfaultNode.setNodeValue(faultNode.toString());
            fault.setNode(soapfaultNode);
        }
    }

    private void setFaultRole(SOAPFactory factory, SOAPFault fault) {
        if (faultRole != null) {
            SOAPFaultRole soapFaultRole = factory.createSOAPFaultRole();
            soapFaultRole.setRoleValue(faultRole.toString());
            fault.setRole(soapFaultRole);
        }
    }

    private void setFaultDetail(SOAPFactory factory, SOAPFault fault) {
        if (faultDetail != null) {
            SOAPFaultDetail soapFaultDetail = factory.createSOAPFaultDetail();
            soapFaultDetail.setText(faultDetail);
            fault.setDetail(soapFaultDetail);
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

    public QName getFaultCodeValue() {
        return faultCodeValue;
    }

    public void setFaultCodeValue(QName faultCodeValue) {

        if (soapVersion == SOAP11) {
            this.faultCodeValue = faultCodeValue;

        } else {
            if (
                SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(faultCodeValue.getNamespaceURI()) &&

                (SOAP12Constants.FAULT_CODE_DATA_ENCODING_UNKNOWN.equals(faultCodeValue.getLocalPart()) ||
                SOAP12Constants.FAULT_CODE_MUST_UNDERSTAND.equals(faultCodeValue.getLocalPart()) ||
                SOAP12Constants.FAULT_CODE_RECEIVER.equals(faultCodeValue.getLocalPart()) ||
                SOAP12Constants.FAULT_CODE_SENDER.equals(faultCodeValue.getLocalPart()) ||
                SOAP12Constants.FAULT_CODE_VERSION_MISMATCH.equals(faultCodeValue.getLocalPart())) ){

                this.faultCodeValue = faultCodeValue;

            } else {
                String msg = "Invalid Fault code value for a SOAP 1.2 fault : " + faultCodeValue;
                handleException(msg);
            }
        }
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    public AXIOMXPath getFaultCodeExpr() {
        return faultCodeExpr;
    }

    public void setFaultCodeExpr(AXIOMXPath faultCodeExpr) {
        this.faultCodeExpr = faultCodeExpr;
    }

    public String getFaultReasonValue() {
        return faultReasonValue;
    }

    public void setFaultReasonValue(String faultReasonValue) {
        this.faultReasonValue = faultReasonValue;
    }

    public AXIOMXPath getFaultReasonExpr() {
        return faultReasonExpr;
    }

    public void setFaultReasonExpr(AXIOMXPath faultReasonExpr) {
        this.faultReasonExpr = faultReasonExpr;
    }

    public URI getFaultNode() {
        return faultNode;
    }

    public void setFaultNode(URI faultNode) {
        if (soapVersion == SOAP11) {
            handleException("A fault node does not apply to a SOAP 1.1 fault");
        }
        this.faultNode = faultNode;
    }

    public URI getFaultRole() {
        return faultRole;
    }

    public void setFaultRole(URI faultRole) {
        this.faultRole = faultRole;
    }

    public String getFaultDetail() {
        return faultDetail;
    }

    public void setFaultDetail(String faultDetail) {
        this.faultDetail = faultDetail;
    }
}
