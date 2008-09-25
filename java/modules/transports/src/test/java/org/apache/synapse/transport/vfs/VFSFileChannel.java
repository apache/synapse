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

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.AxisService;
import org.apache.synapse.transport.testkit.axis2.AxisServiceConfigurator;

public class VFSFileChannel implements AxisServiceConfigurator {
    private final String path;
    private File requestFile;
    
    public VFSFileChannel(String path) {
        this.path = path;
    }

    public File getRequestFile() {
        return requestFile;
    }

    public EndpointReference getEndpointReference() throws Exception {
        return new EndpointReference("vfs:" + requestFile.getAbsoluteFile().toURL());
    }

    public void setupService(AxisService service, boolean isClientSide) throws Exception {
        service.addParameter("transport.vfs.FileURI", "vfs:" + requestFile.toURL());
        service.addParameter("transport.PollInterval", "50ms");
        service.addParameter("transport.vfs.ActionAfterProcess", "DELETE");
    }
    
    protected static File preparePath(VFSTestEnvironment env, String path) {
        File file = new File(env.getRootDir(), path);
        file.getParentFile().mkdirs();
        file.delete();
        return file;
    }
    
    @SuppressWarnings("unused")
    private void setUp(VFSTestEnvironment env) throws Exception {
        requestFile = preparePath(env, path);
    }
}
