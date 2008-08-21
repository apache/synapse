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

package org.apache.synapse.transport.vfs;

import java.io.File;

import org.apache.axis2.description.AxisService;
import org.apache.synapse.transport.testkit.TestEnvironment;

public class VFSTestEnvironment extends TestEnvironment {
    private final File rootDir;
    
    public VFSTestEnvironment(File rootDir) {
        this.rootDir = rootDir;
    }

    @SuppressWarnings("unused")
    private void setUp() {
        rootDir.mkdirs();
    }
    
    public File getRootDir() {
        return rootDir;
    }

    @Override
    public void setupContentType(AxisService service, String contentType) throws Exception {
        service.addParameter("transport.vfs.ContentType", contentType);
    }
}