/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.eip;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediatorTestCase;
import org.apache.synapse.mediators.base.SequenceMediator;

/**
 * Preperation of the split mediator test cases 
 */
public abstract class AbstractSplitMediatorTestCase extends AbstractMediatorTestCase {

    SplitTestHelperMediator helperMediator;
    MessageContext testCtx;
    MediatorFactory fac;

    protected void setUp() throws Exception {
        super.setUp();
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration config = new AxisConfiguration();
        testCtx = new Axis2MessageContext(new org.apache.axis2.context.MessageContext(),
            synCfg, new Axis2SynapseEnvironment(new ConfigurationContext(config), synCfg));
        ((Axis2MessageContext)testCtx).getAxis2MessageContext().setConfigurationContext(new ConfigurationContext(config));
        SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        envelope.getBody().addChild(createOMElement("<original>test-split-context</original>"));
        testCtx.setEnvelope(envelope);
        testCtx.setSoapAction("urn:test");
        SequenceMediator seqMed = new SequenceMediator();
        helperMediator = new SplitTestHelperMediator();
        helperMediator.init(testCtx.getEnvironment());
        seqMed.addChild(helperMediator);
        testCtx.getConfiguration().addSequence("seqRef", seqMed);
        testCtx.getConfiguration().addSequence("main", new SequenceMediator());
        testCtx.getConfiguration().addSequence("fault", new SequenceMediator());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        helperMediator.destroy();
        helperMediator = null;
        testCtx = null;
    }
}
