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

import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.mediators.ListMediator;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The filter mediator combines the regex and xpath filtering functionality. If an xpath
 * is set, it is evaluated; else the given regex is evaluated against the source xpath.
 */
public class FilterMediator extends AbstractListMediator implements
    org.apache.synapse.mediators.FilterMediator {

    private SynapseXPath source = null;
    private Pattern regex = null;
    private SynapseXPath xpath = null;
    private ListMediator elseMediator = null;
    private boolean thenElementPresent = false;
    private String thenKey = null;
    private String elseKey = null;

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

            if (thenKey != null) {

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, (xpath == null ?
                        "Source : " + source + " against : " + regex.pattern() + " matches" :
                        "XPath expression : "  + xpath + " evaluates to true") +
                        " - executing then sequence with key : " + thenKey);
                }

                Mediator seq = synCtx.getSequence(thenKey);
                if (seq != null) {
                    result = seq.mediate(synCtx);
                } else {
                    handleException("Couldn't find the referred then sequence with key : "
                        + thenKey, synCtx);
                }
                
            } else {

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, (xpath == null ?
                        "Source : " + source + " against : " + regex.pattern() + " matches" :
                        "XPath expression : "  + xpath + " evaluates to true") +
                        " - executing child mediators");
                }

                result = super.mediate(synCtx);
            }

        } else {

            if (elseKey != null) {

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, (xpath == null ?
                        "Source : " + source + " against : " + regex.pattern() + " does not match" :
                        "XPath expression : "  + xpath + " evaluates to false") +
                        " - executing the else sequence with key : " + elseKey);
                }

                Mediator elseSeq = synCtx.getSequence(elseKey);

                if (elseSeq != null) {
                    result = elseSeq.mediate(synCtx);
                } else {
                    handleException("Couldn't find the referred else sequence with key : "
                        + elseKey, synCtx);
                }
                
            } else if (elseMediator != null) {

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, (xpath == null ?
                        "Source : " + source + " against : " + regex.pattern() + " does not match" :
                        "XPath expression : "  + xpath + " evaluates to false") +
                        " - executing the else path child mediators");
                }
                result = elseMediator.mediate(synCtx);

            } else {

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, (xpath == null ?
                        "Source : " + source + " against : " + regex.pattern() + " does not match" :
                        "XPath expression : "  + xpath + " evaluates to false and no else path") +
                        " - skipping child mediators");
                }
                result = true;
            }
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
                return xpath.booleanValueOf(synCtx);
            } catch (JaxenException e) {
                handleException("Error evaluating XPath expression : " + xpath, e, synCtx);
            }

        } else if (source != null && regex != null) {
            String sourceString = source.stringValueOf(synCtx);
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


    public SynapseXPath getSource() {
        return source;
    }

    public void setSource(SynapseXPath source) {
        this.source = source;
    }

    public Pattern getRegex() {
        return regex;
    }

    public void setRegex(Pattern regex) {
        this.regex = regex;
    }

    public SynapseXPath getXpath() {
        return xpath;
    }

    public void setXpath(SynapseXPath xpath) {
        this.xpath = xpath;
    }

    public ListMediator getElseMediator() {
        return elseMediator;
    }

    public void setElseMediator(ListMediator elseMediator) {
        this.elseMediator = elseMediator;
    }

    public boolean isThenElementPresent() {
        return thenElementPresent;
    }

    public void setThenElementPresent(boolean thenElementPresent) {
        this.thenElementPresent = thenElementPresent;
    }

    public String getThenKey() {
        return thenKey;
    }

    public void setThenKey(String thenKey) {
        this.thenKey = thenKey;
    }

    public String getElseKey() {
        return elseKey;
    }

    public void setElseKey(String elseKey) {
        this.elseKey = elseKey;
    }
}
