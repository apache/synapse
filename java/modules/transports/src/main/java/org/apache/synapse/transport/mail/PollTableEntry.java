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

package org.apache.synapse.transport.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.synapse.transport.base.AbstractPollTableEntry;

/**
 * Holds information about an entry in the VFS transport poll table used by the
 * VFS Transport Listener
 */
public class PollTableEntry extends AbstractPollTableEntry {

    // operation after mail check
    public static final int DELETE = 0;
    public static final int MOVE   = 1;

    /** The email address mapped to the service */
    private InternetAddress emailAddress = null;

    /** account username to check mail */
    private String userName = null;
    /** account password to check mail */
    private String password = null;
    /** The protocol to be used - pop3 or imap */
    private String protocol = null;
    /** Store POP3 or IMAP mail properties */
    Properties properties = new Properties();    

    /** The mail folder from which to check mail */
    private String folder;
    /** X-Service-Path custom header */
    private String xServicePath;
    /** Content-Type to use for the message */
    private String contentType;
    /** default reply address */
    private InternetAddress replyAddress = null;

    /** list of mail headers to be preserved into the Axis2 message as transport headers */
    private List<String> preserveHeaders = null;
    /** list of mail headers to be removed from the Axis2 message transport headers */
    private List<String> removeHeaders = null;

    /** action to take after a successful poll */
    private int actionAfterProcess = DELETE;
    /** action to take after a failed poll */
    private int actionAfterFailure = DELETE;

    /** folder to move the email after processing */
    private String moveAfterProcess;
    /** folder to move the email after failure */
    private String moveAfterFailure;

    private int maxRetryCount;
    private long reconnectTimeout;

    @Override
    public EndpointReference getEndpointReference() {
        return new EndpointReference(MailConstants.TRANSPORT_PREFIX + emailAddress);
    }

    public InternetAddress getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) throws AddressException {        
        this.emailAddress = new InternetAddress(emailAddress);
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getXServicePath() {
        return xServicePath;
    }

    public void setXServicePath(String xServicePath) {
        this.xServicePath = xServicePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getActionAfterProcess() {
        return actionAfterProcess;
    }

    public void setActionAfterProcess(int actionAfterProcess) {
        this.actionAfterProcess = actionAfterProcess;
    }

    public int getActionAfterFailure() {
        return actionAfterFailure;
    }

    public void setActionAfterFailure(int actionAfterFailure) {
        this.actionAfterFailure = actionAfterFailure;
    }

    public String getMoveAfterProcess() {
        return moveAfterProcess;
    }

    public void setMoveAfterProcess(String moveAfterProcess) {
        this.moveAfterProcess = moveAfterProcess;
    }

    public String getMoveAfterFailure() {
        return moveAfterFailure;
    }

    public void setMoveAfterFailure(String moveAfterFailure) {
        this.moveAfterFailure = moveAfterFailure;
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

    public void setReconnectTimeout(long reconnectTimeout) {
      this.reconnectTimeout = reconnectTimeout;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public InternetAddress getReplyAddress() {
        return replyAddress;
    }

    public void setReplyAddress(String replyAddress) throws AddressException {
        if (replyAddress != null) {
            this.replyAddress = new InternetAddress(replyAddress);   
        }
    }

    /**
     * Get the mail store protocol.
     * This protocol identifier is used in calls to {@link Session#getStore()}.
     * 
     * @return the mail store protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Set the mail store protocol.
     * This protocol identifier is used in calls to {@link Session#getStore()}.
     * 
     * @param protocol the mail store protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Properties getProperties() {
        return properties;
    }

    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    public void addPreserveHeaders(String headerList) {
        if (headerList == null) return;
        StringTokenizer st = new StringTokenizer(headerList, " ,");
        preserveHeaders = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.length() != 0) {
                preserveHeaders.add(token);
            }
        }
    }

    public void addRemoveHeaders(String headerList) {
        if (headerList == null) return;
        StringTokenizer st = new StringTokenizer(headerList, " ,");
        removeHeaders = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.length() != 0) {
                removeHeaders.add(token);
            }
        }
    }

    public boolean retainHeader(String name) {
        if (preserveHeaders != null) {
            return preserveHeaders.contains(name);
        } else if (removeHeaders != null) {
            return !removeHeaders.contains(name);
        } else {
            return true;
        }
    }
}
