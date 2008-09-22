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
package org.apache.synapse.transport.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.base.AbstractTransportSender;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.commons.logging.LogFactory;

/**
 * Transport sender for the UDP protocol.
 * 
 * @see org.apache.synapse.transport.udp
 */
public class UDPSender extends AbstractTransportSender {
    public UDPSender() {
        log = LogFactory.getLog(UDPSender.class);
    }
    
    @Override
    public void init(ConfigurationContext cfgCtx, TransportOutDescription transportOut) throws AxisFault {
        super.init(cfgCtx, transportOut);
    }
    
    @Override
    public void sendMessage(MessageContext msgContext, String targetEPR, OutTransportInfo outTransportInfo) throws AxisFault {
        UDPOutTransportInfo udpOutInfo = new UDPOutTransportInfo(targetEPR);
        MessageFormatter messageFormatter = TransportUtils.getMessageFormatter(msgContext);
        OMOutputFormat format = BaseUtils.getOMOutputFormat(msgContext);
        format.setContentType(udpOutInfo.getContentType());
        byte[] payload = messageFormatter.getBytes(msgContext, format);
        try {
            DatagramSocket socket = new DatagramSocket();
            try {
                socket.send(new DatagramPacket(payload, payload.length, InetAddress.getByName(udpOutInfo.getHost()), udpOutInfo.getPort()));
            }
            finally {
                socket.close();
            }
        }
        catch (IOException ex) {
            throw new AxisFault("Unable to send packet", ex);
        }
    }
}
