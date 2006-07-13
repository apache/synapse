/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    /** The system property used to specify/override the synapse configuration XML location */
    String SYNAPSE_XML = "synapse.xml";

    // -- Synapse message context property keys --
    /** Properties on an outgoing message context starting with this prefix
     * would be copied over to the incoming reply for correlation
     */
    String CORRELATE = "correlate/";

    /** A key with this name on the message context set to Boolean.TRUE, indicates that this is a response */
    String ISRESPONSE_PROPERTY = "synapse.isresponse";

    /** If the message context contains a Boolean.TRUE with this key, WS-A would be turned on send */
    String OUTFLOW_ADDRESSING_ON = "OUTFLOW_ADDRESSING_ON";

    /** If the message context contains a Boolean.TRUE with this key, RM would be turned on send */
    String OUTFLOW_RM_ON = "OUTFLOW_RM_ON";

    /** The message context property name which holds the RM policy to be used for outgoing messages */
    String OUTFLOW_RM_POLICY = "OUTFLOW_RM_POLICY";

    /** If the message context contains a Boolean.TRUE with this key, Rampart would be engaged on send */
    String OUTFLOW_SECURITY_ON = "OUTFLOW_SECURITY_ON";

    /** The message context property name which holds the Security 'Parameter' object to be used for outgoing messages */
    String OUTFLOW_SEC_PARAMETER = "OUTFLOW_SEC_PARAMETER";

    /** The message context property name which holds the Security 'Parameter' object to be used for incoming messages */
    String INFLOW_SEC_PARAMETER = "INFLOW_SEC_PARAMETER";

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

}
