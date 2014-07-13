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

package org.apache.synapse.mediators.filters;

import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractListMediator;
import org.jaxen.JaxenException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * The filter mediator combines the regex and xpath filtering functionality. If an xpath
 * is set, it is evaluated; else the given regex is evaluated against the source xpath.
 */
public class FilterMediator extends AbstractListMediator implements org.apache.synapse.mediators.FilterMediator {

    private AXIOMXPath source = null;
    private Pattern regex = null;
    private AXIOMXPath xpath = null;

    /**
     * Executes the list of sub/child mediators, if the filter condition is satisfied
     *
     * @param synCtx the current message
     * @return true if filter condition fails. else returns as per List mediator semantics
     */
    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Filter mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        boolean result = false;
        if (test(synCtx)) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, (xpath == null ?
                    "Source : " + source + " against : " + regex.pattern() + " matches" :
                    "XPath expression : "  + xpath + " evaluates to true") +
                    " - executing child mediators");
            }
            result = super.mediate(synCtx);

        } else {

            if (traceOrDebugOn) {
                traceOrDebug(traceOn, (xpath == null ?
                    "Source : " + source + " against : " + regex.pattern() + " does not match" :
                    "XPath expression : "  + xpath + " evaluates to false") +
                    " - skipping child mediators");
            }
            result = true;
        }

        if (traceOrDebugOn) {
            trace.trace("End : Filter mediator ");
        }
        return result;
    }

    /**
     * Tests the supplied condition after evaluation against the given XPath
     * or Regex (against a source XPath). When a regular expression is supplied
     * the source XPath is evaluated into a String value, and matched against
     * the given regex
     *
     * @param synCtx the current message for evaluation of the test condition
     * @return true if evaluation of the XPath/Regex results in true
     */
    public boolean test(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (xpath != null) {
            try {
                return xpath.booleanValueOf(synCtx.getEnvelope());
            } catch (JaxenException e) {
                handleException("Error evaluating XPath expression : " + xpath, e, synCtx);
            }

        } else if (source != null && regex != null) {
            String sourceString = Axis2MessageContext.getStringValue(source, synCtx);
            if (sourceString == null) {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Source String : " + source + " evaluates to null");
                }
                return false;
            }
            Matcher matcher = regex.matcher(sourceString);
            if (matcher == null) {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Regex pattren matcher for : " + regex.pattern() +
                        "against source : " + sourceString + " is null");
                }
                return false;
            }
            return matcher.matches();
        }

        return false; // never executes
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
