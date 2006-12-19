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

package org.apache.synapse;

import javax.xml.namespace.QName;

/**
 * Global constants for the Synapse project
 */
public interface Constants {

    /** The Synapse namespace */
    public static final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";

	

    // -- keys related to Axis2 configuration and Synapse initialization --
    /** The key name used to store the Synapse configuration into the Axis2 config */
    String SYNAPSE_CONFIG = "synapse.config";

    /** The key name used to store the Synapse environment into the Axis2 config */
    String SYNAPSE_ENV = "synapse.env";

    /** The name used to denote the Axis2 Parameter specifying the Synapse configuration */
    String SYNAPSE_CONFIGURATION = "SynapseConfiguration";

    /** The name used to denote the SynapseEnvironment implementation class */
    String SYNAPSE_ENV_IMPL = "SynapseEnvironmentImpl";

    /** The system property used to specify/override the synapse configuration XML location */
    String SYNAPSE_XML = "synapse.xml";

    // -- Synapse message context property keys --
    /**
     * The scope for a set-property mediator, when the property should be copied over
     * to the response message - if any, for correlation
     */
    String SCOPE_CORRELATE = "correlate";

    /**
     * The scope for a set-property mediator, when the property should be set
     *  on the underlying Axis2 message context
     */
    String SCOPE_AXIS2 = "axis2";

    /** An string name which holds the out sequence property in the MessageContext */
    String OUT_SEQUENCE = "outSequence";

    /** A key with this name on the message context set to Boolean.TRUE, indicates that this is a response */
    String ISRESPONSE_PROPERTY = "synapse.isresponse";

    /** If the message context contains a Boolean.TRUE with this key, WS-A would be turned on send */
    String OUTFLOW_ADDRESSING_ON = "OUTFLOW_ADDRESSING_ON";

    /** If the message context contains a Boolean.TRUE with this key, RM would be turned on send */
    String OUTFLOW_RM_ON = "OUTFLOW_RM_ON";

    /** The message context property name which holds the RM 'Policy' object for outgoing messages */
    String OUTFLOW_RM_POLICY = "OUTFLOW_RM_POLICY";

    /** If the message context contains a Boolean.TRUE with this key, Rampart would be engaged on send */
    String OUTFLOW_SECURITY_ON = "OUTFLOW_SECURITY_ON";

    /** The message context property name which holds the Security 'Policy' object for outgoing messages */
    String OUTFLOW_SEC_POLICY = "OUTFLOW_SEC_POLICY";

    /** If message context property contains Boolean.TRUE then Axis2 will send this with a separate listener engaged **/
    public static final String OUTFLOW_USE_SEPARATE_LISTENER = "OUTFLOW_USE_SEPARATE_LISTENER";
    
    /** The message context property name which holds the error code for the last encountered exception */
    String ERROR_CODE = "ERROR_CODE";

    /** The message context property name which holds the error message for the last encountered exception */
    String ERROR_MESSAGE = "ERROR_MESSAGE";

    /** The message context property name which holds the error detail (stack trace) for the last encountered exception */
    String ERROR_DETAIL = "ERROR_DETAIL";

    // -- names of modules to be engaged at runtime --
    /** The QName of the WS-RM Sandesha module */
    QName SANDESHA2_MODULE_NAME = new QName("sandesha2");

    /** The QName of the WS-A Addressing module */
    QName ADDRESSING_MODULE_NAME = new QName("addressing");

    /** The QName of the WS-Security Rampart module */
    QName RAMPART_MODULE_NAME = new QName("rampart");
    /** Sandesha2 engaged service being process*/
    String MESSAGE_RECEIVED_RM_ENGAGED = "__MESSAGE_RECEIVED_RM_ENGAGED__";

    String PROCESSED_MUST_UNDERSTAND = "__PROCESSED_MUST_UNDERSTAND__";

    String RESPONSE_SOAP_ENVELOPE = "_RESPONSE_SOAP_ENVELOPE_";

    /** Refers the To header */
    String HEADER_TO = "To";
    /** Refers the From header */
    String HEADER_FROM = "From";
    /** Refers the FaultTo header */
    String HEADER_FAULT = "FaultTo";
    /** Refers the Action header */
    String HEADER_ACTION = "Action";
    /** Refers the ReplyTo header */
    String HEADER_REPLY_TO = "ReplyTo";

    /** The tracing state -off */
    int TRACING_OFF =0;
    /** The tracing state-on */
    int TRACING_ON =1;
    /** The tracing state-unset */
    int TRACING_UNSET=2;
    /** Tracing logger */
    String TRACE_LOGGER ="TRACE_LOGGER";
}
