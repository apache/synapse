/*
 *
 * Copyright ©2002-2005 Infravio, Inc. All rights reserved.
 *
 * Infravio PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *   This software is the confidential and proprietary information of Infravio, Inc
 *   ("Confidential Information").  You shall not disclose such Confidential 
 *   Information and shall use it only in accordance with the terms of the license  
 *   agreement you entered into with Infravio.
 *
 *
 */

package org.apache.synapse.mediators.loadbalancing;

public class LoadBalancingConfiguration {

    LoadBalancingRule rules[] = new LoadBalancingRule[0];


    public LoadBalancingConfiguration() {

    }

    public LoadBalancingRule[] getRules() {
        return rules;
    }

    public void addRule(LoadBalancingRule rule) {
        LoadBalancingRule tmp[] = new LoadBalancingRule[rules.length + 1];
        for (int i = 0; i < rules.length; i++) {
            tmp[i] = rules[i];
        }
        tmp[rules.length] = rule;
        this.rules = tmp;
    }

    public void removeRule() {
        if (rules.length == 0)
            return;
        LoadBalancingRule tmp[] = new LoadBalancingRule[rules.length - 1];
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

    public String toString() {
        StringBuffer buf = new StringBuffer(1024);
        for (int i = 0; i < rules.length; i++) {
            if (rules[i] != null) {
                buf.append(rules[i].toString());
            }
        }
        return buf.toString();
    }

}
