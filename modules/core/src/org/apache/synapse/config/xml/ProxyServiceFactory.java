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
package org.apache.synapse.config.xml;

import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Iterator;

/**
 * Creates a ProxyService instance using the XML fragment specification
 *
 * <proxy name="string" [description="string"] [transports="(http|https|jms)+|all"]>
 *   <target sequence="name" | endpoint="name"/>?   // default is main sequence
 *   <wsdl url="url">?
 *   <schema url="url">*
 *   <policy url="url">*
 *   <property name="string" value="string"/>*
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

        OMAttribute desc = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "description"));
        if (desc != null) {
            proxy.setDescription(desc.getAttributeValue());
        }

        OMAttribute trans = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "transports"));
        if (trans != null) {
            proxy.setTransports(trans.getAttributeValue());
        }

        // read definition of the target of this proxy service. The target could be an 'endpoint'
        // or a named sequence. If none of these are specified, the messages would be mediated
        // by the Synapse main mediator
        OMElement target  = elem.getFirstChildWithName(new QName(Constants.SYNAPSE_NAMESPACE, "target"));
        if (target != null) {
            OMAttribute sequence = target.getAttribute(new QName(Constants.NULL_NAMESPACE, "sequence"));
            if (sequence != null) {
                proxy.setTargetSequence(sequence.getAttributeValue());
            }
            OMAttribute tgtEndpt = target.getAttribute(new QName(Constants.NULL_NAMESPACE, "endpoint"));
            if (tgtEndpt != null) {
                proxy.setTargetEndpoint(tgtEndpt.getAttributeValue());
            }
        }

        // read the WSDL, Schemas and Policies and set to the proxy service
        OMElement wsdl = elem.getFirstChildWithName(new QName(Constants.SYNAPSE_NAMESPACE, "wsdl"));
        if (wsdl != null) {
            OMAttribute wsdlurl = wsdl.getAttribute(new QName(Constants.NULL_NAMESPACE, "url"));
            if (wsdlurl == null) {
                handleException("The 'url' attribute is required for the base WSDL definition");
            } else {
                String wUrl = wsdlurl.getAttributeValue();
                try {
                    proxy.setWsdl(new URL(wUrl));
                } catch (MalformedURLException e) {
                    handleException("Invalid WSDL URL : " + wUrl, e);
                }
            }
        }

        //OMElement schema = elem.getFirstChildWithName(new QName(Constants.SYNAPSE_NAMESPACE, "schema"));
        Iterator policies = elem.getChildrenWithName(new QName(Constants.SYNAPSE_NAMESPACE, "policy"));
        while (policies.hasNext()) {
            Object o = policies.next();
            if (o instanceof OMElement) {
                OMElement policy = (OMElement) o;
                OMAttribute url = policy.getAttribute(new QName(Constants.NULL_NAMESPACE, "url"));
                if (url != null) {
                    try {
                        proxy.addServiceLevelPoliciy(new URL(url.getAttributeValue()));
                    } catch (MalformedURLException e) {
                        handleException("Invalid policy URL : " + url.getAttributeValue());
                    }
                } else {
                    handleException("Policy element does not specify the policy URL");
                }
            } else {
                handleException("Invalid 'policy' element found under element 'policies'");
            }
        }

        Iterator props = elem.getChildrenWithName(new QName(Constants.SYNAPSE_NAMESPACE, "property"));
        while (props.hasNext()) {
            Object o = props.next();
            if (o instanceof OMElement) {
                OMElement prop = (OMElement) o;
                OMAttribute pname = prop.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
                OMAttribute value = prop.getAttribute(new QName(Constants.NULL_NAMESPACE, "value"));
                if (name != null && value != null) {
                    proxy.addProperty(pname.getAttributeValue(), value.getAttributeValue());
                } else {
                    handleException("Invalid property specified for proxy service : " + name);
                }
            } else {
                handleException("Invalid property specified for proxy service : " + name);
            }
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
