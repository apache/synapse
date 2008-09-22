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

package org.apache.synapse.transport.testkit.axis2.client;

import java.io.File;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.format.BinaryFormatter;
import org.apache.axis2.format.PlainTextFormatter;
import org.apache.synapse.transport.testkit.axis2.TransportDescriptionFactory;

public class AxisTestClientContext {
    public static final AxisTestClientContext INSTANCE = new AxisTestClientContext();
    
    private TransportOutDescription trpOutDesc;
    private ConfigurationContext cfgCtx;
    
    private AxisTestClientContext() {}
    
    @SuppressWarnings("unused")
    private void setUp(TransportDescriptionFactory tdf) throws Exception {
        cfgCtx =
            ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                    new File("target/test_rep").getAbsolutePath());
        AxisConfiguration axisCfg = cfgCtx.getAxisConfiguration();

        trpOutDesc = tdf.createTransportOutDescription();
        axisCfg.addTransportOut(trpOutDesc);
        trpOutDesc.getSender().init(cfgCtx, trpOutDesc);
        
        axisCfg.addMessageFormatter("text/plain", new PlainTextFormatter());
        axisCfg.addMessageFormatter("application/octet-stream", new BinaryFormatter());
    }
    
    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        trpOutDesc.getSender().stop();
        trpOutDesc = null;
        cfgCtx.terminate();
        cfgCtx = null;
    }

    public ConfigurationContext getConfigurationContext() {
        return cfgCtx;
    }
}
