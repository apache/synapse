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

import javax.xml.namespace.QName;

/**
 * Global constants for the Apache Synapse project
 */
public final class SynapseConstants {

    /** The Synapse namespace */
    public static final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";
    /** An OMNamespace object for the Synapse NS */
    public static final OMNamespace SYNAPSE_OMNAMESPACE =
            OMAbstractFactory.getOMFactory().createOMNamespace(SYNAPSE_NAMESPACE, "syn");

    /** The name of the main sequence for message mediation */
    public static final String MAIN_SEQUENCE_KEY  = "main";
    /** The name of the fault sequence to execute on failures during mediation */
    public static final String FAULT_SEQUENCE_KEY = "fault";

    /** The name of the Synapse service (used for message mediation) */
    public static final String SYNAPSE_SERVICE_NAME ="__SynapseService";
    /** The operation name used by the Synapse service (for message mediation) */
    public static final QName SYNAPSE_OPERATION_NAME = new QName("mediate");

    //- names of modules to be engaged at runtime -
    /** The Name of the WS-RM Sandesha module */
    public static final String SANDESHA2_MODULE_NAME = "sandesha2";
    /** The Name of the WS-A Addressing module */
    public static final String ADDRESSING_MODULE_NAME = "addressing";
    /** The Name of the WS-Security Rampart module */
    public static final String RAMPART_MODULE_NAME = "rampart";

    //- Standard headers that can be read as get-property('header')-
    /** Refers to the To header */
    public static final String HEADER_TO = "To";
    /** Refers to the From header */
    public static final String HEADER_FROM = "From";
    /** Refers to the FaultTo header */
    public static final String HEADER_FAULT = "FaultTo";
    /** Refers to the Action header */
    public static final String HEADER_ACTION = "Action";
    /** Refers to the ReplyTo header */
    public static final String HEADER_REPLY_TO = "ReplyTo";
    /** Refers to the MessageID header */
    public static final String HEADER_MESSAGE_ID = "MessageID";
    /** Message format: pox, soap11, soap12 */
    public static final String PROPERTY_MESSAGE_FORMAT = "MESSAGE_FORMAT";

    /** The Axis2 client options property name for the Rampart policy */
    public static final String RAMPART_POLICY = "rampartPolicy";
    /** The Axis2 client options property name for the Sandesha policy */
	public static final String SANDESHA_POLICY = "sandeshaPolicy";

    /** The name of the Parameter set on the Axis2Configuration to hold the Synapse Configuration */
    public static final String SYNAPSE_CONFIG = "synapse.config";
    /** The name of the Parameter set on the Axis2Configuration to hold the Synapse Environment */
    public static final String SYNAPSE_ENV = "synapse.env";

    /** The name of the system property that will hold the Synapse home directory */
    public static final String SYNAPSE_HOME = "synapse.home";
    /** The default synapse.properties file path */
    public static final String DEFAULT_PROP_PATH = "synapse.properties";
    /** The name of the system property used to specify/override the synapse config XML location */
    public static final String SYNAPSE_XML = "synapse.xml";
    /** The name of the system property used to specify/override the synapse properties location */
    public static final String SYNAPSE_PROPERTIES = "synapse.properties";

    //- Synapse Message Context Properties -
        /** The Synapse MC property name that holds the name of the Proxy service thats handling it */
        public static final String PROXY_SERVICE = "proxy.name";
        /** The Synapse MC property that marks it as a RESPONSE */
        public static final String RESPONSE = "RESPONSE";
        /** The Synapse MC property that marks the message as a OUT_ONLY message */
        public static final String OUT_ONLY = "OUT_ONLY";

        //-- error handling --
        /** The message context property name which holds the error code for the last encountered exception */
        public static final String ERROR_CODE = "ERROR_CODE";
        /** The MC property name which holds the error message for the last encountered exception */
        public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
        /** The message context property name which holds the error detail (stack trace) for the last encountered exception */
        public static final String ERROR_DETAIL = "ERROR_DETAIL";

        /** Sandesha last message property name */
        public static final String SANDESHA_LAST_MESSAGE = "Sandesha2LastMessage";
        /** Sandesha last sequence key property name */
        public static final String SANDESHA_SEQUENCE_KEY = "Sandesha2SequenceKey";
        /** Sandesha WS-RM specification version property name */
        public static final String SANDESHA_SPEC_VERSION = "Sandesha2RMSpecVersion";

    //- Axis2 Message Context Properties used by Synapse -
    /** an axis2 message context property set to hold the relates to for POX responses */
    public static final String RELATES_TO_FOR_POX = "synapse.RelatesToForPox";

    /** an axis2 message context property set to indicate this is a response message for Synapse */
    public static final String ISRESPONSE_PROPERTY = "synapse.isresponse";


    //- tracing and statistics constants -
        /** Tracing logger name */
        public static final String TRACE_LOGGER ="TRACE_LOGGER";
        public static final String SERVICE_LOGGER_PREFIX ="SERVICE_LOGGER.";

        /** The tracing state -off */
        public static final int TRACING_OFF =0;
        /** The tracing state-on */
        public static final int TRACING_ON =1;
        /** The tracing state-unset */
        public static final int TRACING_UNSET=2;

        /** The statistics state -off */
        public static final int STATISTICS_OFF =0;
        /** The statistics state-on */
        public static final int STATISTICS_ON =1;
        /** The statistics state-unset */
        public static final int STATISTICS_UNSET=2;

        /** key for lookup sequence statistics stack */
        public static final String SEQUENCE_STATS ="synapse.sequence.stats";

        /** key for lookup Proxy Service statistics stack */
        public static final String PROXY_STATS ="synapse.proxy.stats";

        /** key for lookup Proxy Service statistics stack */
        public static final String SERVICE_STATS ="synapse.service.stats";

        /** key for lookup Endpoint statistics stack */
        public static final String ENDPOINT_STATS ="synapse.endpoint.stats";

        /** Sequence statistics category*/
        public static final int  SEQUENCE_STATISTICS = 0;

        /** Proxy Service statistics category */
        public static final int  PROXYSERVICE_STATISTICS = 1;

        /** Endpoint statistics category*/
        public static final int ENDPOINT_STATISTICS = 2;

    //- handling of timed out events from the callbacks -
        /** The System property that states the duration at which the timeout handler runs */
        public static final String TIMEOUT_HANDLER_INTERVAL = "synapse.timeout_handler_interval";

        /**
         * Interval for activating the timeout handler for cleaning up expired requests. Note that
         * there can be an error as large as the value of the interval. But for smaller intervals
         * and larger timeouts this error is negligable.
         */
        public static final long DEFAULT_TIMEOUT_HANDLER_INTERVAL = 15000;

        /**
         * This is a system wide interval for handling otherwise non-expiring callbacks to
         * ensure system stability over a period of time 
         */
        public static final String GLOBAL_TIMEOUT_INTERVAL = "synapse.global_timeout_interval";

        /**
         * this is the timeout for otherwise non-expiring callbacks
         * to ensure system stability over time
         */
        public static final long DEFAULT_GLOBAL_TIMEOUT = 24 * 60 * 60 * 1000;

        /**
         * don't do anything for response timeouts. this means infinite timeout. this is the default
         * action, if the timeout configuration is not explicitly set.
         */
        public static final int NONE = 100;

        /** Discard the callback if the timeout for the response is expired */
        public static final int DISCARD = 101;

        /**
         * Discard the callback and activate specified fault sequence if the timeout for the response
         * is expired
         */
        public static final int DISCARD_AND_FAULT = 102;

        /**
         * Error codes for message sending. We go with closest HTTP fault codes.
         */
        public static final String TIME_OUT = "504";
        public static final String SENDING_FAULT = "503";

    //- Endpoints processing constants -
    /** Property name to store the last endpoint through which the message has flowed */
    public static final String PROCESSED_ENDPOINT = "processed_endpoint";
    
    /** A name to use for anonymous endpoints */
    public static final String ANONYMOUS_ENDPOINT = "AnonymousEndpoint";

    /** A name to use for anonymous sequences in the sequence stack */
    public static final String ANONYMOUS_SEQUENCE = "AnonymousSequence";
    
    /** Message format values in EndpointDefinition. Used by address, wsdl endpoints */
    public static final String FORMAT_POX = "pox";
    public static final String FORMAT_SOAP11 = "soap11";
    public static final String FORMAT_SOAP12 = "soap12";    
}
