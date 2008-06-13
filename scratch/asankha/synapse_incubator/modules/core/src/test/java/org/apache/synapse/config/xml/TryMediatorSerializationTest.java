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

package org.apache.synapse.config.xml;

public class TryMediatorSerializationTest extends AbstractTestCase {

    private TryMediatorFactory tryMediatorFactory = null;
    private TryMediatorSerializer tryMediatorSerializer = null;

    public TryMediatorSerializationTest() {
        tryMediatorFactory = new TryMediatorFactory();
        tryMediatorSerializer = new TryMediatorSerializer();
    }

    public void testTryMediatorSerializationScenarioOne() {

        String tryConfiguration = "<syn:try xmlns:syn=\"http://ws.apache.org/ns/synapse\">" +
                "<syn:sequence><syn:send/></syn:sequence>" +
                "<syn:onError><syn:drop/></syn:onError>" +
                "<syn:finally><syn:send/></syn:finally>" +
                "</syn:try>";

        try {
            assertTrue(serialization(tryConfiguration, tryMediatorFactory, tryMediatorSerializer));
        } catch (Exception e) {
            fail("Exception in test");
        }
    }

    public void testTryMediatorSerializationScenarioTwo() {

        String tryConfiguration = "<syn:try xmlns:syn=\"http://ws.apache.org/ns/synapse\">" +
                "<syn:sequence><syn:sequence ref=\"MySequence1\" /></syn:sequence>" +
                "<syn:onError><syn:sequence ref=\"MySequence2\" /></syn:onError>" +
                "<syn:finally><syn:sequence ref=\"MySequence3\" /></syn:finally>" +
                "</syn:try>";

        try {
            assertTrue(serialization(tryConfiguration, tryMediatorFactory, tryMediatorSerializer));
        } catch (Exception e) {
            fail("Exception in test");
        }
    }
}
