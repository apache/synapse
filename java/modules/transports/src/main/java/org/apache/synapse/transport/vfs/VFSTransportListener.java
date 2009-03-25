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

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.description.*;
import org.apache.axis2.format.DataSourceMessageBuilder;
import org.apache.axis2.format.ManagedDataSource;
import org.apache.axis2.format.ManagedDataSourceFactory;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.axis2.transport.base.AbstractPollingTransportListener;
import org.apache.axis2.transport.base.ManagementSupport;
import org.apache.axis2.transport.base.ParamUtils;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.builder.SOAPBuilder;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.vfs.*;
import org.apache.commons.vfs.impl.StandardFileSystemManager;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import javax.xml.namespace.QName;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * The "vfs" transport is a polling based transport - i.e. it gets kicked off at
 * specified periodic durations, and would iterate through a list of directories or files
 * specified according to poll durations. When scanning a directory, it will match
 * its contents against a given regex to find the set of input files. For compressed
 * files, the contents could be matched against a regex to find individual files.
 * Each of these files thus found would be submitted as an Axis2 "message" into the
 * Axis2 engine.
 *
 * The processed files would be deleted or renamed as specified in the configuration
 *
 * Supported VFS example URIs
 * 
 * file:///directory/filename.ext
 * file:////somehost/someshare/afile.txt
 * jar:../lib/classes.jar!/META-INF/manifest.mf
 * zip:http://somehost/downloads/somefile.zip
 * jar:zip:outer.zip!/nested.jar!/somedir
 * jar:zip:outer.zip!/nested.jar!/some%21dir
 * tar:gz:http://anyhost/dir/mytar.tar.gz!/mytar.tar!/path/in/tar/README.txt
 * tgz:file://anyhost/dir/mytar.tgz!/somepath/somefile
 * gz:/my/gz/file.gz
 * http://somehost:8080/downloads/somefile.jar
 * http://myusername@somehost/index.html
 * webdav://somehost:8080/dist
 * ftp://myusername:mypassword@somehost/pub/downloads/somefile.tgz[?passive=true]
 * sftp://myusername:mypassword@somehost/pub/downloads/somefile.tgz
 * smb://somehost/home
 *
 * axis2.xml - transport definition
 *  <transportReceiver name="file" class="org.apache.synapse.transport.vfs.VFSTransportListener"/>
 *
 * services.xml - service attachment
 *  required parameters
 *  <parameter name="transport.vfs.FileURI">..</parameter>
 *  <parameter name="transport.vfs.ContentType">..</parameter>
 *
 *  optional parameters
 *  <parameter name="transport.vfs.FileNamePattern">..</parameter>
 *  <parameter name="transport.PollInterval">..</parameter>
 * 
 *  <parameter name="transport.vfs.ActionAfterProcess">..</parameter>
 * 	<parameter name="transport.vfs.ActionAfterErrors" >..</parameter>
 *  <parameter name="transport.vfs.ActionAfterFailure">..</parameter>
 *
 *  <parameter name="transport.vfs.ReplyFileURI" >..</parameter>
 *  <parameter name="transport.vfs.ReplyFileName">..</parameter>
 *
 * FTP testing URIs
 * ftp://ftpuser:password@asankha/somefile.csv?passive=true
 * ftp://vfs:apache@vfs.netfirms.com/somepath/somefile.xml?passive=true
 */
public class VFSTransportListener extends AbstractPollingTransportListener<PollTableEntry> 
    implements ManagementSupport {

    public static final String TRANSPORT_NAME = "vfs";

    public static final String DELETE = "DELETE";
    public static final String MOVE = "MOVE";

    /** The VFS file system manager */
    private FileSystemManager fsManager = null;

    /**
     * Initializes the VFS transport by getting the VFS File System manager
     * @param cfgCtx the Axsi2 configuration context
     * @param trpInDesc the VFS transport in description from the axis2.xml
     * @throws AxisFault on error
     */
    @Override
    public void init(ConfigurationContext cfgCtx, TransportInDescription trpInDesc)
        throws AxisFault {
        super.init(cfgCtx, trpInDesc);
        try {
            StandardFileSystemManager fsm = new StandardFileSystemManager();
            fsm.setConfiguration(getClass().getClassLoader().getResource("providers.xml"));
            fsm.init();
            fsManager = fsm;
        } catch (FileSystemException e) {
            handleException("Error initializing the file transport : " + e.getMessage(), e);
        }
    }

    @Override
    protected void poll(PollTableEntry entry) {
        scanFileOrDirectory(entry, entry.getFileURI());
    }

    /**
     * Search for files that match the given regex pattern and create a list
     * Then process each of these files and update the status of the scan on
     * the poll table
     * @param entry the poll table entry for the scan
     * @param fileURI the file or directory to be scanned
     */
    private void scanFileOrDirectory(final PollTableEntry entry, String fileURI) {

        FileObject fileObject = null;

        if (log.isDebugEnabled()) {
            log.debug("Scanning directory or file : " + fileURI);
        }

        boolean wasError = true;
        int retryCount = 0;
        int maxRetryCount = entry.getMaxRetryCount();
        long reconnectionTimeout = entry.getReconnectTimeout();

        while (wasError) {
            try {
                retryCount++;
                fileObject = fsManager.resolveFile(fileURI);

                if (fileObject == null) {
                    log.error("fileObject is null");
                    throw new FileSystemException("fileObject is null");
                }

                wasError = false;

            } catch (FileSystemException e) {
                log.error("cannot resolve fileObject", e);
                if (maxRetryCount <= retryCount)
                    processFailure("cannot resolve fileObject repeatedly: " + e.getMessage(), e, entry);
                return;
            }

            if (wasError) {
                try {
                    Thread.sleep(reconnectionTimeout);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
            }
        }

        try {
            if (fileObject.exists() && fileObject.isReadable()) {

                entry.setLastPollState(PollTableEntry.NONE);
                FileObject[] children = null;
                try {
                    children = fileObject.getChildren();
                } catch (FileSystemException ignore) {}

                // if this is a file that would translate to a single message
                if (children == null || children.length == 0) {

                    if (fileObject.getType() == FileType.FILE) {
                        try {
                            processFile(entry, fileObject);
                            entry.setLastPollState(PollTableEntry.SUCCSESSFUL);
                            metrics.incrementMessagesReceived();
                            
                        } catch (AxisFault e) {
                            entry.setLastPollState(PollTableEntry.FAILED);
                            metrics.incrementFaultsReceiving();
                        }

                        moveOrDeleteAfterProcessing(entry, fileObject);
                    }

                } else {
                    int failCount = 0;
                    int successCount = 0;

                    if (log.isDebugEnabled()) {
                        log.debug("File name pattern :" + entry.getFileNamePattern());
                    }
                    for (FileObject child : children) {
                        if (log.isDebugEnabled()) {
                            log.debug("Matching file :" + child.getName().getBaseName());
                        }
                        if ((entry.getFileNamePattern() != null)
                                && (child.getName().getBaseName().matches(entry.getFileNamePattern()))) {
                            try {
                                if (log.isDebugEnabled()) {
                                    log.debug("Processing file :" + child);
                                }
                                processFile(entry, child);
                                successCount++;
                                // tell moveOrDeleteAfterProcessing() file was success
                                entry.setLastPollState(PollTableEntry.SUCCSESSFUL);
                                metrics.incrementMessagesReceived();

                            } catch (Exception e) {
                                logException("Error processing File URI : " + child.getName(), e);
                                failCount++;
                                // tell moveOrDeleteAfterProcessing() file failed
                                entry.setLastPollState(PollTableEntry.FAILED);
                                metrics.incrementFaultsReceiving();
                            }

                            moveOrDeleteAfterProcessing(entry, child);
                        }
                    }

                    if (failCount == 0 && successCount > 0) {
                        entry.setLastPollState(PollTableEntry.SUCCSESSFUL);
                    } else if (successCount == 0 && failCount > 0) {
                        entry.setLastPollState(PollTableEntry.FAILED);
                    } else {
                        entry.setLastPollState(PollTableEntry.WITH_ERRORS);
                    }
                }

                // processing of this poll table entry is complete
                long now = System.currentTimeMillis();
                entry.setLastPollTime(now);
                entry.setNextPollTime(now + entry.getPollInterval());

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to access or read file or directory : " + fileURI);
                }
            }

        } catch (FileSystemException e) {
            processFailure("Error checking for existence and readability : " + fileURI, e, entry);
        }

    }

    /**
     * Take specified action to either move or delete the processed file, depending on the outcome
     * @param entry the PollTableEntry for the file that has been processed
     * @param fileObject the FileObject representing the file to be moved or deleted
     */
    private void moveOrDeleteAfterProcessing(final PollTableEntry entry, FileObject fileObject) {

        String moveToDirectory = null;
        try {
            switch (entry.getLastPollState()) {
                case PollTableEntry.SUCCSESSFUL:
                    if (entry.getActionAfterProcess() == PollTableEntry.MOVE) {
                        moveToDirectory = entry.getMoveAfterProcess();
                    }
                    break;

                case PollTableEntry.FAILED:
                    if (entry.getActionAfterFailure() == PollTableEntry.MOVE) {
                        moveToDirectory = entry.getMoveAfterFailure();
                    }
                    break;
                
                default:
                    return;
            }

            if (moveToDirectory != null) {
                String prefix = "";
                if(entry.getMoveTimestampFormat() != null) {
                    Date now = new Date();
                    prefix = entry.getMoveTimestampFormat().format(now);
                }
                String destName = moveToDirectory + File.separator + prefix + fileObject.getName().getBaseName();
                if (log.isDebugEnabled()) {
                    log.debug("Moving to file :" + destName);
                }
                FileObject dest = fsManager.resolveFile(destName);
                try {
                    fileObject.moveTo(dest);
                } catch (FileSystemException e) {
                    log.error("Error moving file : " + fileObject + " to " + moveToDirectory, e);
                }
            } else {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Deleting file :" + fileObject);
                    }
                    fileObject.close();
                    if (!fileObject.delete()) {
                        log.error("Cannot delete file : " + fileObject);
                    }
                } catch (FileSystemException e) {
                    log.error("Error deleting file : " + fileObject, e);
                }
            }

        } catch (FileSystemException e) {
            log.error("Error resolving directory to move after processing : " + moveToDirectory, e);
        }
    }

    /**
     * Process a single file through Axis2
     * @param entry the PollTableEntry for the file (or its parent directory or archive)
     * @param file the file that contains the actual message pumped into Axis2
     * @throws AxisFault on error
     */
    private void processFile(PollTableEntry entry, FileObject file) throws AxisFault {

        try {
            FileContent content = file.getContent();
            String fileName = file.getName().getBaseName();
            String filePath = file.getName().getPath();

            metrics.incrementBytesReceived(content.getSize());

            Map<String, Object> transportHeaders = new HashMap<String, Object>();
            transportHeaders.put(VFSConstants.FILE_PATH, filePath);
            transportHeaders.put(VFSConstants.FILE_NAME, fileName);

            try {
                transportHeaders.put(VFSConstants.FILE_LENGTH, Long.valueOf(content.getSize()));
            } catch (FileSystemException ignore) {}
            try {
                transportHeaders.put(VFSConstants.LAST_MODIFIED,
                        Long.valueOf(content.getLastModifiedTime()));
            } catch (FileSystemException ignore) {}

            MessageContext msgContext = createMessageContext();
            
            String contentType = entry.getContentType();
            if (BaseUtils.isBlank(contentType)) {
                if (file.getName().getExtension().toLowerCase().endsWith(".xml")) {
                    contentType = "text/xml";
                } else if (file.getName().getExtension().toLowerCase().endsWith(".txt")) {
                    contentType = "text/plain";
                }
            } else {
                // Extract the charset encoding from the configured content type and
                // set the CHARACTER_SET_ENCODING property as e.g. SOAPBuilder relies on this.
                String charSetEnc = null;
                try {
                    if (contentType != null) {
                        charSetEnc = new ContentType(contentType).getParameter("charset");
                    }
                } catch (ParseException ex) {
                    // ignore
                }
                msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEnc);
            }

            // if the content type was not found, but the service defined it.. use it
            if (contentType == null) {
                if (entry.getContentType() != null) {
                    contentType = entry.getContentType();
                } else if (VFSUtils.getProperty(
                    content, BaseConstants.CONTENT_TYPE) != null) {
                    contentType =
                        VFSUtils.getProperty(content, BaseConstants.CONTENT_TYPE);
                }
            }

            AxisService service = entry.getService();
            if (service != null) {
                msgContext.setAxisService(service);
    
                // find the operation for the message, or default to one
                Parameter operationParam = service.getParameter(BaseConstants.OPERATION_PARAM);
                QName operationQName = (
                    operationParam != null ?
                        BaseUtils.getQNameFromString(operationParam.getValue()) :
                        BaseConstants.DEFAULT_OPERATION);
    
                AxisOperation operation = service.getOperation(operationQName);
                if (operation != null) {
                    msgContext.setAxisOperation(operation);
                }
            }

            // does the service specify a default reply file URI ?
            // FIXME: we should look for the parameter in createPollTableEntry
            //        and store the value in PollTableEntry as we do for the
            //        other parameters
            Parameter param = service.getParameter(VFSConstants.REPLY_FILE_URI);
            if (param != null && param.getValue() != null) {
                msgContext.setProperty(
                    Constants.OUT_TRANSPORT_INFO,
                    new VFSOutTransportInfo((String) param.getValue()));
            }

            // Determine the message builder to use
            Builder builder;
            if (contentType == null) {
                log.debug("No content type specified. Using SOAP builder.");
                builder = new SOAPBuilder();
            } else {
                int index = contentType.indexOf(';');
                String type = index > 0 ? contentType.substring(0, index) : contentType;
                builder = BuilderUtil.getBuilderFromSelector(type, msgContext);
                if (builder == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("No message builder found for type '" + type +
                                "'. Falling back to SOAP.");
                    }
                    builder = new SOAPBuilder();
                }
            }

            // set the message payload to the message context
            InputStream in;
            ManagedDataSource dataSource;
            if (builder instanceof DataSourceMessageBuilder && entry.isStreaming()) {
                in = null;
                dataSource = ManagedDataSourceFactory.create(
                        new FileObjectDataSource(file, contentType));
            } else {
                in = content.getInputStream();
                dataSource = null;
            }
            
            try {
                OMElement documentElement;
                if (in != null) {
                    documentElement = builder.processDocument(in, contentType, msgContext);
                } else {
                    documentElement = ((DataSourceMessageBuilder)builder).processDocument(
                            dataSource, contentType, msgContext);
                }
                msgContext.setEnvelope(TransportUtils.createSOAPEnvelope(documentElement));
                
                handleIncomingMessage(
                    msgContext,
                    transportHeaders,
                    null, //* SOAP Action - not applicable *//
                    contentType
                );
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        handleException("Error closing stream", ex);
                    }
                } else {
                    dataSource.destroy();
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Processed file : " + file + " of Content-type : " + contentType);
            }

        } catch (FileSystemException e) {
            handleException("Error reading file content or attributes : " + file, e);
            
        } finally {
            try {
                file.close();
            } catch (FileSystemException warn) {
                log.warn("Cannot close file after processing : " + file.getName().getPath(), warn);
            }
        }
    }

    @Override
    protected PollTableEntry createPollTableEntry(ParameterInclude params) throws AxisFault {
        String fileURI = ParamUtils.getOptionalParam(params, VFSConstants.TRANSPORT_FILE_FILE_URI);
        if (fileURI == null) {
            return null;
        } else {
            PollTableEntry entry = new PollTableEntry();
            entry.setFileURI(fileURI);
            entry.setFileNamePattern(ParamUtils.getOptionalParam(params,
                    VFSConstants.TRANSPORT_FILE_FILE_NAME_PATTERN));
            entry.setContentType(ParamUtils.getRequiredParam(params,
                    VFSConstants.TRANSPORT_FILE_CONTENT_TYPE));
            String option = ParamUtils.getOptionalParam(
                params, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_PROCESS);
            entry.setActionAfterProcess(
                MOVE.equals(option) ? PollTableEntry.MOVE : PollTableEntry.DELETE);
            option = ParamUtils.getOptionalParam(
                params, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_ERRORS);
            entry.setActionAfterErrors(
                MOVE.equals(option) ? PollTableEntry.MOVE : PollTableEntry.DELETE);
            option = ParamUtils.getOptionalParam(
                params, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_FAILURE);
            entry.setActionAfterFailure(
                MOVE.equals(option) ? PollTableEntry.MOVE : PollTableEntry.DELETE);

            String moveDirectoryAfterProcess = ParamUtils.getOptionalParam(
                params, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_PROCESS);
            entry.setMoveAfterProcess(moveDirectoryAfterProcess);
            String moveDirectoryAfterErrors = ParamUtils.getOptionalParam(
                params, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_ERRORS);
            entry.setMoveAfterErrors(moveDirectoryAfterErrors);
            String moveDirectoryAfterFailure = ParamUtils.getOptionalParam(
                params, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_FAILURE);
            entry.setMoveAfterFailure(moveDirectoryAfterFailure);

            String moveFileTimestampFormat = ParamUtils.getOptionalParam(
                params, VFSConstants.TRANSPORT_FILE_MOVE_TIMESTAMP_FORMAT);
            if(moveFileTimestampFormat != null) {
                DateFormat moveTimestampFormat = new SimpleDateFormat(moveFileTimestampFormat);
                entry.setMoveTimestampFormat(moveTimestampFormat);
            }

            String strStreaming = ParamUtils.getOptionalParam(params, VFSConstants.STREAMING);
            if (strStreaming != null) {
                entry.setStreaming(Boolean.parseBoolean(strStreaming));
            }
            
            String strMaxRetryCount = ParamUtils.getOptionalParam(
                params, VFSConstants.MAX_RETRY_COUNT);
            if(strMaxRetryCount != null)
              entry.setMaxRetryCount(Integer.parseInt(strMaxRetryCount));

            String strReconnectTimeout = ParamUtils.getOptionalParam(
                params, VFSConstants.RECONNECT_TIMEOUT);            
            if(strReconnectTimeout != null)
              entry.setReconnectTimeout(Integer.parseInt(strReconnectTimeout) * 1000);
            
            return entry;
            
        }
    }
}
