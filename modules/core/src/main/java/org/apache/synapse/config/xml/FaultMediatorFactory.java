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
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.transform.FaultMediator;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Creates a fault mediator instance
 *
 * <pre>
 * &lt;makefault [version="soap11|soap12"]&gt;
 *   &lt;code (value="literal" | expression="xpath")/&gt;
 *   &lt;reason (value="literal" | expression="xpath")&gt;
 *   &lt;node&gt;?
 *   &lt;role&gt;?
 *   &lt;detail&gt;?
 * &lt;/makefault&gt;
 * </pre>
 */
public class FaultMediatorFactory extends AbstractMediatorFactory  {

    private static final QName FAULT_Q = new QName(Constants.SYNAPSE_NAMESPACE, "makefault");

    private static final QName ATT_VERSION_Q = new QName(Constants.NULL_NAMESPACE, "version");
    private static final QName CODE_Q        = new QName(Constants.SYNAPSE_NAMESPACE, "code");
    private static final QName REASON_Q      = new QName(Constants.SYNAPSE_NAMESPACE, "reason");
    private static final QName NODE_Q        = new QName(Constants.SYNAPSE_NAMESPACE, "node");
    private static final QName ROLE_Q        = new QName(Constants.SYNAPSE_NAMESPACE, "role");
    private static final QName DETAIL_Q      = new QName(Constants.SYNAPSE_NAMESPACE, "detail");

    private static final QName ATT_VALUE_Q = new QName(Constants.NULL_NAMESPACE, "value");
    private static final QName ATT_EXPR_Q  = new QName(Constants.NULL_NAMESPACE, "expression");

    private static final String SOAP11 = "soap11";
    private static final String SOAP12 = "soap12";

    private static final Log log = LogFactory.getLog(FaultMediatorFactory.class);

    public Mediator createMediator(OMElement elem) {

        FaultMediator faultMediator = new FaultMediator();

        OMAttribute version = elem.getAttribute(ATT_VERSION_Q);
        if (version != null) {
            if (SOAP11.equals(version.getAttributeValue())) {
                faultMediator.setSoapVersion(FaultMediator.SOAP11);
            } else if (SOAP12.equals(version.getAttributeValue())) {
                faultMediator.setSoapVersion(FaultMediator.SOAP12);
            }else {
                String msg = "Invalid SOAP version";
                log.error(msg);
                throw new SynapseException(msg);
            }
        }

        OMElement code = elem.getFirstChildWithName(CODE_Q);
        if (code != null) {
            OMAttribute value = code.getAttribute(ATT_VALUE_Q);
            OMAttribute expression = code.getAttribute(ATT_EXPR_Q);

            if (value != null) {
                String strValue = value.getAttributeValue();
                String prefix, name;
                if (strValue.indexOf(":") != -1) {
                    prefix = strValue.substring(0, strValue.indexOf(":"));
                    name = strValue.substring(strValue.indexOf(":")+1);
                } else {
                    String msg = "A QName is expected for fault code as prefix:name";
                    log.error(msg);
                    throw new SynapseException(msg);
                }
                faultMediator.setFaultCodeValue(
                    new QName(OMElementUtils.getNameSpaceWithPrefix(prefix, code), name, prefix));
                
            } else if (expression != null) {
                try {
                    AXIOMXPath xp = new AXIOMXPath(expression.getAttributeValue());
                    OMElementUtils.addNameSpaces(xp, code, log);
                    faultMediator.setFaultCodeExpr(xp);
                } catch (JaxenException je) {
                    String msg = "Invalid fault code expression : " + je.getMessage();
                    log.error(msg);
                    throw new SynapseException(msg, je);
                }
            } else {
                String msg = "A 'value' or 'expression' attribute must specify the fault code";
                log.error(msg);
                throw new SynapseException(msg);
            }

        } else {
            String msg = "The fault code is a required attribute for the makefault mediator";
            log.error(msg);
            throw new SynapseException(msg);
        }

        OMElement reason = elem.getFirstChildWithName(REASON_Q);
        if (reason != null) {
            OMAttribute value = reason.getAttribute(ATT_VALUE_Q);
            OMAttribute expression = reason.getAttribute(ATT_EXPR_Q);

            if (value != null) {
                faultMediator.setFaultReasonValue(value.getAttributeValue());
            } else if (expression != null) {
                try {
                    AXIOMXPath xp = new AXIOMXPath(expression.getAttributeValue());
                    OMElementUtils.addNameSpaces(xp, reason, log);
                    faultMediator.setFaultReasonExpr(xp);

                } catch (JaxenException je) {
                    String msg = "Invalid fault reason expression : " + je.getMessage();
                    log.error(msg);
                    throw new SynapseException(msg, je);
                }
            } else {
                String msg = "A 'value' or 'expression' attribute must specify the fault code";
                log.error(msg);
                throw new SynapseException(msg);
            }

        } else {
            String msg = "The fault reason is a required attribute for the makefault mediator";
            log.error(msg);
            throw new SynapseException(msg);
        }

        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        initMediator(faultMediator,elem);

        OMElement node = elem.getFirstChildWithName(NODE_Q);
        if (node != null && node.getText() != null) {
            try {
                faultMediator.setFaultNode(new URI(node.getText()));
            } catch (URISyntaxException e) {
                String msg = "Invalid URI specified for fault node : " + node.getText();
                log.error(msg);
                throw new SynapseException(msg);
            }
        }

        OMElement role = elem.getFirstChildWithName(ROLE_Q);
        if (role != null && role.getText() != null) {
            try {
                faultMediator.setFaultRole(new URI(role.getText()));
            } catch (URISyntaxException e) {
                String msg = "Invalid URI specified for fault role : " + role.getText();
                log.error(msg);
                throw new SynapseException(msg);
            }
        }

        OMElement detail = elem.getFirstChildWithName(DETAIL_Q);
        if (detail != null && detail.getText() != null) {
            faultMediator.setFaultDetail(detail.getText());
        }

        return faultMediator;
    }

    public QName getTagQName() {
        return FAULT_Q;
    }
}
