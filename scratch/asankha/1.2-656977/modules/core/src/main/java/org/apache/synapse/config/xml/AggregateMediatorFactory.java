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
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.DropMediator;
import org.apache.synapse.mediators.eip.aggregator.AggregateMediator;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;

/**
 * Factory for {@link AggregateMediator} instances.
 * 
 * <pre>
 * &lt;aggregate&gt;
 *   &lt;correlateOn expression="xpath"/&gt;?
 *   &lt;completeCondition [timeout="time-in-seconds"]&gt;
 *     &lt;messageCount min="int-min" max="int-max"/&gt;?
 *   &lt;/completeCondition&gt;?
 *   &lt;onComplete expression="xpath" [sequence="sequence-ref"]&gt;
 *     (mediator +)?
 *   &lt;/onComplete&gt;
 * &lt;/aggregate&gt;
 * </pre>
 */
public class AggregateMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(AggregateMediatorFactory.class);

    private static final QName AGGREGATE_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "aggregate");
    private static final QName CORELATE_ON_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "correlateOn");
    private static final QName COMPLETE_CONDITION_Q
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "completeCondition");
    private static final QName MESSAGE_COUNT_Q
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "messageCount");
    private static final QName ON_COMPLETE_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "onComplete");
    private static final QName EXPRESSION_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "expression");
    private static final QName TIMEOUT_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "timeout");
    private static final QName MIN_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "min");
    private static final QName MAX_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "max");
    private static final QName SEQUENCE_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "sequence");

    public Mediator createMediator(OMElement elem) {

        AggregateMediator mediator = new AggregateMediator();
        processTraceState(mediator, elem);

        OMElement corelateOn = elem.getFirstChildWithName(CORELATE_ON_Q);
        if (corelateOn != null) {
            OMAttribute corelateExpr = corelateOn.getAttribute(EXPRESSION_Q);
            if (corelateExpr != null) {
                try {
                    mediator.setCorrelateExpression(
                        SynapseXPathFactory.getSynapseXPath(corelateOn, EXPRESSION_Q));
                } catch (JaxenException e) {
                    handleException("Unable to load the corelate XPATH expression", e);
                }
            }
        }

        OMElement completeCond = elem.getFirstChildWithName(COMPLETE_CONDITION_Q);
        if (completeCond != null) {
            OMAttribute completeTimeout = completeCond.getAttribute(TIMEOUT_Q);
            if (completeTimeout != null) {
                mediator.setCompletionTimeoutMillis(
                        Long.parseLong(completeTimeout.getAttributeValue()) * 1000);
            }

            OMElement messageCount = completeCond.getFirstChildWithName(MESSAGE_COUNT_Q);
            if (messageCount != null) {
                OMAttribute min = messageCount.getAttribute(MIN_Q);
                if (min != null) {
                    mediator.setMinMessagesToComplete(Integer.parseInt(min.getAttributeValue()));
                }

                OMAttribute max = messageCount.getAttribute(MAX_Q);
                if (max != null) {
                    mediator.setMaxMessagesToComplete(Integer.parseInt(max.getAttributeValue()));
                }
            }
        }

        OMElement onComplete = elem.getFirstChildWithName(ON_COMPLETE_Q);
        if (onComplete != null) {

            OMAttribute aggregateExpr = onComplete.getAttribute(EXPRESSION_Q);
            if (aggregateExpr != null) {
                try {
                    mediator.setAggregationExpression(
                        SynapseXPathFactory.getSynapseXPath(onComplete, EXPRESSION_Q));
                } catch (JaxenException e) {
                    handleException("Unable to load the aggregating XPATH", e);
                }
            }

            OMAttribute onCompleteSequence = onComplete.getAttribute(SEQUENCE_Q);
            if (onCompleteSequence != null) {
                mediator.setOnCompleteSequenceRef(onCompleteSequence.getAttributeValue());
            } else if (onComplete.getFirstElement() != null) {
                mediator.setOnCompleteSequence(
                        (new SequenceMediatorFactory()).createAnonymousSequence(onComplete));
            } else {
                SequenceMediator sequence = new SequenceMediator();
                sequence.addChild(new DropMediator());
                mediator.setOnCompleteSequence(sequence);
            }
        }
        return mediator;
    }

    public QName getTagQName() {
        return AGGREGATE_Q;
    }
}
