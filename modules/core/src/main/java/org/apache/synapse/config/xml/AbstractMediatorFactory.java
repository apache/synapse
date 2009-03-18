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
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.AspectConfigurable;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parent class for all the {@link MediatorFactory} implementations
 */
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
     * This is to Initialize the mediator with the default attributes.
     * 
     * @deprecated This method is deprecated. As of Synapse 1.3, please use
     *             {@link #processAuditStatus(Mediator, OMElement)}
     *
     * @param mediator of which trace state has to be set
     * @param mediatorOmElement from which the trace state is extracted
     */
    @Deprecated
    protected void processTraceState(Mediator mediator, OMElement mediatorOmElement) {
        processAuditStatus(mediator, mediatorOmElement);
    }
    
    /**
     * This is to Initialize the mediator regarding tracing and statistics.
     *
     * @param mediator of which trace state has to be set
     * @param mediatorOmElement from which the trace state is extracted
     * 
     * @since 1.3
     */
    protected void processAuditStatus(Mediator mediator, OMElement mediatorOmElement) {

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

        OMAttribute statistics = mediatorOmElement.getAttribute(ATT_STATS);
        if (statistics != null) {
            String statisticsValue = statistics.getAttributeValue();
            if (statisticsValue != null) {
                if (mediator instanceof AspectConfigurable) {
                    if (XMLConfigConstants.STATISTICS_ENABLE.equals(statisticsValue)) {
                        ((AspectConfigurable) mediator).enableStatistics();
                    }
                }
            }
        }
    }

    /**
     * Collect the <tt>name</tt> and <tt>value</tt> attributes from the children
     * with a given QName.
     *  
     * @return
     */
    protected Map<String,String> collectNameValuePairs(OMElement elem, QName childElementName) {
        Map<String,String> result = new LinkedHashMap<String,String>();
        for (Iterator it = elem.getChildrenWithName(childElementName); it.hasNext(); ) {
            OMElement child = (OMElement)it.next();
            OMAttribute attName = child.getAttribute(ATT_NAME);
            OMAttribute attValue = child.getAttribute(ATT_VALUE);
            if (attName != null && attValue != null) {
                String name = attName.getAttributeValue().trim();
                String value = attValue.getAttributeValue().trim();
                if (result.containsKey(name)) {
                    handleException("Duplicate " + childElementName.getLocalPart()
                            + " with name " + name);
                } else {
                    result.put(name, value);
                }
            } else {
                handleException("Both of the name and value attributes are required for a "
                        + childElementName.getLocalPart());
            }
        }
        return result;
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
