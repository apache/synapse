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

package org.apache.axis2.transport.http;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.synapse.transport.testkit.SimpleTransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.TransportTestSuite;
import org.apache.synapse.transport.testkit.http.HttpTransportTestSuiteBuilder;

public class SimpleHTTPServerTest extends TestCase {
    public static TestSuite suite() throws Exception {
        TransportTestSuite suite = new TransportTestSuite(SimpleHTTPServerTest.class);
        
        TransportDescriptionFactory tdf =
            new SimpleTransportDescriptionFactory("http", SimpleHTTPServer.class, 
                                                  CommonsHTTPTransportSender.class) {

            @Override
            public TransportInDescription createTransportInDescription() throws Exception {
                TransportInDescription desc = super.createTransportInDescription();
                desc.addParameter(new Parameter(SimpleHTTPServer.PARAM_PORT, "8280"));
                return desc;
            }
        };
        
        new HttpTransportTestSuiteBuilder(suite, tdf).build();
        
        return suite;
    }
}
