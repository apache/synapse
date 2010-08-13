/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.synapse.mediators;

import org.apache.synapse.commons.evaluators.Evaluator;
import org.apache.synapse.commons.evaluators.EvaluatorContext;
import org.apache.synapse.commons.evaluators.EvaluatorException;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.apache.synapse.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.net.URI;
import java.net.URISyntaxException;

public class RewriteRule {

    private static final Log log = LogFactory.getLog(RewriteRule.class);

    private Evaluator condition;

    private String value;
    private SynapseXPath xpath;

    private int fragmentIndex = URLRewriteMediator.FULL_URI;

    public void rewrite(Object[] fragments, MessageContext messageContext, String uriString,
                        Map<String,String> headers) {

        if (condition != null) {
            EvaluatorContext ctx = new EvaluatorContext(uriString, headers);
            if (log.isDebugEnabled()) {
                log.debug("Evaluating condition with URI: " + uriString);
            }

            try {
                if (!condition.evaluate(ctx)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Condition evaluated to 'false' - Skipping the current action");
                    }
                    return;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Condition evaluated to 'true' - Performing the stated action");
                }
            } catch (EvaluatorException e) {
                log.warn("Error while evaluating the condition - Skipping the rule as it failed", e);
                return;
            }
        }

        String result;
        if (xpath != null) {
            result = xpath.stringValueOf(messageContext);
        } else {
            result = value;
        }

        if (fragmentIndex == URLRewriteMediator.FULL_URI) {
            try {
                URI uri;
                if (result != null) {
                    uri = new URI(result);
                    if (log.isDebugEnabled()) {
                        log.debug("Setting the URI to: " + result);
                    }
                } else {
                    uri = new URI("");
                }

                fragments[0] = uri.getScheme();
                fragments[1] = uri.getUserInfo();
                fragments[2] = uri.getHost();
                fragments[3] = uri.getPort();
                // The uri.getPath() return the empty string for empty URIs
                // We are better off setting it to null
                fragments[4] = "".equals(uri.getPath()) ? null : uri.getPath();
                fragments[5] = uri.getQuery();
                fragments[6] = uri.getFragment();

            } catch (URISyntaxException e) {
                return;
            }
        } else if (fragmentIndex == URLRewriteMediator.PORT) {
            // When setting the port we must first convert the value into an integer
            if (result != null) {
                fragments[fragmentIndex] = Integer.parseInt(result);
            } else {
                fragments[fragmentIndex] = -1;
            }
        } else {
            fragments[fragmentIndex] = result;
        }
    }

    public Evaluator getCondition() {
        return condition;
    }

    public void setCondition(Evaluator condition) {
        this.condition = condition;
    }

    public int getFragmentIndex() {
        return fragmentIndex;
    }

    public void setFragmentIndex(int fragmentIndex) {
        this.fragmentIndex = fragmentIndex;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SynapseXPath getXpath() {
        return xpath;
    }

    public void setXpath(SynapseXPath xpath) {
        this.xpath = xpath;
    }
}
