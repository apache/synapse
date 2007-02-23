/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
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

        //    assertTrue(serialization(getXmlOfMediatorSenarioOne(SIMPLE), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioOne(HEADERS), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioOne(FULL), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioOne(CUSTOM), logMediatorFactory, logMediatorSerializer));

//        assertTrue(serialization(getXmlOfMediatorSenarioOne(SIMPLE), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioOne(HEADERS), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioOne(FULL), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioOne(CUSTOM), logMediatorSerializer));


    }

    public void testLogMediatorSerializationSenarioTwo() throws Exception {

//        assertTrue(serialization(getXmlOfMediatorSenarioTwo(SIMPLE, ":"), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioTwo(HEADERS, ":"), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioTwo(FULL, ";"), logMediatorFactory, logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioTwo(CUSTOM, ":"), logMediatorFactory, logMediatorSerializer));

        //       assertTrue(serialization(getXmlOfMediatorSenarioTwo(SIMPLE, ":"), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioTwo(HEADERS, ":"), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioTwo(FULL, ";"), logMediatorSerializer));
        assertTrue(serialization(getXmlOfMediatorSenarioTwo(CUSTOM, ":"), logMediatorSerializer));


    }

    private String getXmlOfMediatorSenarioOne(String level) {
        return "<log xmlns=\"http://ws.apache.org/ns/synapse\" level=\"" + level + "\"><property name=\"Text\" value=\"Sending quote request\"/></log>";

    }

    private String getXmlOfMediatorSenarioTwo(String level, String seperator) {
        return "<log xmlns=\"http://ws.apache.org/ns/synapse\" level=\"" + level + "\" separator=\"" + seperator + "\"><property name=\"Text\" value=\"Sending quote request\"/></log>";

    }

}
