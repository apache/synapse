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

import java.util.Map;
import java.net.URI;
import java.net.URISyntaxException;

public class RewriteRule {

    private Evaluator condition;

    private String value;
    private SynapseXPath xpath;

    private int fragmentIndex = URLRewriteMediator.FULL_URI;

    public void rewrite(Object[] fragments, MessageContext messageContext, String uriString,
                        Map<String,String> headers) {

        EvaluatorContext ctx = new EvaluatorContext(uriString, headers);
        if (condition != null) {
            try {
                if (!condition.evaluate(ctx)) {
                    return;
                }
            } catch (EvaluatorException e) {
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
                URI uri = new URI(result);
                fragments[0] = uri.getScheme();
                fragments[1] = uri.getUserInfo();
                fragments[2] = uri.getHost();
                fragments[3] = uri.getPort();
                fragments[4] = uri.getPath();
                fragments[5] = uri.getQuery();
                fragments[6] = uri.getFragment();
            } catch (URISyntaxException e) {
                return;
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
