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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;

/**
 * Global constants for the Synapse project
 */
public interface Constants {

    /** The Synapse namespace */
    public static final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";

    public static final String MAIN_SEQUENCE_KEY  = "main";
    public static final String FAULT_SEQUENCE_KEY = "fault";

    public static final OMNamespace SYNAPSE_OMNAMESPACE =
            OMAbstractFactory.getOMFactory().createOMNamespace(SYNAPSE_NAMESPACE, "syn");

    // -- keys related to Axis2 configuration and Synapse initialization --
    /** The key name used to store the Synapse configuration into the Axis2 config */
    String SYNAPSE_CONFIG = "synapse.config";

    /** The key name used to store the Synapse environment into the Axis2 config */
    String SYNAPSE_ENV = "synapse.env";

    /** The system property used to specify/override the synapse configuration XML location */
    String SYNAPSE_XML = "synapse.xml";

    /** The system property used to specify/override the synapse home location */
    String SYNAPSE_HOME = "synapse.home";
   
    /** a message context property set to hold the relates to for POX responses */
    String RELATES_TO_FOR_POX = "RelatesToForPox";

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
    

    // -- Synapse message context property keys --
    /** The scope for the synapse message context properties */
    String SCOPE_DEFAULT = "default";
    
    /**
     * The scope for a set-property mediator, when the property should be set
     *  on the underlying Axis2 message context
     */
    String SCOPE_AXIS2 = "axis2";

    /**
     * The scope for a set-property mediator, when the property should be set
     *  on the underlying transport 
     */
    String SCOPE_TRANSPORT = "transport";

    /** An string name which holds the out sequence property in the MessageContext */
    String PROXY_SERVICE = "proxy.name";

    /** A key with this name on the message context set to Boolean.TRUE, indicates that this is a response */
    String ISRESPONSE_PROPERTY = "synapse.isresponse";

    /** If message context property contains Boolean.TRUE then Axis2 will send this with a separate listener engaged **/
    public static final String OUTFLOW_USE_SEPARATE_LISTENER = "OUTFLOW_USE_SEPARATE_LISTENER";
    
    /** The message context property name which holds the error code for the last encountered exception */
    String ERROR_CODE = "ERROR_CODE";

    /** The message context property name which holds the error message for the last encountered exception */
    String ERROR_MESSAGE = "ERROR_MESSAGE";

    /** The message context property name which holds the error detail (stack trace) for the last encountered exception */
    String ERROR_DETAIL = "ERROR_DETAIL";

    // -- names of modules to be engaged at runtime --
    /** The Name of the WS-RM Sandesha module */
    String SANDESHA2_MODULE_NAME = "sandesha2";

    /** The Name of the WS-A Addressing module */
    String ADDRESSING_MODULE_NAME = "addressing";

    /** The Name of the WS-Security Rampart module */
    String RAMPART_MODULE_NAME = "rampart";

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
    /** Refers the MessageID header */
    String HEADER_MESSAGE_ID = "MessageID";

    String RESPONSE = "RESPONSE";

    /** The tracing state -off */
    int TRACING_OFF =0;
    /** The tracing state-on */
    int TRACING_ON =1;
    /** The tracing state-unset */
    int TRACING_UNSET=2;
    /** Tracing logger */
    String TRACE_LOGGER ="TRACE_LOGGER";

    //  -- Synapse property values for WS-RM sequence handling --
    /** WS-RM version 1.0*/
    String SEQUENCE_VERSION_1_0 = "1.0";
    /** WS-RM version 1.1*/
    String SEQUENCE_VERSION_1_1 = "1.1";

    /** Sandesha last message property name */
    String SANDESHA_LAST_MESSAGE = "Sandesha2LastMessage";
    /** Sandesha last sequence key property name */
    String SANDESHA_SEQUENCE_KEY = "Sandesha2SequenceKey";
    /** Sandesha WS-RM specification version property name */
    String SANDESHA_SPEC_VERSION = "Sandesha2RMSpecVersion";

    /** The statistics state -off */
    int STATISTICS_OFF =0;
    /** The statistics state-on */
    int STATISTICS_ON =1;
    /** The statistics state-unset */
    int STATISTICS_UNSET=2;

    String SYNAPSE_ERROR ="syapse_error" ;

    /** key for lookup sequence statistics stack */
    String SEQUENCE_STATISTICS_STACK ="sequence_statistics_stack";

    /** key for lookup Proxy Service statistics stack */
    String PROXYSERVICE_STATISTICS_STACK ="proxyservice_statistics_stack";

    /** key for lookup Proxy Service statistics stack */
    String SYNAPSESERVICE_STATISTICS_STACK ="synapseservice_statistics_stack";      

    /** key for lookup Endpoint statistics stack */
    String ENDPOINT_STATISTICS_STACK ="endpoint_statistics_stack";

    /** Sequence statistics category*/
    int  SEQUENCE_STATISTICS = 0;

    /** Proxy Service statistics category */
    int  PROXYSERVICE_STATISTICS = 1;

    /** Endpoint statistics category*/
    int ENDPOINT_STATISTICS = 2;

    /**
     * Interval for activating the timeout handler for cleaning up expired requests. Note that
     * there can be an error as large as the value of the interval. But for smaller intervals and
     * larger timeouts this error is negligable.
     */
    long TIMEOUT_HANDLER_INTERVAL = 1000;

    /**
     * don't do anything for response timeouts. this means infinite timeout. this is the default
     * action, if the timeout configuration is not explicitly set.
     */
    int NONE = 100;

    /** Discard the callback if the timeout for the response is expired */
    int DISCARD = 101;

    /**
     * Discard the callback and activate specified fault sequence if the timeout for the response
     * is expired
     */
    int DISCARD_AND_FAULT = 102;

    /**
     * Error codes for message sending. We go with closest HTTP fault codes.
     */
    String TIME_OUT = "504";
    String SENDING_FAULT = "503";

    /** Property name to store the last endpoint through which the message has flowed */
    String PROCESSED_ENDPOINT = "processed_endpoint";
    /**  Anonymous Endpoint key   */
    String ANONYMOUS_ENDPOINTS = "AnonymousEndpoints";
    /** Anonymous Sequence  key   */
    String ANONYMOUS_SEQUENCES = "AnonymousSequences";
    /** Anonymous ProxyServices key   */
    String ANONYMOUS_PROXYSERVICES = "AnonymousProxyServices";
}
