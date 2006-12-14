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
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.transform.HeaderMediator;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * This builds a Header Mediator parsing the XML configuration supplied
 *
 * Set header
 *   <pre>
 *      &lt;header name="qname" (value="literal" | expression="xpath")/&gt;
 *   </pre>
 *
 * Remove header
 *   <pre>
 *      &lt;header name="qname" action="remove"/&gt;
 *   </pre>
 */
public class HeaderMediatorFactory extends AbstractMediatorFactory  {

    private static final Log log = LogFactory.getLog(HeaderMediatorFactory.class);

    private static final QName HEADER_Q = new QName(Constants.SYNAPSE_NAMESPACE, "header");

    public Mediator createMediator(OMElement elem) {

        HeaderMediator headerMediator = new HeaderMediator();
        OMAttribute name   = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        OMAttribute value  = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "value"));
        OMAttribute exprn  = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "expression"));
        OMAttribute action = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "action"));

        if (name == null || name.getAttributeValue() == null) {
            String msg = "A valid name attribute is required for the header mediator";
            log.error(msg);
            throw new SynapseException(msg);
        } else {
            String nameAtt = name.getAttributeValue();
            int colonPos = nameAtt.indexOf(":");
            if (colonPos != -1) {
                // has a NS prefix.. find it and the NS it maps into
                String prefix = nameAtt.substring(0, colonPos);
                Iterator it = elem.getAllDeclaredNamespaces();
                while (it.hasNext()) {
                    OMNamespace n = (OMNamespace) it.next();
                    if (prefix.equals(n.getPrefix())) {
                        headerMediator.setQName(
                            new QName(n.getNamespaceURI(), nameAtt.substring(colonPos+1), prefix));
                    }
                }
            } else {
                // no prefix
                headerMediator.setQName(new QName(nameAtt));
            }
        }

        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        initMediator(headerMediator,elem);

        // The action attribute is optional, if provided and equals to 'remove' the
        // header mediator will act as a header remove mediator
        if (action != null && "remove".equals(action.getAttributeValue())) {
            headerMediator.setAction(HeaderMediator.ACTION_REMOVE);
        }

        if (headerMediator.getAction() == HeaderMediator.ACTION_SET &&
            value == null && exprn == null) {
            String msg = "A 'value' or 'expression' attribute is required for a [set] header mediator";
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (value != null && value.getAttributeValue() != null) {
            headerMediator.setValue(value.getAttributeValue());

        } else if (exprn != null && exprn.getAttributeValue() != null) {
            try {
                AXIOMXPath xp = new AXIOMXPath(exprn.getAttributeValue());
                OMElementUtils.addNameSpaces(xp, elem, log);
                headerMediator.setExpression(xp);
            } catch (JaxenException je) {
                String msg = "Invalid XPath expression : " + exprn.getAttributeValue();
                log.error(msg);
                throw new SynapseException(msg, je);
            }
        }

        return headerMediator;
    }

    public QName getTagQName() {
        return HEADER_Q;
    }
}
