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

import org.apache.axis2.transport.OutTransportInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The VFS OutTransportInfo is a holder of information to send an outgoing message
 * (e.g. a Response) to a VFS destination. Thus at a minimum a reference to a
 * File URI (i.e. directory or a file) are held
 */
public class VFSOutTransportInfo implements OutTransportInfo {

    private static final Log log = LogFactory.getLog(VFSOutTransportInfo.class);

    private String outFileURI = null;
    private String outFileName = null;
    private String contentType = null;

    VFSOutTransportInfo(String outFileURI) {
        if (outFileURI.startsWith(VFSConstants.VFS_PREFIX)) {
            this.outFileURI = outFileURI.substring(VFSConstants.VFS_PREFIX.length());
        } else {
            this.outFileURI = outFileURI;
        }
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getOutFileURI() {
        return outFileURI;
    }

    public String getOutFileName() {
        return outFileName;
    }
}
