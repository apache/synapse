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

package org.apache.synapse.transport.utils.conn.logging;

public class LoggingConstants {

    public static final String SOURCE_CONNECTION_LOG_ID =
            "org.apache.synapse.transport.http.conn.SourceConnection";
    public static final String TARGET_CONNECTION_LOG_ID =
            "org.apache.synapse.transport.http.conn.TargetConnection";

    public static final String SOURCE_HEADER_LOG_ID =
            "org.apache.synapse.transport.http.headers.SourceHeaders";
    public static final String TARGET_HEADER_LOG_ID =
            "org.apache.synapse.transport.http.headers.TargetHeaders";

    public static final String SOURCE_SESSION_LOG_ID =
            "org.apache.synapse.transport.http.session.SourceSession";
    public static final String TARGET_SESSION_LOG_ID =
            "org.apache.synapse.transport.http.session.TargetSession";

    public static final String SOURCE_WIRE_LOG_ID =
            "org.apache.synapse.transport.http.wire.SourceWire";
    public static final String TARGET_WIRE_LOG_ID =
            "org.apache.synapse.transport.http.wire.TargetWire";
}
