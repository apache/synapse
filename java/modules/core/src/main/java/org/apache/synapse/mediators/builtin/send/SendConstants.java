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

package org.apache.synapse.mediators.builtin.send;

import javax.xml.namespace.QName;

/**
 * Tempory class to hlod send mediator specific constants. Later decide on where to place these.
 */
public class SendConstants {

    public static final String ESBSEND_ELEMENT          = "send";
    public static final String LOADBALANCE_ELEMENT      = "loadbalance";
    public static final String FAILOVER_ELEMENT         = "failover";   // failover only element
    public static final String RETRY_AFTER_FAILURE_TIME = "retryAfterFailure";
    public static final String MAXIMUM_RETRIES          = "maximumRetries";
    public static final String RETRY_INTERVAL           = "retryInterval";
    public static final String FAILOVER                 = "failover";   // failover attribute in the loadbalance element
    public static final String SESSION_AFFINITY         = "sessionAffinity";
    public static final String ALGORITHM_NAME           = "algorithmName";
    public static final String FAILOVER_GROUP_ELEMENT   = "failover"; // failover group element inside the loadbalance element
    public static final String DISPATCH_MANAGER         = "DISPATCH_MANAGER";
    public static final String DISPATCHERS_ELEMENT      = "dispatchers";
    public static final String DISPATCHER_ELEMENT       = "dispatcher";

    public static final QName ATT_KEY_Q =
            new QName(org.apache.synapse.config.xml.Constants.NULL_NAMESPACE, "key");
    public static final QName ATT_ADDRESS_Q =
            new QName(org.apache.synapse.config.xml.Constants.NULL_NAMESPACE, "address");
}
