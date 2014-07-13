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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.mediators.AbstractListMediator;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A SwitchCaseMediator is a list mediator which has a regex that is matched by
 * its owning SwitchMediator for selection
 */
public class SwitchCaseMediator extends AbstractListMediator {

    private static final Log log = LogFactory.getLog(SwitchCaseMediator.class);

    /** The regular expression pattern to be used */
    private Pattern regex = null;
    /** Is this the default case? */
    private boolean defaultCase = false;

    public SwitchCaseMediator() {}

    public SwitchCaseMediator(Pattern regex, boolean defaultCase, List children) {
        this.regex = regex;
        this.defaultCase = defaultCase;
        this.addAll(children);
    }

    public Pattern getRegex() {
        return regex;
    }

    public void setRegex(Pattern regex) {
        this.regex = regex;
    }

    public boolean isDefaultCase() {
        return defaultCase;
    }

    public void setDefaultCase(boolean defaultCase) {
        this.defaultCase = defaultCase;
    }

    public boolean matches(String value) {
        if (isDefaultCase()) {
            log.debug("This is the default case of the switch");
            return true;
        }
        boolean retVal = regex.matcher(value).matches();
        log.debug("Case : " + regex.pattern() + " evaluated to : " + retVal);
        return retVal;
    }
}