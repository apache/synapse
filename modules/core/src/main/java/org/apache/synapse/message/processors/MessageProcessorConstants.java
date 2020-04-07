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
package org.apache.synapse.message.processors;

public final class MessageProcessorConstants {

    public static final String MESSAGE_STORE = "message.store";
    public static final String PARAMETERS = "parameters";

    /**
     * Scheduled Message Processor parameters
     */
    public static final String QUARTZ_CONF = "quartz.conf";
    public static final String INTERVAL = "interval";
    public static final String CRON_EXPRESSION = "cronExpression";

    /**
     * Message processor parameters
     */
    public static final String MAX_DELIVER_ATTEMPTS = "max.deliver.attempts";

    /**
     * HTTP status codes which are used for message processor retry implementation
     */
    public static final String HTTP_INTERNAL_SERVER_ERROR = "500";
    public static final String HTTP_BAD_REQUEST_ERROR = "400";

}
