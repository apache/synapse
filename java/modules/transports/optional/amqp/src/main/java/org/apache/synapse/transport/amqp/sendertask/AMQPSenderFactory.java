/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.synapse.transport.amqp.sendertask;

import com.rabbitmq.client.Channel;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.amqp.AMQPTransportConstant;
import org.apache.synapse.transport.amqp.AMQPTransportException;
import org.apache.synapse.transport.amqp.AMQPTransportUtils;
import org.apache.synapse.transport.amqp.connectionfactory.AMQPTransportConnectionFactory;
import org.apache.synapse.transport.amqp.connectionfactory.AMQPTransportConnectionFactoryManager;

import java.io.IOException;
import java.util.Map;

public class AMQPSenderFactory {

    private static Log log = LogFactory.getLog(AMQPSenderFactory.class);

    public synchronized static AMQPSender createAMQPSender(
            AMQPTransportConnectionFactoryManager connectionFactoryManager,
            Map<String, String> params) throws IOException {

        boolean isQueueDurable = false;

        boolean isQueueRestricted = false;

        boolean isQueueAutoDelete = true;

        String exchangeType;

        boolean isExchangeDurable = false;

        boolean isExchangeAutoDelete = true;

        boolean isInternalExchange = false;

        String exchangeName;

        Channel channel;

        AMQPSender as = new AMQPSender();

        AMQPTransportConnectionFactory connFac;

        try {
            connFac = connectionFactoryManager.getConnectionFactory(
                    params.get(AMQPTransportConstant.PARAMETER_CONNECTION_FACTORY_NAME));
            channel = connFac.getChannel();
            as.setChannel(channel);
        } catch (AMQPTransportException e) {
            throw new AxisFault("Could not retrieve the channel", e);
        }

        exchangeName = params.get(AMQPTransportConstant.PARAMETER_EXCHANGE_NAME);
        if (exchangeName != null) {
            as.setExchangeName(exchangeName);
        }
        exchangeType = params.get(AMQPTransportConstant.PARAMETER_EXCHANGE_TYPE);
        if (exchangeType == null) {
            exchangeType = "direct";
        }

        Map<String, String> conFacParam = connFac.getParameters();

        Boolean value;
        value = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_EXCHANGE_IS_DURABLE, params, conFacParam);
        if (value != null) {
            isExchangeDurable = value.booleanValue();
        }

        value = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_EXCHANGE_IS_AUTO_DELETE, params, conFacParam);
        if (value != null) {
            isExchangeAutoDelete = value.booleanValue();
        }

        value = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_EXCHANGE_INTERNAL, params, conFacParam);
        if (value != null) {
            isInternalExchange = value.booleanValue();
        }

        String queueName = params.get(AMQPTransportConstant.PARAMETER_QUEUE_NAME);
        as.setQueueName(queueName);

        String routingKey = AMQPTransportUtils.getOptionalStringParameter(
                AMQPTransportConstant.PARAMETER_ROUTING_KEY, params, conFacParam);
        if (routingKey != null) {
            as.setRoutingKey(routingKey);
        }

        value = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_QUEUE_DURABLE, params, conFacParam);
        if (value != null) {
            isQueueDurable = value.booleanValue();
        }

        value = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_QUEUE_RESTRICTED, params, conFacParam);
        if (value != null) {
            isQueueRestricted = value.booleanValue();
        }

        value = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_QUEUE_AUTO_DELETE, params, conFacParam);
        if (value != null) {
            isQueueAutoDelete = value.booleanValue();
        }

        /* use available, otherwise declare
        if (exchangeName != null) {
            channel.exchangeDeclare(
                    exchangeName,
                    exchangeType,
                    isExchangeDurable,
                    isExchangeAutoDelete,
                    isInternalExchange,
                    null);
        } else {
            channel.queueDeclare(queueName, isQueueDurable, isQueueRestricted, isQueueAutoDelete, null);
        }
        */

        return as;
    }
}
