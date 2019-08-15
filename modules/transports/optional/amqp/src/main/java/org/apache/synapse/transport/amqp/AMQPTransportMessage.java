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

import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.axis2.transport.base.BaseConstants;

import java.util.Date;
import java.util.Map;

/**
 * Represent a AMQP message transfer between the broker and the consumer/producer.
 */
public class AMQPTransportMessage {

    private String soapAction;

    private String messageId;

    private String contentType;

    private String contentEncoding;

    private Integer deliveryMode;

    private Integer priority;

    private String correlationId;

    private String replyTo;

    private String expiration;

    private Date timestamp;

    private String type;

    private String userId;

    private String appId;

    private String clusterId;

    private Map<String, Object> headers;

    /**
     * Keeps a reference to the AMQP message.
     */
    private byte[] body;

    private Envelope envelope;

    private BasicProperties basicProperties;

    private QueueingConsumer.Delivery delivery;

    public AMQPTransportMessage(QueueingConsumer.Delivery delivery) {
        this.delivery = delivery;
        this.body = delivery.getBody();
        this.envelope = delivery.getEnvelope();
        this.basicProperties = delivery.getProperties();
        this.messageId = delivery.getProperties().getMessageId();
        this.contentType = delivery.getProperties().getContentType();
        this.contentEncoding = delivery.getProperties().getContentEncoding();
        this.deliveryMode = delivery.getProperties().getDeliveryMode();
        this.priority = delivery.getProperties().getPriority();
        this.correlationId = delivery.getProperties().getCorrelationId();
        this.replyTo = delivery.getProperties().getReplyTo();
        this.expiration = delivery.getProperties().getExpiration();
        this.timestamp = delivery.getProperties().getTimestamp();
        this.type = delivery.getProperties().getType();
        this.userId = delivery.getProperties().getUserId();
        this.appId = delivery.getProperties().getAppId();
        this.clusterId = delivery.getProperties().getClusterId();
        this.headers = delivery.getProperties().getHeaders();

        if (delivery.getProperties().getHeaders() != null) {
            this.soapAction = (String) delivery.getProperties().getHeaders().get(BaseConstants.SOAPACTION);
        }
    }

    public AMQPTransportMessage(BasicProperties basicProperties, byte[] message) {
        this.body = message;
        this.basicProperties = basicProperties;
    }

    public String getSoapAction() {
        return soapAction;
    }

    public String getMessageId() {
        return messageId;
    }


    public String getContentType() {
        return contentType;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }


    public Integer getDeliveryMode() {
        return deliveryMode;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public String getExpiration() {
        return expiration;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public String getUserId() {
        return userId;
    }

    public String getAppId() {
        return appId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public QueueingConsumer.Delivery getDelivery() {
        return delivery;
    }

    public Envelope getEnvelope() {
        return envelope;
    }
}