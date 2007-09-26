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
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.filters.FilterMediator;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Creates a filter mediator instance
 *
 * <pre>
 * &lt;filter (source="xpath" regex="string") | xpath="xpath"&gt;
 *   mediator+
 * &lt;/filter&gt;
 * </pre>
 */
public class FilterMediatorFactory extends AbstractListMediatorFactory {

    private static final QName FILTER_Q    = new QName(SynapseConstants.SYNAPSE_NAMESPACE, "filter");

    public Mediator createMediator(OMElement elem) {
        FilterMediator filter = new FilterMediator();

        OMAttribute attXpath  = elem.getAttribute(ATT_XPATH);
        OMAttribute attSource = elem.getAttribute(ATT_SOURCE);
        OMAttribute attRegex  = elem.getAttribute(ATT_REGEX);

        if (attXpath != null) {
            if (attXpath.getAttributeValue() != null && attXpath.getAttributeValue().trim().length() == 0) {
                String msg = "Invalid attribute value specified for xpath";
                log.error(msg);
                throw new SynapseException(msg);

            } else {
                try {
                    filter.setXpath(new AXIOMXPath(attXpath.getAttributeValue()));
                } catch (JaxenException e) {
                    String msg = "Invalid XPath expression for attribute xpath : " + attXpath.getAttributeValue();
                    log.error(msg);
                    throw new SynapseException(msg);
                }
            }
            OMElementUtils.addNameSpaces(filter.getXpath(), elem, log);

        } else if (attSource != null && attRegex != null) {

            if (
                (attSource.getAttributeValue() != null && attSource.getAttributeValue().trim().length() == 0) ||
                (attRegex.getAttributeValue()  != null && attRegex.getAttributeValue().trim().length() == 0) ){
                String msg = "Invalid attribute values for source and/or regex specified";
                log.error(msg);
                throw new SynapseException(msg);

            } else {
                try {
                    filter.setSource(new AXIOMXPath(attSource.getAttributeValue()));
                } catch (JaxenException e) {
                    String msg = "Invalid XPath expression for attribute source : " + attSource.getAttributeValue();
                    log.error(msg);
                    throw new SynapseException(msg);
                }
                try {
                    filter.setRegex(Pattern.compile(attRegex.getAttributeValue()));
                } catch (PatternSyntaxException pse) {
                    String msg = "Invalid Regular Expression for attribute regex : " + attRegex.getAttributeValue();
                    log.error(msg);
                    throw new SynapseException(msg);
                }
            }
            OMElementUtils.addNameSpaces(filter.getSource(), elem, log);

        } else {
            String msg = "An xpath or (source, regex) attributes are required for a filter";
            log.error(msg);
            throw new SynapseException(msg);
        }
        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        processTraceState(filter,elem);
        addChildren(elem, filter);
        return filter;
    }

    public QName getTagQName() {
        return FILTER_Q;
    }
}
