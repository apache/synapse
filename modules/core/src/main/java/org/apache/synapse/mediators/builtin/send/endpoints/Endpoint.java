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

package org.apache.synapse.mediators.builtin.send.endpoints;

import org.apache.synapse.MessageContext;

public interface Endpoint {

    public void send(MessageContext synMessageContext);

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext);

    public void setParentEndpoint(Endpoint parentEndpoint);

    public String getName();

    public void setName(String name);

    public boolean isActive();

    public void setActive(boolean active);

    public void setDynamic(boolean dynamic);

    public void setRegistryKey(String registryKey);
}
