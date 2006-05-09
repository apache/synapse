/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse.mediators.filters;

import org.apache.synapse.mediators.AbstractListMediator;

import java.util.regex.Pattern;
import java.util.List;

/**
 * A SwitchCaseMediator is a list mediator which has a regex that is matched by
 * its owning SwitchMediator for selection
 */
public class SwitchCaseMediator extends AbstractListMediator {

    private Pattern regex = null;
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
        if (isDefaultCase())
            return true;
        return regex.matcher(value).matches();
    }
}