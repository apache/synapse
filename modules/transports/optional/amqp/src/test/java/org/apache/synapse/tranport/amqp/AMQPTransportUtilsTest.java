/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.tranport.amqp;

import com.rabbitmq.client.Address;
import junit.framework.TestCase;
import org.apache.axis2.description.AxisService;
import org.apache.synapse.transport.amqp.AMQPTransportConstant;
import org.apache.synapse.transport.amqp.AMQPTransportUtils;

import java.util.HashMap;
import java.util.Map;

public class AMQPTransportUtilsTest extends TestCase {

    private Map<String, String> svcMap = new HashMap<String, String>();

    private Map<String, String> cfMap = new HashMap<String, String>();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Do not edit the values, test may fail!
        svcMap.put(AMQPTransportConstant.PARAMETER_EXCHANGE_NAME, "directExchange");
        svcMap.put(AMQPTransportConstant.PARAMETER_EXCHANGE_INTERNAL, "true");
        svcMap.put(AMQPTransportConstant.PARAMETER_NO_OF_CONCURRENT_CONSUMERS, "2");

        cfMap.put(AMQPTransportConstant.PARAMETER_EXCHANGE_TYPE, "direct");
        cfMap.put(AMQPTransportConstant.PARAMETER_QUEUE_DURABLE, "true");
        cfMap.put(AMQPTransportConstant.PARAM_INITIAL_RE_CONNECTION_DURATION, "10");
    }

    public void testGetStringProperty() throws Exception {
        assertEquals("In valid string value received,",
                "username@domain.com", AMQPTransportUtils.getStringProperty("string", null));
    }

    public void testGetIntProperty() throws Exception {
        assertEquals("In valid int value received,", 10, AMQPTransportUtils.getIntProperty("int", -1));
    }

    public void testGetLongProperty() throws Exception {
        assertEquals("In valid long value received,", 13, AMQPTransportUtils.getLongProperty("long", -1));
    }

    public void testGetDoubleProperty() throws Exception {
        assertEquals("In valid double value received,", 14.4, AMQPTransportUtils.getDoubleProperty("double", -1));
    }

    public void testGetBooleanProperty() throws Exception {
        assertEquals(
                "In valid boolean value received,",
                false,
                AMQPTransportUtils.getBooleanProperty("boolean2", true).booleanValue());

        assertEquals(
                "In valid boolean value received,",
                true,
                AMQPTransportUtils.getBooleanProperty("boolean1", false).booleanValue());
    }

    public void testGetServiceStringParameters() throws Exception {
        AxisService service = new AxisService();
        service.addParameter("param1", "value1");

        Map<String, String> paramMap =
                AMQPTransportUtils.getServiceStringParameters(service.getParameters());
        assertEquals("In valid parameter for key param1", "value1", paramMap.get("param1"));
    }

    public void testGetOptionalStringParameter() throws Exception {
        assertEquals(
                "In valid value received",
                svcMap.get(AMQPTransportConstant.PARAMETER_EXCHANGE_NAME),
                AMQPTransportUtils.getOptionalStringParameter(
                        AMQPTransportConstant.PARAMETER_EXCHANGE_NAME, svcMap, cfMap));

        assertEquals(
                "In valid value received",
                cfMap.get(AMQPTransportConstant.PARAMETER_EXCHANGE_TYPE),
                AMQPTransportUtils.getOptionalStringParameter(
                        AMQPTransportConstant.PARAMETER_EXCHANGE_TYPE, svcMap, cfMap));
    }

    public void testGetOptionalBooleanParameter() throws Exception {
        assertEquals("Invalid value",
                Boolean.valueOf(svcMap.get(AMQPTransportConstant.PARAMETER_EXCHANGE_INTERNAL)),
                AMQPTransportUtils.getOptionalBooleanParameter(
                        AMQPTransportConstant.PARAMETER_EXCHANGE_INTERNAL, svcMap, cfMap));

        assertEquals("Invalid value",
                Boolean.valueOf(cfMap.get(AMQPTransportConstant.PARAMETER_QUEUE_DURABLE)),
                AMQPTransportUtils.getOptionalBooleanParameter(
                        AMQPTransportConstant.PARAMETER_QUEUE_DURABLE, svcMap, cfMap));
    }

    public void testGetOptionalIntParameter() throws Exception {
        assertEquals("Invalid value",
                Integer.parseInt(cfMap.get(AMQPTransportConstant.PARAM_INITIAL_RE_CONNECTION_DURATION)),
                AMQPTransportUtils.getOptionalIntParameter(
                        AMQPTransportConstant.PARAM_INITIAL_RE_CONNECTION_DURATION, svcMap, cfMap).intValue());

        assertEquals("Invalid value",
                Integer.parseInt(svcMap.get(AMQPTransportConstant.PARAMETER_NO_OF_CONCURRENT_CONSUMERS)),
                AMQPTransportUtils.getOptionalIntParameter(
                        AMQPTransportConstant.PARAMETER_NO_OF_CONCURRENT_CONSUMERS, svcMap, cfMap).intValue());
    }

    public void testGetBindingKeys() throws Exception {
        String keys[] = AMQPTransportUtils.split("ERROR,WARN,DEBUG", ",");
        assertEquals("Invalid value", "ERROR", keys[0]);
        assertEquals("Invalid value", "WARN", keys[1]);
        assertEquals("Invalid value", "DEBUG", keys[2]);
    }

    public void testGetAddressArray() throws Exception {
        try {
            Address[] addresses1 = AMQPTransportUtils.getAddressArray(
                    "wso2.org:443,rajika.org:25", ",", ':');
            assertEquals("Invalid value", "wso2.org", addresses1[0].getHost());
            assertEquals("Invalid value", 25, addresses1[1].getPort());
            assertEquals("Invalid value", "rajika.org", addresses1[1].getHost());
        } catch (NumberFormatException e) {
            fail("Should not throw an exception, " + e.getMessage());
        }

        try {
            AMQPTransportUtils.getAddressArray(
                    "hostName1:443,hostName2:25,hostName3:invalidPort", ",", ':');
            fail("Should not come here because above should throw an exception");
        } catch (NumberFormatException e) {
            // expected
        }
    }

    public void testParseAMQPUri() throws Exception {
        String url = "amqp://SimpleStockQuoteService?transport.amqp.ConnectionFactoryName=producer&transport.amqp.QueueName=producer" ;
        Map<String, String> uriParam = AMQPTransportUtils.parseAMQPUri(url);
        assertEquals("Invalid value", "producer", uriParam.get(AMQPTransportConstant.PARAMETER_QUEUE_NAME));

    }
}
