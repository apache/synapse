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

package org.apache.synapse.mediators.dblookup;

import org.apache.axiom.om.xpath.AXIOMXPath;
import org.jaxen.JaxenException;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class Query {
    String rawStatement = null;
    List parameters = new ArrayList();
    PreparedStatement statement = null;

    public PreparedStatement getStatement() {
        return statement;
    }

    public void setStatement(PreparedStatement statement) {
        this.statement = statement;
    }

    public void addParameter(String propertyName, String xpath, String type) throws JaxenException {
        parameters.add(new Parameter(propertyName, xpath, type));
    }

    public List getParameters() {
        return parameters;
    }

    public class Parameter {
        String propertyName = null;
        AXIOMXPath xpath = null;
        int type = 0;

        Parameter(String value, String xpath, String type) throws JaxenException {
            this.propertyName = value;
            if (xpath != null) {
                this.xpath = new AXIOMXPath(xpath);
            }

            if ("string".equals(type)) {
                this.type = Types.VARCHAR;
            } else if ("int".equals(type)) {
                this.type = Types.INTEGER;
            } else {
                // todo
            }
        }

        public String getPropertyName() {
            return propertyName;
        }

        public AXIOMXPath getXpath() {
            return xpath;
        }

        public int getType() {
            return type;
        }
    }

    public String getRawStatement() {
        return rawStatement;
    }

    public void setRawStatement(String rawStatement) {
        this.rawStatement = rawStatement;
    }
}

