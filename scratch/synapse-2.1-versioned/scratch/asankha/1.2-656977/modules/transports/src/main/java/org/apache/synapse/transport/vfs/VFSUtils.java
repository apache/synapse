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

import org.apache.synapse.transport.base.BaseUtils;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileSystemException;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

public class VFSUtils extends BaseUtils {

    private static final Log log = LogFactory.getLog(VFSUtils.class);

    private static BaseUtils _instance = new VFSUtils();

    public static BaseUtils getInstace() {
        return _instance;
    }

    /**
     * Get a String property from FileContent message
     *
     * @param message the File message
     * @param property property name
     * @return property value
     */
    @Override
    public String getProperty(Object message, String property) {
        try {
            Object o = ((FileContent)message).getAttributes().get(property);
            if (o instanceof String) {
                return (String) o;
            }
        } catch (FileSystemException e) {}
        return null;
    }

    @Override
    public InputStream getInputStream(Object message) {
        try {
            return ((FileContent) message).getInputStream();
        } catch (FileSystemException e) {
            handleException("Error creating an input stream to : " +
                ((FileContent) message).getFile().getName(), e);
        }
        return null;
    }

    @Override
    public String getMessageTextPayload(Object message) {
        try {
            return new String(
                getBytesFromInputStream(getInputStream(message),
                (int) ((FileContent) message).getSize()));
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error reading message payload as text for : " +
                ((FileContent) message).getFile().getName(), e);
            }
        }
        return null;
    }

    @Override
    public byte[] getMessageBinaryPayload(Object message) {
        try {
            return getBytesFromInputStream(getInputStream(message),
                (int) ((FileContent) message).getSize());
        } catch (Exception e) {
            handleException("Error reading message payload as a byte[] for : " +
                ((FileContent) message).getFile().getName(), e);
        }
        return new byte[0];
    }

    public static String getFileName(MessageContext msgCtx, VFSOutTransportInfo vfsOutInfo) {
        String fileName = null;

        // first preference to a custom filename set on the current message context
        Map transportHeaders = (Map) msgCtx.getProperty(MessageContext.TRANSPORT_HEADERS);
        if (transportHeaders != null) {
            fileName = (String) transportHeaders.get(VFSConstants.REPLY_FILE_NAME);
        }

        // if not, does the service (in its service.xml) specify one?
        if (fileName == null) {
            Parameter param = msgCtx.getAxisService().getParameter(VFSConstants.REPLY_FILE_NAME);
            if (param != null) {
                fileName = (String) param.getValue();
            }
        }

        // next check if the OutTransportInfo specifies one
        if (fileName == null) {
            fileName = vfsOutInfo.getOutFileName();
        }

        // if none works.. use default
        if (fileName == null) {
            fileName = VFSConstants.DEFAULT_RESPONSE_FILE;
        }
        return fileName;
    }
    
    public static int getMaxRetryCount(MessageContext msgCtx, VFSOutTransportInfo vfsOutInfo) {
          if(vfsOutInfo.getMaxRetryCount() != 0) {
            return vfsOutInfo.getMaxRetryCount();
          }
          
          return VFSConstants.DEFAULT_MAX_RETRY_COUNT; 
    }    

    public static long getReconnectTimout(MessageContext msgCtx, VFSOutTransportInfo vfsOutInfo) {
      if(vfsOutInfo.getReconnectTimeout() != 0) {
        return vfsOutInfo.getReconnectTimeout();
      }

      return VFSConstants.DEFAULT_RECONNECT_TIMEOUT; 
    }   
    
    public static byte[] getBytesFromInputStream(InputStream is, int length) throws IOException {

        byte[] bytes = new byte[length];
        int offset = 0;
        int numRead = 0;

        try {
            while (offset < bytes.length &&
                (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }

            // Ensure all the bytes have been read in
            if (offset < bytes.length) {
                handleException("Could not completely read the stream to conver to a byte[]");
            }
        } finally {
            try {
                is.close();
            } catch (IOException ignore) {}
        }
        return bytes;
    }
}
