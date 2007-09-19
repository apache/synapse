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

    private static final Log log = LogFactory.getLog(FilterMediator.class);
    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);
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

        if (log.isDebugEnabled()) {
            log.debug("Filter mediator mediate()");
        }
        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        try {
            if (shouldTrace) {
                trace.trace("Start : Filter mediator ");
            }
            if (test(synCtx)) {
                if (log.isDebugEnabled()) {
                    log.debug("Filter condition satisfied.. executing child mediators");
                }
                return super.mediate(synCtx);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Filter condition failed.. will skip executing child mediators");
                }
                return true;
            }
        } finally {
            if (shouldTrace) {
                trace.trace("End : Filter mediator ");
            }
        }
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
        try {
            if (xpath != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Evaluating XPath expression : " + xpath);
                }
                if (shouldTrace(synCtx.getTracingState())) {
                    trace.trace("XPath expression : " + xpath + " evaluates to : " +
                            xpath.booleanValueOf(synCtx.getEnvelope()));
                }
                return xpath.booleanValueOf(synCtx.getEnvelope());

            } else if (source != null && regex != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Evaluating regular expression : " + regex.pattern() +
                            " against source : " + source);
                }
                String sourceString = Axis2MessageContext.getStringValue(source, synCtx);
                if (sourceString == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Source String has been evaluated to Null");
                    }
                    return false;
                }
                if (shouldTrace(synCtx.getTracingState())) {
                    trace.trace("Regular expression : " + regex.pattern() + " and Source " +
                            sourceString + " matches : " + regex.matcher(sourceString).matches());
                }
                Matcher matcher = regex.matcher(sourceString);
                if (matcher == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Can not find a Regex Pattren Matcher");
                    }
                    return false;
                }
                return matcher.matches();
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
