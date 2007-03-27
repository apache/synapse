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

/**
 *
 *
 */

public class LogMediatorSerializationTest extends AbstractTestCase {

    LogMediatorFactory logMediatorFactory;
    LogMediatorSerializer logMediatorSerializer;

    private static final String SIMPLE = "simple";
    private static final String HEADERS = "headers";
    private static final String FULL = "full";
    private static final String CUSTOM = "custom";

    public LogMediatorSerializationTest() {
        super(LogMediatorSerializationTest.class.getName());
        logMediatorFactory = new LogMediatorFactory();
        logMediatorSerializer = new LogMediatorSerializer();
    }

    public void testLogMediatorSerializationSenarioOne() throws Exception {

        //    assertTrue(serialization(getXmlOfMediatorScenarioOne(SIMPLE), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioOne(HEADERS), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioOne(FULL), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioOne(CUSTOM), logMediatorFactory, logMediatorSerializer));

//        assertTrue(serialization(getXmlOfMediatorScenarioOne(SIMPLE), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioOne(HEADERS), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioOne(FULL), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioOne(CUSTOM), logMediatorSerializer));


    }

    public void testLogMediatorSerializationScenarioTwo() throws Exception {

//        assertTrue(serialization(getXmlOfMediatorScenarioTwo(SIMPLE, ":"), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioTwo(HEADERS, ":"), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioTwo(FULL, ";"), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioTwo(CUSTOM, ":"), logMediatorFactory, logMediatorSerializer));

        //       assertTrue(serialization(getXmlOfMediatorScenarioTwo(SIMPLE, ":"), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioTwo(HEADERS, ":"), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioTwo(FULL, ";"), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorScenarioTwo(CUSTOM, ":"), logMediatorSerializer));


    }

    private String getXmlOfMediatorScenarioOne(String level) {
        return "<log xmlns=\"http://ws.apache.org/ns/synapse\" level=\"" + level + "\"><property name=\"Text\" value=\"Sending quote request\"/></log>";

    }

    private String getXmlOfMediatorScenarioTwo(String level, String seperator) {
        return "<log xmlns=\"http://ws.apache.org/ns/synapse\" level=\"" + level + "\" separator=\"" + seperator + "\"><property name=\"Text\" value=\"Sending quote request\"/></log>";

    }

}
