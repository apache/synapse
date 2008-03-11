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

package org.apache.synapse.util;

import org.apache.axiom.om.xpath.AXIOMXPath;

/**
 * 
 */
public class SynapseXPath {

    private AXIOMXPath aXIOMXPath;
    private boolean bodyRelative;

    public SynapseXPath(AXIOMXPath aXIOMXPath) {
        this.aXIOMXPath = aXIOMXPath;
    }

    public SynapseXPath(AXIOMXPath aXIOMXPath, boolean bodyRelative) {
        this.aXIOMXPath = aXIOMXPath;
        this.bodyRelative = bodyRelative;
    }

    public AXIOMXPath getAXIOMXPath() {
        return aXIOMXPath;
    }

    public void setAXIOMXPath(AXIOMXPath aXIOMXPath) {
        this.aXIOMXPath = aXIOMXPath;
    }

    public boolean isBodyRelative() {
        return bodyRelative;
    }

    public void setBodyRelative(boolean bodyRelative) {
        this.bodyRelative = bodyRelative;
    }
}
