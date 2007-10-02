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

import org.apache.axiom.om.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.config.xml.endpoints.EndpointAbstractSerializer;
import org.apache.synapse.config.xml.endpoints.EndpointSerializer;
import org.apache.synapse.core.axis2.ProxyService;

import java.util.Iterator;
import java.util.ArrayList;
import java.net.URI;

/**
 * <proxyService name="string" [transports="(http |https |jms )+|all"]>
 *    <description>..</description>?
 *    <target [inSequence="name"] [outSequence="name"] [faultSequence="name"] [endpoint="name"]>
 *       <endpoint>...</endpoint>
 *       <inSequence>...</inSequence>
 *       <outSequence>...</outSequence>
 *       <faultSequence>...</faultSequence>
 *    </target>?
 *    <publishWSDL uri=".." key="string">
 *       <wsdl:definition>...</wsdl:definition>?
 *       <wsdl20:description>...</wsdl20:description>?
 *    </publishWSDL>?
 *    <policy key="string">
 *       // optional service parameters
 *    <parameter name="string">
 *       text | xml
 *    </parameter>?
 * </proxyService>
 */
public class ProxyServiceSerializer {

    private static final Log log = LogFactory.getLog(PropertyMediatorSerializer.class);

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = fac.createOMNamespace(XMLConfigConstants.SYNAPSE_NAMESPACE, "syn");
    protected static final OMNamespace nullNS = fac.createOMNamespace(XMLConfigConstants.NULL_NAMESPACE, "");

    public static OMElement serializeProxy(OMElement parent, ProxyService service) {

        OMElement proxy = fac.createOMElement("proxy", synNS);
        if (service.getName() != null) {
            proxy.addAttribute(fac.createOMAttribute(
                    "name", nullNS, service.getName()));
        } else {
            handleException("Invalid proxy service. Service name is required");
        }
        String descriptionStr = service.getDescription();
        if (descriptionStr != null) {
            OMElement description = fac.createOMElement("description", synNS);
            description.addChild(fac.createOMText(descriptionStr));
            proxy.addChild(description);
        }
        ArrayList transports = service.getTransports();
        if (transports != null && !transports.isEmpty()) {
            String transportStr = "" + transports.get(0);
            for (int i = 1; i < transports.size(); i++) {
                transportStr = transportStr + " " + transports.get(i);
            }
            proxy.addAttribute(fac.createOMAttribute("transports", nullNS, transportStr));
        }

        if (service.isStartOnLoad()) {
            proxy.addAttribute(fac.createOMAttribute(
                    "startOnLoad", nullNS, "true"));
        } else {
            proxy.addAttribute(fac.createOMAttribute(
                    "startOnLoad", nullNS, "false"));
        }
        String endpoint = service.getTargetEndpoint();

        OMElement target = fac.createOMElement("target", synNS);
        Endpoint inLineEndpoint = service.getTargetInLineEndpoint();
        if (endpoint != null) {
            target.addAttribute(fac.createOMAttribute(
                    "endpoint", nullNS, endpoint));
            proxy.addChild(target);
        } else if (inLineEndpoint != null) {
            EndpointSerializer serializer
                    = EndpointAbstractSerializer.getEndpointSerializer(inLineEndpoint);
            OMElement epElement = serializer.serializeEndpoint(inLineEndpoint);
            target.addChild(epElement);            
            proxy.addChild(target);
        }
            String inSeq = service.getTargetInSequence();
            String outSeq = service.getTargetOutSequence();
            String faultSeq = service.getTargetFaultSequence();
            SequenceMediatorSerializer serializer = new SequenceMediatorSerializer();
            if (inSeq != null) {
                target.addAttribute(fac.createOMAttribute("inSequence", nullNS, inSeq));
                proxy.addChild(target);
            } else {
                SequenceMediator inLineInSeq = service.getTargetInLineInSequence();
                if (inLineInSeq != null) {
                    OMElement inSeqElement = serializer.serializeAnonymousSequence(null, inLineInSeq);
                    inSeqElement.setLocalName("inSequence");
                    target.addChild(inSeqElement);
                    proxy.addChild(target);
                }
            }
            if (outSeq != null) {
                target.addAttribute(fac.createOMAttribute("outSequence", nullNS, outSeq));
                proxy.addChild(target);
            } else {
                SequenceMediator inLineOutSeq = service.getTargetInLineOutSequence();
                if (inLineOutSeq != null) {
                    OMElement outSeqElement = serializer.serializeAnonymousSequence(null, inLineOutSeq);
                    outSeqElement.setLocalName("outSequence");
                    target.addChild(outSeqElement);
                    proxy.addChild(target);
                }
            }
            if (faultSeq != null) {
                target.addAttribute(fac.createOMAttribute("faultSequence", nullNS, faultSeq));
                proxy.addChild(target);
            } else {
                SequenceMediator inLineFaultSeq = service.getTargetInLineFaultSequence();
                if (inLineFaultSeq != null) {
                    OMElement faultSeqElement = serializer.serializeAnonymousSequence(null, inLineFaultSeq);
                    faultSeqElement.setLocalName("faultSequence");
                    target.addChild(faultSeqElement);
                    proxy.addChild(target);
                }
            }

        

        String wsdlKey = service.getWSDLKey();
        URI wsdlUri = service.getWsdlURI();
        Object inLineWSDL = service.getInLineWSDL();
        OMElement wsdl = fac.createOMElement("publishWSDL", synNS);
        if (wsdlKey != null) {
            wsdl.addAttribute(fac.createOMAttribute(
                    "key", nullNS, wsdlKey));
            proxy.addChild(wsdl);
        } else if (inLineWSDL != null) {
            wsdl.addChild((OMNode) inLineWSDL);
            proxy.addChild(wsdl);
        } else if (wsdlUri != null) {
            wsdl.addAttribute(fac.createOMAttribute(
                    "uri", nullNS, wsdlUri.toString()));
            proxy.addChild(wsdl);
        }

        // TODO still schemas are not used
        // Iterator iter = service.getSchemas();
        // ....

        Iterator iter = service.getServiceLevelPolicies().iterator();
        while (iter.hasNext()) {
            String policyKey = (String) iter.next();
            OMElement policy = fac.createOMElement("policy", synNS);
            policy.addAttribute(fac.createOMAttribute(
                    "key", nullNS, policyKey));
            proxy.addChild(policy);
        }

        iter = service.getParameterMap().keySet().iterator();
        while (iter.hasNext()) {
            String propertyName = (String) iter.next();
            OMElement property = fac.createOMElement("parameter", synNS);
            property.addAttribute(fac.createOMAttribute(
                    "name", nullNS, propertyName));
            Object value = service.getParameterMap().get(propertyName);
            if (value != null) {
                if (value instanceof String) {
                    property.setText(((String) value).trim());
                    proxy.addChild(property);
                } else if (value instanceof OMNode) {
                    property.addChild((OMNode) value);
                    proxy.addChild(property);
                }
            }
        }

        if (service.isWsRMEnabled()) {
            proxy.addChild(fac.createOMElement("enableRM", synNS));
        }
        if (service.isWsSecEnabled()) {
            proxy.addChild(fac.createOMElement("enableSec", synNS));
        }

        int isEnableStatistics = service.getStatisticsState();
        String statisticsValue = null;
        if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_ON) {
            statisticsValue = XMLConfigConstants.STATISTICS_ENABLE;
        } else if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_OFF) {
            statisticsValue = XMLConfigConstants.STATISTICS_DISABLE;
        }
        if (statisticsValue != null) {
            proxy.addAttribute(fac.createOMAttribute(
                    XMLConfigConstants.STATISTICS_ATTRIB_NAME, nullNS, statisticsValue));
        }

        int traceState = service.getTraceState();
        String traceValue = null;
        if (traceState == org.apache.synapse.SynapseConstants.TRACING_ON) {
            traceValue = XMLConfigConstants.TRACE_ENABLE;
        } else if (traceState == org.apache.synapse.SynapseConstants.TRACING_OFF) {
            traceValue = XMLConfigConstants.TRACE_DISABLE;
        }
        if (traceValue != null) {
            proxy.addAttribute(fac.createOMAttribute(
                    XMLConfigConstants.TRACE_ATTRIB_NAME, nullNS, traceValue));
        }
        if (parent != null) {
            parent.addChild(proxy);
        }
        return proxy;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
