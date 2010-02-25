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

package org.apache.synapse.config.xml.endpoints;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.statistics.StatisticsConfigurable;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.endpoints.*;
import org.apache.synapse.endpoints.EndpointDefinition;

/**
 * All endpoint serializers should implement this interface. Use EndpointSerializer to
 * obtain the correct EndpointSerializer implementation for a particular endpoint.
 * EndpointSerializer implementation may call other EndpointSerializer implementations to serialize
 * nested endpoints.
 *
 * @see EndpointFactory
 */
public abstract class EndpointSerializer {

    private Log log;

    protected OMFactory fac;

    protected EndpointSerializer() {
        log = LogFactory.getLog(this.getClass());
    }

    /**
     * Core method which is exposed to the external use, and serializes the {@link Endpoint} to the
     * XML format
     *
     * @param endpoint to be serialized
     * @return XML format of the serialized endpoint
     */
    public static OMElement getElementFromEndpoint(Endpoint endpoint) {
        return getEndpointSerializer(endpoint).serializeEndpoint(endpoint);
    }

    /**
     * Serializes the given endpoint implementation to an XML object.
     *
     * @param endpoint Endpoint implementation to be serialized.
     * @return OMElement containing XML configuration.
     */
    protected abstract OMElement serializeEndpoint(Endpoint endpoint);

    /**
     * Serializes the QoS infomation of the endpoint to the XML element
     *
     * @param endpointDefinition specifies the QoS information of the endpoint
     * @param element            to which the QoS information will be serialized
     */
    protected void serializeCommonEndpointProperties(
            EndpointDefinition endpointDefinition, OMElement element) {
    
        if (endpointDefinition.getTraceState() == SynapseConstants.TRACING_ON) {
            element.addAttribute(fac.createOMAttribute(XMLConfigConstants.TRACE_ATTRIB_NAME,
                    null, XMLConfigConstants.TRACE_ENABLE));
        } else if (endpointDefinition.getTraceState() == SynapseConstants.TRACING_OFF) {
            element.addAttribute(fac.createOMAttribute(XMLConfigConstants.TRACE_ATTRIB_NAME,
                    null, XMLConfigConstants.TRACE_DISABLE));
        }

        StatisticsConfigurable statisticsConfigurable =
                endpointDefinition.getAspectConfiguration();

        if (statisticsConfigurable != null &&
                statisticsConfigurable.isStatisticsEnable()) {

            element.addAttribute(fac.createOMAttribute(
                    XMLConfigConstants.STATISTICS_ATTRIB_NAME, null,
                    XMLConfigConstants.STATISTICS_ENABLE));
        }
        
        if (endpointDefinition.isUseSwa()) {
            element.addAttribute(fac.createOMAttribute("optimize", null, "swa"));
        } else if (endpointDefinition.isUseMTOM()) {
            element.addAttribute(fac.createOMAttribute("optimize", null, "mtom"));
        }

        if (endpointDefinition.getCharSetEncoding() != null) {
            element.addAttribute(fac.createOMAttribute(
                    "encoding", null, endpointDefinition.getCharSetEncoding()));
        }

        if (endpointDefinition.isAddressingOn()) {
            OMElement addressing = fac.createOMElement(
                    "enableAddressing", SynapseConstants.SYNAPSE_OMNAMESPACE);

            if (endpointDefinition.getAddressingVersion() != null) {
                addressing.addAttribute(fac.createOMAttribute(
                        "version", null, endpointDefinition.getAddressingVersion()));
            }

            if (endpointDefinition.isUseSeparateListener()) {
                addressing.addAttribute(fac.createOMAttribute("separateListener", null, "true"));
            }
            element.addChild(addressing);
        }

        if (endpointDefinition.isReliableMessagingOn()) {
            OMElement rm = fac.createOMElement("enableRM", SynapseConstants.SYNAPSE_OMNAMESPACE);

            if (endpointDefinition.getWsRMPolicyKey() != null) {
                rm.addAttribute(fac.createOMAttribute(
                        "policy", null, endpointDefinition.getWsRMPolicyKey()));
            }
            element.addChild(rm);
        }

        if (endpointDefinition.isSecurityOn()) {
            OMElement sec = fac.createOMElement("enableSec", SynapseConstants.SYNAPSE_OMNAMESPACE);

            if (endpointDefinition.getWsSecPolicyKey() != null) {
                sec.addAttribute(fac.createOMAttribute(
                        "policy", null, endpointDefinition.getWsSecPolicyKey()));
            } else {
                if (endpointDefinition.getInboundWsSecPolicyKey() != null) {
                    sec.addAttribute(fac.createOMAttribute(
                            "inboundPolicy", null, endpointDefinition.getInboundWsSecPolicyKey()));
                }
                if (endpointDefinition.getOutboundWsSecPolicyKey() != null) {
                    sec.addAttribute(fac.createOMAttribute("outboundPolicy",
                            null, endpointDefinition.getOutboundWsSecPolicyKey()));
                }
            }
            element.addChild(sec);
        }

        if (endpointDefinition.getTimeoutAction() != SynapseConstants.NONE ||
                endpointDefinition.getTimeoutDuration() > 0) {

            OMElement timeout = fac.createOMElement(
                    "timeout", SynapseConstants.SYNAPSE_OMNAMESPACE);
            element.addChild(timeout);

            OMElement duration = fac.createOMElement(
                    "duration", SynapseConstants.SYNAPSE_OMNAMESPACE);
            duration.setText(Long.toString(endpointDefinition.getTimeoutDuration()));
            timeout.addChild(duration);

            OMElement action = fac.createOMElement("action", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpointDefinition.getTimeoutAction() == SynapseConstants.DISCARD) {
                action.setText("discard");
            } else if (endpointDefinition.getTimeoutAction()
                    == SynapseConstants.DISCARD_AND_FAULT) {
                action.setText("fault");
            }
            timeout.addChild(action);
        }

        if (endpointDefinition.getInitialSuspendDuration() != -1 ||
            !endpointDefinition.getSuspendErrorCodes().isEmpty()) {

            OMElement suspendOnFailure = fac.createOMElement(
                org.apache.synapse.config.xml.XMLConfigConstants.SUSPEND_ON_FAILURE,
                SynapseConstants.SYNAPSE_OMNAMESPACE);

            if (!endpointDefinition.getSuspendErrorCodes().isEmpty()) {
                OMElement errorCodes = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.ERROR_CODES,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                errorCodes.setText(endpointDefinition.getSuspendErrorCodes().
                    toString().replaceAll("[\\[\\] ]", ""));
                suspendOnFailure.addChild(errorCodes);
            }

            if (endpointDefinition.getInitialSuspendDuration() != -1) {
                OMElement initialDuration = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.SUSPEND_INITIAL_DURATION,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                initialDuration.setText(Long.toString(endpointDefinition.getInitialSuspendDuration()));
                suspendOnFailure.addChild(initialDuration);
            }

            if (endpointDefinition.getSuspendProgressionFactor() != -1) {
                OMElement progressionFactor = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.SUSPEND_PROGRESSION_FACTOR,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                progressionFactor.setText(Float.toString(endpointDefinition.getSuspendProgressionFactor()));
                suspendOnFailure.addChild(progressionFactor);
            }

            if (endpointDefinition.getSuspendMaximumDuration() != -1 &&
                    endpointDefinition.getSuspendMaximumDuration() != Long.MAX_VALUE) {
                OMElement suspendMaximum = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.SUSPEND_MAXIMUM_DURATION,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                suspendMaximum.setText(Long.toString(endpointDefinition.getSuspendMaximumDuration()));
                suspendOnFailure.addChild(suspendMaximum);
            }

            element.addChild(suspendOnFailure);
        }

        if (endpointDefinition.getRetryDurationOnTimeout() > 0 ||
            !endpointDefinition.getTimeoutErrorCodes().isEmpty()) {

            OMElement markAsTimedout = fac.createOMElement(
                org.apache.synapse.config.xml.XMLConfigConstants.MARK_FOR_SUSPENSION,
                SynapseConstants.SYNAPSE_OMNAMESPACE);

            if (!endpointDefinition.getTimeoutErrorCodes().isEmpty()) {
                OMElement errorCodes = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.ERROR_CODES,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                errorCodes.setText(endpointDefinition.getTimeoutErrorCodes().
                    toString().replaceAll("[\\[\\] ]", ""));
                markAsTimedout.addChild(errorCodes);
            }

            if (endpointDefinition.getRetriesOnTimeoutBeforeSuspend() > 0) {
                OMElement retries = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.RETRIES_BEFORE_SUSPENSION,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                retries.setText(Long.toString(endpointDefinition.getRetriesOnTimeoutBeforeSuspend()));
                markAsTimedout.addChild(retries);
            }

            if (endpointDefinition.getRetryDurationOnTimeout() > 0) {
                OMElement retryDelay = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.RETRY_DELAY,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                retryDelay.setText(Long.toString(endpointDefinition.getRetryDurationOnTimeout()));
                markAsTimedout.addChild(retryDelay);
            }

            element.addChild(markAsTimedout);
        }
    }

    protected void serializeSpecificEndpointProperties(EndpointDefinition endpointDefinition,
        OMElement element) {

        // overridden by the Serializers which has specific serialization
    }


    protected void handleException(String message) {
        log.error(message);
        throw new SynapseException(message);
    }

    /**
     * Returns the EndpointSerializer implementation for the given endpoint. Throws a SynapseException,
     * if there is no serializer for the given endpoint type.
     *
     * @param endpoint Endpoint implementaion.
     * @return EndpointSerializer implementation.
     */
    public static EndpointSerializer getEndpointSerializer(Endpoint endpoint) {

        if (endpoint instanceof AddressEndpoint) {
            return new AddressEndpointSerializer();
        } else if (endpoint instanceof DefaultEndpoint) {
            return new DefaultEndpointSerializer();
        } else if (endpoint instanceof WSDLEndpoint) {
            return new WSDLEndpointSerializer();
        } else if (endpoint instanceof IndirectEndpoint) {
            return new IndirectEndpointSerializer();
        } else if (endpoint instanceof ResolvingEndpoint) {
            return new ResolvingEndpointSerializer();
        } else if (endpoint instanceof SALoadbalanceEndpoint) {
            return new SALoadbalanceEndpointSerializer();
        } else if (endpoint instanceof DynamicLoadbalanceEndpoint){
            return new DynamicLoadbalanceEndpointSerializer();
        } else if (endpoint instanceof LoadbalanceEndpoint) {
            return new LoadbalanceEndpointSerializer();
        } else if (endpoint instanceof FailoverEndpoint) {
            return new FailoverEndpointSerializer();
        }

        throw new SynapseException("Serializer for endpoint " +
                endpoint.getClass().toString() + " is not defined.");
    }
}
