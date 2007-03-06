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

package org.apache.synapse;

/**
 * This is the generic interface for the fault handlers in synapse. In the synapse message context
 * it has a stck of fault handlers and when ever a Synapse (Runtime) Exception has thrown this stack
 * will be examined by one of the SynapseMR, ProxyServiceMR, or CallbackReceiver and faultStack
 * will be poped to get the most relevant FaultHandler and execute the handleFault method.
 */
public interface FaultHandler {

    /**
     * This will be executed to handle any Exceptions occured within the Synapse environment.
     * @param synCtx SynapseMessageContext of which the fault occured message comprises
     * @throws SynapseException in case there is a failure in the fault execution
     */
    public void handleFault(MessageContext synCtx) throws SynapseException;
}
