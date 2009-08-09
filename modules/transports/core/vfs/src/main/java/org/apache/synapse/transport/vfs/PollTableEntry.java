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

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.transport.base.AbstractPollTableEntry;
import org.apache.axis2.transport.base.ParamUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Holds information about an entry in the VFS transport poll table used by the
 * VFS Transport Listener
 */
public class PollTableEntry extends AbstractPollTableEntry {

    // operation after scan
    public static final int DELETE = 0;
    public static final int MOVE   = 1;

    /** File or Directory to scan */
    private String fileURI;
    /** The URI to send replies to. May be null. */
    private String replyFileURI;
    /** file name pattern for a directory or compressed file entry */
    private String fileNamePattern;
    /** Content-Type to use for the message */
    private String contentType;

    /** action to take after a successful poll */
    private int actionAfterProcess = DELETE;
    /** action to take after a poll with errors */
    private int actionAfterErrors = DELETE;
    /** action to take after a failed poll */
    private int actionAfterFailure = DELETE;

    /** where to move the file after processing */
    private String moveAfterProcess;
    /** where to move the file after encountering some errors */
    private String moveAfterErrors;
    /** where to move the file after total failure */
    private String moveAfterFailure;
    /** moved file will have this formatted timestamp prefix */    
    private DateFormat moveTimestampFormat;

    private boolean streaming;

    private int maxRetryCount;
    private long reconnectTimeout;

    @Override
    public EndpointReference[] getEndpointReferences(String ip) {
        return new EndpointReference[] { new EndpointReference("vfs:" + fileURI) };
    }

    public String getFileURI() {
        return fileURI;
    }

    public String getReplyFileURI() {
        return replyFileURI;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public String getContentType() {
        return contentType;
    }

    public int getActionAfterProcess() {
        return actionAfterProcess;
    }

    public int getActionAfterErrors() {
        return actionAfterErrors;
    }

    public int getActionAfterFailure() {
        return actionAfterFailure;
    }

    public String getMoveAfterProcess() {
        return moveAfterProcess;
    }

    private void setMoveAfterProcess(String moveAfterProcess) {
        if (moveAfterProcess == null) {
            this.moveAfterProcess = null;
        } else if (moveAfterProcess.startsWith(VFSConstants.VFS_PREFIX)) {
            // to recover a good directory location if user entered with the vfs: prefix
            // because transport uris are given like that
            this.moveAfterProcess = moveAfterProcess.substring(VFSConstants.VFS_PREFIX.length());
        } else {
            this.moveAfterProcess = moveAfterProcess;
        }
    }

    public String getMoveAfterErrors() {
        return moveAfterErrors;
    }

    private void setMoveAfterErrors(String moveAfterErrors) {
        if (moveAfterErrors == null) {
            this.moveAfterErrors = null;
        } else if (moveAfterErrors.startsWith(VFSConstants.VFS_PREFIX)) {
            this.moveAfterErrors = moveAfterErrors.substring(VFSConstants.VFS_PREFIX.length());
        } else {
            this.moveAfterErrors = moveAfterErrors;
        }  
    }

    public String getMoveAfterFailure() {
        return moveAfterFailure;
    }

    private void setMoveAfterFailure(String moveAfterFailure) {
        if (moveAfterFailure == null) {
            this.moveAfterFailure = null;
        } else if (moveAfterFailure.startsWith(VFSConstants.VFS_PREFIX)) {
            this.moveAfterFailure = moveAfterFailure.substring(VFSConstants.VFS_PREFIX.length());
        } else {
            this.moveAfterFailure = moveAfterFailure;
        }
    }

    public boolean isStreaming() {
        return streaming;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public long getReconnectTimeout() {
        return reconnectTimeout;
    }

    public DateFormat getMoveTimestampFormat() {
        return moveTimestampFormat;
    }

    @Override
    public boolean loadConfiguration(ParameterInclude params) throws AxisFault {
        
        fileURI = ParamUtils.getOptionalParam(params, VFSConstants.TRANSPORT_FILE_FILE_URI);
        if (fileURI == null) {
            return false;
        } else {
            
            if (fileURI.startsWith(VFSConstants.VFS_PREFIX)) {
                fileURI = fileURI.substring(VFSConstants.VFS_PREFIX.length());
            }
            
            replyFileURI = ParamUtils.getOptionalParam(params, VFSConstants.REPLY_FILE_URI);
            fileNamePattern = ParamUtils.getOptionalParam(params,
                    VFSConstants.TRANSPORT_FILE_FILE_NAME_PATTERN);

            contentType = ParamUtils.getRequiredParam(params,
                    VFSConstants.TRANSPORT_FILE_CONTENT_TYPE);
            String option = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_PROCESS);
            actionAfterProcess = VFSTransportListener.MOVE.equals(option) ?
                    PollTableEntry.MOVE : PollTableEntry.DELETE;

            option = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_ERRORS);
            actionAfterErrors = VFSTransportListener.MOVE.equals(option) ?
                    PollTableEntry.MOVE : PollTableEntry.DELETE;

            option = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_FAILURE);
            actionAfterFailure = VFSTransportListener.MOVE.equals(option) ?
                    PollTableEntry.MOVE : PollTableEntry.DELETE;

            String moveDirectoryAfterProcess = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_PROCESS);
            setMoveAfterProcess(moveDirectoryAfterProcess);

            String moveDirectoryAfterErrors = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_ERRORS);
            setMoveAfterErrors(moveDirectoryAfterErrors);

            String moveDirectoryAfterFailure = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_FAILURE);
            setMoveAfterFailure(moveDirectoryAfterFailure);

            String moveFileTimestampFormat = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_MOVE_TIMESTAMP_FORMAT);
            if(moveFileTimestampFormat != null) {
                moveTimestampFormat = new SimpleDateFormat(moveFileTimestampFormat);
            }

            String strStreaming = ParamUtils.getOptionalParam(params, VFSConstants.STREAMING);
            if (strStreaming != null) {
                streaming = Boolean.parseBoolean(strStreaming);
            }
            
            String strMaxRetryCount = ParamUtils.getOptionalParam(
                    params, VFSConstants.MAX_RETRY_COUNT);
            maxRetryCount = strMaxRetryCount != null ? Integer.parseInt(strMaxRetryCount) :
                    VFSConstants.DEFAULT_MAX_RETRY_COUNT;

            String strReconnectTimeout = ParamUtils.getOptionalParam(
                    params, VFSConstants.RECONNECT_TIMEOUT);
            reconnectTimeout = strReconnectTimeout != null ?
                    Integer.parseInt(strReconnectTimeout) * 1000 :
                    VFSConstants.DEFAULT_RECONNECT_TIMEOUT;
            
            return super.loadConfiguration(params);
        }
    }
}
