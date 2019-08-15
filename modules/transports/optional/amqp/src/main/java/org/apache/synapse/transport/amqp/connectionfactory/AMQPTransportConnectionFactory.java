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
package org.apache.synapse.transport.amqp.connectionfactory;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterIncludeImpl;
import org.apache.synapse.transport.amqp.AMQPTransportConstant;
import org.apache.synapse.transport.amqp.AMQPTransportException;
import org.apache.synapse.transport.amqp.AMQPTransportUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * This wrap the connection factory definition in axis2.xml. See below for an example definition.
 * <pre>
 * {@code
 * <transportReceiver name="amqp" class="org.wso2.carbon.transports.amqp.AMQPTransportListener">
 * <parameter name="example-connection-factory1" locked="false">
 *      <parameter name="transport.amqp.Uri" locked="false">amqp://userName:password@hostName:portNumber/virtualHost</parameter>
 *      <parameter name="transport.amqp.BrokerList" locked="false">hostName1:portNumber1,hostName2:portNumber2,hostName3:portNumber3</parameter>
 * </parameter>
 *
 * <parameter name="example-connection-factory2" locked="false">
 *     <parameter name="example-connection-factory1" locked="false">
 *     <parameter name="transport.amqp.Uri" locked="false">amqp://userName:password@hostName:portNumber/virtualHost</parameter>
 *     <parameter name="transport.amqp.BrokerList" locked="false">hostName1:portNumber1,hostName2:portNumber2,hostName3:portNumber3</parameter>
 * </parameter>
 *
 * <parameter name="default" locked="false">
 *     <parameter name="example-connection-factory1" locked="false">
 *     <parameter name="transport.amqp.Uri" locked="false">amqp://userName:password@hostName:portNumber/virtualHost</parameter>
 *     <parameter name="transport.amqp.BrokerList" locked="false">hostName1:portNumber1,hostName2:portNumber2,hostName3:portNumber3</parameter>
 * </parameter>
 * </transportReceiver>
 * }
 * </pre>
 */
public class AMQPTransportConnectionFactory {

    /**
     * The name of the connection factory definition.
     */
    private String name = null;

    /**
     * The list of parameters(see above) in the connection factory definition.
     */
    private Map<String, String> parameters = new HashMap<String, String>();

    /**
     * The AMQP connection to the broker maintain per connection factory.
     */
    private Connection connection = null;

    /**
     * The AMQP channel for this connection, maintains per connection.
     */
    private Channel channel = null;

    public AMQPTransportConnectionFactory(
            Map<String, String> parameters,
            ExecutorService es)
            throws AMQPTransportException {
        try {
            connection = createConnection(es, parameters);
            channel = createChannel(connection, parameters);
        } catch (Exception e) {
            String msg = "Could not initialize the connection factory with parameters\n";
            for (Map.Entry entry : parameters.entrySet()) {
                msg = msg + entry.getKey() + ":" + entry.getValue() + "\n";
            }
            throw new AMQPTransportException(msg, e);
        }
    }

    public AMQPTransportConnectionFactory(Parameter parameter, ExecutorService es)
            throws AMQPTransportException {
        try {
            this.name = parameter.getName();

            ParameterIncludeImpl pi = new ParameterIncludeImpl();

            if (!(parameter.getValue() instanceof OMElement)) {
                throw new AMQPTransportException("The connection factory '" + parameter.getName() +
                        "' is invalid. It's required to have the least connection factory definition with '" +
                        AMQPTransportConstant.PARAMETER_CONNECTION_URI + "' parameter. Example: \n" +
                        "\n<transportReceiver name=\"amqp\" class=\"org.wso2.carbon.transports.amqp.AMQPTransportListener\">\n" +
                        "   <parameter name=\"default\" locked=\"false\">\n" +
                        "      <parameter name=\"transport.amqp.Uri\" locked=\"false\">amqp://rajika:rajika123@localhost:5672/default</parameter>\n" +
                        "   </parameter>\n" +
                        "</transportReceiver>\n");
            }
            try {
                pi.deserializeParameters(parameter.getParameterElement());
            } catch (AxisFault axisFault) {
                throw new AMQPTransportException("Error reading connection factory configuration from '" +
                        parameter.getName() + "'", axisFault);
            }

            for (Parameter entry : pi.getParameters()) {
                parameters.put(entry.getName(), (String) entry.getValue());
            }

            connection = createConnection(es, parameters);
            channel = createChannel(connection, parameters);

        } catch (Exception e) {
            throw new AMQPTransportException("" +
                    "Could not initialize the connection factory '" + parameter.getName() + "'", e);
        }
    }

    /**
     * Shutdown this connection.
     *
     * @throws IOException
     */
    public void shutDownConnection() throws IOException {
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }

    /**
     * Shutdown this channel.
     *
     * @throws IOException
     */
    public void shutDownChannel() throws IOException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    /**
     * Get the channel in this connection.
     *
     * @return channel associated with this
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Get the connection
     * @return the connection to broker.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Return the name of this connection factory(the name given in axis2.xml)
     *
     * @return name of this connection factory
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value of parameter.
     *
     * @param parameterName name of the parameter.
     * @return the value of the parameter.
     */
    public String getParameterValue(final String parameterName) {
        return parameters.get(parameterName);
    }

    /**
     * Returns the list of all parameters in this connection factory.
     *
     * @return the list of parameters.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    private Connection createConnection(ExecutorService es, Map<String, String> parameters)
            throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setUri(parameters.get(AMQPTransportConstant.PARAMETER_CONNECTION_URI));

        if (parameters.get(AMQPTransportConstant.PARAMETER_BROKER_LIST) != null) {
            Address[] addresses = AMQPTransportUtils.getAddressArray(
                    parameters.get(AMQPTransportConstant.PARAMETER_BROKER_LIST), ",", ':');
            return connectionFactory.newConnection(es, addresses);
        }
        return connectionFactory.newConnection(es);
    }

    private Channel createChannel(Connection connection, Map<String, String> parameters)
            throws IOException {
        Channel ch;
        if (parameters.get(AMQPTransportConstant.PARAMETER_AMQP_CHANNEL_NUMBER) != null) {
            int index = 0;
            try {
                index = Integer.parseInt(parameters.get(
                        AMQPTransportConstant.PARAMETER_AMQP_CHANNEL_NUMBER));
            } catch (NumberFormatException e) {
                index = 1; // assume default,
                // fair dispatch see http://www.rabbitmq.com/tutorials/tutorial-two-java.html
            }
            ch = connection.createChannel(index);

        } else {
            ch = connection.createChannel();
        }

        int prefetchSize = 1024;
        if (parameters.get(AMQPTransportConstant.PARAMETER_CHANNEL_PREFETCH_SIZE) != null) {
            try {
                prefetchSize = Integer.parseInt(
                        parameters.get(AMQPTransportConstant.PARAMETER_CHANNEL_PREFETCH_SIZE));
            } catch (NumberFormatException e) {
                prefetchSize = 1024; // assume default
            }
        }

        int prefetchCount = 0;
        if (parameters.get(AMQPTransportConstant.PARAMETER_CHANNEL_PREFETCH_COUNT) != null) {
            try {
                prefetchCount = Integer.parseInt(
                        parameters.get(AMQPTransportConstant.PARAMETER_CHANNEL_PREFETCH_COUNT));
                ch.basicQos(prefetchCount);
            } catch (NumberFormatException e) {
                prefetchCount = 0; // assume default
            }
        }

        boolean useGlobally = false;
        if (parameters.get(AMQPTransportConstant.PARAMETER_CHANNEL_QOS_GLOBAL) != null) {
            useGlobally = Boolean.parseBoolean(parameters.get(
                    AMQPTransportConstant.PARAMETER_CHANNEL_QOS_GLOBAL));
        }
        return ch;
    }
}