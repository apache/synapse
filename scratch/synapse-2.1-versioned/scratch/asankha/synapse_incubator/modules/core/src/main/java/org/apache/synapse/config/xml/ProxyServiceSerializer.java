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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.ProxyService;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * <proxy name="string" [description="string"] [transports="(http|https|jms)+|all"]>
 *   <target sequence="name" | endpoint="name"/>?   // default is main sequence
 *   <wsdl key="string">?
 *   <schema key="string">*
 *   <policy key="string">*
 *   <property name="string" value="string"/>*
 *   <enableRM/>+
 *   <enableSec/>+
 * </proxy>
 */
public class ProxyServiceSerializer {

    private static final Log log = LogFactory.getLog(PropertyMediatorSerializer.class);

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = fac.createOMNamespace(Constants.SYNAPSE_NAMESPACE, "syn");
    protected static final OMNamespace nullNS = fac.createOMNamespace(Constants.NULL_NAMESPACE, "");

    public static OMElement serializeProxy(OMElement parent, ProxyService service) {

        OMElement proxy = fac.createOMElement("proxy", synNS);
        if (service.getName() != null) {
            proxy.addAttribute(fac.createOMAttribute(
                "name", nullNS, service.getName()));
        } else {
            handleException("Invalid proxy service. Service name is required");
        }

        if (service.getDescription() != null) {
            proxy.addAttribute(fac.createOMAttribute(
                "description", nullNS, service.getDescription()));
        }

        String wsdlKey = service.getWSDLKey();
        if(wsdlKey != null) {
            OMElement wsdl = fac.createOMElement("wsdl", synNS);
            wsdl.addAttribute(fac.createOMAttribute(
                "key", nullNS, wsdlKey));
            proxy.addChild(wsdl);
        }

        if (service.getTransports() != null && service.getTransports().size() != 0) {
            ArrayList transports = service.getTransports();
            String transportStr = "" + transports.get(0);
            for(int i = 1; i < transports.size(); i++) {
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

        if (service.getTargetEndpoint() != null) {
            OMElement target = fac.createOMElement("target", synNS);
            target.addAttribute(fac.createOMAttribute(
                "endpoint", nullNS, service.getTargetEndpoint()));
            proxy.addChild(target);
        } else if (service.getTargetInSequence() != null || service.getTargetOutSequence() != null) {
            OMElement target = fac.createOMElement("target", synNS);
            if (service.getTargetInSequence() != null) {
                target.addAttribute(fac.createOMAttribute("inSequence", nullNS, service.getTargetInSequence()));
            }
            if (service.getTargetOutSequence() != null) {
                target.addAttribute(fac.createOMAttribute("outSequence", nullNS, service.getTargetOutSequence()));
            }
            proxy.addChild(target);
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

        iter = service.getPropertyMap().keySet().iterator();
        while (iter.hasNext()) {
            String propertyName = (String) iter.next();
            OMElement property = fac.createOMElement("property", synNS);
            property.addAttribute(fac.createOMAttribute(
                "name", nullNS, propertyName));
            property.addAttribute(fac.createOMAttribute(
                "value", nullNS, (String) service.getPropertyMap().get(propertyName)));
            proxy.addChild(property);
        }

        if (service.isWsRMEnabled()) {
            proxy.addChild(fac.createOMElement("enableRM", synNS));
        }

        if (service.isWsSecEnabled()) {
            proxy.addChild(fac.createOMElement("enableSec", synNS));
        }
        int traceState = service.getTraceState();
        String traceValue = null;
        if (traceState == org.apache.synapse.Constants.TRACING_ON) {
            traceValue = Constants.TRACE_ENABLE;
        } else if (traceState == org.apache.synapse.Constants.TRACING_OFF) {
            traceValue = Constants.TRACE_DISABLE;
        }
        if (traceValue != null) {
            proxy.addAttribute(fac.createOMAttribute(
                    Constants.TRACE_ATTRIB_NAME, nullNS, traceValue));
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
