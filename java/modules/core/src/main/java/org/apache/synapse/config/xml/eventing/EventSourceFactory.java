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

package org.apache.synapse.config.xml.eventing;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.databinding.utils.ConverterUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.PropertyHelper;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.eventing.SynapseEventingConstants;
import org.apache.synapse.eventing.SynapseSubscription;
import org.apache.synapse.eventing.SynapseSubscriptionManager;
import org.apache.synapse.eventing.filters.XPathBasedEventFilter;
import org.wso2.eventing.SubscriptionData;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * <eventSource name="blah">
 * <subscriptionManager class="org.apache.synapse.events.DefaultInMemorySubscriptionManager">
 * <property name="other" value="some text property"/>
 * </subscriptionManager>
 * <subscription id="static1">
 * <filter....>
 * <sequence...>
 * <endpoint..>
 * </subscription>*
 * <eventSource>
 */
public class EventSourceFactory {

    private static final Log log = LogFactory.getLog(EventSourceFactory.class);
    private static final QName SUBSCRIPTION_MANAGER_QNAME
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "subscriptionManager");
    private static final QName PROPERTIES_QNAME =
            new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "property");
    private static final QName WS_EVENTING_QNAME
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "wsEventing");
    private static final QName SUBSCRIPTION_QNAME =
            new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "subscription");
    private static final QName FILTER_QNAME =
            new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "filter");
    private static final QName ENDPOINT_QNAME =
            new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "endpoint");
    private static final QName ADDRESS_QNAME =
            new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "address");
    private static final QName EXPIRES_QNAME =
            new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "expires");
    private static final QName FILTER_SOURCE_QNAME = new QName("source");
    private static final QName FILTER_DIALECT_QNAME = new QName("dialect");
    private static final QName ID_QNAME = new QName("id");
    private static final QName EP_URI_QNAME = new QName("uri");

    public static SynapseEventSource createEventSource(OMElement elem) {

        SynapseEventSource eventSource = null;

        OMAttribute name = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));
        if (name == null) {
            handleException("The 'name' attribute is required for a event source de");
        } else {
            eventSource = new SynapseEventSource(name.getAttributeValue());
        }

        OMElement subscriptionManagerElem = elem.getFirstChildWithName(SUBSCRIPTION_MANAGER_QNAME);
        if (eventSource != null && subscriptionManagerElem != null) {

            OMAttribute clazz = subscriptionManagerElem
                    .getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "class"));
            if (clazz != null) {
                String className = clazz.getAttributeValue();
                try {
                    Class subscriptionManagerClass = Class.forName(className);
                    SynapseSubscriptionManager manager =
                            (SynapseSubscriptionManager) subscriptionManagerClass.newInstance();
                    Iterator itr = subscriptionManagerElem.getChildrenWithName(PROPERTIES_QNAME);
                    while (itr.hasNext()) {
                        OMElement propElem = (OMElement) itr.next();
                        String propName =
                                propElem.getAttribute(new QName("name")).getAttributeValue();
                        String propValue =
                                propElem.getAttribute(new QName("value")).getAttributeValue();
                        manager.addProperty(propName, propValue);
                        PropertyHelper.setStaticProperty(propElem, manager);
                    }
                    eventSource.setSubscriptionManager(manager);
                    eventSource.getSubscriptionManager()
                            .init(); // Initialise before doing further processing, required for static subscriptions
                } catch (ClassNotFoundException e) {
                    handleException("SynapseSubscriptionManager class not found", e);
                } catch (IllegalAccessException e) {
                    handleException("Unable to access the SynapseSubscriptionManager object", e);
                } catch (InstantiationException e) {
                    handleException("Unable to instantiate the SynapseSubscriptionManager object",
                            e);
                }
            } else {
                handleException("SynapseSubscription manager class is a required attribute");
            }
        } else {
            handleException(
                    "SynapseSubscription Manager has not been specified for the event source");
        }
        createStaticSubscriptions(elem, eventSource);
        return eventSource;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    /**
     * Generate the static subscriptions
     *
     * @param elem
     * @param synapseEventSource
     */
    private static void createStaticSubscriptions(OMElement elem,
                                                  SynapseEventSource synapseEventSource) {
        for (Iterator iterator = elem.getChildrenWithName(SUBSCRIPTION_QNAME);
             iterator.hasNext();) {
            SynapseSubscription synapseSubscription = new SynapseSubscription();
            OMElement elmSubscription = (OMElement) iterator.next();
            synapseSubscription.setId(elmSubscription.getAttribute(ID_QNAME).getAttributeValue());
            //process the filter
            OMElement elmFilter = elmSubscription.getFirstChildWithName(FILTER_QNAME);
            OMAttribute dialectAttr = elmFilter.getAttribute(FILTER_DIALECT_QNAME);
            if (dialectAttr != null && dialectAttr.getAttributeValue() != null) {
                if (SynapseEventingConstants.TOPIC_FILTER_DIALECT
                        .equals(dialectAttr.getAttributeValue())) {
                    XPathBasedEventFilter filter = new XPathBasedEventFilter();
                    OMAttribute sourceAttr = elmFilter.getAttribute(FILTER_SOURCE_QNAME);
                    if (sourceAttr != null) {
                        filter.setResultValue(sourceAttr.getAttributeValue());
                    } else {
                        handleException(
                                "Error in creating static subscription. Filter source not defined");
                    }
                    synapseSubscription.setFilter(filter);
                    SubscriptionData subscriptionData = new SubscriptionData();
                    subscriptionData.setProperty(SynapseEventingConstants.FILTER_VALUE,
                            sourceAttr.getAttributeValue());
                    subscriptionData.setProperty(SynapseEventingConstants.FILTER_DIALECT,
                            dialectAttr.getAttributeValue());
                    synapseSubscription.setSubscriptionData(subscriptionData);
                }

            } else {
                handleException(
                        "Error in creating static subscription. Filter dialect not defined");
            }
            OMElement elmEndpoint = elmSubscription.getFirstChildWithName(ENDPOINT_QNAME);
            if (elmEndpoint != null) {
                OMElement elmAddress = elmEndpoint.getFirstChildWithName(ADDRESS_QNAME);
                if (elmAddress != null) {
                    AddressEndpoint endpoint = new AddressEndpoint();
                    EndpointDefinition def = new EndpointDefinition();
                    OMAttribute uriAttr = elmAddress.getAttribute(EP_URI_QNAME);
                    if (uriAttr != null) {
                        def.setAddress(uriAttr.getAttributeValue());
                        endpoint.setDefinition(def);
                        synapseSubscription.setEndpoint(endpoint);
                        synapseSubscription.setEndpointUrl(uriAttr.getAttributeValue());
                        synapseSubscription.setAddressUrl(uriAttr.getAttributeValue());
                    } else {
                        handleException("Error in creating static subscription. URI not defined");
                    }
                } else {
                    handleException("Error in creating static subscription. Address not defined");
                }

            } else {
                handleException("Error in creating static subscription. Endpoint not defined");
            }
            OMElement elmExpires = elmSubscription.getFirstChildWithName(EXPIRES_QNAME);
            if (elmExpires != null) {
                try {
                    if (elmExpires.getText().startsWith("P")) {
                        synapseSubscription.setExpires(ConverterUtil
                                .convertToDuration(elmExpires.getText()).getAsCalendar());
                    } else {
                        synapseSubscription
                                .setExpires(ConverterUtil.convertToDateTime(elmExpires.getText()));
                    }
                } catch (Exception e) {
                    handleException("Error in creating static subscription. invalid date format",
                            e);
                }
            } else {
                synapseSubscription.setExpires(null);
            }
            synapseSubscription.setStaticEntry(true);
            synapseEventSource.getSubscriptionManager().addSubscription(synapseSubscription);
        }
    }
}
