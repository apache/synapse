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

import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.base.BaseUtils;
import org.apache.synapse.transport.base.AbstractPollingTransportListener;
import org.apache.synapse.transport.base.ManagementSupport;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.description.*;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.vfs.*;
import org.apache.commons.vfs.impl.StandardFileSystemManager;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

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
public class VFSTransportListener extends AbstractPollingTransportListener 
    implements ManagementSupport {

    public static final String TRANSPORT_NAME = "vfs";

    public static final String DELETE = "DELETE";
    public static final String MOVE = "MOVE";

    /** Keep the list of directories/files and poll durations */
    private final List<PollTableEntry> pollTable = new ArrayList<PollTableEntry>();
    /** Keep the list of removed pollTable entries */
    private final List<PollTableEntry> removeTable = new ArrayList<PollTableEntry>();
    /** The VFS file system manager */
    private FileSystemManager fsManager = null;

    /**
     * Initializes the VFS transport by getting the VFS File System manager
     * @param cfgCtx the Axsi2 configuration context
     * @param trpInDesc the VFS transport in description from the axis2.xml
     * @throws AxisFault on error
     */
    public void init(ConfigurationContext cfgCtx, TransportInDescription trpInDesc)
        throws AxisFault {
        setTransportName(TRANSPORT_NAME);
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

    /**
     * On a poller tick, iterate over the list of directories/files and check if
     * it is time to scan the contents for new files
     */
    public void onPoll() {
        if (!removeTable.isEmpty()) {
            pollTable.removeAll(removeTable);
        }
        
        for (PollTableEntry entry : pollTable) {
            long startTime = System.currentTimeMillis();
            if (startTime > entry.getNextPollTime()) {
                scanFileOrDirectory(entry, entry.getFileURI());
            }
        }
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
        
        while(wasError) {
          try {
            retryCount++;
            fileObject = fsManager.resolveFile(fileURI);
            
            if(fileObject == null) {
              log.error("fileObject is null");
              throw new FileSystemException("fileObject is null");
            }
            
            wasError = false;
                                
          } catch(FileSystemException e) {
            log.error("cannot resolve fileObject", e);
            if(maxRetryCount <= retryCount)
              processFailure("cannot resolve fileObject repeatedly: " + e.getMessage(), e, entry);
              return;
          }
        
          if(wasError) {
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

                case PollTableEntry.WITH_ERRORS:
                    if (entry.getActionAfterProcess() == PollTableEntry.MOVE) {
                        moveToDirectory = entry.getMoveAfterErrors();
                    }
                    break;

                case PollTableEntry.FAILED:
                    if (entry.getActionAfterProcess() == PollTableEntry.MOVE) {
                        moveToDirectory = entry.getMoveAfterFailure();
                    }
                    break;
                case PollTableEntry.NONE:
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

            // compute the unique message ID
            String messageId = filePath + "_" + fileName +
                "_" + System.currentTimeMillis() + "_" + (int) Math.random() * 1000;

            String contentType = entry.getContentType();
            if (!BaseUtils.isValid(contentType)) {
                if (file.getName().getExtension().toLowerCase().endsWith(".xml")) {
                    contentType = "text/xml";
                } else if (file.getName().getExtension().toLowerCase().endsWith(".txt")) {
                    contentType = "text/plain";
                }
            }

            // if the content type was not found, but the service defined it.. use it
            if (contentType == null) {
                if (entry.getContentType() != null) {
                    contentType = entry.getContentType();
                } else if (VFSUtils.getInstace().getProperty(
                    content, BaseConstants.CONTENT_TYPE) != null) {
                    contentType =
                        VFSUtils.getInstace().getProperty(content, BaseConstants.CONTENT_TYPE);
                }
            }

            MessageContext msgContext = createMessageContext();
            // set to bypass dispatching if we know the service - we already should!
            AxisService service = cfgCtx.getAxisConfiguration().getService(entry.getServiceName());
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

            // does the service specify a default reply file URI ?
            Parameter param = service.getParameter(VFSConstants.REPLY_FILE_URI);
            if (param != null && param.getValue() != null) {
                msgContext.setProperty(
                    Constants.OUT_TRANSPORT_INFO,
                    new VFSOutTransportInfo((String) param.getValue()));
            }


            // set the message payload to the message context
            InputStream in = content.getInputStream();
            try {
                SOAPEnvelope envelope;
                try {
                    envelope = TransportUtils.createSOAPMessage(msgContext, in, contentType);
                } catch (XMLStreamException ex) {
                    handleException("Error parsing XML", ex);
                    return;
                }
                msgContext.setEnvelope(envelope);
            }
            finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    handleException("Error closing stream", ex);
                }
            }

            handleIncomingMessage(
                msgContext,
                transportHeaders,
                null, //* SOAP Action - not applicable *//
                contentType
            );

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

    /**
     * method to log a failure to the log file and to update the last poll status and time
     * @param msg text for the log message
     * @param e optiona exception encountered or null
     * @param entry the PollTableEntry
     */
    private void processFailure(String msg, Exception e, PollTableEntry entry) {
        if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }
        long now = System.currentTimeMillis();
        entry.setLastPollState(PollTableEntry.FAILED);
        entry.setLastPollTime(now);
        entry.setNextPollTime(now + entry.getPollInterval());
    }

    /**
     * Get the EPR for the given service over the VFS transport
     * vfs:uri (@see http://jakarta.apache.org/commons/vfs/filesystems.html for the URI formats)
     * @param serviceName service name
     * @param ip          ignored
     * @return the EPR for the service
     * @throws AxisFault not used
     */
    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        for (PollTableEntry entry : pollTable) {
            if (entry.getServiceName().equals(serviceName)
                    || serviceName.startsWith(entry.getServiceName() + ".")) {
                return new EndpointReference[]{new EndpointReference("vfs:" + entry.getFileURI())};
            }
        }
        return null;
    }

    protected void startListeningForService(AxisService service) {

        if (service.getName().startsWith("__")) {
            return;
        }

        Parameter param = service.getParameter(BaseConstants.TRANSPORT_POLL_INTERVAL);
        long pollInterval = BaseConstants.DEFAULT_POLL_INTERVAL;
        if (param != null && param.getValue() instanceof String) {
            try {
                pollInterval = Integer.parseInt(param.getValue().toString());
            } catch (NumberFormatException e) {
                log.error("Invalid poll interval : " + param.getValue() + " for service : " +
                    service.getName() + " default to : "
                        + (BaseConstants.DEFAULT_POLL_INTERVAL/1000) + "sec", e);
                disableTransportForService(service);
            }
        }

        PollTableEntry entry = new PollTableEntry();
        try {
            entry.setFileURI(
                BaseUtils.getRequiredServiceParam(service, VFSConstants.TRANSPORT_FILE_FILE_URI));
            entry.setFileNamePattern(BaseUtils.getOptionalServiceParam(service,
                    VFSConstants.TRANSPORT_FILE_FILE_NAME_PATTERN));
            entry.setContentType(BaseUtils.getRequiredServiceParam(service,
                    VFSConstants.TRANSPORT_FILE_CONTENT_TYPE));
            String option = BaseUtils.getOptionalServiceParam(
                service, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_PROCESS);
            entry.setActionAfterProcess(
                MOVE.equals(option) ? PollTableEntry.MOVE : PollTableEntry.DELETE);
            option = BaseUtils.getOptionalServiceParam(
                service, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_ERRORS);
            entry.setActionAfterErrors(
                MOVE.equals(option) ? PollTableEntry.MOVE : PollTableEntry.DELETE);
            option = BaseUtils.getOptionalServiceParam(
                service, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_FAILURE);
            entry.setActionAfterFailure(
                MOVE.equals(option) ? PollTableEntry.MOVE : PollTableEntry.DELETE);

            String moveDirectoryAfterProcess = BaseUtils.getOptionalServiceParam(
                service, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_PROCESS);
            entry.setMoveAfterProcess(moveDirectoryAfterProcess);
            String moveDirectoryAfterErrors = BaseUtils.getOptionalServiceParam(
                service, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_ERRORS);
            entry.setMoveAfterErrors(moveDirectoryAfterErrors);
            String moveDirectoryAfterFailure = BaseUtils.getOptionalServiceParam(
                service, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_FAILURE);
            entry.setMoveAfterFailure(moveDirectoryAfterFailure);

            String moveFileTimestampFormat = BaseUtils.getOptionalServiceParam(
                service, VFSConstants.TRANSPORT_FILE_MOVE_TIMESTAMP_FORMAT);
            if(moveFileTimestampFormat != null) {
                DateFormat moveTimestampFormat = new SimpleDateFormat(moveFileTimestampFormat);
                entry.setMoveTimestampFormat(moveTimestampFormat);
            }

            String strMaxRetryCount = BaseUtils.getOptionalServiceParam(
                service, VFSConstants.MAX_RETRY_COUNT);
            if(strMaxRetryCount != null)
              entry.setMaxRetryCount(Integer.parseInt(strMaxRetryCount));

            String strReconnectTimeout = BaseUtils.getOptionalServiceParam(
                service, VFSConstants.RECONNECT_TIMEOUT);            
            if(strReconnectTimeout != null)
              entry.setReconnectTimeout(Integer.parseInt(strReconnectTimeout) * 1000);
            
            entry.setServiceName(service.getName());
            schedulePoll(service, pollInterval);            
            pollTable.add(entry);

        } catch (AxisFault axisFault) {
            String msg = "Error configuring the File/VFS transport for Service : " +
                service.getName() + " :: " + axisFault.getMessage();
            log.warn(msg);
            disableTransportForService(service);
        }
    }

    protected void stopListeningForService(AxisService service) {
        Iterator iter = pollTable.iterator();
        while (iter.hasNext()) {
            PollTableEntry entry = (PollTableEntry) iter.next();
            if (service.getName().equals(entry.getServiceName())) {
                cancelPoll(service);
                removeTable.add(entry);
            }
        }
    }
}
