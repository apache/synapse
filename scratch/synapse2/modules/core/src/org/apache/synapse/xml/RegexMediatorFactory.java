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


package org.apache.synapse.xml;



import javax.xml.namespace.QName;

import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.filters.RegexMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;

/**
 *
 * 
 */
public class RegexMediatorFactory extends
        AbstractListMediatorFactory {
    private static final String REGEX = "regex";

    private static final QName REGEX_Q = new QName(Constants.SYNAPSE_NAMESPACE,
            REGEX);

    private static final QName REGEX_PATTERN_ATT_Q = new QName("pattern");

    private static final QName REGEX_PROPERTY_ATT_Q = new QName("property");

    private static final QName REGEX_HEADER_ATT_Q = new QName("message-address");



    public Mediator createMediator(SynapseEnvironment se, OMElement el) {
        RegexMediator rm = new RegexMediator();
        super.addChildrenAndSetName(se, el, rm);

        OMAttribute patt = el.getAttribute(REGEX_PATTERN_ATT_Q);
        if (patt == null) {
            throw new SynapseException(REGEX + " must have "
                    + REGEX_PATTERN_ATT_Q + " attribute: " + el.toString());
        }

        OMAttribute prop = el.getAttribute(REGEX_PROPERTY_ATT_Q);
        OMAttribute head = el.getAttribute(REGEX_HEADER_ATT_Q);
        if (prop == null && head == null) {
            throw new SynapseException(REGEX + " must have either "
                    + REGEX_PROPERTY_ATT_Q + " or " + REGEX_HEADER_ATT_Q
                    + " attributes: " + el.toString());
        }
        rm.setPattern(patt.getAttributeValue());
        if (prop != null) {
            rm.setPropertyName(prop.getAttributeValue());
        } else {
            String header = head.getAttributeValue();
            rm.setHeaderType(header);
        }
        return rm;
    }

    public QName getTagQName() {
        return REGEX_Q;
    }

}
