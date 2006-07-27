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
    public static final String OUTFLOW_SECURITY  = "OutflowSecurity";
    public static final String INFLOW_SECURITY   = "InflowSecurity";
}
