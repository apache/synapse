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
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.AnonymousListMediator;
import org.apache.synapse.config.xml.SwitchCase;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.filters.SwitchMediator;
import org.apache.synapse.util.xpath.SynapseXPath;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/*
 *
 * Content Base Router Mediator implements the Content Base Router EIP,
 * This pattern route messages to the appropriate sequence,
 * according to the message contents.
 * Routing decision is taken by matching given Xpath expression and RegEx
 */
public class ContentBasedRouterMediator extends AbstractMediator {
    private static final String DELIMITER_1 = ";";
    private static final String DELIMITER_2 = ",";
    private static final String DELIMITER_3 = ":";

    /**
     * Route messages to the appropriate sequence according to the message contents
     *
     * @param messageContext the current message for mediation
     * @return  true
     */
    public boolean mediate(MessageContext messageContext) {
        SwitchMediator switchMediator = new SwitchMediator();
        initParams(messageContext, switchMediator);
        switchMediator.mediate(messageContext);
        return true;
    }

    /**
     * For each message we build the routing mediators programmatically
     *
     * @param synCtx         MessageContext
     * @param switchMediator SwitchMediator Object
     */
    private void initParams(MessageContext synCtx, SwitchMediator switchMediator) {
        //fetch routung expression
        Object sourceXpath = EIPUtils.lookupFunctionParam(synCtx, "routing_exp");
        if (sourceXpath == null) {
            String msg = "A 'source' XPath attribute is required for a switch mediator";
            log.error(msg);
            throw new SynapseException(msg);
        } else {
            try {
                switchMediator.setSource((SynapseXPath) sourceXpath);
            } catch (Exception e) {
                String msg = "Invalid XPath for attribute 'source'";
                log.error(msg);
                throw new SynapseException(msg);
            }
        }
        String matchContent = (String) EIPUtils.lookupFunctionParam(synCtx, "match_content");
        String[] contentSet = matchContent.split(DELIMITER_1);
        if (contentSet.length == 1) {
            String caseItr = contentSet[0].trim();
            createCases(switchMediator, caseItr);
        } else if (contentSet.length == 2) {
            String caseItr = contentSet[0].trim();
            createCases(switchMediator, caseItr);

            String caseDefault = contentSet[1].trim();
            if (caseDefault == null || "".equals(caseDefault)) {
                String msg = "Default Sequence Reference is not defined";
                log.error(msg);
            }
            AnonymousListMediator mediator = new AnonymousListMediator();
            SequenceMediator sequenceMediator = new SequenceMediator();
            Value value = new Value(caseDefault);
            sequenceMediator.setKey(value);
            mediator.addChild(sequenceMediator);
            SwitchCase defaultCase = new SwitchCase();
            defaultCase.setCaseMediator(mediator);
            switchMediator.setDefaultCase(defaultCase);

        } else {
            String msg = "Invalid match content";
            log.error(msg);
        }

    }

    /**
     * Creates cases for the switch mediator
     *
     * @param switchMediator SwitchMediator Object
     * @param caseItr        String parameter for Cases
     * @return true
     */
    private boolean createCases(SwitchMediator switchMediator, String caseItr) {
        String[] caseSet = caseItr.split(DELIMITER_2);
        if (caseSet.length == 0) {
            log.warn("No Definitions found for dynamic routing");
            return false;
        }

        SwitchCase aCase;
        for (String newCase : caseSet) {
            if (newCase != null && !"".equals(newCase.trim())) {
                aCase = createCase(newCase.trim());
                switchMediator.addCase(aCase);
            } else {
                String msg = "Sequence Reference has not defined";
                log.error(msg);
            }
        }
        return true;
    }

    /**
     * Creates case Object for the switch mediator
     *
     * @param caseConfig String parameter for Case expression
     * @return SwitchCase Object
     */
    private SwitchCase createCase(String caseConfig) {
        SwitchCase aCase = new SwitchCase();
        String[] caseAttr = caseConfig.split(DELIMITER_3);
        String regEx = null;
        String seqRef = null;

        if (caseAttr.length == 2) {
            regEx = caseAttr[0].trim();
            seqRef = caseAttr[1].trim();
        } else if (caseAttr.length == 1) {
            regEx = caseAttr[0].trim();
            seqRef = "main";
        } else {
            String msg = "Unsupported routing condition";
            log.error(msg);
        }

        if (regEx == null || "".equals(regEx.trim())) {
            String msg = "The 'regex' attribute is required for a switch case definition";
            log.error(msg);
            throw new SynapseException(msg);
        }
        try {
            aCase.setRegex(Pattern.compile(regEx));
        } catch (PatternSyntaxException pse) {
            String msg = "Invalid Regular Expression for attribute 'regex' : " + regEx;
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (seqRef == null || "".equals(seqRef.trim())) {
            String msg = "Sequence Reference has not defined";
            log.error(msg);

        }
        AnonymousListMediator mediator = new AnonymousListMediator();
        SequenceMediator sequenceMediator = new SequenceMediator();
        Value value = new Value(seqRef);
        sequenceMediator.setKey(value);
        mediator.addChild(sequenceMediator);
        aCase.setCaseMediator(mediator);

        return aCase;
    }
}
