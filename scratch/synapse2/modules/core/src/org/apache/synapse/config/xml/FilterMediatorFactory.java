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

import org.apache.synapse.api.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.filters.FilterMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.Iterator;

/**
 * Creates a filter mediator instance
 *
 * <filter (source="xpath" regex="string") | xpath="xpath">
 *   mediator+
 * </filter>
 */
public class FilterMediatorFactory extends AbstractListMediatorFactory {

    private static final Log log = LogFactory.getLog(FaultMediatorFactory.class);

    private static final QName FILTER_Q    = new QName(Constants.SYNAPSE_NAMESPACE, "filter");

    public Mediator createMediator(OMElement elem) {
        FilterMediator filter = new FilterMediator();
        super.addChildren(elem, filter);

        OMAttribute attXpath  = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "xpath"));
        OMAttribute attSource = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "source"));
        OMAttribute attRegex  = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "regex"));

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
            addNameSpaces(elem, filter.getXpath());

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
            addNameSpaces(elem, filter.getSource());

        } else {
            String msg = "An xpath or (source, regex) attributes are required for a filter";
            log.error(msg);
            throw new SynapseException(msg);
        }
        return filter;
    }

    public QName getTagQName() {
        return FILTER_Q;
    }

    private void addNameSpaces(OMElement elem, AXIOMXPath xp) {
        try {
            Iterator it = elem.getAllDeclaredNamespaces();
            while (it.hasNext()) {
                OMNamespace n = (OMNamespace) it.next();
                xp.addNamespace(n.getPrefix(), n.getName());
            }
        } catch (JaxenException je) {
            String msg = "Error adding declared name spaces " + elem;
            log.error(msg);
            throw new SynapseException(msg, je);
        }
    }
}
