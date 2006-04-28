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

package org.apache.synapse.mediators.builtin.axis2;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.Mediator;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;

import javax.xml.namespace.QName;

/**
 * <p/> This returns a fault in response to this message
 */
public class FaultMediator implements Mediator{

    private Log log = LogFactory.getLog(getClass());

    private QName faultCode;
    private String reason;

    public boolean mediate(SynapseMessage smc) {
        log.debug("process");

        SOAPEnvelope envelop = smc.getEnvelope();
        SOAPFactory factory;
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
        try {
            // TODO : Figure out how to easily gen the correct fault

            // Replace this
            OMDocument soapFaultDocument = factory.createOMDocument();
            SOAPEnvelope faultEnvelope = factory.getDefaultFaultEnvelope();
            soapFaultDocument.addChild(faultEnvelope);

            smc.setEnvelope(faultEnvelope);
        } catch (Exception e) {
            throw new SynapseException(e);
        }
        smc.setResponse(true);

        // Flipping the headers
        EndpointReference tempEPR = smc.getTo();
        smc.setTo(smc.getReplyTo());
        smc.setReplyTo(tempEPR);

        smc.getSynapseEnvironment().injectMessage(smc);

        return false;
    }

    public QName getFaultCode() {
        return faultCode;
    }

    public void setFaultCode(QName faultCode) {
        this.faultCode = faultCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
