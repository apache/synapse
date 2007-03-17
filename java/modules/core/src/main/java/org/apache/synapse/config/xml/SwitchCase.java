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

import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A SwitchCase define a case element of Switch Mediator and It has a list mediator and
 * a regex that is matched by its owning SwitchMediator for selection.
 * If any SwitchCase has selected ,Then the list mediator of it, will responsible
 * for message mediation
 */

public class SwitchCase {

    private static final Log log = LogFactory.getLog(SwitchCase.class);
    /** The regular expression pattern to be used */
    private Pattern regex = null;
    /** The list mediator for which responsible message mediation  */
    private AnonymousListMediator caseMediator;

    public SwitchCase() {
    }

    /**
     * To delegate message mediation to list mediator
     *
     * @param synCtx
     * @return boolean value
     */
    public boolean mediate(MessageContext synCtx) {
        if (caseMediator != null) {
            return caseMediator.mediate(synCtx);
        }
        return true;
    }

    /**
     * To get list mediator of this case element
     *
     * @return List mediator of  switch case
     */
    public AnonymousListMediator getCaseMediator() {
        return caseMediator;
    }

    /**
     * To set the list mediator
     *
     * @param caseMediator
     */
    public void setCaseMediator(AnonymousListMediator caseMediator) {
        this.caseMediator = caseMediator;
    }

    /**
     * To get the regular expression pattern
     *
     * @return Pattern
     */
    public Pattern getRegex() {
        return regex;
    }

    /**
     * To set the regular expression pattern
     *
     * @param regex
     */
    public void setRegex(Pattern regex) {
        this.regex = regex;
    }

    /**
     * To evaluate regular expression pattern to a get switch case
     *
     * @param value
     * @return boolean value
     */
    public boolean matches(String value) {
        Matcher matcher = regex.matcher(value);
        if(matcher == null){
            return false;
        }
        boolean retVal = matcher.matches();
        log.debug("Case : " + regex.pattern() + " evaluated to : " + retVal);
        return retVal;
    }
}
