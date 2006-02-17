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

package sampleMediators.deprecation;

public class DeprecationConfiguration {

    private DeprecationRule rules[] = new DeprecationRule[0];
    private String enabled;

    public DeprecationConfiguration() {
    }

    public void addRule(DeprecationRule rule) {
        DeprecationRule tmp[] = new DeprecationRule[rules.length + 1];
        for (int i = 0; i < rules.length; i++) {
            tmp[i] = rules[i];
        }
        tmp[rules.length] = rule;
        this.rules = tmp;
    }

    public void removeRule() {
        if (rules.length == 0)
            return;
        DeprecationRule tmp[] = new DeprecationRule[rules.length - 1];
        for (int i = 0; i < rules.length - 1; i++) {
            tmp[i] = rules[i];
        }
        this.rules = tmp;
    }

    public boolean hasRule(String service) {
        for (int i = 0; i < rules.length; i++) {
            if (rules[i].getService().equals(service)) {
                return true;
            }
        }
        return false;
    }

    public DeprecationRule[] getRules() {
        return rules;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getEnabled() {
        return this.enabled;
    }

}