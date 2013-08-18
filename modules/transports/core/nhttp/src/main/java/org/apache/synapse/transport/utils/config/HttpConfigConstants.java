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

package org.apache.synapse.transport.utils.config;

public class HttpConfigConstants {

    public static final String CONNECTION_TIMEOUT = "http.connection.timeout";
    public static final String INTEREST_OPS_QUEUEING = "http.nio.interest-ops-queueing";
    public static final String TCP_NODELAY = "http.tcp.nodelay";
    public static final String SOCKET_BUFFER_SIZE = "http.socket.buffer-size";
    public static final String SOCKET_RCV_BUFFER_SIZE = "http.socket.rcv-buffer-size";
    public static final String SOCKET_SND_BUFFER_SIZE = "http.socket.snd-buffer-size";
    public static final String SO_LINGER = "http.socket.linger";
    public static final String SO_REUSEADDR = "http.socket.reuseaddr";
    public static final String SO_TIMEOUT = "http.socket.timeout";
    public static final String LISTENER_SO_TIMEOUT = "http.socket.timeout.listener";
    public static final String SENDER_SO_TIMEOUT = "http.socket.timeout.sender";
    public static final String SELECT_INTERVAL = "http.nio.select-interval";

    public static final String HTTP_MALFORMED_INPUT_ACTION = "http.malformed.input.action";
    public static final String HTTP_UNMAPPABLE_INPUT_ACTION = "http.unmappable.input.action";
}
