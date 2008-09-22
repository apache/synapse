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

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.transport.base.datagram.DatagramEndpoint;

/**
 * UDP endpoint description.
 */
public class Endpoint extends DatagramEndpoint {
    private int port;
    private int maxPacketSize;
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
		this.port = port;
	}

	public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
		this.maxPacketSize = maxPacketSize;
	}

	public EndpointReference getEndpointReference(String ip) {
        return new EndpointReference("udp://" + ip + ":" + getPort() + "?contentType=" + getContentType());
    }
}
