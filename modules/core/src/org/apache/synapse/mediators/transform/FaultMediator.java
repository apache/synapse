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
import org.apache.axiom.soap.*;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.mediators.AbstractMediator;

import javax.xml.namespace.QName;

/**
 * This transforms the current message instance into a SOAP Fault message. If the
 * original message was SOAP 1.1 the fault will also be SOAP 1.1 else, SOAP 1.2
 *
 * TODO this class needs more attention! - revisit later
 */
public class FaultMediator extends AbstractMediator {
    
    private static final Log log = LogFactory.getLog(FaultMediator.class);

    public static final int SOAP11 = 1;
    public static final int SOAP12 = 2;

    private int soapVersion;
    private QName code;
    private String reason;

    //TODO support SOAP 1.2 fault stuff..
    //Node, Role, detail etc

    public boolean mediate(SynapseMessageContext synCtx) {
        log.debug(getType() + " mediate()");
        SynapseMessage synMsg = synCtx.getSynapseMessage();
        SOAPEnvelope envelop = synMsg.getEnvelope();
        SOAPFactory factory;

        switch (soapVersion) {
            case SOAP11:
                factory = OMAbstractFactory.getSOAP11Factory();
                break;
            case SOAP12:
                factory = OMAbstractFactory.getSOAP12Factory();
                break;
            default : {
                if (envelop != null) {
                    if (envelop.getNamespace().getName().equals(
                        SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
                        factory = OMAbstractFactory.getSOAP12Factory();
                    } else {
                        factory = OMAbstractFactory.getSOAP11Factory();
                    }
                } else {
                    factory = OMAbstractFactory.getSOAP11Factory();
                }
            }
        }

        OMDocument soapFaultDocument = factory.createOMDocument();
        SOAPEnvelope faultEnvelope = factory.getDefaultFaultEnvelope();
        soapFaultDocument.addChild(faultEnvelope);

        /*SOAPFaultReason reason = factory.createSOAPFaultReason();
        reason.setText(getReason());

        SOAPFault fault = factory.createSOAPFault();
        fault.setReason(reason);

        SOAPEnvelope faultEnvelope = factory.getDefaultFaultEnvelope();
        faultEnvelope.getBody().addFault(fault);*/

        // set the fault message to the "faultTo" of the original message if it exists
        // else to the "replyTo"
        EndpointReference toEPR = synMsg.getTo();
        EndpointReference faultToEPR = synMsg.getFaultTo();
        if (faultToEPR != null) {
            synMsg.setTo(faultToEPR);
            synMsg.setReplyTo(toEPR);
        } else {
            EndpointReference replyToEPR = synMsg.getReplyTo();
            synMsg.setTo(replyToEPR);
            synMsg.setReplyTo(toEPR);
        }
        synMsg.setResponse(true);

        try {
            synMsg.setEnvelope(faultEnvelope);
        } catch (AxisFault af) {
            String msg = "Error replacing SOAP envelope with a fault envelope " + af.getMessage();
            log.error(msg);
            throw new SynapseException(af);
        }
        return true;
    }

    public QName getCode() {
        return code;
    }

    public void setCode(QName code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getSoapVersion() {
        return soapVersion;
    }

    public void setSoapVersion(int soapVersion) {
        this.soapVersion = soapVersion;
    }


}
