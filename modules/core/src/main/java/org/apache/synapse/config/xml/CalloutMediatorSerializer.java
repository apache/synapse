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
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.endpoints.EndpointSerializer;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.builtin.CalloutMediator;

/**
 * <pre>
 * &lt;callout [serviceURL="string"] [action="string"][passHeaders="true|false"] [initAxis2ClientOptions="true|false"]&gt;
 *      &lt;configuration [axis2xml="string"] [repository="string"]/&gt;?
 *      &lt;endpoint/&gt;?
 *      &lt;source xpath="expression" | key="string"&gt;?
 *      &lt;target xpath="expression" | key="string"/&gt;?
 *      &lt;enableSec policy="string" | outboundPolicy="String" | inboundPolicy="String" /&gt;?
 * &lt;/callout&gt;
 * </pre>
 */
public class CalloutMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeSpecificMediator(Mediator m) {

        if (!(m instanceof CalloutMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
            return null;
        }

        CalloutMediator mediator = (CalloutMediator) m;
        OMElement callout = fac.createOMElement("callout", synNS);
        saveTracingState(callout, mediator);

        if (mediator.getServiceURL() != null) {
            callout.addAttribute(fac.createOMAttribute("serviceURL", nullNS, mediator.getServiceURL()));
        }

        Endpoint endpoint = mediator.getEndpoint();
        if (endpoint != null) {
            callout.addChild(EndpointSerializer.getElementFromEndpoint(endpoint));
        }

        if (mediator.getAction() != null) {
            callout.addAttribute(fac.createOMAttribute("action", nullNS, mediator.getAction()));
        }

        if (mediator.getClientRepository() != null || mediator.getAxis2xml() != null) {
            OMElement config = fac.createOMElement("configuration", synNS);
            if (mediator.getClientRepository() != null) {
                config.addAttribute(fac.createOMAttribute(
                        "repository", nullNS, mediator.getClientRepository()));
            }
            if (mediator.getAxis2xml() != null) {
                config.addAttribute(fac.createOMAttribute(
                        "axis2xml", nullNS, mediator.getAxis2xml()));
            }
            callout.addChild(config);
        }

        if (mediator.isPassHeaders()) {
            callout.addAttribute(fac.createOMAttribute("passHeaders", nullNS, "true"));
        }

        if (!mediator.isInitClientOptions()) {
            callout.addAttribute(fac.createOMAttribute("initAxis2ClientOptions", nullNS, "false"));
        }

        if (mediator.getRequestXPath() != null || mediator.getRequestKey() != null) {
            OMElement source = fac.createOMElement("source", synNS, callout);
            if (mediator.getRequestXPath() != null) {
                SynapseXPathSerializer.serializeXPath(mediator.getRequestXPath(), source, "xpath");
            } else if (mediator.getRequestKey() != null) {
                source.addAttribute(fac.createOMAttribute(
                        "key", nullNS, mediator.getRequestKey()));
            }
        }

        if (mediator.getTargetXPath() != null || mediator.getTargetKey() != null) {
            OMElement target = fac.createOMElement("target", synNS, callout);
            if (mediator.getTargetXPath() != null) {
                SynapseXPathSerializer.serializeXPath(mediator.getTargetXPath(), target, "xpath");
            } else if (mediator.getTargetKey() != null) {
                target.addAttribute(fac.createOMAttribute(
                        "key", nullNS, mediator.getTargetKey()));
            }
        }

        if (mediator.isSecurityOn()) {
            OMElement security = fac.createOMElement("enableSec", synNS);
            if (mediator.getWsSecPolicyKey() != null) {
                security.addAttribute(fac.createOMAttribute(
                        "policy", nullNS, mediator.getWsSecPolicyKey()));
                callout.addChild(security);
            } else if (mediator.getOutboundWsSecPolicyKey() != null || mediator.getInboundWsSecPolicyKey() != null) {
                if (mediator.getOutboundWsSecPolicyKey() != null) {
                    security.addAttribute(fac.createOMAttribute(
                            "outboundPolicy", nullNS, mediator.getOutboundWsSecPolicyKey()));
                }
                if (mediator.getInboundWsSecPolicyKey() != null) {
                    security.addAttribute(fac.createOMAttribute(
                            "inboundPolicy", nullNS, mediator.getInboundWsSecPolicyKey()));
                }
                callout.addChild(security);
            }
        }

        return callout;
    }

    public String getMediatorClassName() {
        return CalloutMediator.class.getName();
    }
}
