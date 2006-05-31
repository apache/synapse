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

/**
 * Creates a ProxyService instance using the XML fragment specification
 *
 * <proxy name="string" type="wsdl|jms|rest" [description="string"]>
 *   <endpoint protocols="(http|https|jms)+|all" uri="uri">
 *   <target sequence="name" | endpoint="name"/>?
 *   <wsdl url="url">?
 *   <schema url="url">*
 *   <policy url="url">*
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

        // set the type of the proxy service
        OMAttribute type = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "type"));
        if (type != null) {
            String sType = type.getAttributeValue();
            if ("wsdl".equals(sType)) {
                proxy.setType(ProxyService.WSDL_TYPE);
            } else if ("jms".equals(sType)) {
                proxy.setType(ProxyService.JMS_TYPE);
            } else if ("rest".equals(sType)) {
                proxy.setType(ProxyService.REST_TYPE);
            } else {
                handleException("Unknown proxy type : " + sType);
            }
        }

        // read endpoint definition and set it to the proxy service
        OMElement endpt   = elem.getFirstChildWithName(new QName(Constants.SYNAPSE_NAMESPACE, "endpoint"));
        if (endpt == null) {
            handleException("The proxy services endpoint definition is missing for " + proxy.getName());
        } else {
            // read endpoint protocol
            OMAttribute proto = endpt.getAttribute(new QName(Constants.NULL_NAMESPACE, "protocol"));
            if (proto != null) {
                proxy.setEndpointProtocols(proto.getAttributeValue());
            }

            // read endpoint uri where the service will be made available
            OMAttribute uri   = endpt.getAttribute(new QName(Constants.NULL_NAMESPACE, "uri"));
            if (uri != null) {
                proxy.setEndpointURI(uri.getAttributeValue());
            }
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
        OMElement wsdl   = elem.getFirstChildWithName(new QName(Constants.SYNAPSE_NAMESPACE, "wsdl"));
        if (wsdl == null && proxy.getType() == ProxyService.WSDL_TYPE) {
            handleException("A WSDL URL is required for a WSDL based proxy service");

        } else if (proxy.getType() == ProxyService.WSDL_TYPE) {
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
        } else if (proxy.getType() == ProxyService.JMS_TYPE) {
            // TODO
            throw new UnsupportedOperationException("JMS Proxy services are not yet implemented");
        } else if (proxy.getType() == ProxyService.REST_TYPE) {
            // TODO
            throw new UnsupportedOperationException("REST Proxy services are not yet implemented");
        }

        //OMElement schema = elem.getFirstChildWithName(new QName(Constants.SYNAPSE_NAMESPACE, "schema"));
        //OMElement policy = elem.getFirstChildWithName(new QName(Constants.SYNAPSE_NAMESPACE, "policy"));

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
