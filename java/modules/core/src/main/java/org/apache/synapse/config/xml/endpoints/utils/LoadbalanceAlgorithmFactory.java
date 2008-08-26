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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.clustering.Member;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;
import org.apache.synapse.endpoints.algorithms.RoundRobin;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Factory of all load balance algorithms. ESBSendMediatorFactroy will use this to create the
 * appropriate algorithm implementation.
 */
public class LoadbalanceAlgorithmFactory {

    private static final Log log = LogFactory.getLog(LoadbalanceAlgorithmFactory.class);

    public static LoadbalanceAlgorithm createLoadbalanceAlgorithm(OMElement loadbalanceElement) {
        return getLoadbalanceAlgorithm(loadbalanceElement);
    }

    public static LoadbalanceAlgorithm createLoadbalanceAlgorithm(OMElement loadbalanceElement,
                                                                  List<Endpoint> endpoints) {
        LoadbalanceAlgorithm algorithm = getLoadbalanceAlgorithm(loadbalanceElement);
        algorithm.setEndpoints(endpoints);
        return algorithm;
    }

    public static LoadbalanceAlgorithm createLoadbalanceAlgorithm2(OMElement loadbalanceElement,
                                                                   List<Member> members) {

        LoadbalanceAlgorithm algorithm = getLoadbalanceAlgorithm(loadbalanceElement);
        algorithm.setApplicationMembers(members);
        return algorithm;
    }

    private static LoadbalanceAlgorithm getLoadbalanceAlgorithm(OMElement loadbalanceElement) {
        LoadbalanceAlgorithm algorithm = new RoundRobin();  // Default algorithm is round-robin
        OMAttribute policyAtt =
                loadbalanceElement.getAttribute(new QName(null,
                                                          XMLConfigConstants.LOADBALANCE_POLICY));
        OMAttribute algorithmAtt =
                loadbalanceElement.getAttribute(new QName(null,
                                                          XMLConfigConstants.LOADBALANCE_ALGORITHM));
        if (policyAtt != null && algorithmAtt != null) {
            String msg = "You cannot specify both the 'policy' & 'algorithm' in the configuration. " +
                         "It is sufficient to provide only the 'algorithm'.";
            log.fatal(msg); // We cannot continue execution. Hence it is logged at fatal level
            throw new SynapseException(msg);
        }
        if (algorithmAtt != null) {
            String algorithmStr = algorithmAtt.getAttributeValue().trim();
            try {
                algorithm = (LoadbalanceAlgorithm) Class.forName(algorithmStr).newInstance();
            } catch (Exception e) {
                String msg = "Cannot instantiate LoadbalanceAlgorithm implementation class " +
                             algorithmStr;
                log.fatal(msg, e); // We cannot continue execution. Hence it is logged at fatal level
                throw new SynapseException(msg, e);
            }
        } else if (policyAtt != null) {
            if (!policyAtt.getAttributeValue().trim().equals("roundRobin")) {
                String msg = "Unsupported algorithm " + policyAtt.getAttributeValue().trim() +
                             " specified. Please use the 'algorithm' attribute to specify the " +
                             "correct loadbalance algorithm implementation.";
                log.fatal(msg); // We cannot continue execution. Hence it is logged at fatal level
                throw new SynapseException(msg);
            }
        }
        return algorithm;
    }
}
