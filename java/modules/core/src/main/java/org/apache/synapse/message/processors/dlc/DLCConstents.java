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
package org.apache.synapse.message.processors.dlc;

/**
 * class <code>DLCConstents</code> holds the constants that are used in the Dead Letter channel
 */
public final class DLCConstents {

    /**
     * Max number of redeliver attempts per message
     */
    public static final String MAX_REDELIVERY_COUNT = "redelivery.count";

    /**
     * Message context property that holds the name of the target endpoint to be replayed
     */
    public static final String REPLAY_ENDPOINT = "replay.endpoint";

    /**
     * Message context property that holds the name of the target sequence to be replayed
     */
    public static final String REPLAY_SEQUENCE = "replay.sequence";

    /**
     * Message context property that holds the name of the fault handler that must be set to
     * the Message before replaying it
     */
    public static final String REPLAY_FAULT_HANDLER = "replay.fault.handler";

    /**
     *Message context property that holds number of redelivers for a given message
     */
    public static final String NO_OF_REDELIVERS = "number.of.redelivers";


}
