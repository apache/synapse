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

import org.apache.synapse.transport.base.*;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.vfs.*;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMOutputFormat;

import java.io.IOException;

/**
 * axis2.xml - transport definition
 *  <transportSender name="file" class="org.apache.synapse.transport.vfs.VFSTransportSender"/>
 */
public class VFSTransportSender extends AbstractTransportSender implements ManagementSupport {

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
            StandardFileSystemManager fsm = new StandardFileSystemManager();
            fsm.setConfiguration(getClass().getClassLoader().getResource("providers.xml"));
            fsm.init();
            fsManager = fsm;
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
            FileObject replyFile = null;
            try {
                
                boolean wasError = true;
                int retryCount = 0;
                int maxRetryCount = VFSUtils.getMaxRetryCount(msgCtx, vfsOutInfo);
                long reconnectionTimeout = VFSUtils.getReconnectTimout(msgCtx, vfsOutInfo);
                boolean append = vfsOutInfo.isAppend();
                
                while(wasError == true) {
                  try {
                    retryCount++;
                    replyFile = fsManager.resolveFile(vfsOutInfo.getOutFileURI());
                    
                    if(replyFile == null) {
                      log.error("replyFile is null");
                      throw new FileSystemException("replyFile is null");
                    }
                    
                    wasError = false;
                                        
                  } catch(FileSystemException e) {
                    log.error("cannot resolve replyFile", e);
                    if(maxRetryCount <= retryCount)
                      handleException("cannot resolve replyFile repeatedly: " + e.getMessage(), e);
                  }
                
                  if(wasError == true) {
                    try {
                      Thread.sleep(reconnectionTimeout);
                    } catch (InterruptedException e2) {
                      e2.printStackTrace();
                    }
                  }
                }
                
                if (replyFile.exists()) {

                    if (replyFile.getType() == FileType.FOLDER) {
                        // we need to write a file containing the message to this folder
                        FileObject responseFile = fsManager.resolveFile(replyFile,
                            VFSUtils.getFileName(msgCtx, vfsOutInfo));
                        if (!responseFile.exists()) {
                            responseFile.createFile();
                        }
                        populateResponseFile(responseFile, msgCtx, append);

                    } else if (replyFile.getType() == FileType.FILE) {
                        populateResponseFile(replyFile, msgCtx, append);
                        
                    } else {
                        handleException("Unsupported reply file type : " + replyFile.getType() +
                            " for file : " + vfsOutInfo.getOutFileURI());
                    }
                } else {
                    replyFile.createFile();
                    populateResponseFile(replyFile, msgCtx, append);
                }
            } catch (FileSystemException e) {
                handleException("Error resolving reply file : " +
                    vfsOutInfo.getOutFileURI(), e);
            } finally {
                if (replyFile != null) {
                    try {
                        replyFile.close();
                    } catch (FileSystemException ignore) {}
                }
            }
        } else {
            handleException("Unable to determine out transport information to send message");
        }
    }

    private void populateResponseFile(FileObject responseFile, MessageContext msgContext, boolean append) throws AxisFault {
        MessageFormatter messageFormatter = BaseUtils.getMessageFormatter(msgContext);
        OMOutputFormat format = BaseUtils.getOMOutputFormat(msgContext);
        try {
            CountingOutputStream os = new CountingOutputStream(responseFile.getContent().getOutputStream(append));
            try {
                messageFormatter.writeTo(msgContext, format, os, true);
            } finally {
                os.close();
            }
            // update metrics
            metrics.incrementMessagesSent();
            metrics.incrementBytesSent(os.getByteCount());
        } catch (FileSystemException e) {
            metrics.incrementFaultsSending();
            handleException("IO Error while creating response file : " + responseFile.getName(), e);
        } catch (IOException e) {
            metrics.incrementFaultsSending();
            handleException("IO Error while creating response file : " + responseFile.getName(), e);
        }
    }
}
