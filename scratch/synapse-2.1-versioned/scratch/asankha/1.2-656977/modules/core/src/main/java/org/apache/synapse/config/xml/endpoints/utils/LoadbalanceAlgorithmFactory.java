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

package org.apache.synapse.config.xml.endpoints.utils;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;
import org.apache.synapse.endpoints.algorithms.RoundRobin;
import org.apache.synapse.config.xml.XMLConfigConstants;

import javax.xml.namespace.QName;
import java.util.ArrayList;

/**
 * Factory of all load balance algorithms. ESBSendMediatorFactroy will use this to create the
 * appropriate algorithm implementation.
 */
public class LoadbalanceAlgorithmFactory {

    public static LoadbalanceAlgorithm createLoadbalanceAlgorithm(OMElement loadbalanceElement, ArrayList endpoints) {

        LoadbalanceAlgorithm algorithm = null;

        String algorithmName = "roundRobin";
        OMAttribute algoAttribute = loadbalanceElement.getAttribute(new QName(null, XMLConfigConstants.ALGORITHM_NAME));
        if(algoAttribute != null) {
            algorithmName = algoAttribute.getAttributeValue();
        }

        if(algorithmName.equalsIgnoreCase("roundRobin")) {
                algorithm = new RoundRobin(endpoints);
        }

        return algorithm;
    }
}
