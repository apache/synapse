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
    public static final QName DEFINITIONS_ELT   = new QName(Constants.SYNAPSE_NAMESPACE, "definitions");
    public static final QName SEQUENCE_ELT      = new QName(Constants.SYNAPSE_NAMESPACE, "sequence");
    public static final QName ENDPOINT_ELT      = new QName(Constants.SYNAPSE_NAMESPACE, "endpoint");
    public static final QName PROPERTY_ELT      = new QName(Constants.SYNAPSE_NAMESPACE, "set-property");
    public static final QName RULES_ELT         = new QName(Constants.SYNAPSE_NAMESPACE, "rules");
    public static final QName REGISTRY_ELT      = new QName(Constants.SYNAPSE_NAMESPACE, "registry");
    public static final QName PROXIES_ELT       = new QName(Constants.SYNAPSE_NAMESPACE, "proxies");
    public static final QName PROXY_ELT         = new QName(Constants.SYNAPSE_NAMESPACE, "proxy");

    public static final String SYNAPSE_NAMESPACE = org.apache.synapse.Constants.SYNAPSE_NAMESPACE;
    public static final String NULL_NAMESPACE    = "";
    public static final String RAMPART_POLICY    = "rampartPolicy";
    public static final String SANDESHA_POLICY   = "sandeshaPolicy";

    /** The trace local name */
    public static final String TRACE_ATTRIB_NAME ="trace";
    /** The Trace value 'enable' */
    public static final String TRACE_ENABLE ="enable";
    /** The Trace value 'disable' */
    public static final  String TRACE_DISABLE ="disable";
    
    // -- variables for the scoping of a property mediator --
    /** The String value for a Synapse messagecontext property */
    public static final String SCOPE_CORRELATE = org.apache.synapse.Constants.SCOPE_CORRELATE;
    /** The String value for an Axis2 messagecontext property */
    public static final String SCOPE_AXIS2 = org.apache.synapse.Constants.SCOPE_AXIS2;
}
