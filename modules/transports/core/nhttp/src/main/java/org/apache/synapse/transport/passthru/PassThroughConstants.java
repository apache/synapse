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

package org.apache.synapse.transport.passthru;

public class PassThroughConstants {

    public static final String REQUEST_MESSAGE_CONTEXT = "REQUEST_MESSAGE_CONTEXT";
    public static final String CONNECTION_POOL = "CONNECTION_POOL";

    public static final String TRUE = "TRUE";

    public static final String FAULT_MESSAGE = "FAULT_MESSAGE"; // corresponds with BaseConstants
    public static final String FAULTS_AS_HTTP_200 = "FAULTS_AS_HTTP_200";
    public static final String SC_ACCEPTED = "SC_ACCEPTED";
    public static final String HTTP_SC = "HTTP_SC";
    public static final String FORCE_HTTP_1_0 = "FORCE_HTTP_1.0";
    public static final String DISABLE_CHUNKING = "DISABLE_CHUNKING";
    public static final String FULL_URI = "FULL_URI";
    public static final String NO_KEEPALIVE = "NO_KEEPALIVE";
    public static final String DISABLE_KEEPALIVE = "http.connection.disable.keepalive";
    public static final String IGNORE_SC_ACCEPTED = "IGNORE_SC_ACCEPTED";
    public static final String FORCE_SC_ACCEPTED = "FORCE_SC_ACCEPTED";
    public static final String DISCARD_ON_COMPLETE = "DISCARD_ON_COMPLETE";

    public static final String SERVICE_URI_LOCATION = "ServiceURI";

    public static final String WSDL_EPR_PREFIX = "WSDLEPRPrefix";
    public static final String BIND_ADDRESS = "bind-address" ;

    public static final String EPR_TO_SERVICE_NAME_MAP = "service.epr.map";
    public static final String NON_BLOCKING_TRANSPORT = "NonBlockingTransport";
    public static final String SERIALIZED_BYTES = "SerializedBytes";

    public static final String CONTENT_TYPE = "CONTENT_TYPE";

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    public static final String CONTENT_TYPE_MULTIPART_RELATED = "multipart/related";

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

    public static final String REST_URL_POSTFIX = "REST_URL_POSTFIX";
    public static final String SERVICE_PREFIX = "SERVICE_PREFIX";
    public static final String NO_ENTITY_BODY = "NO_ENTITY_BODY";

    protected static final String PASS_THROUGH_TRANSPORT_WORKER_POOL =
            "PASS_THROUGH_TRANSPORT_WORKER_POOL";
    public static final String PASS_THROUGH_SOURCE_CONFIGURATION =
            "PASS_THROUGH_SOURCE_CONFIGURATION";
    public static final String PASS_THROUGH_SOURCE_CONNECTION = "pass-through.Source-Connection";
    protected static final String PASS_THROUGH_SOURCE_REQUEST = "pass-through.Source-Request";

    protected static final String PASS_THROUGH_TARGET_CONNECTION = "pass-through.Target-Connection";
    protected static final String PASS_THROUGH_TARGET_RESPONSE = "pass-through.Target-Response";

    public static final String PASS_THROUGH_RESPONSE_SOURCE_BUFFER = "synapse.response-source-buffer";

    public static final String PASS_THROUGH_PIPE = "pass-through.pipe";

    // used to define the default content type as a parameter in the axis2.xml
    public static final String REQUEST_CONTENT_TYPE = "DEFAULT_REQUEST_CONTENT_TYPE";

    // This is a workaround  for  axis2 RestUtils behaviour
    public static final String REST_REQUEST_CONTENT_TYPE = "synapse.internal.rest.contentType";

    public static final String MESSAGE_BUILDER_INVOKED = "message.builder.invoked";
    
    public static final String CLONE_PASS_THROUGH_PIPE_REQUEST = "clone_pass-through.pipe_connected";
    
    /**
     * Name of the .mar file
     */
    public final static String SECURITY_MODULE_NAME = "rampart";
    
    public final static String REST_GET_DELETE_INVOKE ="rest_get_delete_invoke";
    
    public final static String PASS_THROUGH_MESSAGE_LENGTH ="PASS_THROUGH_MESSAGE_LENGTH";
    
	public static final String CONF_LOCATION = "conf.location";

    public static final String LOCATION = "Location";
    
	public static final String BUFFERED_INPUT_STREAM = "bufferedInputStream";
	
	//JMX statistic calculation Constants
	public static final String REQ_ARRIVAL_TIME = "REQ_ARRIVAL_TIME";
	public static final String REQ_DEPARTURE_TIME = "REQ_DEPARTURE_TIME";
	public static final String RES_ARRIVAL_TIME = "RES_ARRIVAL_TIME";
	public static final String RES_HEADER_ARRIVAL_TIME = "RES_HEADER_ARRIVAL_TIME";
	public static final String RES_DEPARTURE_TIME = "RES_DEPARTURE_TIME";

	public static final String MESSAGE_OUTPUT_FORMAT = "MESSAGE_OUTPUT_FORMAT";
	
	public static final String FORCE_SOAP_FAULT = "FORCE_SOAP_FAULT";
	
	public static final String FORCE_PASS_THROUGH_BUILDER = "force.passthrough.builder";
	
	public static final String GET_REQUEST_HANDLED = "GET_REQUEST_HANDLED";
	
	public static final String WAIT_BUILDER_IN_STREAM_COMPLETE="WAIT_BUILDER_IN_STREAM_COMPLETE"; 
 	
	public static final String BUILDER_OUTPUT_STREAM="BUILDER_OUTPUT_STREAM";
	
	public static final String PASSTHRU_CONNECT_ERROR="PASSTHRU_CONNECT_ERROR";

    public static final String COPY_CONTENT_LENGTH_FROM_INCOMING="COPY_CONTENT_LENGTH_FROM_INCOMING";
    
    public static final String ORIGINAL_CONTENT_LENGTH ="ORIGINAL_CONTENT_LENGTH";
    
    //Http method type constants to be used in synapse
    public static final String HTTP_HEAD = "HEAD";

    //use to create source buffer in passthrough transport
    public static final String SOURCE ="source";
}
