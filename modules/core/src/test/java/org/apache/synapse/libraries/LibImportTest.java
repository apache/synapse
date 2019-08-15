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
package org.apache.synapse.libraries;

import junit.framework.TestCase;
import org.apache.synapse.libraries.imports.SynapseImport;
import org.apache.synapse.libraries.model.Library;
import org.apache.synapse.libraries.util.LibDeployerUtils;
import org.apache.synapse.libraries.util.LibDeployerUtilsTest;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: charitha
 * Date: 4/16/12
 * Time: 4:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class LibImportTest extends AbstractLibTestCase {


    public void testValidImports() throws URISyntaxException {
        Library library = LibDeployerUtils.createSynapseLibrary(getResourcePath());
        SynapseImport validSynImport = new SynapseImport();
        validSynImport.setLibName("SynapseLinkedinLib");
        validSynImport.setLibPackage("org.apache.synapse.linkedin");
        if (validSynImport != null) {
            LibDeployerUtils.loadLibArtifacts(validSynImport, library);
        }
        assertEquals("SynapseLinkedinLib", library.getQName().getLocalPart());
        assertEquals("org.apache.synapse.linkedin", library.getPackage());
        assertEquals("synapse library for Linkedin", library.getDescription());
        assertNotNull(library.getArtifact("org.apache.synapse.linkedin.post_status"));
        assertNotNull(library.getArtifact("org.apache.synapse.linkedin.show_headline"));
        assertNotNull(library.getArtifact("org.apache.synapse.linkedin.send_message"));
        assertNotNull(library.getArtifact("org.apache.synapse.linkedin.register_user"));
        library.unLoadLibrary();
        assertEquals("SynapseLinkedinLib", library.getQName().getLocalPart());
        assertEquals("org.apache.synapse.linkedin", library.getPackage());
        assertEquals("synapse library for Linkedin", library.getDescription());
        assertNull(library.getArtifact("org.apache.synapse.linkedin.post_status"));
        assertNull(library.getArtifact("org.apache.synapse.linkedin.show_headline"));
        assertNull(library.getArtifact("org.apache.synapse.linkedin.send_message"));
        assertNull(library.getArtifact("org.apache.synapse.linkedin.register_user"));
    }

    public void testInValidImportsCaseOne() {
        Library library = LibDeployerUtils.createSynapseLibrary(getResourcePath());
        SynapseImport invalidSynImport = new SynapseImport();
        invalidSynImport.setLibName("testSynapseLinkedinLib");
        invalidSynImport.setLibPackage("org.apache.synapse.linkedin");
        if (invalidSynImport != null) {
            LibDeployerUtils.loadLibArtifacts(invalidSynImport, library);
        }
        assertEquals("SynapseLinkedinLib", library.getQName().getLocalPart());
        assertEquals("org.apache.synapse.linkedin", library.getPackage());
        assertNull(library.getArtifact("org.apache.synapse.linkedin.post_status"));
    }

    public void testInValidImportsCaseTwo() {
        Library library = LibDeployerUtils.createSynapseLibrary(getResourcePath());
        SynapseImport invalidSynImport = new SynapseImport();
        invalidSynImport.setLibName("SynapseLinkedinLib");
        invalidSynImport.setLibPackage("test.org.apache.synapse.linkedin");
        if (invalidSynImport != null) {
            LibDeployerUtils.loadLibArtifacts(invalidSynImport, library);
        }
        assertEquals("SynapseLinkedinLib", library.getQName().getLocalPart());
        assertEquals("org.apache.synapse.linkedin", library.getPackage());
        assertNull(library.getArtifact("org.apache.synapse.linkedin.post_status"));
    }

    public void testInValidImportsCaseThree() {
        Library library = LibDeployerUtils.createSynapseLibrary(getResourcePath());
        SynapseImport invalidSynImport = new SynapseImport();
        invalidSynImport.setLibName("testSynapseLinkedinLib");
        invalidSynImport.setLibPackage("test.org.apache.synapse.linkedin");
        if (invalidSynImport != null) {
            LibDeployerUtils.loadLibArtifacts(invalidSynImport, library);
        }
        assertEquals("SynapseLinkedinLib", library.getQName().getLocalPart());
        assertEquals("org.apache.synapse.linkedin", library.getPackage());
        assertNull(library.getArtifact("org.apache.synapse.linkedin.post_status"));
    }

}
