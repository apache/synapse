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

package org.apache.synapse.transport.testkit.server.axis2;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.engine.AbstractDispatcher;

/**
 * Dispatcher that falls back to a default operation. This class is useful
 * to implement "catch all" services that accept any kind of message.
 * It is similar to {@link org.apache.synapse.core.axis2.SynapseDispatcher}.
 */
public class DefaultOperationDispatcher extends AbstractDispatcher {
    public static final QName DEFAULT_OPERATION_NAME = new QName("__default__");
    
    @Override
    public void initDispatcher() {
        super.init(new HandlerDescription("DefaultOperationDispatcher"));
    }

    @Override
    public AxisService findService(MessageContext messageContext)
            throws AxisFault {
        return null;
    }

    @Override
    public AxisOperation findOperation(AxisService service,
            MessageContext messageContext) throws AxisFault {
        return service.getOperation(DEFAULT_OPERATION_NAME);
    }
}
