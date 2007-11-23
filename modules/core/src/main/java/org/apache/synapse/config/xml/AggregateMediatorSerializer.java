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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.eip.aggregator.AggregateMediator;
import org.apache.synapse.mediators.ext.ClassMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * &lt;aggregate&gt;
 *  &lt;corelateOn expression="XPATH-expression"/&gt;
 *  &lt;completeCondition timeout="time-in-seconds"&gt;
 *   &lt;messageCount min="int-min" max="int-max"/&gt;
 *  &lt;/completeCondition&gt;
 *  &lt;onComplete expression="XPATH-expression" sequence="sequence-ref"&gt;
 *   (mediator +)?
 *  &lt;/onComplete&gt;
 *  &lt;invalidate sequence="sequence-ref" timeout="time-in-seconds"&gt;
 *   (mediator +)?
 *  &lt;/invalidate&gt;
 * &lt;/aggregate&gt;
 */
public class AggregateMediatorSerializer extends AbstractMediatorSerializer {

    private static final Log log = LogFactory.getLog(AggregateMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof AggregateMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }
        AggregateMediator mediator = (AggregateMediator) m;
        OMElement aggregator = fac.createOMElement("aggregate", synNS);
        saveTracingState(aggregator, mediator);

        if (mediator.getCorelateExpression() != null) {
            OMElement corelateOn = fac.createOMElement("corelateOn", synNS);
            corelateOn.addAttribute("expression", mediator.getCorelateExpression().toString(), nullNS);
            super.serializeNamespaces(corelateOn, mediator.getCorelateExpression());
            aggregator.addChild(corelateOn);
        }

        OMElement completeCond = fac.createOMElement("completeCondition", synNS);
        if (mediator.getCompleteTimeout() != 0) {
            completeCond.addAttribute("timeout", Long.toString(mediator.getCompleteTimeout()), nullNS);
        }
        OMElement messageCount = fac.createOMElement("messageCount", synNS);
        if (mediator.getMinMessagesToComplete() != 0) {
            messageCount.addAttribute("min", Integer.toString(mediator.getMinMessagesToComplete()), nullNS);
        }
        if (mediator.getMaxMessagesToComplete() != 0) {
            messageCount.addAttribute("max", Integer.toString(mediator.getMaxMessagesToComplete()), nullNS);
        }
        completeCond.addChild(messageCount);
        aggregator.addChild(completeCond);

        OMElement onCompleteElem = fac.createOMElement("onComplete", synNS);
        if (mediator.getAggregationExpression() != null) {
            onCompleteElem.addAttribute("expression", mediator.getAggregationExpression().toString(), nullNS);
            super.serializeNamespaces(onCompleteElem, mediator.getAggregationExpression());
        }
        if (mediator.getOnCompleteSequenceRef() != null) {
            onCompleteElem.addAttribute("sequence", mediator.getOnCompleteSequenceRef(), nullNS);
        } else if (mediator.getOnCompleteSequence() != null) {
            new SequenceMediatorSerializer().serializeChildren(
                    onCompleteElem, mediator.getOnCompleteSequence().getList());
        }
        aggregator.addChild(onCompleteElem);

        OMElement invalidateElem = fac.createOMElement("invalidate", synNS);
        invalidateElem.addAttribute("timeout", Long.toString(mediator.getInvlidateToDestroyTime()), nullNS);
        if (mediator.getInvalidMsgSequenceRef() != null) {
            invalidateElem.addAttribute("sequence", mediator.getInvalidMsgSequenceRef(), nullNS);
        } else if (mediator.getInvalidMsgSequence() != null) {
            new SequenceMediatorSerializer().serializeChildren(
                    invalidateElem, mediator.getInvalidMsgSequence().getList());
        }
        aggregator.addChild(invalidateElem);

        if (parent != null) {
            parent.addChild(aggregator);
        }

        return aggregator;
    }

    public String getMediatorClassName() {
        return AggregateMediator.class.getName();
    }
}
