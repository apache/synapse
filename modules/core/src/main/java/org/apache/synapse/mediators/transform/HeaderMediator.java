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

package org.apache.synapse.mediators.transform;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.xpath.SynapseXPath;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The header mediator is able to set a given value as a SOAP header, or remove a given
 * header from the current message instance. This supports the headers currently
 * supported by the HeaderType class. If an expression is supplied, its runtime value
 * is evaluated using the current message. Unless the action is set to remove, the
 * default behavior of this mediator is to set a header value.
 */
public class HeaderMediator extends AbstractMediator {

    public static final int ACTION_SET = 0;
    public static final int ACTION_REMOVE = 1;

    /** The qName of the header @see HeaderType */
    private QName qName = null;
    /** The literal value to be set as the header (if one was specified) */
    private String value = null;
    /** Set the header (ACTION_SET) or remove it (ACTION_REMOVE). Defaults to ACTION_SET */
    private int action = ACTION_SET;
    /** Optional embedded XML content of the header element */
    private List<OMElement> embeddedXmlContent = new ArrayList<OMElement>();
    /** An expression which should be evaluated, and the result set as the header value */
    private SynapseXPath expression = null;
    /** The scope that decides which header should be updated: SOAP or Transport. */
    private String scope = null;

    /**
     * Sets/Removes a SOAP header on the current message
     *
     * @param synCtx the current message which is altered as necessary
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Header mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        if (XMLConfigConstants.HEADER_SCOPE_TRANSPORT.equals(scope)) {
            alterTransportHeader(synCtx);
        } else {
            alterSOAPOrKnownHeader(synCtx);
        }

        synLog.traceOrDebug("End : Header mediator");
        return true;
    }

    private void alterSOAPOrKnownHeader(MessageContext synCtx) {

        SynapseLog synLog = getLog(synCtx);

        if (action == ACTION_SET) {

            String value = (getExpression() == null ? getValue() : expression.stringValueOf(synCtx));

            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Setting header : " + qName + " to : " + value);
            }

            if (!isImplicit() &&
                (qName.getNamespaceURI() == null || "".equals(qName.getNamespaceURI()))) {

                // is this a "well known" Synapse header?
                if (SynapseConstants.HEADER_TO.equals(qName.getLocalPart())) {
                    synCtx.setTo(new EndpointReference(value));
                } else if (SynapseConstants.HEADER_FROM.equals(qName.getLocalPart())) {
                    synCtx.setFrom(new EndpointReference(value));
                } else if (SynapseConstants.HEADER_ACTION.equals(qName.getLocalPart())) {
                    synCtx.setWSAAction(value);
                } else if (SynapseConstants.HEADER_FAULT.equals(qName.getLocalPart())) {
                    synCtx.setFaultTo(new EndpointReference(value));
                } else if (SynapseConstants.HEADER_REPLY_TO.equals(qName.getLocalPart())) {
                    synCtx.setReplyTo(new EndpointReference(value));
                } else if (SynapseConstants.HEADER_RELATES_TO.equals(qName.getLocalPart())) {
                    synCtx.setRelatesTo(new RelatesTo[] { new RelatesTo(value) });
                } else {
                    addCustomHeader(synCtx, value);
                }
            } else {
                addCustomHeader(synCtx, value);
            }

        } else {

            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Removing Header : " + qName);
            }

            if (qName.getNamespaceURI() == null || "".equals(qName.getNamespaceURI())) {

                // is this a "well known" Synapse header?
                if (SynapseConstants.HEADER_TO.equals(qName.getLocalPart())) {
                    synCtx.setTo(null);
                } else if (SynapseConstants.HEADER_FROM.equals(qName.getLocalPart())) {
                    synCtx.setFrom(null);
                } else if (SynapseConstants.HEADER_ACTION.equals(qName.getLocalPart())) {
                    synCtx.setWSAAction(null);
                } else if (SynapseConstants.HEADER_FAULT.equals(qName.getLocalPart())) {
                    synCtx.setFaultTo(null);
                } else if (SynapseConstants.HEADER_REPLY_TO.equals(qName.getLocalPart())) {
                    synCtx.setReplyTo(null);
                } else if (SynapseConstants.HEADER_RELATES_TO.equals(qName.getLocalPart())) {
                    synCtx.setRelatesTo(null);
                } else {
                    SOAPEnvelope envelope = synCtx.getEnvelope();
                    if (envelope != null) {
                        SOAPHeader header = envelope.getHeader();
                        if (header != null) {
                            removeFromHeaderList(header.
                                getHeaderBlocksWithNSURI(""));
                        }
                    }
                }

            } else {
                SOAPEnvelope envelope = synCtx.getEnvelope();
                if (envelope != null) {
                    SOAPHeader header = envelope.getHeader();
                    if (header != null) {
                        removeFromHeaderList(header.
                            getHeaderBlocksWithNSURI(qName.getNamespaceURI()));
                    }
                }
            }
        }
    }

    private void alterTransportHeader(MessageContext synCtx) {

        SynapseLog synLog = getLog(synCtx);
        String headerName = qName.getLocalPart();

        String value = (getExpression() == null ? getValue() : expression.stringValueOf(synCtx));

        if (action == ACTION_SET) {

            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Setting Transport header : " + headerName + " to : " + value);
            }

            //Setting Transport Headers
            Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
            org.apache.axis2.context.MessageContext axis2MessageCtx =
                    axis2smc.getAxis2MessageContext();
            Object headers = axis2MessageCtx.
                    getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

            if (headers != null && headers instanceof Map) {
                Map headersMap = (Map) headers;
                headersMap.put(headerName, value);
            } else if (headers == null) {
                Map headersMap = new HashMap();
                headersMap.put(headerName, value);
                axis2MessageCtx.setProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
            }
        } else {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Removing Transport Header : " + qName);
            }

            // Removing transport headers
            Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
            org.apache.axis2.context.MessageContext axis2MessageCtx =
                    axis2smc.getAxis2MessageContext();
            Object headers = axis2MessageCtx.getProperty(
                    org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers != null && headers instanceof Map) {
                Map headersMap = (Map) headers;
                headersMap.remove(headerName);
            } else {
                synLog.traceOrDebug("No transport headers found");
            }
        }
    }

    private void addCustomHeader(MessageContext synCtx, String value) {
        SOAPEnvelope env = synCtx.getEnvelope();
        if (env == null) {
            return;
        }
        SOAPFactory fac = (SOAPFactory) env.getOMFactory();
        SOAPHeader header = env.getOrCreateHeader();
        if (!isImplicit()) {
            SOAPHeaderBlock hb = header.addHeaderBlock(qName.getLocalPart(),
                    fac.createOMNamespace(qName.getNamespaceURI(), qName.getPrefix()));
            hb.setText(value);
        } else if (hasEmbeddedXml()) {
            for (OMElement e : embeddedXmlContent) {
                // Don't attach the original OMElement here.
                // If another mediator down the line, changes the OM, we are in trouble.
                header.addChild(e.cloneOMElement());
            }
        }
    }

    private void removeFromHeaderList(List headersList) {
        if (headersList == null || headersList.isEmpty()) {
            return;
        }
        for (Object o : headersList) {
            if (o instanceof SOAPHeaderBlock) {
                SOAPHeaderBlock header = (SOAPHeaderBlock) o;
                if (header.getLocalName().equals(qName.getLocalPart())) {
                    header.detach();
                }
            } else if (o instanceof OMElement) {
                OMElement omElem = (OMElement) o;
                if (omElem.getLocalName().equals(qName.getLocalPart())) {
                    omElem.detach();
                }
            }
        }
    }

    public List<OMElement> getEmbeddedXml() {
        return embeddedXmlContent;
    }

    public void addEmbeddedXml(OMElement element) {
        if (element != null && !embeddedXmlContent.contains(element)) {
            embeddedXmlContent.add(element);
        }
    }

    public boolean hasEmbeddedXml() {
        return !embeddedXmlContent.isEmpty();
    }

    public boolean isImplicit() {
        return getQName() == null;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public QName getQName() {
        return qName;
    }

    public void setQName(QName qName) {
        this.qName = qName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SynapseXPath getExpression() {
        return expression;
    }

    public void setExpression(SynapseXPath expression) {
        this.expression = expression;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public boolean isContentAware() {
        if (XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)) {
            return false;
        } else {
            return true;
        }
    }
}
