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

package org.apache.synapse.mediators.filters;

import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.Util;
import org.apache.synapse.mediators.AbstractListMediator;
import org.jaxen.JaxenException;

import java.util.regex.Pattern;
import java.util.List;
import java.util.Iterator;

/**
 * The filter mediator combines the regex and xpath filtering functionality. If an xpath
 * is set, it is evaluated; else the given regex is evaluated against the source xpath.
 */
public class FilterMediator extends AbstractListMediator implements org.apache.synapse.api.FilterMediator {

    private AXIOMXPath source = null;
    private Pattern regex = null;
    private AXIOMXPath xpath = null;

    /**
     * Executes the list of sub/child mediators, if the filter condition is satisfied
     * @param synMsg the current message
     * @return true if filter condition fails. else returns as per List mediator semantics
     */
    public boolean mediate(SynapseMessage synMsg) {
        log.debug(getType() + " mediate()");
        if (test(synMsg)) {
            return super.mediate(synMsg);
        } else {
            return true;
        }
    }

    /**
     * Tests the supplied condition after evaluation against the given XPath
     * or Regex (against a source XPath). When a regular expression is supplied
     * the source XPath is evaluated into a String value, and matched against
     * the given regex
     * @param synMsg the current message for evaluation of the test condition
     * @return true if evaluation of the XPath/Regex results in true
     */
    public boolean test(SynapseMessage synMsg) {
        try {
            if (xpath != null) {
                return xpath.booleanValueOf(synMsg.getEnvelope());

            } else if (source != null && regex != null) {
                return regex.matcher(Util.getStringValue(source, synMsg)).matches();

            } else {
                log.error("Invalid configuration specified");
                return false;
            }

        } catch (JaxenException e) {
            log.error("XPath error : " + e.getMessage());
            return false;
        }
    }


    public AXIOMXPath getSource() {
        return source;
    }

    public void setSource(AXIOMXPath source) {
        this.source = source;
    }

    public Pattern getRegex() {
        return regex;
    }

    public void setRegex(Pattern regex) {
        this.regex = regex;
    }

    public AXIOMXPath getXpath() {
        return xpath;
    }

    public void setXpath(AXIOMXPath xpath) {
        this.xpath = xpath;
    }

}
