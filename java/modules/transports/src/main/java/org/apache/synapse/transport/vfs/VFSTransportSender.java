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
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.vfs.*;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.llom.OMSourcedElementImpl;

import javax.activation.DataHandler;
import javax.xml.stream.XMLStreamException;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 * axis2.xml - transport definition
 *  <transportSender name="file" class="org.apache.synapse.transport.vfs.VFSTransportSender"/>
 */
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
                replyFile = fsManager.resolveFile(vfsOutInfo.getOutFileURI());
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

    private void populateResponseFile(FileObject responseFile, MessageContext msgContext) throws AxisFault {

        // check the first element of the SOAP body, do we have content wrapped using the
        // default wrapper elements for binary (BaseConstants.DEFAULT_BINARY_WRAPPER) or
        // text (BaseConstants.DEFAULT_TEXT_WRAPPER) ? If so, do not create SOAP messages
        // within the files but just get the payload in its native format

        OMElement firstChild = msgContext.getEnvelope().getBody().getFirstElement();
        if (firstChild != null) {
            if (BaseConstants.DEFAULT_BINARY_WRAPPER.equals(firstChild.getQName())) {
                try {
                    OutputStream os = responseFile.getContent().getOutputStream();
                    OMNode omNode = firstChild.getFirstOMChild();
                    if (omNode != null && omNode instanceof OMText) {
                        Object dh = ((OMText) omNode).getDataHandler();
                        if (dh != null && dh instanceof DataHandler) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            try {
                                ((DataHandler) dh).writeTo(baos);
                            } catch (IOException e) {
                                handleException("Error serializing binary content of element : " +
                                    BaseConstants.DEFAULT_BINARY_WRAPPER, e);
                            }
                            os.write(baos.toByteArray());
                        }
                    }
                } catch (FileSystemException e) {
                    handleException("Error getting an output stream to file : " +
                        responseFile.getName().getBaseName(), e);
                } catch (IOException e) {
                    handleException("Error getting binary content of message", e);
                }

            } else if (BaseConstants.DEFAULT_TEXT_WRAPPER.equals(firstChild.getQName())) {
                try {
                    OutputStream os = responseFile.getContent().getOutputStream();

                    if (firstChild instanceof OMSourcedElementImpl) {
                        firstChild.serializeAndConsume(os);
                    } else {
                        os.write(firstChild.getText().getBytes());
                    }
                } catch (FileSystemException e) {
                    handleException("Error getting an output stream to file : " +
                        responseFile.getName().getBaseName(), e);
                } catch (IOException e) {
                    handleException("Error getting text content of message as bytes", e);
                } catch (XMLStreamException e) {
                    handleException("Error serializing OMSourcedElement content", e);
                }
            } else {
                populateSOAPFile(responseFile, msgContext);
            }
        } else {
            populateSOAPFile(responseFile, msgContext);
        }
    }

    /**
     * Populate file with a SOAP formatted message
     * @param responseFile the response file created
     * @param msgContext the message context that holds the message to be written
     * @throws AxisFault on error
     */
    private void populateSOAPFile(FileObject responseFile, MessageContext msgContext) throws AxisFault {
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
