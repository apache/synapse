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
package org.apache.synapse.transport.base.datagram;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.TransportUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.base.MetricsCollector;

/**
 * Task encapsulating the processing of a datagram.
 * Instances of this class will be dispatched to worker threads for
 * execution.
 */
public class ProcessPacketTask implements Runnable {
    private static final Log log = LogFactory.getLog(ProcessPacketTask.class);
    
    private final DatagramEndpoint endpoint;
    private final byte[] data;
    private final int length;
    
    public ProcessPacketTask(DatagramEndpoint endpoint, byte[] data, int length) {
        this.endpoint = endpoint;
        this.data = data;
        this.length = length;
    }
    
    public void run() {
        MetricsCollector metrics = endpoint.getMetrics();
        try {
            InputStream inputStream = new ByteArrayInputStream(data, 0, length);
            MessageContext msgContext = endpoint.getListener().createMessageContext();
            msgContext.setAxisService(endpoint.getService());
            SOAPEnvelope envelope = TransportUtils.createSOAPMessage(msgContext, inputStream, endpoint.getContentType());
            msgContext.setEnvelope(envelope);
            AxisEngine.receive(msgContext);
            metrics.incrementMessagesReceived();
            metrics.incrementBytesReceived(length);
        } catch (Exception ex) {
            metrics.incrementFaultsReceiving();
            StringBuilder buffer = new StringBuilder("Error during processing of datagram:\n");
            Utils.hexDump(buffer, data, length);
            log.error(buffer.toString(), ex);
        }
    }
}
