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

package org.apache.synapse.samples.framework.config;

public class SynapseServerConfiguration {

    private String serverName;
    private String synapseHome;
    private String axis2Repo;
    private String axis2Xml;
    private String synapseXml;
    private boolean clusteringEnabled;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getSynapseHome() {
        return synapseHome;
    }

    public void setSynapseHome(String synapseHome) {
        this.synapseHome = synapseHome;
    }

    public String getAxis2Repo() {
        return axis2Repo;
    }

    public void setAxis2Repo(String axis2Repo) {
        this.axis2Repo = axis2Repo;
    }

    public String getAxis2Xml() {
        return axis2Xml;
    }

    public void setAxis2Xml(String axis2Xml) {
        this.axis2Xml = axis2Xml;
    }

    public String getSynapseXml() {
        return synapseXml;
    }

    public void setSynapseXml(String synapseXml) {
        this.synapseXml = synapseXml;
    }

    public boolean isClusteringEnabled() {
        return clusteringEnabled;
    }

    public void setClusteringEnabled(boolean clusteringEnabled) {
        this.clusteringEnabled = clusteringEnabled;
    }
}
