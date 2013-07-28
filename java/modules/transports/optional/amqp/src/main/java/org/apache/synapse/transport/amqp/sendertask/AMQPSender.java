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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.synapse.transport.amqp.AMQPTransportConstant;
import org.apache.synapse.transport.amqp.AMQPTransportException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Performs the actual sending of the AMQP message, this class is designed to be cached
 * ,so it accepts the message context from out side rather than from the constructor.
 *
 */
public class AMQPSender {

    private Channel channel = null;

    private String queueName = null;

    private String exchangeName = null;

    private String routingKey = null;

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public Channel getChannel() {
        return channel;
    }

    public void sendAMQPMessage(MessageContext mc, String correlationId, String replyTo)
            throws AMQPTransportException, IOException {

        OMOutputFormat format = BaseUtils.getOMOutputFormat(mc);
        MessageFormatter formatter;
        try {
            formatter = MessageProcessorSelector.getMessageFormatter(mc);
        } catch (AxisFault axisFault) {
            throw new AxisFault("Unable to get the message formatter to use");
        }

        AMQP.BasicProperties.Builder builder = new
                AMQP.BasicProperties().builder();

        String msgId = getProperty(mc, AMQPTransportConstant.PROPERTY_AMQP_MESSAGE_ID);
        if (msgId == null) {
            msgId = mc.getMessageID();
        }
        builder.messageId(msgId);

        String contentType = getProperty(mc, AMQPTransportConstant.PROPERTY_AMQP_CONTENT_TYPE);
        if (contentType == null) {
            contentType = getProperty(mc, Constants.Configuration.CONTENT_TYPE);
        }
        if (contentType != null) {
            builder.contentType(contentType);
        }

        if (correlationId != null) {
            builder.correlationId(correlationId);
        }

        if (replyTo != null) {
            builder.replyTo(replyTo);
        }

        String encoding = getProperty(mc, AMQPTransportConstant.PROPERTY_AMQP_CONTENT_ENCODING);
        if (encoding == null) {
            encoding = getProperty(mc, Constants.Configuration.CHARACTER_SET_ENCODING);
        }
        if (encoding != null) {
            builder.contentEncoding(encoding);
        }

        String deliverMode = getProperty(mc, AMQPTransportConstant.PROPERTY_AMQP_DELIVER_MODE);
        if (deliverMode != null) {
            builder.deliveryMode(Integer.parseInt(deliverMode));
        }

        String priority = getProperty(mc, AMQPTransportConstant.PROPERTY_AMQP_PRIORITY);
        if (priority != null) {
            builder.priority(Integer.parseInt(priority));
        }


        String expiration = getProperty(mc, AMQPTransportConstant.PROPERTY_AMQP_EXPIRATION);
        if (expiration != null) {
            builder.expiration(expiration);
        }

        String timeStamp = getProperty(mc, AMQPTransportConstant.PROPERTY_AMQP_TIME_STAMP);
        if (timeStamp != null) {
            builder.timestamp(new Date(Long.parseLong(timeStamp)));
        }

        String type = getProperty(mc, AMQPTransportConstant.PROPERTY_AMQP_TYPE);
        if (type != null) {
            builder.type(type);
        }

        String userId = getProperty(mc, AMQPTransportConstant.PROPERTY_AMQP_USER_ID);
        if (userId != null) {
            builder.type(userId);
        }

        String appId = getProperty(mc, AMQPTransportConstant.PROPERTY_AMQP_APP_ID);
        if (appId != null) {
            builder.appId(appId);
        }

        String clusterId = getProperty(mc, AMQPTransportConstant.PROPERTY_AMQP_CLUSTER_ID);
        if (clusterId != null) {
            builder.clusterId(clusterId);
        }

        // add any custom properties set with AMQP_HEADER_* as headers
        Map<String, Object> headers = new HashMap<String, Object>();
        Map<String, Object> prop = mc.getProperties();
        for (String key : prop.keySet()) {
            if (key.contains(AMQPTransportConstant.AMQP_HEADER)) {
                headers.put(key, prop.get(key));
            }
        }
        builder.headers(headers);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        formatter.writeTo(mc, format, out, false);
        byte[] msg = out.toByteArray();

        if (exchangeName != null) {
            if (routingKey != null) {
                channel.basicPublish(exchangeName, routingKey, builder.build(), msg);
            } else {
                channel.basicPublish(exchangeName, "", builder.build(), msg);
            }
        } else {
            channel.basicPublish("", queueName, builder.build(), msg);
        }
    }

    private String getProperty(MessageContext mc, String key) {
        return (String) mc.getProperty(key);
    }
}