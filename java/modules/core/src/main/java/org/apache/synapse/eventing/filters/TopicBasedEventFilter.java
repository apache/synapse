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

package org.apache.synapse.eventing.filters;

import org.apache.synapse.MessageContext;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.wso2.eventing.EventFilter;
import org.wso2.eventing.Event;

/**
 * Topic baed event filter that match the subscription based on a given topic
 */
public class TopicBasedEventFilter implements EventFilter<MessageContext> {

    private SynapseXPath sourceXpath;
    private String resultValue;
    private static final String FILTER_SEP = "/";

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public String toString() {
        return resultValue;
    }

    public SynapseXPath getSourceXpath() {
        return sourceXpath;
    }

    public void setSourceXpath(SynapseXPath sourceXpath) {
        this.sourceXpath = sourceXpath;
    }

    public boolean match(Event<MessageContext> event) {
        MessageContext messageContext = event.getMessage();
        String evaluatedValue = sourceXpath.stringValueOf(messageContext);
        if (evaluatedValue.equals(resultValue)) {
            return true;
        } else if (evaluatedValue.startsWith((resultValue + FILTER_SEP).trim())) {
            return true;
        }
        return false;
    }
}