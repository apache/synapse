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

import org.apache.synapse.transport.base.AbstractTransportSender;
import org.apache.synapse.transport.base.BaseUtils;
import org.apache.synapse.transport.base.BaseTransportException;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.vfs.*;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMOutputFormat;

import java.io.OutputStream;

public class VFSTransportSender extends AbstractTransportSender {

    public static final String TRANSPORT_NAME = "vfs";

    /** The VFS file system manager */
    private FileSystemManager fsManager = null;

    /**
     * The public constructor
     */
    public VFSTransportSender() {
        log = LogFactory.getLog(VFSTransportSender.class);
    }

    /**
     * Initialize the VFS file system manager and be ready to send messages
     * @param cfgCtx the axis2 configuration context
     * @param transportOut the transport-out description
     * @throws AxisFault on error
     */
    public void init(ConfigurationContext cfgCtx, TransportOutDescription transportOut) throws AxisFault {
        setTransportName(TRANSPORT_NAME);
        super.init(cfgCtx, transportOut);
        try {
            fsManager = VFS.getManager();
        } catch (FileSystemException e) {
            handleException("Error initializing the file transport : " + e.getMessage(), e);
        }
    }

    /**
     * Send the given message over the VFS transport
     *
     * @param msgCtx the axis2 message context
     * @return the result of the send operation / handler
     * @throws AxisFault on error
     */
    public void sendMessage(MessageContext msgCtx, String targetAddress,
        OutTransportInfo outTransportInfo) throws AxisFault {

        VFSOutTransportInfo vfsOutInfo = null;

        if (targetAddress != null) {
            vfsOutInfo = new VFSOutTransportInfo(targetAddress);
        } else if (outTransportInfo != null && outTransportInfo instanceof VFSOutTransportInfo) {
            vfsOutInfo = (VFSOutTransportInfo) outTransportInfo;
        }

        if (vfsOutInfo != null) {
            try {
                FileObject replyFile = fsManager.resolveFile(vfsOutInfo.getOutFileURI());
                if (replyFile.exists()) {

                    if (replyFile.getType() == FileType.FOLDER) {
                        // we need to write a file containing the message to this folder
                        FileObject responseFile = fsManager.resolveFile(replyFile,
                            VFSUtils.getFileName(msgCtx, vfsOutInfo));
                        if (!responseFile.exists()) {
                            responseFile.createFile();
                        }
                        populateResponseFile(responseFile, msgCtx);

                    } else if (replyFile.getType() == FileType.FILE) {
                        populateResponseFile(replyFile, msgCtx);
                        
                    } else {
                        handleException("Unsupported reply file type : " + replyFile.getType() +
                            " for file : " + vfsOutInfo.getOutFileURI());
                    }
                } else {
                    replyFile.createFile();
                    populateResponseFile(replyFile, msgCtx);
                }
            } catch (FileSystemException e) {
                handleException("Error resolving reply file : " +
                    vfsOutInfo.getOutFileURI(), e);
            }
        } else {
            handleException("Unable to determine out transport information to send message");
        }
    }

    private void populateResponseFile(FileObject responseFile, MessageContext msgContext) throws AxisFault {

        OMOutputFormat format = BaseUtils.getOMOutputFormat(msgContext);
        MessageFormatter messageFormatter = null;
        try {
            messageFormatter = TransportUtils.getMessageFormatter(msgContext);
        } catch (AxisFault axisFault) {
            throw new BaseTransportException("Unable to get the message formatter to use");
        }

        try {
            OutputStream os = responseFile.getContent().getOutputStream();
            messageFormatter.writeTo(msgContext, format, os, true);
        } catch (FileSystemException e) {
            handleException("IO Error while creating response file : " + responseFile.getName(), e);
        }
    }
}
