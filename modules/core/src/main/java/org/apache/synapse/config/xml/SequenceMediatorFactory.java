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
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.base.SequenceMediator;

import javax.xml.namespace.QName;

/**
 * Factory for {@link SequenceMediator} instances.
 * <p>
 * It follows the following configuration:
 *
 * <pre>
 * &lt;sequence name="string" [onError="string"] [trace="enable|disable"]&gt;
 *   mediator+
 * &lt;/sequence&gt;
 * </pre>
 *
 * OR
 *
 * <pre>
 * &lt;sequence key="name"/&gt;
 * </pre>
 */
public class SequenceMediatorFactory extends AbstractListMediatorFactory {

    private static final QName SEQUENCE_Q
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "sequence");

    public QName getTagQName() {
        return SEQUENCE_Q;
    }

    public SequenceMediator createAnonymousSequence(OMElement elem) {
        SequenceMediator seqMediator = new SequenceMediator();
        OMAttribute e = elem.getAttribute(ATT_ONERROR);
        if (e != null) {
            seqMediator.setErrorHandler(e.getAttributeValue());
        }
        processTraceState(seqMediator, elem);
        addChildren(elem, seqMediator);
        OMAttribute statistics = elem.getAttribute(ATT_STATS);
        if (statistics != null) {
            String statisticsValue = statistics.getAttributeValue();
            if (statisticsValue != null) {
                if (XMLConfigConstants.STATISTICS_ENABLE.equals(statisticsValue)) {
                    seqMediator.setStatisticsState(
                        org.apache.synapse.SynapseConstants.STATISTICS_ON);
                } else if (XMLConfigConstants.STATISTICS_DISABLE.equals(statisticsValue)) {
                    seqMediator.setStatisticsState(
                        org.apache.synapse.SynapseConstants.STATISTICS_OFF);
                }
            }
        }
        return seqMediator;
    }
    
    public Mediator createMediator(OMElement elem) {

        SequenceMediator seqMediator = new SequenceMediator();

        OMAttribute n = elem.getAttribute(ATT_NAME);
        OMAttribute e = elem.getAttribute(ATT_ONERROR);
        if (n != null) {
            seqMediator.setName(n.getAttributeValue());
            if (e != null) {
                seqMediator.setErrorHandler(e.getAttributeValue());
            }
            processTraceState(seqMediator, elem);
            addChildren(elem, seqMediator);

        } else {
            n = elem.getAttribute(ATT_KEY);
            if (n != null) {
                seqMediator.setKey(n.getAttributeValue());
                if (e != null) {
                    String msg = "A sequence mediator with a reference to another " +
                        "sequence can not have 'ErrorHandler'";
                    log.error(msg);
                    throw new SynapseException(msg);
                }
            } else {
                String msg = "A sequence mediator should be a named sequence or a reference " +
                    "to another sequence (i.e. a name attribute or key attribute is required)";
                log.error(msg);
                throw new SynapseException(msg);
            }
        }

        OMAttribute statistics = elem.getAttribute(ATT_STATS);
        if (statistics != null) {
            String statisticsValue = statistics.getAttributeValue();
            if (statisticsValue != null) {
                if (XMLConfigConstants.STATISTICS_ENABLE.equals(statisticsValue)) {
                    seqMediator.setStatisticsState(
                        org.apache.synapse.SynapseConstants.STATISTICS_ON);
                } else if (XMLConfigConstants.STATISTICS_DISABLE.equals(statisticsValue)) {
                    seqMediator.setStatisticsState(
                        org.apache.synapse.SynapseConstants.STATISTICS_OFF);
                }
            }
        }

        return seqMediator;
    }
}
