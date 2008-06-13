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

package org.apache.synapse.mediators.json;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.json.JsonMediator;

public class JsonMediatorTest extends TestCase {
    private static final String ENV =
            "<m:someOtherElement xmlns:m=\"http://someother\">" +
            "<m0:CheckPriceRequest xmlns:m0=\"http://www.apache-synapse.org/test\">\n" +
            "<m0:Code>String</m0:Code>\n" +
            "</m0:CheckPriceRequest>" +
            "</m:someOtherElement>";

    private static final String JSON_OBJ =
            "{\"soapenv:Envelope\":{\"soapenv:Body\":{\"m:someOtherElement\":{\"m0:CheckPriceRequest\":{\"xmlns:m0\":\"http://www.apache-synapse.org/test\",\"m0:Code\":\"String\"},\"xmlns:m\":\"http://someother\"}},\"xmlns:soapenv\":\"http://schemas.xmlsoap.org/soap/envelope/\",\"soapenv:Header\":{}}}";

    private JsonMediator jsonMediator = null;
    public void testJsonMediator() throws Exception{
        jsonMediator = new JsonMediator();
        jsonMediator.setDirection("XTJ");

        // invoke transformation, with static enveope
        MessageContext synCtx = TestUtils.createLightweightSynapseMessageContext(ENV);
        jsonMediator.mediate(synCtx);
        org.apache.axis2.context.MessageContext mc =
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        assertNotNull(mc.getProperty("JSONObject"));
        String json_obj =mc.getProperty("JSONObject").toString();
        assertEquals(json_obj,JSON_OBJ);

    }
}
