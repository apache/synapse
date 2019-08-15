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
package org.apache.synapse.transport.amqp.tx;

import com.rabbitmq.client.Channel;

import java.io.IOException;

/**
 * Wrap the normal transaction API and the light weight publisher confirm apis'.
 * See http://www.rabbitmq.com/blog/2011/02/10/introducing-publisher-confirms/,
 *
 */
public class AMQPTransportProducerTx {

    /**
     * Use light weight publisher confirm to handle transaction ? Default is
     * set to true for high performance
     */
    private boolean isLightWeightPublisherConfirm = true;

    private Channel channel;

    public AMQPTransportProducerTx(boolean lightWeightPublisherConfirm,
                                   Channel channel) {
        isLightWeightPublisherConfirm = lightWeightPublisherConfirm;
        this.channel = channel;
    }

    public void start() throws IOException {
        if (isLightWeightPublisherConfirm) {
            channel.confirmSelect();
        } else {
            channel.txSelect();
        }
    }

    public void end() throws InterruptedException, IOException {
        if (isLightWeightPublisherConfirm) {
            channel.waitForConfirms();
        } else {
            channel.txCommit();
        }
    }
}