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
package org.apache.synapse.libraries.util;

import org.apache.synapse.libraries.AbstractLibTestCase;
import org.apache.synapse.libraries.model.Library;

import javax.xml.namespace.QName;

/**
 * Created by IntelliJ IDEA.
 * User: charitha
 * Date: 3/30/12
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class LibDeployerUtilsTest extends AbstractLibTestCase {

    String path;

    public void setUp() {
        path = getResourcePath();
    }

    public void testCreateSynapseLibrary() {
        Library library = LibDeployerUtils.createSynapseLibrary(path);
        assertNotNull(library);
        assertEquals(new QName("org.apache.synapse.linkedin","SynapseLinkedinLib"), library.getQName());
        assertEquals("SynapseLinkedinLib", library.getQName().getLocalPart());
        assertEquals("org.apache.synapse.linkedin", library.getPackage());
        assertEquals("synapse library for Linkedin", library.getDescription());
    }

}
