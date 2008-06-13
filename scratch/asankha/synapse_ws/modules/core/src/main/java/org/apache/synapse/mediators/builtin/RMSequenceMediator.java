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
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.jaxen.JaxenException;

public class RMSequenceMediator extends AbstractMediator {

    private AXIOMXPath correlation = null;
    private AXIOMXPath lastMessage = null;
    private Boolean single = null;
    private String version = null;

    private static final String WSRM_SpecVersion_1_0 = "Spec_2005_02";
    private static final String WSRM_SpecVersion_1_1 = "Spec_2007_02";
    // set sequence expiry time to 5 minutes
    private static final long SEQUENCE_EXPIRY_TIME = 300000;
    private static Map sequenceMap = Collections.synchronizedMap(new HashMap());

    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : RMSequence mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        if (!(synCtx instanceof Axis2MessageContext)) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Only axis2 message contexts are supported");
            }

        } else {
            Axis2MessageContext axis2MessageCtx = (Axis2MessageContext) synCtx;
            org.apache.axis2.context.MessageContext orgMessageCtx =
                axis2MessageCtx.getAxis2MessageContext();

            cleanupSequenceMap();

            String version = getVersionValue();
            orgMessageCtx.getOptions().setProperty(
                SynapseConstants.SANDESHA_SPEC_VERSION, version);

            if (isSingle()) {
                String sequenceID = UUIDGenerator.getUUID();
                String offeredSeqID = UUIDGenerator.getUUID();

                orgMessageCtx.getOptions().setProperty(
                    SynapseConstants.SANDESHA_SEQUENCE_KEY, sequenceID);
                orgMessageCtx.getOptions().setProperty(
                    SandeshaClientConstants.OFFERED_SEQUENCE_ID, offeredSeqID);
                orgMessageCtx.getOptions().setProperty(
                    SynapseConstants.SANDESHA_LAST_MESSAGE, "true");

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Using WS-RM version " + version +
                        " and a single message sequence : " + sequenceID +
                        " and offering sequence : " + offeredSeqID);
                }

            } else {

                String correlationValue = getCorrelationValue(synCtx);
                boolean lastMessage = isLastMessage(synCtx);
                String offeredSeqID = null;

                if (!sequenceMap.containsKey(correlationValue)) {
                    offeredSeqID = UUIDGenerator.getUUID();
                    orgMessageCtx.getOptions().setProperty(
                        SandeshaClientConstants.OFFERED_SEQUENCE_ID, offeredSeqID);
                }

                String sequenceID = retrieveSequenceID(correlationValue);
                orgMessageCtx.getOptions().setProperty(
                    SynapseConstants.SANDESHA_SEQUENCE_KEY, sequenceID);

                if (lastMessage) {
                    orgMessageCtx.getOptions().setProperty(
                        SynapseConstants.SANDESHA_LAST_MESSAGE, "true");
                    sequenceMap.remove(correlationValue);
                }

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Correlation value : " + correlationValue +
                        " last message = " + lastMessage + " using sequence : " + sequenceID +
                        (offeredSeqID != null ? " offering sequence : " + offeredSeqID : ""));
                }
            }
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : RMSequence mediator");
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

            if (node != null) {
                return node.getText();
            } else {
                handleException("XPath expression : " + getCorrelation() +
                    " did not return any node", smc);
            }

        } catch (JaxenException e) {
            handleException("Error evaluating XPath expression to determine correlation : " +
                getCorrelation(), e, smc);
        }
        return null; // never called
    }

    private String getVersionValue() {
        if (XMLConfigConstants.SEQUENCE_VERSION_1_1.equals(getVersion())) {
            return WSRM_SpecVersion_1_1;
        } else {
            return WSRM_SpecVersion_1_0;
        }
    }

    private boolean isLastMessage(MessageContext smc) {
        if (getLastMessage() == null) {
            return false;
        } else {
            try {
                return getLastMessage().booleanValueOf(smc.getEnvelope());
            } catch (JaxenException e) {
                handleException("Error evaluating XPath expression to determine if last message : " +
                    getLastMessage(), e, smc);
            }
            return false;
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
