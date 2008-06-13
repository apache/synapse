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

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.transform.FaultMediator;

/**
 * <pre>
 * &lt;makefault [version="soap11|soap12"]&gt;
 *   &lt;code (value="literal" | expression="xpath")/&gt;
 *   &lt;reason (value="literal" | expression="xpath")&gt;
 *   &lt;node&gt;?
 *   &lt;role&gt;?
 *   &lt;detail&gt;?
 * &lt;/makefault&gt;
 * </pre>
 */
public class FaultMediatorSerializer extends AbstractMediatorSerializer {

    private static final String SOAP11 = "soap11";

    private static final String SOAP12 = "soap12";

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof FaultMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        FaultMediator mediator = (FaultMediator) m;
        OMElement fault = fac.createOMElement("makefault", synNS);
        saveTracingState(fault,mediator);

        if(mediator.getSoapVersion()==FaultMediator.SOAP11) {
           fault.addAttribute(fac.createOMAttribute(
                "version", nullNS, SOAP11));
        }else if(mediator.getSoapVersion()==FaultMediator.SOAP12) {
           fault.addAttribute(fac.createOMAttribute(
                "version", nullNS, SOAP12));
        }

        OMElement code = fac.createOMElement("code", synNS, fault);
        if (mediator.getFaultCodeValue() != null) {
            code.addAttribute(fac.createOMAttribute(
                "value", nullNS, mediator.getFaultCodeValue().getPrefix() + ":"
                    + mediator.getFaultCodeValue().getLocalPart()));
            code.declareNamespace(mediator.getFaultCodeValue().getNamespaceURI(),
                    mediator.getFaultCodeValue().getPrefix());

        } else if (mediator.getFaultCodeExpr() != null) {
            code.addAttribute(fac.createOMAttribute(
                "expression", nullNS, mediator.getFaultCodeExpr().toString()));
            super.serializeNamespaces(code, mediator.getFaultCodeExpr());

        } else {
            handleException("Fault code is required for a fault mediator");
        }

        OMElement reason = fac.createOMElement("reason", synNS, fault);
        if (mediator.getFaultReasonValue() != null) {
            reason.addAttribute(fac.createOMAttribute(
                "value", nullNS, mediator.getFaultReasonValue()));

        } else if (mediator.getFaultReasonExpr() != null) {
            reason.addAttribute(fac.createOMAttribute(
                "expression", nullNS, mediator.getFaultReasonExpr().toString()));
            super.serializeNamespaces(code, mediator.getFaultReasonExpr());

        } else {
            handleException("Fault reason is required for a fault mediator");
        }


        if (mediator.getFaultNode() != null) {
            OMElement node = fac.createOMElement("node", synNS, fault);
            node.setText(mediator.getFaultNode().toString());
        }

        if (mediator.getFaultRole() != null) {
            OMElement role = fac.createOMElement("role", synNS, fault);
            role.setText(mediator.getFaultRole().toString());
        }

        if (mediator.getFaultDetail() != null) {
            OMElement detail = fac.createOMElement("detail", synNS, fault);
            detail.setText(mediator.getFaultDetail());
        }

        if (parent != null) {
            parent.addChild(fault);
        }
        return fault;
    }

    public String getMediatorClassName() {
        return FaultMediator.class.getName();
    }
}
