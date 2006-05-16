/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.config.xml;

import javax.xml.namespace.QName;

import org.apache.synapse.config.xml.Constants;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.transform.FaultMediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Util;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Creates a fault mediator instance
 *
 * <makefault [version="soap11|soap12"]>
 *   <code (value="literal" | expression="xpath")/>
 *   <reason (value="literal" | expression="xpath")>
 *   <node>?
 *   <role>?
 *   <detail>?
 * </makefault>
 */
public class FaultMediatorFactory extends AbstractMediatorFactory {

    private static final QName HEADER_Q = new QName(Constants.SYNAPSE_NAMESPACE, "makefault");

    private static final QName ATT_VERSION_Q = new QName(Constants.NULL_NAMESPACE, "version");
    private static final QName CODE_Q        = new QName(Constants.SYNAPSE_NAMESPACE, "code");
    private static final QName REASON_Q      = new QName(Constants.SYNAPSE_NAMESPACE, "reason");
    private static final QName NODE_Q      = new QName(Constants.SYNAPSE_NAMESPACE, "node");
    private static final QName ROLE_Q      = new QName(Constants.SYNAPSE_NAMESPACE, "role");
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
                    new QName(Util.getNameSpaceWithPrefix(prefix, code), name));
                
            } else if (expression != null) {
                try {
                    AXIOMXPath xp = new AXIOMXPath(expression.getAttributeValue());
                    Util.addNameSpaces(xp, code, log);
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
                    Util.addNameSpaces(xp, reason, log);
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

        }else {
            String msg = "The fault reason is a required attribute for the makefault mediator";
            log.error(msg);
            throw new SynapseException(msg);
        }

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
        return HEADER_Q;
    }

}
