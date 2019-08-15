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
package org.apache.synapse.mediators;

import javax.xml.namespace.QName;

import org.apache.synapse.config.xml.XMLConfigConstants;

/**
 * A template parameter is a variable which is used when creating templates and contains following configuration
 * parameter name="p1" [default="value|expression"] [optional=(true|false)]
 *
 */
public class TemplateParameter {
    public static final QName PARAMETER_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "parameter");
    public static final QName ATT_NAME_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "name");
    public static final QName ATT_DEFAULT_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "default");
    public static final QName ATT_OPTIONAL_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "optional");


    private String name;
    private Value defaultValue;
    private boolean isOptional;

    public TemplateParameter() {
        this.name = null;
        this.defaultValue = null;
        this.isOptional = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Value getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Value defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public void setOptional(boolean optional) {
        isOptional = optional;
    }

}
