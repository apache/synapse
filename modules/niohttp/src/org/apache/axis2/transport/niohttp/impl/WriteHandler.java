/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.axis2.transport.niohttp.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class WriteHandler {

    private static final Log log = LogFactory.getLog(WriteHandler.class);

    private ByteBuffer buffer;
    private boolean connectionClose;

    public void setMessage(ByteBuffer buffer, boolean connectionClose) {
        this.buffer = buffer;
        this.connectionClose = connectionClose;
    }

    public boolean isConnectionClose() {
        return connectionClose;
    }

    /**
     * @param socket
     * @return true if response has been completely written
     */
    public boolean handle(SocketChannel socket) {
        try {
            log.debug("Writing to wire : \n" + Util.dumpAsHex(buffer.array(), buffer.limit()));
            if (buffer.remaining() > 0) {
                log.debug("Writing response..");
                socket.write(buffer);
                if (buffer.remaining() == 0) {
                    log.debug("Completely wrote response");
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
