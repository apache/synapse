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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.filters.SwitchCaseMediator;

import javax.xml.namespace.QName;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SwitchCaseMediatorFactory extends AbstractListMediatorFactory {

    private static final Log log = LogFactory.getLog(SwitchCaseMediatorFactory.class);

    private final QName SWITCH_CASE_Q = new QName(Constants.SYNAPSE_NAMESPACE, "case");

    public Mediator createMediator(OMElement elem) {

        SwitchCaseMediator switchCaseMediator = new SwitchCaseMediator();
        OMAttribute regex = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "regex"));
        if (regex == null) {
            String msg = "The 'regex' attribute is required for a switch case definition";
            log.error(msg);
            throw new SynapseException(msg);
        }

        try {
            switchCaseMediator.setRegex(Pattern.compile(regex.getAttributeValue()));
        } catch (PatternSyntaxException pse) {
            String msg = "Invalid Regular Expression for attribute 'regex' : " + regex.getAttributeValue();
            log.error(msg);
            throw new SynapseException(msg);
        }

        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        initMediator(switchCaseMediator,elem);

        super.addChildren(elem, switchCaseMediator);
        return switchCaseMediator;
    }

    public QName getTagQName() {
        return SWITCH_CASE_Q;
    }
}
