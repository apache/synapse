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

package org.apache.synapse.transport.nhttp;

public class NhttpConstants {
    public static final String TRUE = "TRUE";
    /**
     * A message context property indicating "TRUE", if a transport or the message builder
     * has information that the current message is a fault (e.g. SOAP faults, non-HTTP 2xx, etc)
     */
    public static final String FAULT_MESSAGE = "FAULT_MESSAGE"; // corresponds with BaseConstants
    public static final String FAULTS_AS_HTTP_200 = "FAULTS_AS_HTTP_200";
    public static final String SC_ACCEPTED = "SC_ACCEPTED";
    public static final String HTTP_SC = "HTTP_SC";
    public static final String FORCE_HTTP_1_0 = "FORCE_HTTP_1.0";
    public static final String DISABLE_CHUNKING = "DISABLE_CHUNKING";
    public static final String POST_TO_URI = "POST_TO_URI";
    public static final String NO_KEEPALIVE = "NO_KEEPALIVE";
    public static final String DISABLE_KEEPALIVE = "http.connection.disable.keepalive";
    public static final String IGNORE_SC_ACCEPTED = "IGNORE_SC_ACCEPTED";
    public static final String FORCE_SC_ACCEPTED = "FORCE_SC_ACCEPTED";
    public static final String DISCARD_ON_COMPLETE = "DISCARD_ON_COMPLETE";

    public static final String WSDL_EPR_PREFIX = "WSDLEPRPrefix";
    public static final String REMOTE_HOST ="REMOTE_HOST";
    public static final String BIND_ADDRESS = "bind-address";
    public static final String SERVICE_URI_LOCATION = "ServiceURI";
    public static final String EPR_TO_SERVICE_NAME_MAP = "service.epr.map";
    public static final String NON_BLOCKING_TRANSPORT = "NonBlockingTransport";
    public static final String SERIALIZED_BYTES = "SerializedBytes";
    public static final String REQUEST_READ = "REQUEST_READ";
    public static final String CONTENT_TYPE = "CONTENT_TYPE";
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public static final String SEND_TIMEOUT = "SEND_TIMEOUT";

    public static final String HIDDEN_SERVICE_PARAM_NAME = "hiddenService";

    /** An Axis2 message context property indicating a transport send failure */
    public static final String SENDING_FAULT = "SENDING_FAULT";
    /** The message context property name which holds the error code for the last encountered exception */
    public static final String ERROR_CODE = "ERROR_CODE";
    /** The MC property name which holds the error message for the last encountered exception */
    public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
    /** The message context property name which holds the error detail (stack trace) for the last encountered exception */
    public static final String ERROR_DETAIL = "ERROR_DETAIL";
    /** The message context property name which holds the exception (if any) for the last encountered exception */
    public static final String ERROR_EXCEPTION = "ERROR_EXCEPTION";

    // ********** DO NOT CHANGE THESE UNLESS CORRESPONDING SYNAPSE CONSTANT ARE CHANGED ************
    public static final int RCV_IO_ERROR_SENDING   = 101000;
    public static final int RCV_IO_ERROR_RECEIVING = 101001;

    public static final int SND_IO_ERROR_SENDING   = 101500;
    public static final int SND_IO_ERROR_RECEIVING = 101501;

    public static final int CONNECTION_FAILED  = 101503;
    public static final int CONNECTION_TIMEOUT = 101504;
    public static final int CONNECTION_CLOSED  = 101505;
    public static final int PROTOCOL_VIOLATION = 101506;
    public static final int CONNECT_CANCEL     = 101507;
    public static final int CONNECT_TIMEOUT    = 101508;
    public static final int SEND_ABORT         = 101509;
    // ********** DO NOT CHANGE THESE UNLESS CORRESPONDING SYNAPSE CONSTANT ARE CHANGED ************

    public static final String REST_URL_POSTFIX = "REST_URL_POSTFIX";
    public static final String SERVICE_PREFIX = "SERVICE_PREFIX";
    public static final String HTTP_REQ_METHOD = "HTTP_REQ_METHOD";
    public static final String NO_ENTITY_BODY = "NO_ENTITY_BODY";
    public static final String ENDPOINT_PREFIX = "ENDPOINT_PREFIX";
}
