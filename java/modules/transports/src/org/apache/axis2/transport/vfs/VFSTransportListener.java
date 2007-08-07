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
package org.apache.axis2.transport.vfs;

import org.apache.axis2.transport.base.AbstractTransportListener;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.vfs.*;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * The "file" transport is a polling based transport - i.e. it gets kicked off at
 * a specified duration, and would iterate through a list of directories or files
 * specified according to poll durations. When scanning a directory, it will match
 * its contents against a given regex to find the set of input files. For compressed
 * files, the contents could be matched against a regex to find individual files.
 * Each of these files thus found would be submitted as an Axis2 "message" into the
 * Axis2 engine.
 *
 * The processed files would be deleted or renamed as specified in the configuration
 *
file:///directory/filename.ext
file:////somehost/someshare/afile.txt
jar:../lib/classes.jar!/META-INF/manifest.mf
zip:http://somehost/downloads/somefile.zip
jar:zip:outer.zip!/nested.jar!/somedir
jar:zip:outer.zip!/nested.jar!/some%21dir
tar:gz:http://anyhost/dir/mytar.tar.gz!/mytar.tar!/path/in/tar/README.txt
tgz:file://anyhost/dir/mytar.tgz!/somepath/somefile
gz:/my/gz/file.gz
http://somehost:8080/downloads/somefile.jar
http://myusername@somehost/index.html
webdav://somehost:8080/dist
ftp://myusername:mypassword@somehost/pub/downloads/somefile.tgz
sftp://myusername:mypassword@somehost/pub/downloads/somefile.tgz
smb://somehost/home

axis2.xml - transport definition
    <transportReceiver name="file" class="org.apache.axis2.transport.file.FileTransportListener">
      <parameter name="transport.file.Directory" locked="false">..</parameter>
    </transportReceiver>

services.xml - service attachment
    <parameter name="transport.file.FileURI" locked="true">..</parameter>
    <parameter name="transport.file.FileNamePattern" locked="true">..</parameter>
    <parameter name="transport.file.ContentType" locked="true">..</parameter>

    <parameter name="transport.PollInterval" locked="true">..</parameter>

    <parameter name="transport.file.ActionAfterProcess" locked="true">..</parameter>
	<parameter name="transport.file.ActionAfterErrors" locked="true">..</parameter>
    <parameter name="transport.file.ActionAfterFailure" locked="true">..</parameter>
 */
public class VFSTransportListener extends AbstractTransportListener {

    public static final String TRANSPORT_NAME = "vfs";
    public static final String FILE_PATH = "FILE_PATH";
    public static final String FILE_NAME = "FILE_NAME";
    public static final String FILE_LENGTH = "FILE_LENGTH";
    public static final String LAST_MODIFIED = "LAST_MODIFIED";

    public static final String DELETE = "DELETE";
    public static final String MOVE = "MOVE";

    /** Keep the list of directories/files and poll durations */
    private final List pollTable = new ArrayList();

    /** The VFS file system manager */
    private FileSystemManager fsManager = null;
    /** default interval in ms before polls */
    private int pollInterval = BaseConstants.DEFAULT_POLL_INTERVAL;
    /** The main timer that runs as a daemon thread */
    private final Timer timer = new Timer("PollTimer", true);
    /** is a poll already executing? */
    protected boolean pollInProgress = false;
    /** a lock to prevent concurrent execution of polling */
    private final Object pollLock = new Object();
    /** a map that keeps track of services to the timer tasks created for them */
    private Map serviceToTimerTaskMap = new HashMap();

    static {
        log = LogFactory.getLog(VFSTransportListener.class);
    }

    public void init(ConfigurationContext cfgCtx, TransportInDescription trpInDesc)
        throws AxisFault {
        setTransportName(TRANSPORT_NAME);
        super.init(cfgCtx, trpInDesc);
        try {
            fsManager = VFS.getManager();
        } catch (FileSystemException e) {
            handleException("Error initializing the file transport : " + e.getMessage(), e);
        }
    }

    /**
     * On a poller tick, iterate over the list of directories/files and check if
     * it is time to scan the contents for new files
     */
    public void onPoll() {
        Iterator iter = pollTable.iterator();
        while (iter.hasNext()) {
            PollTableEntry entry = (PollTableEntry) iter.next();
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
        try {
            if (log.isDebugEnabled()) {
                log.debug("Scanning directory or file : " + fileURI);
            }
            fileObject = fsManager.resolveFile(fileURI);

        } catch (FileSystemException e) {
            processFailure("Unable to resolve file or directory : " + fileURI, e, entry);
        }

        try {
            if (fileObject.exists() && fileObject.isReadable()) {

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
                        } catch (AxisFault e) {
                            entry.setLastPollState(PollTableEntry.FAILED);
                        }
                    }

                } else {
                    int failCount = 0;
                    int successCount = 0;

                    for (int i = 0; i < children.length; i++) {
                        if (children[i].getName().getBaseName().matches(
                            entry.getFileNamePattern())) {
                            try {
                                processFile(entry, children[i]);
                                successCount++;
                            } catch (Exception e) {
                                logException("Error processing file : " + entry.getFileURI(), e);
                                failCount++;
                            }
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
                moveOrDeleteAfterProcessing(entry, fileObject);

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
    private void moveOrDeleteAfterProcessing(PollTableEntry entry, FileObject fileObject) {

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
            }

            if (moveToDirectory != null) {
                FileObject dest = fsManager.resolveFile(moveToDirectory);
                dest = dest.getChild(fileObject.getName().getBaseName());
                try {
                    fileObject.moveTo(dest);
                } catch (FileSystemException e) {
                    log.error("Error moving file : " + fileObject + " to " + moveToDirectory, e);
                }
            } else {
                try {
                    fileObject.delete();
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

            Map transportHeaders = new HashMap();
            transportHeaders.put(FILE_PATH, filePath);
            transportHeaders.put(FILE_NAME, fileName);

            try {
                transportHeaders.put(FILE_LENGTH, Long.valueOf(content.getSize()));
            } catch (FileSystemException ignore) {}
            try {
                transportHeaders.put(LAST_MODIFIED, Long.valueOf(content.getLastModifiedTime()));
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
            VFSUtils.getInstace().setSOAPEnvelope(content, msgContext, contentType);

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
        Iterator iter = pollTable.iterator();
        while (iter.hasNext()) {
            PollTableEntry entry = (PollTableEntry) iter.next();
            if (entry.getServiceName().equals(serviceName)) {
                return new EndpointReference[]{ new EndpointReference("vfs:" + entry.getFileURI())};                
            }
        }
        return null;
    }

    protected void startListeningForService(AxisService service) {

        Parameter param = service.getParameter(BaseConstants.TRANSPORT_POLL_INTERVAL);
        long pollInterval = BaseConstants.DEFAULT_POLL_INTERVAL;
        if (param != null && param.getValue() instanceof String) {
            try {
                pollInterval = Integer.parseInt(param.getValue().toString());
            } catch (NumberFormatException e) {
                log.error("Invalid poll interval : " + param.getValue() + " for service : " +
                    service.getName() + " default to : " + (BaseConstants.DEFAULT_POLL_INTERVAL/1000) + "sec", e);
            }
        }

        PollTableEntry entry = new PollTableEntry();
        try {
            entry.setFileURI(
                BaseUtils.getServiceParam(service, VFSConstants.TRANSPORT_FILE_FILE_URI));
            entry.setFileNamePattern(
                BaseUtils.getServiceParam(service, VFSConstants.TRANSPORT_FILE_FILE_NAME_PATTERN));
            entry.setContentType(
                BaseUtils.getServiceParam(service, VFSConstants.TRANSPORT_FILE_CONTENT_TYPE));
            String option = BaseUtils.getServiceParam(
                service, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_PROCESS);
            entry.setActionAfterProcess(
                MOVE.equals(option) ? PollTableEntry.MOVE : PollTableEntry.DELETE);
            option = BaseUtils.getServiceParam(
                service, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_ERRORS);
            entry.setActionAfterErrors(
                MOVE.equals(option) ? PollTableEntry.MOVE : PollTableEntry.DELETE);
            option = BaseUtils.getServiceParam(
                service, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_FAILURE);
            entry.setActionAfterFailure(
                MOVE.equals(option) ? PollTableEntry.MOVE : PollTableEntry.DELETE);

            entry.setServiceName(service.getName());
            schedulePoll(service, pollInterval);            
            pollTable.add(entry);

        } catch (AxisFault axisFault) {
            String msg = "Error configuring the File/VFS transport for Service : " +
                service.getName() + " :: " + axisFault.getMessage();
            log.warn(msg, axisFault);
            //cfgCtx.getAxisConfiguration().getFaultyServices().put(service.getName(), msg);
        }
    }

    protected void stopListeningForService(AxisService service) {
        Iterator iter = pollTable.iterator();
        while (iter.hasNext()) {
            PollTableEntry entry = (PollTableEntry) iter.next();
            if (service.getName().equals(entry.getServiceName())) {
                cancelPoll(service);
                pollTable.remove(entry);
            }
        }
    }

    /**
     * Schedule a repeated poll at the specified interval for the given service
     * @param service the service to be polled
     * @param pollInterval the interval between successive polls
     */
    public void schedulePoll(AxisService service, long pollInterval) {
        TimerTask task = (TimerTask) serviceToTimerTaskMap.get(service);

        // if a timer task exists, cancel it first and create a new one
        if (task != null) {
            task.cancel();
        }

        task = new TimerTask() {
            public void run() {
                if (pollInProgress) {
                    if (log.isDebugEnabled()) {
                        log.debug("Transport " + transportName +
                                " onPoll() trigger : already executing poll..");
                    }
                    return;
                }

                workerPool.execute(new Runnable() {
                    public void run() {
                        synchronized (pollLock) {
                            pollInProgress = true;
                            try {
                                onPoll();
                            } finally {
                                pollInProgress = false;
                            }
                        }
                    }
                });
            }
        };
        serviceToTimerTaskMap.put(service, task);
        timer.scheduleAtFixedRate(task, pollInterval, pollInterval);
    }

    /**
     * Cancel any pending timer tasks for the given service
     * @param service the service for which the timer task should be cancelled
     */
    public void cancelPoll(AxisService service) {
        TimerTask task = (TimerTask) serviceToTimerTaskMap.get(service);
        if (task != null) {
            task.cancel();
        }
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }
}
