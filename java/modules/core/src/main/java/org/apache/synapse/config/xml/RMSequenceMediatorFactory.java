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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.builtin.RMSequenceMediator;
import org.jaxen.JaxenException;

/**
 * Creates a RMSequence mediator through the supplied XML configuration
 * <p/>
 * <pre>
 * &lt;RMSequence (correlation="xpath" [last-message="xpath"]) | single="true" [version="1.0|1.1"]/&gt;
 * </pre>
 */
public class RMSequenceMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(LogMediatorFactory.class);

    private static final QName SEQUENCE_Q = new QName(Constants.SYNAPSE_NAMESPACE, "RMSequence");

    public Mediator createMediator(OMElement elem) {

        RMSequenceMediator sequenceMediator = new RMSequenceMediator();
        OMAttribute correlation =
            elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "correlation"));
        OMAttribute lastMessage =
            elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "last-message"));
        OMAttribute single = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "single"));
        OMAttribute version = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "version"));

        if (single == null && correlation == null) {
            String msg = "The 'single' attribute value of true or a 'correlation' attribute is " +
                "required for the configuration of a RMSequence mediator";
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (correlation != null) {
            if (correlation.getAttributeValue() != null &&
                correlation.getAttributeValue().trim().length() == 0) {
                String msg = "Invalid attribute value specified for correlation";
                log.error(msg);
                throw new SynapseException(msg);

            } else {
                try {
                    sequenceMediator.setCorrelation(new AXIOMXPath(correlation.getAttributeValue()));
                } catch (JaxenException e) {
                    String msg = "Invalid XPath expression for attribute correlation : "
                        + correlation.getAttributeValue();
                    log.error(msg);
                    throw new SynapseException(msg);
                }
            }
            OMElementUtils.addNameSpaces(sequenceMediator.getCorrelation(), elem, log);
        }

        if (single != null) {
            sequenceMediator.setSingle(Boolean.valueOf(single.getAttributeValue()));
        }

        if (sequenceMediator.isSingle() && sequenceMediator.getCorrelation() != null) {
            String msg = "Invalid RMSequence mediator. A RMSequence can't have both a "
                + "single attribute value of true and a correlation attribute specified.";
            log.error(msg);
            throw new SynapseException(msg);

        } else if (!sequenceMediator.isSingle() && sequenceMediator.getCorrelation() == null) {
            String msg = "Invalid RMSequence mediator. A RMSequence must have a "
                + "single attribute value of true or a correlation attribute specified.";
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (lastMessage != null) {
            if (lastMessage.getAttributeValue() != null &&
                lastMessage.getAttributeValue().trim().length() == 0) {
                String msg = "Invalid attribute value specified for last-message";
                log.error(msg);
                throw new SynapseException(msg);

            } else {
                try {
                    sequenceMediator.setLastMessage(new AXIOMXPath(lastMessage.getAttributeValue()));
                } catch (JaxenException e) {
                    String msg = "Invalid XPath expression for attribute last-message : "
                        + lastMessage.getAttributeValue();
                    log.error(msg);
                    throw new SynapseException(msg);
                }
            }
            OMElementUtils.addNameSpaces(sequenceMediator.getLastMessage(), elem, log);
        }

        if (sequenceMediator.isSingle() && sequenceMediator.getLastMessage() != null) {
            String msg = "Invalid RMSequence mediator. A RMSequence can't have both a "
                + "single attribute value of true and a last-message attribute specified.";
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (version != null) {
            if (!Constants.SEQUENCE_VERSION_1_0.equals(version.getAttributeValue()) &&
                !Constants.SEQUENCE_VERSION_1_1.equals(version.getAttributeValue())) {
                String msg = "Only '" + Constants.SEQUENCE_VERSION_1_0 + "' or '" +
                    Constants.SEQUENCE_VERSION_1_1
                    + "' values are allowed for attribute version for a RMSequence mediator"
                    + ", Unsupported version " + version.getAttributeValue();
                log.error(msg);
                throw new SynapseException(msg);
            }
            sequenceMediator.setVersion(version.getAttributeValue());
        }

        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        initMediator(sequenceMediator, elem);

        return sequenceMediator;
    }

    public QName getTagQName() {
        return SEQUENCE_Q;
    }
}
