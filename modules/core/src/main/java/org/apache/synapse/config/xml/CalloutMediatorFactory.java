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
import org.apache.axis2.util.JavaUtils;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.endpoints.EndpointFactory;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.builtin.CalloutMediator;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.Properties;

/**
 * Factory for {@link CalloutMediator} instances.
 * 
 * <pre>
 * &lt;callout [serviceURL="string"] [action="string"][passHeaders="true|false"] [initAxis2ClientOptions="true|false"]&gt;
 *      &lt;configuration [axis2xml="string"] [repository="string"]/&gt;?
 *      &lt;endpoint/&gt;?
 *      &lt;source xpath="expression" | key="string"&gt;?
 *      &lt;target xpath="expression" | key="string"/&gt;?
 *      &lt;enableSec policy="string" | outboundPolicy="String" | inboundPolicy="String" /&gt;?
 * &lt;/callout&gt;
 * </pre>
 */
public class CalloutMediatorFactory extends AbstractMediatorFactory {

    private static final QName TAG_NAME
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "callout");
    private static final QName ATT_URL = new QName("serviceURL");
    private static final QName ATT_ACTION = new QName("action");
    private static final QName ATT_AXIS2XML = new QName("axis2xml");
    private static final QName ATT_REPOSITORY = new QName("repository");
    private static final QName ATT_PASS_HEADERS = new QName("passHeaders");
    private static final QName ATT_INIT_AXI2_CLIENT_OPTIONS = new QName("initAxis2ClientOptions");
    private static final QName Q_CONFIG
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "configuration");
    private static final QName Q_SOURCE
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "source");
    private static final QName Q_TARGET
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "target");
    private static final QName Q_SEC
                = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "enableSec");
    private static final QName ATT_POLICY
            = new QName(XMLConfigConstants.NULL_NAMESPACE, "policy");
    private static final QName ATT_OUTBOUND_SEC_POLICY
                = new QName(XMLConfigConstants.NULL_NAMESPACE, "outboundPolicy");
    private static final QName ATT_INBOUND_SEC_POLICY
                = new QName(XMLConfigConstants.NULL_NAMESPACE, "inboundPolicy");
    private static final QName Q_ENDPOINT = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "endpoint");

    public Mediator createSpecificMediator(OMElement elem, Properties properties) {

        CalloutMediator callout = new CalloutMediator();

        OMAttribute attServiceURL = elem.getAttribute(ATT_URL);
        OMAttribute attAction     = elem.getAttribute(ATT_ACTION);
        OMAttribute attPassHeaders = elem.getAttribute(ATT_PASS_HEADERS);
        OMAttribute attInitClientOptions = elem.getAttribute(ATT_INIT_AXI2_CLIENT_OPTIONS);
        OMElement epElement = elem.getFirstChildWithName(Q_ENDPOINT);
        OMElement   configElt     = elem.getFirstChildWithName(Q_CONFIG);
        OMElement   sourceElt     = elem.getFirstChildWithName(Q_SOURCE);
        OMElement   targetElt     = elem.getFirstChildWithName(Q_TARGET);
        OMElement wsSec = elem.getFirstChildWithName(Q_SEC);

        if (attServiceURL != null) {
            callout.setServiceURL(attServiceURL.getAttributeValue());
        }

        if (epElement != null) {
            Endpoint endpoint = EndpointFactory.getEndpointFromElement(epElement, true, properties);
            if (endpoint != null) {
                if (endpoint instanceof AbstractEndpoint &&
                        ((AbstractEndpoint) endpoint).isLeafEndpoint()) {
                    callout.setEndpoint(endpoint);
                } else {
                    handleException("Callout mediator only supports leaf endpoints");
                }
            }
        }

        if (attAction != null) {
            callout.setAction(attAction.getAttributeValue());
        }

        if (attPassHeaders != null &&
                JavaUtils.isTrueExplicitly(attPassHeaders.getAttributeValue())) {
            callout.setPassHeaders(true);
        }

        if (attInitClientOptions != null &&
                JavaUtils.isFalseExplicitly(attInitClientOptions.getAttributeValue())) {
            callout.setInitClientOptions(false);
        }

        if (configElt != null) {

            OMAttribute axis2xmlAttr = configElt.getAttribute(ATT_AXIS2XML);
            OMAttribute repoAttr = configElt.getAttribute(ATT_REPOSITORY);

            if (axis2xmlAttr != null && axis2xmlAttr.getAttributeValue() != null) {
                File axis2xml = new File(axis2xmlAttr.getAttributeValue());
                if (axis2xml.exists() && axis2xml.isFile()) {
                    callout.setAxis2xml(axis2xmlAttr.getAttributeValue());
                } else {
                    handleException("Invalid axis2.xml path: " + axis2xmlAttr.getAttributeValue());
                }
            }

            if (repoAttr != null && repoAttr.getAttributeValue() != null) {
                File repo = new File(repoAttr.getAttributeValue());
                if (repo.exists() && repo.isDirectory()) {
                    callout.setClientRepository(repoAttr.getAttributeValue());
                } else {
                    handleException("Invalid repository path: " + repoAttr.getAttributeValue());
                }
            }
        }

        if (sourceElt != null) {
            if (sourceElt.getAttribute(ATT_XPATH) != null) {
                try {
                    callout.setRequestXPath(
                        SynapseXPathFactory.getSynapseXPath(sourceElt, ATT_XPATH));
                } catch (JaxenException e) {
                    handleException("Invalid source XPath : "
                        + sourceElt.getAttributeValue(ATT_XPATH));
                }
            } else if (sourceElt.getAttribute(ATT_KEY) != null) {
                callout.setRequestKey(sourceElt.getAttributeValue(ATT_KEY));
            } else {
                handleException("A 'xpath' or 'key' attribute " +
                    "is required for the Callout 'source'");
            }
        }

        if (targetElt != null) {
            if (targetElt.getAttribute(ATT_XPATH) != null) {
                try {
                    callout.setTargetXPath(
                        SynapseXPathFactory.getSynapseXPath(targetElt, ATT_XPATH));
                } catch (JaxenException e) {
                    handleException("Invalid target XPath : "
                        + targetElt.getAttributeValue(ATT_XPATH));
                }
            } else if (targetElt.getAttribute(ATT_KEY) != null) {
                callout.setTargetKey(targetElt.getAttributeValue(ATT_KEY));
            } else {
                handleException("A 'xpath' or 'key' attribute " +
                    "is required for the Callout 'target'");
            }
        }

        if (wsSec != null) {
            callout.setSecurityOn(true);
            OMAttribute policyKey = wsSec.getAttribute(ATT_POLICY);
            OMAttribute outboundPolicyKey = wsSec.getAttribute(ATT_OUTBOUND_SEC_POLICY);
            OMAttribute inboundPolicyKey = wsSec.getAttribute(ATT_INBOUND_SEC_POLICY);
            if (policyKey != null) {
                callout.setWsSecPolicyKey(policyKey.getAttributeValue());
            } else if (outboundPolicyKey != null || inboundPolicyKey != null) {
                if (outboundPolicyKey != null) {
                    callout.setOutboundWsSecPolicyKey(outboundPolicyKey.getAttributeValue());
                }
                if (inboundPolicyKey != null) {
                    callout.setInboundWsSecPolicyKey(inboundPolicyKey.getAttributeValue());
                }
            } else {
                callout.setSecurityOn(false);
                handleException("A policy key is required to enable security");
            }
        }

        return callout;
    }

    public QName getTagQName() {
        return TAG_NAME;
    }
}
