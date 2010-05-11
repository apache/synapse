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

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.filters.router.ConditionalRouterMediator;
import org.apache.synapse.mediators.filters.router.Route;

/**
 * <pre>
 *  &lt;conditionalRouter continueAfter="(true|false)"&gt;
 *   &lt;route breakRoute="(true|false)"&gt;
 *     &lt;condition ../&gt;
 *     &lt;target ../&gt;
 *   &lt;/route&gt;
 *  &lt;/conditionalRouter&gt;
 * </pre>
 */
public class ConditionalRouterMediatorSerializer extends AbstractMediatorSerializer {
    
    public OMElement serializeMediator(OMElement parent, Mediator m) {
        OMElement conditionalRouterElem = fac.createOMElement("conditionalRouter", synNS);
        saveTracingState(conditionalRouterElem, m);

        ConditionalRouterMediator conditionalRouterMediator = (ConditionalRouterMediator) m;
        if (conditionalRouterMediator.isContinueAfterExplicitlySet()) {
            conditionalRouterElem.addAttribute("continueAfter",
                    Boolean.toString(conditionalRouterMediator.isContinueAfter()), nullNS);
        }

        for (Route route : conditionalRouterMediator.getRoutes()) {
            OMElement routeElem = fac.createOMElement("route", synNS);

            if (route.isBreakRouteExplicitlySet()) {
                routeElem.addAttribute("breakRoute", Boolean.toString(route.isBreakRoute()), nullNS);
            }

            if (route.getTarget() != null) {
                routeElem.addChild(TargetSerializer.serializeTarget(route.getTarget()));
            } else {
                handleException("Route in a conditional router has to have a target");
            }

            if (route.getEvaluator() != null) {
                // todo serialize the route evaluator
            }
            conditionalRouterElem.addChild(routeElem);
        }

        if (parent != null) {
            parent.addChild(conditionalRouterElem);
        }

        return conditionalRouterElem;
    }

    public String getMediatorClassName() {
        return ConditionalRouterMediator.class.getName();
    }
}
