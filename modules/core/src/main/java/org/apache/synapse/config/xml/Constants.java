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

import javax.xml.namespace.QName;

/**
 * Constants used in the XML processing
 */
public interface Constants {
	public static final QName DEFINITIONS_ELT = new QName(
			Constants.SYNAPSE_NAMESPACE, "definitions");

	public static final QName SEQUENCE_ELT = new QName(
			Constants.SYNAPSE_NAMESPACE, "sequence");

	public static final QName ENDPOINT_ELT = new QName(
			Constants.SYNAPSE_NAMESPACE, "endpoint");

	public static final QName ENTRY_ELT = new QName(
			Constants.SYNAPSE_NAMESPACE, "localEntry");

	public static final QName REGISTRY_ELT = new QName(
			Constants.SYNAPSE_NAMESPACE, "registry");

	public static final QName STARTUP_ELT = new QName(
			Constants.SYNAPSE_NAMESPACE, "startup");

	public static final Object QUARTZ_QNAME = new QName("http://www.opensymphony.com/quartz/JobSchedulingData", "quartz");
	
	public static final QName PROXY_ELT = new QName(
			Constants.SYNAPSE_NAMESPACE, "proxy");

	public static final String SYNAPSE_NAMESPACE = org.apache.synapse.Constants.SYNAPSE_NAMESPACE;

	public static final String NULL_NAMESPACE = "";

	public static final String RAMPART_POLICY = "rampartPolicy";

	public static final String SANDESHA_POLICY = "sandeshaPolicy";

	/** The Trace attribute name */
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

	// -- variables for the scoping of a property mediator --
	/** The String value for an Axis2 messagecontext property */
	public static final String SCOPE_AXIS2 = org.apache.synapse.Constants.SCOPE_AXIS2;

	/**
	 * The scope for a set-property mediator, when the property should be set on
	 * the underlying transport
	 */
	String SCOPE_TRANSPORT = org.apache.synapse.Constants.SCOPE_TRANSPORT;

	// -- Synapse message context property keys --
	/** The scope for the synapse message context properties */
	String SCOPE_DEFAULT = org.apache.synapse.Constants.SCOPE_DEFAULT;

	// -- Synapse property values for WS-RM sequence handling --
	/** The String value for a WS-RM version 1.0 */
	public static final String SEQUENCE_VERSION_1_0 = org.apache.synapse.Constants.SEQUENCE_VERSION_1_0;

	/** The String value for a WS-RM version 1.1 */
	public static final String SEQUENCE_VERSION_1_1 = org.apache.synapse.Constants.SEQUENCE_VERSION_1_1;

	

	// -- Synapse Send mediator releated constants -- //
	String SEND_ELEMENT = "send";

	String LOADBALANCE_ELEMENT = "loadbalance";

	/** failover only element */
	String FAILOVER_ELEMENT = "failover";

	String SUSPEND_DURATION_ON_FAILURE = "suspendDurationOnFailure";

	String MAXIMUM_RETRIES = "maximumRetries";

	String RETRY_INTERVAL = "retryInterval";

	/** failover attribute in the loadbalance element */
	String FAILOVER = "failover";

	String SESSION_AFFINITY = "sessionAffinity";

	String ALGORITHM_NAME = "policy";

	/** failover group element inside the loadbalance element */
	String FAILOVER_GROUP_ELEMENT = "failover";

	String DISPATCH_MANAGER = "DISPATCH_MANAGER";

	String DISPATCHERS_ELEMENT = "dispatchers";

	String DISPATCHER_ELEMENT = "dispatcher";

	QName ATT_KEY_Q = new QName(NULL_NAMESPACE, "key");

	QName ATT_ADDRESS_Q = new QName(NULL_NAMESPACE, "address");

	String ONREJECT = "onReject";

	String ONACCEPT = "onAccept";
}
