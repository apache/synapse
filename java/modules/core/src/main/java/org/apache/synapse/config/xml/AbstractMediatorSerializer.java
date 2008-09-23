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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.statistics.AuditConfigurable;
import org.apache.synapse.mediators.MediatorProperty;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * Parent class for all the {@link MediatorSerializer} implementations
 */
public abstract class AbstractMediatorSerializer implements MediatorSerializer {

    /** the standard log for mediators, will assign the logger for the actual subclass */
    protected static Log log;

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS
            = fac.createOMNamespace(XMLConfigConstants.SYNAPSE_NAMESPACE, "syn");
    protected static final OMNamespace nullNS
            = fac.createOMNamespace(XMLConfigConstants.NULL_NAMESPACE, "");
    protected static final QName PROP_Q
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "property", "syn");

    /**
     * A constructor that makes subclasses pick up the correct logger
     */
    protected AbstractMediatorSerializer() {
        log = LogFactory.getLog(this.getClass());
    }

    /**
     * Perform common functions and finalize the mediator serialization.
     * i.e. process any common attributes
     *
     * @param mediatorOmElement the OMElement being created
     * @param mediator the Mediator instance being serialized
     */
    protected static void saveTracingState(OMElement mediatorOmElement, Mediator mediator) {
        int traceState = mediator.getTraceState();
        String traceValue = null;
        if (traceState == org.apache.synapse.SynapseConstants.TRACING_ON) {
            traceValue = XMLConfigConstants.TRACE_ENABLE;
        } else if (traceState == org.apache.synapse.SynapseConstants.TRACING_OFF) {
            traceValue = XMLConfigConstants.TRACE_DISABLE;
        }
        if (traceValue != null) {
            mediatorOmElement.addAttribute(fac.createOMAttribute(
                XMLConfigConstants.TRACE_ATTRIB_NAME, nullNS, traceValue));
        }
        
        if (mediator instanceof AuditConfigurable) {
            if (((AuditConfigurable) mediator).isStatisticsEnable()) {
                mediatorOmElement.addAttribute(fac.createOMAttribute(
                        XMLConfigConstants.STATISTICS_ATTRIB_NAME, nullNS,
                        XMLConfigConstants.STATISTICS_ENABLE));
            }
        }

    }

    protected void serializeMediatorProperties(OMElement parent,
                                               Collection<MediatorProperty> props,
                                               QName childElementName) {

        for (MediatorProperty mp : props) {
            OMElement prop = fac.createOMElement(childElementName, parent);
            if (mp.getName() != null) {
                prop.addAttribute(fac.createOMAttribute("name", nullNS, mp.getName()));
            } else {
                handleException("Mediator property name missing");
            }

            if (mp.getValue() != null) {
                prop.addAttribute(fac.createOMAttribute("value", nullNS, mp.getValue()));

            } else if (mp.getExpression() != null) {
                SynapseXPathSerializer.serializeXPath(mp.getExpression(), prop, "expression");

            } else {
                handleException("Mediator property must have a literal value or be an expression");
            }
        }
    }
    
    protected void serializeMediatorProperties(OMElement parent,
            Collection<MediatorProperty> props) {
        
        serializeMediatorProperties(parent, props, PROP_Q);
    }

    protected void serializeProperties(OMElement parent, Collection<MediatorProperty> props) {
        serializeMediatorProperties(parent, props);
    }

    protected void serializeNamespaces(OMElement elem, AXIOMXPath xpath) {
        for (Object obj : xpath.getNamespaces().keySet()) {
            String prefix = (String) obj;
            String uri = xpath.getNamespaceContext().translateNamespacePrefixToUri(prefix);
            if (!XMLConfigConstants.SYNAPSE_NAMESPACE.equals(uri)) {
                elem.declareNamespace(uri, prefix);
            }
        }
    }

    protected void handleException(String msg) {
        LogFactory.getLog(this.getClass()).error(msg);
        throw new SynapseException(msg);
    }

    protected void handleException(String msg, Exception e) {
        LogFactory.getLog(this.getClass()).error(msg, e);
        throw new SynapseException(msg, e);
    }
}
