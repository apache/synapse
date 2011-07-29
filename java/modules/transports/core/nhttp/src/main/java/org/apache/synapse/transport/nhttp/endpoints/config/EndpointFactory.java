/**
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

package org.apache.synapse.transport.nhttp.endpoints.config;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.nhttp.endpoints.Endpoint;
import org.apache.synapse.transport.nhttp.endpoints.EndpointsConfiguration;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Create the Endpoint from the given XML.
 */
public class EndpointFactory {
    private static final Log log = LogFactory.getLog(EndpointFactory.class);

    public Endpoint create(OMElement xml) throws AxisFault {
        OMAttribute urlPatternAttr = xml.getAttribute(new QName(EndpointsConfiguration.URL_PATTERN));
        if (urlPatternAttr == null) {
            handleException(EndpointsConfiguration.URL_PATTERN +
                    " attribute is mandory for an endpoint configuration");
            return null;
        }

        String pattern = urlPatternAttr.getAttributeValue();
        Endpoint endpoint = new Endpoint(Pattern.compile(pattern));

        OMElement messageBuilders = xml.getFirstChildWithName(
                new QName(EndpointsConfiguration.MESSAGE_BUILDERS));

        if (messageBuilders != null) {
            OMAttribute defaultBuilderAttr = messageBuilders.getAttribute(
                    new QName("defaultBuilder"));
            if (defaultBuilderAttr != null) {
                Builder builder = loadBuilder(defaultBuilderAttr.getAttributeValue());
                if (builder != null) {
                    endpoint.setDefaultBuilder(builder);
                }
            }

            Iterator it = messageBuilders.getChildrenWithName(
                    new QName(EndpointsConfiguration.MESSAGE_BUILDER));
            while(it.hasNext()) {
                OMElement builderElement = (OMElement) it.next();

                OMAttribute contentTypeAttr = builderElement.getAttribute(
                        new QName(EndpointsConfiguration.CONTENT_TYPE));
                if (contentTypeAttr == null) {
                    handleException(EndpointsConfiguration.CONTENT_TYPE +
                            " attribute cannot be null for endpoint " +
                            "with the " + EndpointsConfiguration.URL_PATTERN + " : " + pattern);
                }

                OMAttribute classAttr = builderElement.getAttribute(
                        new QName(EndpointsConfiguration.CLASS));
                if (classAttr == null) {
                    handleException(EndpointsConfiguration.CLASS +
                            " attribute cannot be null for endpoint " +
                            "with the " + EndpointsConfiguration.URL_PATTERN + " : " + pattern);
                }

                if (classAttr != null && contentTypeAttr != null) {
                    Builder builder = loadBuilder(classAttr.getAttributeValue());
                    if (builder != null) {
                        endpoint.addBuilder(contentTypeAttr.getAttributeValue(), builder);
                    }
                }
            }
        }

        Iterator paramItr = xml.getChildrenWithName(
                new QName(EndpointsConfiguration.PARAMETER));
        while (paramItr.hasNext()) {
            OMElement p = (OMElement) paramItr.next();
            OMAttribute paramNameAttr = p.getAttribute(new QName(EndpointsConfiguration.NAME));
            if (paramNameAttr == null) {
                handleException("Parameter " + EndpointsConfiguration.NAME + " cannot be null");
            } else {
                endpoint.addParameter(new Parameter(paramNameAttr.getAttributeValue(), p.getText()));
            }
        }

        return endpoint;
    }

    private Builder loadBuilder(String name) throws AxisFault {
        try {
            if (name != null) {
                Class c = Class.forName(name);
                Object o = c.newInstance();
                if (o instanceof Builder) {
                    return (Builder) o;
                } else {
                    handleException("Class : " + name +
                            " should be a Builder");
                }
            }
        } catch (ClassNotFoundException e) {
            handleException("Error creating builder: " + name, e);
        } catch (InstantiationException e) {
            handleException("Error initializing builder: " + name, e);
        } catch (IllegalAccessException e) {
            handleException("Error initializing builder: " + name, e);
        }

        return null;
    }

    private void handleException(String msg) throws AxisFault {
        log.error(msg);
        throw new AxisFault(msg);
    }

    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }
}
