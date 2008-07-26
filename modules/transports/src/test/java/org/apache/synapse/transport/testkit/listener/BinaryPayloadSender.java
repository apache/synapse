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

package org.apache.synapse.transport.testkit.listener;

import java.io.ByteArrayOutputStream;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;

public abstract class BinaryPayloadSender extends AbstractMessageSender implements XMLMessageSender {
    public BinaryPayloadSender() {
        super();
    }

    public BinaryPayloadSender(String name) {
        super(name);
    }

    public void sendMessage(ListenerTestSetup setup, String endpointReference, String contentType, String charset, OMElement message) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OMOutputFormat outputFormat = new OMOutputFormat();
        outputFormat.setCharSetEncoding(charset);
        outputFormat.setIgnoreXMLDeclaration(true);
        message.serializeAndConsume(baos, outputFormat);
        sendMessage(setup, endpointReference, contentType, baos.toByteArray());
    }

    /**
     * Send a message to the transport listener. It is not recommended to use the
     * corresponding transport sender to achieve this. Instead the implementation
     * should use protocol specific libraries or APIs.
     * 
     * @param endpointReference the endpoint reference of the service
     * @param contentType the content type of the message
     * @param content the content of the message
     * @throws Exception
     */
    public abstract void sendMessage(ListenerTestSetup setup, String endpointReference, String contentType, byte[] content) throws Exception;
}