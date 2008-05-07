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

/**
 * Utility class defining constants used by the UDP transport.
 */
public class UDPConstants {
    private UDPConstants() {}
    
    public static final String TRANSPORT_NAME = "udp";
    
    public static final int DEFAULT_MAX_PACKET_SIZE = 1024;
    
    public static final String PORT_KEY = "transport.udp.port";
    public static final String CONTENT_TYPE_KEY = "transport.udp.contentType";
    public static final String MAX_PACKET_SIZE_KEY = "transport.udp.maxPacketSize";
}
