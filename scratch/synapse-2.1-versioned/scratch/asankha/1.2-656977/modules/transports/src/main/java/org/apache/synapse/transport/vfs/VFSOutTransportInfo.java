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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.axis2.transport.OutTransportInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.net.ssl.internal.www.protocol.https.Handler;

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

    VFSOutTransportInfo(String outFileURI) {
        if (outFileURI.startsWith(VFSConstants.VFS_PREFIX)) {
            this.outFileURI = outFileURI.substring(VFSConstants.VFS_PREFIX.length());
        } else {
            this.outFileURI = outFileURI;
        }
        
        Map properties = getProperties(outFileURI);
        if(properties.containsKey(VFSConstants.MAX_RETRY_COUNT)) {
          String strMaxRetryCount = (String) properties.get(VFSConstants.MAX_RETRY_COUNT);
            maxRetryCount = Integer.parseInt(strMaxRetryCount);
        }
        if(properties.containsKey(VFSConstants.RECONNECT_TIMEOUT)) {
          String strReconnectTimeout = (String) properties.get(VFSConstants.RECONNECT_TIMEOUT);
            reconnectTimeout = Long.parseLong(strReconnectTimeout) * 1000;
        }        
        if (properties.containsKey(VFSConstants.APPEND)) {
            String strAppend = (String)properties.get(VFSConstants.APPEND);
            append = Boolean.parseBoolean(strAppend);
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

    public void setReconnectTimeout(long reconnectTimeout) {
      this.reconnectTimeout = reconnectTimeout;
    }
    
    public boolean isAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public static Map getProperties(String url) {
        Map h = new HashMap();
        int propPos = url.indexOf("?");
        if (propPos != -1) {
            StringTokenizer st = new StringTokenizer(url.substring(propPos + 1), "&");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int sep = token.indexOf("=");
                if (sep != -1) {
                    h.put(token.substring(0, sep), token.substring(sep + 1));
                } else {
                    continue; // ignore, what else can we do?
                }
            }
        }
        return h;
    }
}