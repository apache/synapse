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
import org.apache.synapse.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class RewriteRule {

    private static final Log log = LogFactory.getLog(RewriteRule.class);

    private Evaluator condition;
    private List<RewriteAction> actions = new ArrayList<RewriteAction>();

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

        for (RewriteAction action : actions) {
            action.execute(fragments, messageContext);
        }
    }

    public Evaluator getCondition() {
        return condition;
    }

    public void setCondition(Evaluator condition) {
        this.condition = condition;
    }

    public void addRewriteAction(RewriteAction action) {
        actions.add(action);
    }
}
