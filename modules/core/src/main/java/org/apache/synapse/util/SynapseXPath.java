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
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.om.impl.llom.OMDocumentImpl;
import org.apache.synapse.mediators.GetPropertyFunction;
import org.apache.synapse.SynapseConstants;
import org.jaxen.JaxenException;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.XPathFunctionContext;

import java.util.List;

/**
 * 
 */
public class SynapseXPath extends AXIOMXPath {

    private boolean bodyRelative;

    public SynapseXPath(String xpathString) throws JaxenException {
        super(xpathString);
    }

    public SynapseXPath(String xpathString, boolean bodyRelative) throws JaxenException {
        super(xpathString);
        this.bodyRelative = bodyRelative;
    }

    public boolean isBodyRelative() {
        return bodyRelative;
    }

    public void setBodyRelative(boolean bodyRelative) {
        this.bodyRelative = bodyRelative;
    }
}
