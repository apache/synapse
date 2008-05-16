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

import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;

public abstract class AbstractMediatorFactory implements MediatorFactory {

    /** the standard log for mediators, will assign the logger for the actual subclass */
    protected static Log log;
    protected static final QName ATT_NAME    = new QName("name");
    protected static final QName ATT_VALUE   = new QName("value");
    protected static final QName ATT_XPATH   = new QName("xpath");
    protected static final QName ATT_REGEX   = new QName("regex");
    protected static final QName ATT_SEQUENCE = new QName("sequence");
    protected static final QName ATT_EXPRN   = new QName("expression");
    protected static final QName ATT_KEY     = new QName("key");
    protected static final QName ATT_SOURCE  = new QName("source");
    protected static final QName ATT_TARGET  = new QName("target");
    protected static final QName ATT_ONERROR = new QName("onError");
    protected static final QName ATT_STATS
        = new QName(XMLConfigConstants.STATISTICS_ATTRIB_NAME);
    protected static final QName PROP_Q
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "property");
    protected static final QName FEATURE_Q
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "feature");
    protected static final QName TARGET_Q
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "target");

    /**
     * A constructor that makes subclasses pick up the correct logger
     */
    protected AbstractMediatorFactory() {
        log = LogFactory.getLog(this.getClass());
    }

    /**
     * This is to Initialize the mediator with the default attributes
     *
     * @param mediator
     * @param mediatorOmElement
     */
    protected void processTraceState(Mediator mediator, OMElement mediatorOmElement) {

        OMAttribute trace = mediatorOmElement.getAttribute(
            new QName(XMLConfigConstants.NULL_NAMESPACE, XMLConfigConstants.TRACE_ATTRIB_NAME));

        if (trace != null) {
            String traceValue = trace.getAttributeValue();
            if (traceValue != null) {
                if (traceValue.equals(XMLConfigConstants.TRACE_ENABLE)) {
                    mediator.setTraceState(org.apache.synapse.SynapseConstants.TRACING_ON);
                } else if (traceValue.equals(XMLConfigConstants.TRACE_DISABLE)) {
                    mediator.setTraceState(org.apache.synapse.SynapseConstants.TRACING_OFF);
                }
            }
        }
    }

    protected void handleException(String message, Exception e) {
        LogFactory.getLog(this.getClass()).error(message, e);
        throw new SynapseException(message, e);
    }

    protected void handleException(String message) {
        LogFactory.getLog(this.getClass()).error(message);
        throw new SynapseException(message);
    }
}
