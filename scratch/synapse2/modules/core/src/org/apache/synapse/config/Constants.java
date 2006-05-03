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
package org.apache.synapse.config;

import javax.xml.namespace.QName;

/**
 * <p/>
 * Constants used in the XML processing
 */
public interface Constants {
    public static final QName DEFINITIONS_ELT = new QName("definitions");
    public static final QName SEQUENCE_ELT = new QName("sequence");
    public static final QName ENDPOINT_ELT = new QName("endpoint");
    public static final QName PROPERTY_ELT = new QName("set-property");

    public static final QName RULES_ELT = new QName("rules");

    public static final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";
    public static final String SYNAPSE = "synapse";
    public static final String NULL_NAMESPACE = "";
}
