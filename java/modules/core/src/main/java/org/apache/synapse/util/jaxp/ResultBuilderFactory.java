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

package org.apache.synapse.util.jaxp;

import org.apache.synapse.core.SynapseEnvironment;

/**
 * Factory for {@link ResultBuilder} instances.
 */
public interface ResultBuilderFactory {
    /**
     * Create a new {@link ResultBuilder} instance.
     * 
     * @param synEnv the Synapse environment
     * @param isSoapEnvelope
     *           <code>true</code> if the expected output is a SOAP envelope. In this case an
     *           invocation of {@link ResultBuilder#getNode()} on the returned instance must
     *           return a {@link org.apache.axiom.soap.SOAPEnvelope}.
     * @return the newly created instance
     */
    ResultBuilder createResultBuilder(SynapseEnvironment synEnv, boolean isSoapEnvelope);
}
