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

import org.apache.axiom.om.impl.exception.XMLComparisonException;

/**
 *
 *
 */

public class POXMediatorSerializationTest extends AbstractTestCase {

    POXMediatorFactory mediatorFactory;
    POXMediatorSerializer mediatorSerializer;


    public POXMediatorSerializationTest() {
        this.mediatorFactory = new POXMediatorFactory();
        this.mediatorSerializer = new POXMediatorSerializer();
    }

    public void testPOXMediatorSerializationSenarioOne() throws XMLComparisonException {
        String inptXml = " <pox value=\"true\" xmlns=\"http://ws.apache.org/ns/synapse\"></pox>";
        assertTrue(serialization(inptXml, mediatorFactory, mediatorSerializer));
        assertTrue(serialization(inptXml, mediatorSerializer));
    }

    public void testPOXMediatorSerializationSenarioTwo() throws XMLComparisonException {
        String inptXml = " <pox value=\"false\" xmlns=\"http://ws.apache.org/ns/synapse\"></pox>";
        assertTrue(serialization(inptXml, mediatorFactory, mediatorSerializer));
        assertTrue(serialization(inptXml, mediatorSerializer));
    }
}
