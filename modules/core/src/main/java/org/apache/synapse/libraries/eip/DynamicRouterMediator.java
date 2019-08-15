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

package org.apache.synapse.libraries.eip;

import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.evaluators.*;
import org.apache.synapse.commons.evaluators.source.HeaderTextRetriever;
import org.apache.synapse.commons.evaluators.source.SourceTextRetriever;
import org.apache.synapse.commons.evaluators.source.URLTextRetriever;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.mediators.filters.router.ConditionalRoute;
import org.apache.synapse.mediators.filters.router.ConditionalRouterMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/*
 *
 * The Dynamic Router Mediator implements the Dynamic Router EIP ,
 * this pattern route a message consecutively through a series of condition steps, which is parsed by the conditions parameter
 * The list of sequences through which the message should pass is decided dynamically at run time.
 * Checks whether the route condition evaluates to true and mediates using the given sequence
 * Routing decision is based on the message contents such as HTTP url,HTTP headers or combination of both
 */


public class DynamicRouterMediator extends AbstractMediator {
    private static final String DELIMITER_1 = ",";
    private static final String DELIMITER_2 = ";";
    private static final String DELIMITER_3 = "=";
    private static final String DELIMITER_4 = ":";
    private static final String DELIMITER_5 = "\\{AND}";
    private static final String DELIMITER_6 = "\\{OR}";
    MessageContext synCtx;

    /**
     * Route a message consecutively through a series of condition steps
     *
     * @param msgCtx the current message for mediation
     * @return
     */
    public boolean mediate(MessageContext msgCtx) {
        synCtx = msgCtx;
        ConditionalRouterMediator conditionalRouterMediator = new ConditionalRouterMediator();
        conditionalRouterMediator.setContinueAfter(false);
        createDynamicRoute(msgCtx, conditionalRouterMediator);
        conditionalRouterMediator.mediate(msgCtx);

        return true;
    }

    /**
     * Initialize dynamic route with the given String of parameters
     *
     * @param synCtx                    MessageContext
     * @param conditionalRouterMediator ConditionalRouterMediator
     * @return true
     */
    private boolean createDynamicRoute(MessageContext synCtx, ConditionalRouterMediator conditionalRouterMediator) {
        String conditionRouts = (String) EIPUtils.lookupFunctionParam(synCtx, "conditions");
        String[] conditionsSet = conditionRouts.split(DELIMITER_1);
        if (conditionsSet.length == 0) {
            log.warn("No Definitions found for dynamic routing");
            return false;
        }

        ConditionalRoute conditionalRoute;

        for (String conditionRoute : conditionsSet) {
            if (conditionRoute != null && !"".equals(conditionRoute.trim())) {
                conditionalRoute = createConRoute(conditionRoute.trim());
                conditionalRouterMediator.addRoute(conditionalRoute);
            }
        }
        return true;
    }

    /**
     * Creates ConditionalRoute with given the String of parameters
     *
     * @param conRoute String of expression
     * @return conditionalRoute ConditionalRoute
     */
    private ConditionalRoute createConRoute(String conRoute) {
        ConditionalRoute conditionalRoute = new ConditionalRoute();
        String[] conditionDefs = conRoute.split(DELIMITER_2);
        conditionalRoute.setBreakRoute(false);
        if (conditionDefs.length < 2) {
            handleException("Conditional Route is not defined", synCtx);
            return null;
        }

        String condition = conditionDefs[0];
        String target = conditionDefs[1];

        if (condition != null && !"".equals(condition.trim())) {
            createCondition(conditionalRoute, condition.trim());
        } else {
            handleException("Routing condition can not be empty", synCtx);
        }
        if (target != null && !"".equals(target.trim())) {
            createTarget(conditionalRoute, target);
        } else {
            handleException("Routing Target can not be empty", synCtx);
        }


        return conditionalRoute;
    }

    /**
     * Creates Condition parameter for ConditionalRoute
     *
     * @param conRoute  ConditionalRoute
     * @param condition String expression
     */
    private void createCondition(ConditionalRoute conRoute, String condition) {


        if (!condition.contains("{AND}") && !condition.contains("{OR}")) {
            Evaluator evaluator = createMatchEvaluator(condition);
            conRoute.setEvaluator(evaluator);
        } else if (condition.contains("{AND}") && !condition.contains("{OR}")) {
            Evaluator andEvaluator = createAndEvaluator(condition);
            conRoute.setEvaluator(andEvaluator);
        } else if (condition.contains("{OR}") && !condition.contains("{AND}")) {
            Evaluator orEvaluator = createOrEvaluator(condition);
            conRoute.setEvaluator(orEvaluator);
        } else {
            handleException("Routing condition is wrong", synCtx);
        }


    }

    /**
     * Creates Evaluator parameters for Condition
     *
     * @param matchElements String expression
     * @return matchEvaluator Evaluator
     */
    private Evaluator createMatchEvaluator(String matchElements) {
        String[] matchElem = matchElements.split(DELIMITER_3);

        String matchValue = matchElem[0].trim();
        String matchConfig = matchElem[1].trim();
        MatchEvaluator matchEvaluator = new MatchEvaluator();
        String[] matchParams = matchConfig.split(DELIMITER_4);

        SourceTextRetriever textRetriever = null;
        String regEx = null;
        String source ;
        if (matchParams.length == 1) {
            regEx = matchParams[0].trim();
            if (matchValue.equals(EvaluatorConstants.URL)) {
                textRetriever = new URLTextRetriever();
            }
        } else if (matchParams.length == 2) {
            source = matchParams[0].trim();

            if (matchValue.equals(EvaluatorConstants.HEADER)) {
                if (source != null) {
                    textRetriever = new HeaderTextRetriever(source);
                } else {
                    handleException(EvaluatorConstants.SOURCE + " attribute is required", synCtx);
                }
            } else if (matchValue.equals(EvaluatorConstants.URL)) {
                textRetriever = new URLTextRetriever();
                if (source != null) {
                    ((URLTextRetriever) textRetriever).setSource(source);
                }
            } else {
                handleException("Unsupported evaluator:" + matchValue, synCtx);
            }

            regEx = matchParams[1].trim();
        } else {
            handleException("Unsupported condition" + matchConfig, synCtx);
        }

        matchEvaluator.setTextRetriever(textRetriever);

        if (regEx == null) {
            handleException(EvaluatorConstants.REGEX + " attribute is required", synCtx);
            return null;
        }
        matchEvaluator.setRegex(Pattern.compile(regEx));

        return matchEvaluator;
    }

    /**
     * Creates AndEvaluator parameters for Condition
     *
     * @param andConfig String expression
     * @return andEvaluator Evaluator
     */
    private Evaluator createAndEvaluator(String andConfig) {

        String[] andEvals = andConfig.split(DELIMITER_5);
        AndEvaluator andEvaluator = new AndEvaluator();
        List<Evaluator> evaluators = new ArrayList<Evaluator>();

        for (String matchElements : andEvals) {

            if (matchElements != null && !"".equals(matchElements.trim())) {
                Evaluator evaluator = createMatchEvaluator(matchElements.trim());
                evaluators.add(evaluator);

            }
        }

        if (evaluators.size() > 1) {
            andEvaluator.setEvaluators(evaluators.toArray(new Evaluator[evaluators.size()]));

        } else {
            handleException("Two or more expressions should be provided under And", synCtx);

        }
        return andEvaluator;
    }

    /**
     * Creates OrEvaluator parameters for Condition
     *
     * @param orConfig String expression
     * @return orEvaluator Evaluator
     */
    private Evaluator createOrEvaluator(String orConfig) {

        String[] orEvals = orConfig.split(DELIMITER_6);
        OrEvaluator orEvaluator = new OrEvaluator();
        List<Evaluator> evaluators = new ArrayList<Evaluator>();

        for (String matchElements : orEvals) {

            if (matchElements != null && !"".equals(matchElements.trim())) {
                Evaluator evaluator = createMatchEvaluator(matchElements.trim());
                evaluators.add(evaluator);

            }
        }

        if (evaluators.size() > 1) {
            orEvaluator.setEvaluators(evaluators.toArray(new Evaluator[evaluators.size()]));
        } else {
            handleException("Two or more expressions should be provided under Or", synCtx);

        }
        return orEvaluator;
    }

    /**
     * Creates Target parameter for ConditionalRoute
     *
     * @param conRoute  ConditionalRoute
     * @param targetVal String expression
     */
    private void createTarget(ConditionalRoute conRoute, String targetVal) {
        Target target = new Target();
        String[] elements = targetVal.split(DELIMITER_3);
        String type = elements[0].trim();
        String value = elements[1].trim();
        if (type.equalsIgnoreCase("seq")) {
            target.setAsynchronous(false);
            target.setSequenceRef(value);
            conRoute.setTarget(target);
        } else {
            handleException("Target Sequence has defined wrong", synCtx);
        }

    }

}
