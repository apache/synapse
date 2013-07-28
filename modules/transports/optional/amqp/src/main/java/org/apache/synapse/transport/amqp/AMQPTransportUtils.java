/*
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
package org.apache.synapse.transport.amqp;

import com.rabbitmq.client.Address;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

/**
 * Contains some utility methods for the AMQP transport implementation
 */
public final class AMQPTransportUtils {

    private static final Log log = LogFactory.getLog(AMQPTransportUtils.class);

    private static Properties prop;

    static {
        prop = loadProperties("amqp-transport.properties");
    }

    private static Properties loadProperties(String filePath) {
        Properties properties = new Properties();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        if (log.isDebugEnabled()) {
            log.debug("Loading a file '" + filePath + "' from classpath");
        }

        InputStream in = cl.getResourceAsStream(filePath);
        if (in == null) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to load file  ' " + filePath + " '");
            }

            filePath = "repository/conf" +
                    File.separatorChar + filePath;
            if (log.isDebugEnabled()) {
                log.debug("Loading a file '" + filePath + "' from classpath");
            }

            in = cl.getResourceAsStream(filePath);
            if (in == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to load file  ' " + filePath + " '");
                }
            }
        }
        if (in != null) {
            try {
                properties.load(in);
            } catch (IOException e) {
                String msg = "Error loading properties from a file at :" + filePath;
                log.error(msg, e);
            }
        }
        return properties;
    }

    /**
     * Read the value of a string property.
     *
     * @param name name of the property.
     * @param def  default value.
     * @return the string value
     * @throws NumberFormatException in case invalid property value.
     */
    public static String getStringProperty(String name, String def) throws NumberFormatException {
        String val = System.getProperty(name);
        return val == null ?
                (prop.get(name) == null ? def : (String) prop.get(name)) :
                val;
    }

    /**
     * Read the value of a int property.
     *
     * @param name name of the property.
     * @param def  default value if no value is found.
     * @return the property value.
     * @throws NumberFormatException in case of an invalid property value.
     */
    public static int getIntProperty(String name, int def) throws NumberFormatException {
        String val = System.getProperty(name);
        return val == null ?
                (prop.get(name) == null ? def : Integer.parseInt((String) prop.get(name))) :
                Integer.parseInt(val);
    }

    /**
     * Read the value of a property of type long
     *
     * @param name name of the property
     * @param def  value of the property
     * @return value of property
     * @throws NumberFormatException throws in case of an error.
     */
    public static long getLongProperty(String name, long def) throws NumberFormatException {
        String val = System.getProperty(name);
        return val == null ?
                (prop.get(name) == null ? def : Long.parseLong((String) prop.get(name))) :
                Long.parseLong(val);
    }

    public static double getDoubleProperty(String name, double def) throws NumberFormatException {
        String val = System.getProperty(name);
        return val == null ?
                (prop.get(name) == null ? def : Double.parseDouble((String) prop.get(name))) :
                Double.parseDouble(val);
    }

    public static Boolean getBooleanProperty(String name, boolean def) throws NumberFormatException {
        String val = System.getProperty(name);
        return val == null ?
                (prop.get(name) == null ? def : Boolean.parseBoolean((String) prop.get(name))) :
                Boolean.parseBoolean(val);
    }

    public static Map<String, String> getServiceStringParameters(List<Parameter> list) {
        Map<String, String> map = new HashMap<String, String>();
        for (Parameter p : list) {
            if (p.getValue() instanceof String) {
                map.put(p.getName(), (String) p.getValue());
            }
        }
        return map;
    }

    public static String getOptionalStringParameter(String key, Map<String, String> srcMap1,
                                                    Map<String, String> srcMap2) {

        String value = srcMap1.get(key);
        if (value == null) {
            value = srcMap2.get(key);
        }
        return value;
    }

    public static Boolean getOptionalBooleanParameter(String key, Map<String, String> srcMap1,
                                                      Map<String, String> srcMap2) {
        String value = srcMap1.get(key);
        if (value == null) {
            value = srcMap2.get(key);
        }
        if (value == null) {
            return null;
        } else {
            return Boolean.valueOf(value);
        }
    }

    public static Integer getOptionalIntParameter(String key,
                                                  Map<String, String> srcMap1,
                                                  Map<String, String> srcMap2)
            throws AMQPTransportException {
        String value = srcMap1.get(key);
        if (value == null) {
            value = srcMap2.get(key);
        }
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new AMQPTransportException(
                        "Invalid value '" + value + "' for the key '" + key + "'");
            }
        }
        return null;
    }

    public static Double getOptionalDoubleParameter(String key,
                                                    Map<String, String> srcMap1,
                                                    Map<String, String> srcMap2)
            throws AMQPTransportException {
        String value = srcMap1.get(key);
        if (value == null) {
            value = srcMap2.get(key);
        }
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new AMQPTransportException(
                        "Invalid value '" + value + "' for the key '" + key + "'");
            }
        }
        return null;
    }

    public static String[] split(String src, final String delimiter) {
        return src.split(delimiter);
    }

    /**
     * Digest the address array in the form hostName1:portNumber1,hostName2:portNumber2,hostName3:portNumber3
     *
     * @param addressString Address array string
     * @param regex         the first regex to split the string
     * @param subRegex      the sub regex to split the string
     * @return the address array
     * @throws NumberFormatException in case an invalid port.
     */
    public static Address[] getAddressArray(String addressString,
                                            final String regex,
                                            final char subRegex) throws NumberFormatException {
        String[] hosts = addressString.split(regex);
        Address[] addresses = new Address[hosts.length];
        for (int i = 0; i < hosts.length; i++) {
            addresses[i] = new Address(
                    hosts[i].substring(0, hosts[i].indexOf(subRegex)),
                    Integer.parseInt(hosts[i].substring(hosts[i].indexOf(subRegex) + 1)));

        }
        return addresses;
    }

    /**
     * Move elements between buffers. No need of additional synchronization locks,
     * BlockingQueue#drainTo is thread safe, but not atomic, which is not a problem.
     * See {@link BlockingQueue#drainTo(java.util.Collection, int)}
     *
     * @param src       source buffer
     * @param dest      destination buffer
     * @param blockSize blockSize of message bulk that need to move
     * @throws AMQPTransportException in case of drains fails
     */

    public static void moveElements(BlockingQueue<AMQPTransportMessage> src,
                                    List<AMQPTransportMessage> dest,
                                    final int blockSize) throws AMQPTransportException {
        try {
            src.drainTo(dest, blockSize);
        } catch (Exception e) {
            throw new AMQPTransportException(e.getMessage(), e);
        }
    }

    public static Map<String, String> parseAMQPUri(String amqpUri) throws AMQPTransportException {
        // amqp://SimpleStockQuoteService?transport.amqp.ConnectionFactoryName=producer&amp;transport.amqp.QueueName=producer

        // amqp epr has the following format
        // amqp://[string]?key1=value1&key2=value2&key3=value*
        // valid epr definitions
        // amqp://SimpleStockQuoteService?transport.amqp.ConnectionFactoryName=producer
        // amqp://?transport.amqp.ConnectionFactoryName=producer&transport.amqp.QueueName=producer
        // amqp://SimpleStockQuoteService?transport.amqp.ConnectionFactoryName=producer&transport.amqp.QueueName=producer

        // the parameter 'transport.amqp.QueueName' has high precedence over the value given between
        // amqp:// and ?, if the parameter transport.amqp.QueueName is missing consider the value
        // between amqp:// and ?, as the queue/exchange name

        Map<String, String> params = new HashMap<String, String>();
        String svcName = amqpUri.substring(7, amqpUri.indexOf('?'));
        String kv = amqpUri.substring(amqpUri.indexOf('?') + 1);
        String[] values = kv.split("&");

        for (String str : values) {
            str  = str.trim();
            params.put(str.substring(0, str.indexOf('=')), str.substring(str.indexOf('=') + 1));
        }
        if (!params.keySet().contains(AMQPTransportConstant.PARAMETER_QUEUE_NAME)) {
            params.put(AMQPTransportConstant.PARAMETER_QUEUE_NAME, svcName);
        }

        return params;
    }
}