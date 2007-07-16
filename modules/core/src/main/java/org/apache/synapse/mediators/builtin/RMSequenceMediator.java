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

package org.apache.synapse.mediators.builtin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Collections;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.util.UUIDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.jaxen.JaxenException;

public class RMSequenceMediator extends AbstractMediator {

    private static Log log = LogFactory.getLog(RMSequenceMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);

    private AXIOMXPath correlation = null;
    private AXIOMXPath lastMessage = null;
    private Boolean single = null;
    private String version = null;

    private static final String WSRM_SpecVersion_1_0 = "Spec_2005_02";
    private static final String WSRM_SpecVersion_1_1 = "Spec_2007_02";
    // set sequence expiry time to 5 minutes
    private static final long SEQUENCE_EXPIRY_TIME = 300000;
    private static Map sequenceMap = Collections.synchronizedMap(new HashMap());

    public boolean mediate(MessageContext smc) {
        if (log.isDebugEnabled()) {
            log.debug("RMSequence Mediator  ::  mediate() ");
        }
        boolean shouldTrace = shouldTrace(smc.getTracingState());
        if (shouldTrace) {
            trace.trace("Start : RMSequence mediator");
        }
        if (!(smc instanceof Axis2MessageContext)) {
            if (log.isDebugEnabled()) {
                log.debug("RMSequence Mediator  ::  only axis2 message context is supported ");
            }
            return true;
        }
        Axis2MessageContext axis2MessageCtx = (Axis2MessageContext) smc;
        org.apache.axis2.context.MessageContext orgMessageCtx =
            axis2MessageCtx.getAxis2MessageContext();

        cleanupSequenceMap();

        String version = getVersionValue();
        orgMessageCtx.getOptions().setProperty(
            Constants.SANDESHA_SPEC_VERSION, version);
        if (log.isDebugEnabled()) {
            log.debug("using WS-RM version " + version);
        }

        if (isSingle()) {
            String sequenceID = UUIDGenerator.getUUID();
            orgMessageCtx.getOptions().setProperty(
                Constants.SANDESHA_SEQUENCE_KEY, sequenceID);
            orgMessageCtx.getOptions().setProperty(
                SandeshaClientConstants.OFFERED_SEQUENCE_ID, UUIDGenerator.getUUID());
            orgMessageCtx.getOptions().setProperty(
                Constants.SANDESHA_LAST_MESSAGE, "true");
            return true;
        }

        String correlationValue = getCorrelationValue(smc);
        if (log.isDebugEnabled()) {
            log.debug("correlation value is " + correlationValue);
        }

        boolean lastMessage = isLastMessage(smc);
        if (log.isDebugEnabled()) {
            log.debug("Is this message the last message in sequence: " + lastMessage);
        }

        if (!sequenceMap.containsKey(correlationValue)) {
            orgMessageCtx.getOptions().setProperty(
                SandeshaClientConstants.OFFERED_SEQUENCE_ID, UUIDGenerator.getUUID());       
        }

        String sequenceID = retrieveSequenceID(correlationValue);
        orgMessageCtx.getOptions().setProperty(
            Constants.SANDESHA_SEQUENCE_KEY, sequenceID);
        if (log.isDebugEnabled()) {
            log.debug("RMSequence Mediator  ::  using sequence " + sequenceID);
        }

        if (lastMessage) {
            orgMessageCtx.getOptions().setProperty(
                Constants.SANDESHA_LAST_MESSAGE, "true");
            sequenceMap.remove(correlationValue);
        }

        if (shouldTrace) {
            trace.trace("End : RMSequence mediator");
        }
        return true;
    }

    private String retrieveSequenceID(String correlationValue) {
        String sequenceID = null;
        if (!sequenceMap.containsKey(correlationValue)) {
            sequenceID = UUIDGenerator.getUUID();
            if (log.isDebugEnabled()) {
                log.debug("setting sequenceID " + sequenceID + " for correlation " + correlationValue);
            }
            Entry sequenceEntry = new Entry();
            sequenceEntry.setValue(sequenceID);
            sequenceEntry.setExpiryTime(System.currentTimeMillis() + SEQUENCE_EXPIRY_TIME);
            sequenceMap.put(correlationValue, sequenceEntry);
        } else {
            sequenceID = (String) ((Entry) sequenceMap.get(correlationValue)).getValue();
            if (log.isDebugEnabled()) {
                log.debug("got sequenceID " + sequenceID + " for correlation " + correlationValue);
            }
        }
        return sequenceID;
    }

    private String getCorrelationValue(MessageContext smc) {
        OMElement node = null;
        try {
            node = (OMElement) getCorrelation().selectSingleNode(smc.getEnvelope());
        } catch (JaxenException e) {
            log.error("XPath error : " + e.getMessage());
            throw new SynapseException("XPath error : " + e.getMessage());
        }
        if (node == null) {
            if (log.isDebugEnabled()) {
                log.debug("XPath expression did not return any node");
            }
            throw new SynapseException("XPath expression did not return any node");
        }
        return node.getText();
    }

    private String getVersionValue() {
        if (Constants.SEQUENCE_VERSION_1_1.equals(getVersion())) {
            return WSRM_SpecVersion_1_1;
        } else {
            return WSRM_SpecVersion_1_0;
        }
    }

    private boolean isLastMessage(MessageContext smc) {
        if (getLastMessage() == null) {
            return false;
        }
        try {
            return getLastMessage().booleanValueOf(smc.getEnvelope());
        } catch (JaxenException e) {
            log.error("XPath error : " + e.getMessage());
            throw new SynapseException("XPath error : " + e.getMessage());
        }
    }

    private synchronized void cleanupSequenceMap() {
        Iterator itKey = sequenceMap.keySet().iterator();
        while (itKey.hasNext()) {
            Object key = itKey.next();
            Entry sequenceEntry = (Entry) sequenceMap.get(key);
            if (sequenceEntry.isExpired()) {
                sequenceMap.remove(key);
            }
        }
    }

    public boolean isSingle() {
        if (getSingle() != null && getSingle().booleanValue()) {
            return true;

        } else {
            return false;
        }
    }

    public AXIOMXPath getCorrelation() {
        return correlation;
    }

    public void setCorrelation(AXIOMXPath correlation) {
        this.correlation = correlation;
    }

    public AXIOMXPath getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(AXIOMXPath lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Boolean getSingle() {
        return single;
    }

    public void setSingle(Boolean single) {
        this.single = single;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
