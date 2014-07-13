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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.ProxyService;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.ArrayList;

/**
 * Creates a ProxyService instance using the XML fragment specification
 *
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
public class ProxyServiceFactory {

    private static final Log log = LogFactory.getLog(ProxyServiceFactory.class);

    public static ProxyService createProxy(OMElement elem) {

        ProxyService proxy = new ProxyService();

        OMAttribute name = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        if (name == null) {
            handleException("The 'name' attribute is required for a Proxy service definition");
        } else {
            proxy.setName(name.getAttributeValue());
        }

        OMAttribute trans = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "transports"));
        if (trans != null) {
            String transports = trans.getAttributeValue();
            if (transports == null || ProxyService.ALL_TRANSPORTS.equals(transports)) {
                        // default to all transports using service name as destination
            } else {
                StringTokenizer st = new StringTokenizer(transports, " ,");
                ArrayList transportList = new ArrayList();
                while(st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if(token.length() != 0) {
                        transportList.add(token);
                    }
                }
                proxy.setTransports(transportList);
            }
        }
        OMAttribute trace = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, Constants.TRACE_ATTRIB_NAME));
        if (trace != null) {
            String traceValue = trace.getAttributeValue();
            if (traceValue != null) {
                if (traceValue.equals(Constants.TRACE_ENABLE)) {
                    proxy.setTraceState(org.apache.synapse.Constants.TRACING_ON);
                } else if (traceValue.equals(Constants.TRACE_DISABLE)) {
                    proxy.setTraceState(org.apache.synapse.Constants.TRACING_OFF);
                }
            }
        }
        OMAttribute startOnLoad = elem.getAttribute(
                new QName(Constants.NULL_NAMESPACE, "startOnLoad"));
        if(startOnLoad != null) {
            proxy.setStartOnLoad(Boolean.valueOf(startOnLoad.getAttributeValue()).booleanValue());
        } else {
            proxy.setStartOnLoad(true);
        }

        // read definition of the target of this proxy service. The target could be an 'endpoint'
        // or a named sequence. If none of these are specified, the messages would be mediated
        // by the Synapse main mediator
        OMElement target  = elem.getFirstChildWithName(
                new QName(Constants.SYNAPSE_NAMESPACE, "target"));
        if (target != null) {
            OMAttribute inSequence = target.getAttribute(
                    new QName(Constants.NULL_NAMESPACE, "inSequence"));
            if (inSequence != null) {
                proxy.setTargetInSequence(inSequence.getAttributeValue());
            }
            OMAttribute outSequence = target.getAttribute(
                    new QName(Constants.NULL_NAMESPACE, "outSequence"));
            if (outSequence != null) {
                proxy.setTargetOutSequence(outSequence.getAttributeValue());
            }
            OMAttribute tgtEndpt = target.getAttribute(
                    new QName(Constants.NULL_NAMESPACE, "endpoint"));
            if (tgtEndpt != null) {
                proxy.setTargetEndpoint(tgtEndpt.getAttributeValue());
            }
        }

        // read the WSDL, Schemas and Policies and set to the proxy service
        OMElement wsdl = elem.getFirstChildWithName(new QName(Constants.SYNAPSE_NAMESPACE, "wsdl"));
        if (wsdl != null) {
            OMAttribute wsdlkey = wsdl.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
            if (wsdlkey == null) {
                handleException("The 'key' attribute is required for the base WSDL definition");
            } else {
                proxy.setWSDLKey(wsdlkey.getAttributeValue());
            }
        }

//        OMElement schema = elem.getFirstChildWithName(
//                new QName(Constants.SYNAPSE_NAMESPACE, "schema"));
        Iterator policies = elem.getChildrenWithName(
                new QName(Constants.SYNAPSE_NAMESPACE, "policy"));
        while (policies.hasNext()) {
            Object o = policies.next();
            if (o instanceof OMElement) {
                OMElement policy = (OMElement) o;
                OMAttribute key = policy.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
                if (key != null) {
                    proxy.addServiceLevelPolicy(key.getAttributeValue());
                } else {
                    handleException("Policy element does not specify the policy key");
                }
            } else {
                handleException("Invalid 'policy' element found under element 'policies'");
            }
        }

        Iterator props = elem.getChildrenWithName(
                new QName(Constants.SYNAPSE_NAMESPACE, "property"));
        while (props.hasNext()) {
            Object o = props.next();
            if (o instanceof OMElement) {
                OMElement prop = (OMElement) o;
                OMAttribute pname = prop.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
                OMAttribute value = prop.getAttribute(new QName(Constants.NULL_NAMESPACE, "value"));
                if (pname != null && value != null) {
                    proxy.addProperty(pname.getAttributeValue(), value.getAttributeValue());
                } else {
                    handleException("Invalid property specified for proxy service : " + name);
                }
            } else {
                handleException("Invalid property specified for proxy service : " + name);
            }
        }

        if (elem.getFirstChildWithName(
                new QName(Constants.SYNAPSE_NAMESPACE, "enableRM")) != null) {
            proxy.setWsRMEnabled(true);
        }

        if (elem.getFirstChildWithName(
                new QName(Constants.SYNAPSE_NAMESPACE, "enableSec")) != null) {
            proxy.setWsSecEnabled(true);
        }

        return proxy;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

}
