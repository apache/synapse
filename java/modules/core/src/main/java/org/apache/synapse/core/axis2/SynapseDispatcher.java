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

package org.apache.synapse.core.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;

/**
 * This is the Axis2 Dispatcher which is registered with the Axis2 engine. It dispatches
 * each and every message received to the SynapseMessageReceiver for processing.
 */
public class SynapseDispatcher extends AbstractDispatcher {

    private static final Log log = LogFactory.getLog(SynapseDispatcher.class);

    private static final long serialVersionUID = -6970206989111592645L;

    private static final String SYNAPSE_SERVICE_NAME = "synapse";

    private static final QName MEDIATE_OPERATION_NAME = new QName("mediate");

    public void initDispatcher() {
        QName qn = new QName("http://synapse.apache.org", "SynapseDispatcher");
        HandlerDescription hd = new HandlerDescription(qn.getLocalPart());
        super.init(hd);
    }

    public AxisService findService(MessageContext mc) throws AxisFault {
        AxisConfiguration ac = mc.getConfigurationContext().getAxisConfiguration();
        AxisService as = ac.getService(SYNAPSE_SERVICE_NAME);
        return as;
    }

    public AxisOperation findOperation(AxisService svc, MessageContext mc) throws AxisFault {
        AxisOperation ao = svc.getOperation(MEDIATE_OPERATION_NAME);
        return ao;
    }
}
