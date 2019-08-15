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

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMNode;
import org.w3c.dom.Node;

/**
 * Utility class with AXIOM helper methods.
 */
public class AXIOMUtils {
    private AXIOMUtils() {}
    
    /**
     * Get a {@link Source} backed by a given AXIOM node.
     * 
     * @param node an AXIOM node
     * @return a {@link Source} object that can be used with XSL transformers,
     *         schema validators, etc.
     */
    public static Source asSource(OMNode node) {
        // Note: Once we depend on JDK 1.6, we could also use StAXSource from JAXP 1.4.
        if (node instanceof Node) {
            return new DOMSource((Node)node);
        } else {
            return ((OMContainer)node).getSAXSource(true);
        }
    }
}
