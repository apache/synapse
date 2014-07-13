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

package org.apache.synapse.config.xml;

import org.apache.synapse.SynapseConstants;
import javax.xml.namespace.QName;

/**
 * Constants used in the processing of XML configuration language
 */
public class XMLConfigConstants {

    // re-definition of the Synapse NS here to make things easier for the XML config lang code
    public static final String SYNAPSE_NAMESPACE = SynapseConstants.SYNAPSE_NAMESPACE;

    //- Mediators -
    //-- PropertyMediator --
    /** The scope name for synapse message context properties */
    public static final String SCOPE_DEFAULT = "default";
    /** The scope name for axis2 message context properties */
    public static final String SCOPE_AXIS2 = "axis2";
    /** The scope name for axis2 message context client options properties */
    public static final String SCOPE_CLIENT = "axis2-client";
    /** The scope name for transport header properties */
    public static final String SCOPE_TRANSPORT = "transport";

    //-- WS-RM sequence mediator --
    /** WS-RM version 1.0*/
    public static final String SEQUENCE_VERSION_1_0 = "1.0";
    /** WS-RM version 1.1*/
    public static final String SEQUENCE_VERSION_1_1 = "1.1";

    //- configuration language constants -
    public static final QName DEFINITIONS_ELT = new QName(SYNAPSE_NAMESPACE, "definitions");
    public static final QName SEQUENCE_ELT    = new QName(SYNAPSE_NAMESPACE, "sequence");
    public static final QName ENDPOINT_ELT    = new QName(SYNAPSE_NAMESPACE, "endpoint");
    public static final QName ENTRY_ELT       = new QName(SYNAPSE_NAMESPACE, "localEntry");
    public static final QName REGISTRY_ELT    = new QName(SYNAPSE_NAMESPACE, "registry");
    public static final QName TASK_ELT        = new QName(SYNAPSE_NAMESPACE, "task");
    public static final QName PROXY_ELT       = new QName(SYNAPSE_NAMESPACE, "proxy");
    public static final String NULL_NAMESPACE = "";
    public static final Object QUARTZ_QNAME   =
        new QName("http://www.opensymphony.com/quartz/JobSchedulingData", "quartz");

	/** The Trace attribute name, for proxy services, sequences */
	public static final String TRACE_ATTRIB_NAME = "trace";
	/** The Trace value 'enable' */
	public static final String TRACE_ENABLE = "enable";
	/** The Trace value 'disable' */
	public static final String TRACE_DISABLE = "disable";

	/** The statistics attribute name */
	public static final String STATISTICS_ATTRIB_NAME = "statistics";
	/** The statistics value 'enable' */
	public static final String STATISTICS_ENABLE = "enable";
	/** The statistics value 'disable' */
	public static final String STATISTICS_DISABLE = "disable";

	public static final String SUSPEND_DURATION_ON_FAILURE = "suspendDurationOnFailure";
	public static final String ALGORITHM_NAME = "policy";

    public static final String ONREJECT = "onReject";
	public static final String ONACCEPT = "onAccept";
}
