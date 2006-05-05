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

import org.apache.synapse.SynapseContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.Constants;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.transform.HeaderMediator;
import org.apache.synapse.mediators.transform.HeaderMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;

/**
 * This builds a Header Mediator parsing the XML configuration supplied
 *
 * Set header
 *   <header name="qname" (value="literal" | expression="xpath")/>
 *
 * Remove header
 *   <header name="qname" action="remove"/>
 */
public class HeaderMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(HeaderMediatorFactory.class);

    private static final QName HEADER_Q = new QName(Constants.SYNAPSE_NAMESPACE, "header");

    public Mediator createMediator(SynapseContext synCtx, OMElement elem) {

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
            headerMediator.setName(name.getAttributeValue());
        }

        // The action attribute is optional, if provided and equals to 'remove' the
        // header mediator will act as a header remove mediator
        if (action != null && "remove".equals(action.getAttributeValue())) {
            headerMediator.setAction(HeaderMediator.ACTION_REMOVE);
        }

        if (value == null && exprn == null) {
            String msg = "A 'value' or 'expression' attribute is required for a header mediator";
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (value != null && value.getAttributeValue() != null) {
            headerMediator.setValue(value.getAttributeValue());

        } else if (exprn != null && exprn.getAttributeValue() != null) {
            try {
                headerMediator.setExpression(new AXIOMXPath(exprn.getAttributeValue()));
            } catch (JaxenException je) {
                String msg = "Invalid XPath expression : " + exprn.getAttributeValue();
                log.error(msg);
                throw new SynapseException(msg, je);
            }
            
        } else {
            String msg = "Invalid attribute value for the attribute 'expression' or 'value'";
            log.error(msg);
            throw new SynapseException(msg);
        }

        return headerMediator;
    }

    public QName getTagQName() {
        return HEADER_Q;
    }

}
