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

package org.apache.synapse.config.xml.rest;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.rest.API;
import org.apache.synapse.rest.Handler;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.rest.version.VersionStrategy;

import javax.xml.namespace.QName;
import java.util.Iterator;

public class APIFactory {

    private static final Log log = LogFactory.getLog(APIFactory.class);

    public static API createAPI(OMElement apiElt) {
        OMAttribute nameAtt = apiElt.getAttribute(new QName("name"));
        if (nameAtt == null || "".equals(nameAtt.getAttributeValue())) {
            handleException("Attribute 'name' is required for an API definition");
            return null;
        }

        OMAttribute contextAtt = apiElt.getAttribute(new QName("context"));
        if (contextAtt == null || "".equals(contextAtt.getAttributeValue())) {
            handleException("Attribute 'context' is required for an API definition");
            return null;
        }

        API api = new API(nameAtt.getAttributeValue(), contextAtt.getAttributeValue());

        OMAttribute hostAtt = apiElt.getAttribute(new QName("hostname"));
        if (hostAtt != null && !"".equals(hostAtt.getAttributeValue())) {
            api.setHost(hostAtt.getAttributeValue());
        }

        VersionStrategy vStrategy = VersionStrategyFactory.createVersioningStrategy(api, apiElt);

        api.setVersionStrategy(vStrategy);

        OMAttribute portAtt = apiElt.getAttribute(new QName("port"));
        if (portAtt != null && !"".equals(portAtt.getAttributeValue())) {
            api.setPort(Integer.parseInt(portAtt.getAttributeValue()));
        }

        Iterator resources = apiElt.getChildrenWithName(new QName(
                XMLConfigConstants.SYNAPSE_NAMESPACE, "resource"));
        boolean noResources = true;
        while (resources.hasNext()) {
            OMElement resourceElt = (OMElement) resources.next();
            api.addResource(ResourceFactory.createResource(resourceElt));
            noResources = false;
        }

        if (noResources) {
            handleException("An API must contain at least one resource definition");
        }

        OMElement handlersElt = apiElt.getFirstChildWithName(new QName(
                XMLConfigConstants.SYNAPSE_NAMESPACE, "handlers"));
        if (handlersElt != null) {
            Iterator handlers = handlersElt.getChildrenWithName(new QName(
                    XMLConfigConstants.SYNAPSE_NAMESPACE, "handler"));
            while (handlers.hasNext()) {
                OMElement handlerElt = (OMElement) handlers.next();
                defineHandler(api, handlerElt);
            }
        }

        OMAttribute transport = apiElt.getAttribute(
                new QName(XMLConfigConstants.NULL_NAMESPACE, "transport"));
        if (transport != null) {
            String transports = transport.getAttributeValue();
            if (!"".equals(transports)) {
                if (Constants.TRANSPORT_HTTP.equals(transports)) {
                    api.setProtocol(RESTConstants.PROTOCOL_HTTP_ONLY);
                } else if (Constants.TRANSPORT_HTTPS.equals(transports)) {
                    api.setProtocol(RESTConstants.PROTOCOL_HTTPS_ONLY);
                } else {
                    handleException("Invalid protocol name: " + transports);
                }
            }
        }
        return api;
    }

    private static void defineHandler(API api, OMElement handlerElt) {
        String handlerClass = handlerElt.getAttributeValue(new QName("class"));
        if (handlerClass == null || "".equals(handlerClass)) {
            handleException("A handler element must have a class attribute");
        }

        try {
            Class clazz = APIFactory.class.getClassLoader().loadClass(handlerClass);
            Handler handler = (Handler) clazz.newInstance();
            api.addHandler(handler);
        } catch (Exception e) {
            handleException("Error initializing API handler: " + handlerClass, e);
        }
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
