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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.base.SequenceMediator;

/**
 * <pre>
 * &lt;sequence name="string" [onError="string"]&gt;
 *   mediator+
 * &lt;/sequence&gt;
 * </pre>
 *
 * OR
 *
 * <pre>
 * &lt;sequence ref="name"/&gt;
 * </pre>
 */
public class SequenceMediatorSerializer extends AbstractListMediatorSerializer
     {

    private static final Log log = LogFactory.getLog(SequenceMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof SequenceMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        SequenceMediator mediator = (SequenceMediator) m;
        OMElement sequence = fac.createOMElement("sequence", synNS);

        if (mediator.getRef() != null) {
            sequence.addAttribute(fac.createOMAttribute(
                "ref", nullNS, mediator.getRef()));
        } else if (mediator.getName() != null) {
            sequence.addAttribute(fac.createOMAttribute(
                "name", nullNS, mediator.getName()));

            if (mediator.getErrorHandler() != null) {
                sequence.addAttribute(fac.createOMAttribute(
                    "onError", nullNS, mediator.getErrorHandler()));
            }
            finalizeSerialization(sequence,mediator);            
            super.serializeChildren(sequence, mediator.getList());
        }

        if (parent != null) {
            parent.addChild(sequence);
        }
        return sequence;
    }

    public String getMediatorClassName() {
        return SequenceMediator.class.getName();
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
