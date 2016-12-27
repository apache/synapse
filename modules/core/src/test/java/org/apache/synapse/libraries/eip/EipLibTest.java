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

package org.apache.synapse.libraries.eip;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.InvokeMediatorFactory;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.libraries.imports.SynapseImport;
import org.apache.synapse.libraries.model.Library;
import org.apache.synapse.libraries.util.LibDeployerUtils;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.template.InvokeMediator;

import java.util.Properties;

/**
 * Tests for synapse template invoke
 */
public class EipLibTest extends AbstractEipLibTestCase {
    private MessageContext synCtx;


    protected void setUp() throws Exception {
        super.setUp();
        Library library = LibDeployerUtils.createSynapseLibrary(getResourcePath());
        SynapseImport validSynImport = new SynapseImport();
        validSynImport.setLibName("EipLibrary");
        validSynImport.setLibPackage("synapse.lang.eip");
        if (validSynImport != null) {
            LibDeployerUtils.loadLibArtifacts(validSynImport, library);
        }
        assertEquals("EipLibrary", library.getQName().getLocalPart());
        assertEquals("synapse.lang.eip", library.getPackage());
        assertEquals("eip synapse library", library.getDescription());
        assertNotNull(library.getArtifact("synapse.lang.eip.splitter"));
        //setting up synapse context & configuration
        SynapseConfiguration synConf = new SynapseConfiguration();
        synConf.addSynapseLibrary(library.toString(), library);
        synConf.addSequence("main", new SequenceMediator());
        synConf.addSequence("fault", new SequenceMediator());
        AxisConfiguration config = new AxisConfiguration();
        synCtx = new Axis2MessageContext(new org.apache.axis2.context.MessageContext(),
                synConf, new Axis2SynapseEnvironment(new ConfigurationContext(config), synConf));
        //((Axis2MessageContext)synCtx).getAxis2MessageContext().setConfigurationContext(new ConfigurationContext(config));
        SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        envelope.getBody().addChild(SynapseConfigUtils.stringToOM("<original><itr><a>IBM</a></itr><itr><a>DELL</a></itr></original>"));
        synCtx.setEnvelope(envelope);
        SequenceMediator seqMed = new SequenceMediator();
        synCtx.getConfiguration().addSequence("seqRef", seqMed);

    }


    public void testValidEipLibInvoke() throws Exception {

        //Invoke Template
        MediatorFactory fac = new InvokeMediatorFactory();

        InvokeMediator iterate = (InvokeMediator) fac.createMediator(SynapseConfigUtils.stringToOM("<call-template xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "target=\"synapse.lang.eip.splitter\">" +
                "<with-param xmlns=\"http://ws.apache.org/ns/synapse\" name=\"iterate_exp\" value=\"{{//original/itr}}\"/>" +
                "<with-param xmlns=\"http://ws.apache.org/ns/synapse\" name=\"endpoint_uri\" value=\"http://localhost:9000/services/IterateTestService\"/>" +
                "<with-param xmlns=\"http://ws.apache.org/ns/synapse\" name=\"sequence_ref\" value=\"seqRef\"/>" +
                "</call-template>"), new Properties());

        boolean returnValue = iterate.mediate(synCtx);

        //Test Template Parameters
        assertEquals("<itr><a>IBM</a></itr><itr><a>DELL</a></itr>", synCtx.getProperty("ItrExp"));
        assertEquals("http://localhost:9000/services/IterateTestService", synCtx.getProperty("EndPUri"));
        assertEquals("<original><itr><a>IBM</a></itr><itr><a>DELL</a></itr></original>", synCtx.getProperty("AttachPath"));
        assertEquals("seqRef", synCtx.getProperty("SRef"));

        //Test Template invoke & mediation
        assertTrue(returnValue);
        assertEquals("", synCtx.getProperty("Endpoint_1"));
        assertEquals("http://localhost:9000/services/IterateTestService", synCtx.getProperty("Endpoint_2"));
    }


}
