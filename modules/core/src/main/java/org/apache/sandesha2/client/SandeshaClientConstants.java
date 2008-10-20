/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.client;

/**
 * Constants that are needed for the Sandesha2 Client API.
 */
public class SandeshaClientConstants {
	public static final String AcksTo = "Sandesha2AcksTo";
	public static final String LAST_MESSAGE = "Sandesha2LastMessage";
	public static final String OFFERED_SEQUENCE_ID = "Sandesha2OfferedSequenceId";
	public static final String INTERNAL_SEQUENCE_ID = "Sandesha2InternalSequenceId";
	public static final String SANDESHA_DEBUG_MODE = "Sandesha2DebugMode";
	public static final String SEQUENCE_KEY = "Sandesha2SequenceKey";
	public static final String MESSAGE_NUMBER = "Sandesha2MessageNumber";
	public static final String RM_SPEC_VERSION = "Sandesha2RMSpecVersion";
	public static final String DUMMY_MESSAGE = "Sandesha2DummyMessage"; //If this property is set, even though this message will invoke the RM handlers, this will not be sent as an actual application message
	public static final String UNRELIABLE_MESSAGE = "Sandesha2UnreliableMessage";
	public static final String SANDESHA_LISTENER = "Sandesha2Listener";
	public static final String USE_REPLY_TO_AS_ACKS_TO = "UseReplyToAsAcksTo";
	public static final String OFFERED_ENDPOINT = "OfferedEndpoint";
	public static final String AVOID_AUTO_TERMINATION = "AviodAutoTermination";
	public static final String AUTO_START_NEW_SEQUENCE = "AutoStartNewSequence";
	public static final String FORBID_MIXED_EPRS_ON_SEQUENCE = "ForbidMixedEPRsOnSequence";//if true means a sequence will not disallow both sync and async clients
	public static final String ONE_WAY_SEQUENCE = "OneWaySequence"; // if set Sandesha2 will not send any sequene offers
}
