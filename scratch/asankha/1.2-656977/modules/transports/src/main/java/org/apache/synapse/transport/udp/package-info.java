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

/**
 * Transport implementation for the UDP protocol.
 * <p>
 * This package contains a transport listener implementation allowing Axis to
 * receive and process UDP packets. It is an implementation of "raw" UDP in the
 * sense that the message is directly extracted from the UDP payload without
 * any intermediate application protocol. This has two important implications:
 * <ul>
 *   <li>The only way to route the incoming message to the appropriate Axis service
 *       is to bind the service a specific UDP port. The port number must be
 *       explicitly defined in the service configuration. This is different
 *       from protocols such as HTTP where the message can be routed
 *       based on the URL in the request.</li>
 *   <li>The transport has no way to detect the content type of an incoming
 *       message. Indeed, there is no equivalent to HTTP's
 *       <tt>Content-Type</tt> header. Again the expected content type must be
 *       configured explicitly for the service.</li>
 * </ul>
 * See the documentation of {@link org.apache.synapse.transport.udp.UDPListener}
 * for more information about how to configure a service to accept UDP packets.
 * <p>
 * It should also be noted that given its characteristics, UDP is not a
 * suitable transport protocol for SOAP, except maybe in very particular
 * circumstances. Indeed, UDP is an unreliable protocol:
 * <ul>
 *   <li>There is no delivery guarantee, i.e. packets may be lost.</li>
 *   <li>Messages may arrive out of order.</li>
 *   <li>Messages may be duplicated, i.e. delivered twice.</li>
 * </ul>
 * This transport implementation is useful mainly to integrate Axis (and in
 * particular Synapse) with existing UDP based protocols. See
 * {@link org.apache.synapse.format.syslog} for an example of this kind
 * of protocol.
 * 
 * <h4>Known issues</h4>
 * 
 * <ul>
 *   <li>Packets longer than the configured maximum packet size
 *       are silently truncated. Packet truncation should be detected
 *       and trigger an error.</li>
 *   <li>The listener doesn't implement all management operations
 *       specified by
 *       {@link org.apache.synapse.transport.base.ManagementSupport}.</li>
 * </ul>
 */
package org.apache.synapse.transport.udp;