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
import org.apache.synapse.mediators.builtin.CalloutMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;

/**
 * <callout serviceURL="string" [action="string"]>
 *      <source xpath="expression" | key="string">
 *      <target xpath="expression" | key="string"/>
 * </callout>
 */
public class CalloutMediatorFactory extends AbstractMediatorFactory {

    private static final QName TAG_NAME = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "callout");
    private static final QName ATT_URL = new QName("serviceURL");
    private static final QName ATT_ACTION = new QName("action");
    private static final QName Q_SOURCE = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "source");
    private static final QName Q_TARGET = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "target");

    public Mediator createMediator(OMElement elem) {

        CalloutMediator callout = new CalloutMediator();

        OMAttribute attServiceURL = elem.getAttribute(ATT_URL);
        OMAttribute attAction     = elem.getAttribute(ATT_ACTION);
        OMElement   sourceElt     = elem.getFirstChildWithName(Q_SOURCE);
        OMElement   targetElt     = elem.getFirstChildWithName(Q_TARGET);

        if (attServiceURL != null) {
            callout.setServiceURL(attServiceURL.getAttributeValue());
        } else {
            handleException("The 'serviceURL' attribute is required for the Callout mediator");
        }

        if (attAction != null) {
            callout.setAction(attAction.getAttributeValue());
        }

        if (sourceElt != null) {
            if (sourceElt.getAttribute(ATT_XPATH) != null) {
                try {
                    callout.setRequestXPathString(sourceElt.getAttributeValue(ATT_XPATH));
                    OMElementUtils.addNameSpaces(callout.getRequestXPath(), sourceElt, log);
                } catch (JaxenException e) {
                    handleException("Invalid source XPath : " + sourceElt.getAttributeValue(ATT_XPATH));
                }
            } else if (sourceElt.getAttribute(ATT_KEY) != null) {
                callout.setRequestKey(sourceElt.getAttributeValue(ATT_KEY));
            } else {
                handleException("A 'xpath' or 'key' attribute is required for the Callout 'source'");
            }
        } else {
            handleException("The message 'source' must be specified for a Callout mediator");
        }

        if (targetElt != null) {
            if (targetElt.getAttribute(ATT_XPATH) != null) {
                try {
                    callout.setTargetXPathString(targetElt.getAttributeValue(ATT_XPATH));
                    OMElementUtils.addNameSpaces(callout.getTargetXPath(), targetElt, log);
                } catch (JaxenException e) {
                    handleException("Invalid target XPath : " + targetElt.getAttributeValue(ATT_XPATH));
                }
            } else if (targetElt.getAttribute(ATT_KEY) != null) {
                callout.setTargetKey(targetElt.getAttributeValue(ATT_KEY));
            } else {
                handleException("A 'xpath' or 'key' attribute is required for the Callout 'target'");
            }
        } else {
            handleException("The message 'target' must be specified for a Callout mediator");
        }

        return callout;
    }

    public QName getTagQName() {
        return TAG_NAME;
    }
}
