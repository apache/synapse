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

package org.apache.synapse.transport.base;


import javax.xml.namespace.QName;

public class BaseConstants {
    // -- status of a transport --
    public final static int STOPPED = 0;
    public final static int STARTED = 1;
    public final static int PAUSED  = 2;

    /**
     * The JMS message property specifying the SOAP Action
     */
    public static final String SOAPACTION = "SOAPAction";
    /**
     * The JMS message property specifying the content type
     */
    public static final String CONTENT_TYPE = "Content-Type";
    /**
     * content type identifier for multipart / MTOM messages
     */
    public static final String MULTIPART_RELATED = "multipart/related";
    /**
     * character set marker to identify charset from a Content-Type string
     */
    public static final String CHARSET_PARAM = "; charset=";

    //------------------------------------ defaults ------------------------------------
    /**
     * The default operation name to be used for non SOAP/XML messages
     * if the operation cannot be determined
     */
    public static final QName DEFAULT_OPERATION = new QName("urn:mediate");
    /**
     * The name of the element which wraps binary content into a SOAP envelope
     */

    // This has to match org.apache.synapse.util.PayloadHelper
    // at some future point this can be merged into Axiom as a common base
    public final static String AXIOMPAYLOADNS = "http://ws.apache.org/commons/ns/payload";

   
    public static final QName DEFAULT_BINARY_WRAPPER =
            new QName(AXIOMPAYLOADNS, "binary");
    /**
     * The name of the element which wraps plain text content into a SOAP envelope
     */
    public static final QName DEFAULT_TEXT_WRAPPER =
            new QName(AXIOMPAYLOADNS, "text");

    //-------------------------- services.xml parameters --------------------------------
    /**
     * The Parameter name indicating the operation to dispatch non SOAP/XML messages
     */
    public static final String OPERATION_PARAM = "Operation";
    /**
     * The Parameter name indicating the wrapper element for non SOAP/XML messages
     */
    public static final String WRAPPER_PARAM = "Wrapper";
    /**
     * the parameter in the services.xml that specifies the poll interval for a service
     */
    public static final String TRANSPORT_POLL_INTERVAL = "transport.PollInterval";
    /**
     * The default poll interval in milliseconds.
     */
    public static final int DEFAULT_POLL_INTERVAL = 5 * 60 * 1000; // 5 mins by default
}
