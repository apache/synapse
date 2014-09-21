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
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

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
    private int maxRetryCount = 3;
    private long reconnectTimeout = 30000;
    private boolean append;
    private boolean fileLocking;
    private boolean isUseTempFile = false;

    /**
     * Constructs the VFSOutTransportInfo containing the information about the file to which the
     * response has to be submitted to.
     *
     * @param outFileURI URI of the file to which the message is delivered
     */
    VFSOutTransportInfo(String outFileURI, boolean fileLocking) {
        if (outFileURI.startsWith(VFSConstants.VFS_PREFIX)) {
            this.outFileURI = outFileURI.substring(VFSConstants.VFS_PREFIX.length());
        } else {
            this.outFileURI = outFileURI;
        }

        Map<String, String> properties = BaseUtils.getEPRProperties(outFileURI);
        if (properties.containsKey(VFSConstants.MAX_RETRY_COUNT)) {
            String strMaxRetryCount = properties.get(VFSConstants.MAX_RETRY_COUNT);
            maxRetryCount = Integer.parseInt(strMaxRetryCount);
        } else {
            maxRetryCount = VFSConstants.DEFAULT_MAX_RETRY_COUNT;
        }

        if (properties.containsKey(VFSConstants.TRANSPORT_FILE_USE_TEMP_FILE)) {
            String useTempFile = properties.get(VFSConstants.TRANSPORT_FILE_USE_TEMP_FILE);
            isUseTempFile = Boolean.valueOf(useTempFile);
        }

        if (properties.containsKey(VFSConstants.RECONNECT_TIMEOUT)) {
            String strReconnectTimeout = properties.get(VFSConstants.RECONNECT_TIMEOUT);
            reconnectTimeout = Long.parseLong(strReconnectTimeout) * 1000;
        } else {
            reconnectTimeout = VFSConstants.DEFAULT_RECONNECT_TIMEOUT;
        }

        if (properties.containsKey(VFSConstants.TRANSPORT_FILE_LOCKING)) {
            String strFileLocking = properties.get(VFSConstants.TRANSPORT_FILE_LOCKING);
            if (VFSConstants.TRANSPORT_FILE_LOCKING_ENABLED.equals(strFileLocking)) {
                this.fileLocking = true;
            } else if (VFSConstants.TRANSPORT_FILE_LOCKING_DISABLED.equals(strFileLocking)) {
                this.fileLocking = false;
            }
        } else {
            this.fileLocking = fileLocking;
        }

        if (properties.containsKey(VFSConstants.APPEND)) {
            append = Boolean.parseBoolean(properties.get(VFSConstants.APPEND));
        }

        if (log.isDebugEnabled()) {
            log.debug("Using the fileURI        : " + this.outFileURI);
            log.debug("Using the maxRetryCount  : " + maxRetryCount);
            log.debug("Using the reconnectionTimeout : " + reconnectTimeout);
            log.debug("Using the append         : " + append);
            log.debug("File locking             : " + (this.fileLocking ? "ON" : "OFF"));
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

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public long getReconnectTimeout() {
        return reconnectTimeout;
    }

    public boolean isUseTempFile() {
        return isUseTempFile;
    }

    public void setReconnectTimeout(long reconnectTimeout) {
        this.reconnectTimeout = reconnectTimeout;
    }

    public boolean isAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isFileLockingEnabled() {
        return fileLocking;
    }
}