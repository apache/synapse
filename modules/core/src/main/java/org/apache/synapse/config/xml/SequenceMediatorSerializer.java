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
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.base.SequenceMediator;

/**
 * Serializer for {@link SequenceMediator} instances.
 * 
 * @see SequenceMediatorFactory
 */
public class SequenceMediatorSerializer extends AbstractListMediatorSerializer {

    public OMElement serializeAnonymousSequence(OMElement parent, SequenceMediator mediator) {
        OMElement sequence = fac.createOMElement("sequence", synNS);
        int isEnableStatistics = mediator.getStatisticsState();
        String statisticsValue = null;
        if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_ON) {
            statisticsValue = XMLConfigConstants.STATISTICS_ENABLE;
        } else if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_OFF) {
            statisticsValue = XMLConfigConstants.STATISTICS_DISABLE;
        }
        if (statisticsValue != null) {
            sequence.addAttribute(fac.createOMAttribute(
                    XMLConfigConstants.STATISTICS_ATTRIB_NAME, nullNS, statisticsValue));
        }
        if (mediator.getErrorHandler() != null) {
            sequence.addAttribute(fac.createOMAttribute(
                    "onError", nullNS, mediator.getErrorHandler()));
        }
        saveTracingState(sequence, mediator);
        serializeChildren(sequence, mediator.getList());
        if (parent != null) {
            parent.addChild(sequence);
        }
        return sequence;
    }

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof SequenceMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        SequenceMediator mediator = (SequenceMediator) m;
        OMElement sequence = fac.createOMElement("sequence", synNS);

        // is this a dynamic sequence we loaded from a registry? if so we have no work to here
        // except make sure that we refer back to the registry key used when we loaded ourself
        if (mediator.isDynamic()) {
            sequence.addAttribute(fac.createOMAttribute(
                    "name", nullNS, mediator.getName()));
            sequence.addAttribute(fac.createOMAttribute(
                    "key", nullNS, mediator.getRegistryKey()));

        } else {

            int isEnableStatistics = mediator.getStatisticsState();
            String statisticsValue = null;
            if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_ON) {
                statisticsValue = XMLConfigConstants.STATISTICS_ENABLE;
            } else if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_OFF) {
                statisticsValue = XMLConfigConstants.STATISTICS_DISABLE;
            }
            if (statisticsValue != null) {
                sequence.addAttribute(fac.createOMAttribute(
                        XMLConfigConstants.STATISTICS_ATTRIB_NAME, nullNS, statisticsValue));
            }

            if (mediator.getKey() != null) {
                sequence.addAttribute(fac.createOMAttribute(
                        "key", nullNS, mediator.getKey()));
            } else if (mediator.getName() != null) {
                sequence.addAttribute(fac.createOMAttribute(
                        "name", nullNS, mediator.getName()));

                if (mediator.getErrorHandler() != null) {
                    sequence.addAttribute(fac.createOMAttribute(
                            "onError", nullNS, mediator.getErrorHandler()));
                }
                saveTracingState(sequence, mediator);
                serializeChildren(sequence, mediator.getList());
            }
        }

        if (parent != null) {
            parent.addChild(sequence);
        }
        return sequence;
    }

    public String getMediatorClassName() {
        return SequenceMediator.class.getName();
    }
}
