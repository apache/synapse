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

package org.apache.synapse.format.hessian;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.activation.DataHandler;

import junit.framework.TestCase;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.commons.io.IOUtils;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.util.SynapseBinaryDataSource;

/**
 * Test the HessianMessageBuilder using several Hessian example
 * messages (including normal and fault messages) provided as 
 * test resources.
 */
public class HessianMessageBuilderTest extends TestCase {

    private static final String HESSIAN_DUMMY_FAULT_V1_RESPONSE = "hessianDummyFaultResponse_V1.bin";

    private static final String HESSIAN_DUMMY_REQUEST = "hessianDummyRequest.bin";

    private static final String HESSIAN_INCOMPLETE = "hessianIncomplete.bin";
    
    public void testProcessDocumentFaultWithSynEnv() throws IOException {
        SynapseEnvironment synEnv = new Axis2SynapseEnvironment(new ConfigurationContext(
                new AxisConfiguration()), new SynapseConfiguration());
        testProcessDocumentFault(synEnv);
    }

    public void testProcessDocumentFaultWithoutSynEnv() throws IOException {
        testProcessDocumentFault(null);
    }

    public void testProcessDocumentWithSynEnv() throws IOException {
        SynapseEnvironment synEnv = new Axis2SynapseEnvironment(new ConfigurationContext(
                new AxisConfiguration()), new SynapseConfiguration());
        testProcessDocument(synEnv);
    }

    public void testProcessDocumentWithoutSynEnv() throws IOException {
        testProcessDocument(null);
    }
    
    public void testIncompleteHessianMessage() throws IOException {
        test(HESSIAN_INCOMPLETE, null);
    }
    
    private MessageContext test(String testMessageName, SynapseEnvironment synEnv)
            throws IOException {

        // create mock message context
        MessageContext msgContext = new MessageContext();
        AxisConfiguration axisConfig = new AxisConfiguration();
        ConfigurationContext configContext = new ConfigurationContext(axisConfig);
        axisConfig.addParameter(SynapseConstants.SYNAPSE_ENV, synEnv);
        msgContext.setConfigurationContext(configContext);

        HessianMessageBuilder messageBuilder = new HessianMessageBuilder();
        InputStream is = getClass().getResourceAsStream(testMessageName);
        OMElement element = messageBuilder.processDocument(is,
                HessianConstants.HESSIAN_CONTENT_TYPE, msgContext);
        OMNode hessianNode = element.getFirstOMChild();
        OMText hessianTextNode = (OMText) hessianNode;
        SynapseBinaryDataSource synapseBinaryDataSource = (SynapseBinaryDataSource) ((DataHandler) hessianTextNode
                .getDataHandler()).getDataSource();
        InputStream inputStream = synapseBinaryDataSource.getInputStream();
        byte[] originalByteArray = IOUtils.toByteArray(getClass().getResourceAsStream(
                testMessageName));
        byte[] builderByteArray = IOUtils.toByteArray(inputStream);
        assertTrue(Arrays.equals(originalByteArray, builderByteArray));

        return msgContext;
    }

    private void testProcessDocumentFault(SynapseEnvironment synEnv) throws IOException {

        MessageContext axis2MessageContext = test(HESSIAN_DUMMY_FAULT_V1_RESPONSE, synEnv);
        assertEquals(SynapseConstants.TRUE, axis2MessageContext
                .getProperty(BaseConstants.FAULT_MESSAGE));
    }

    private void testProcessDocument(SynapseEnvironment synEnv) throws IOException {

        MessageContext axis2MessageContext = test(HESSIAN_DUMMY_REQUEST, synEnv);
        assertNull(axis2MessageContext.getProperty(BaseConstants.FAULT_MESSAGE));
    }

}
