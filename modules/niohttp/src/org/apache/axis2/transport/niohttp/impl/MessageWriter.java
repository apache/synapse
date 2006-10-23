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

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MessageWriter {

    private static final Log log = LogFactory.getLog(MessageWriter.class);

    /** Are we at the streaming of the body state now? */
    private boolean streamingBody = false;
    /** Writing message as a Http request or a response? */
    private boolean requestMode = true;
    /** Should the connection be closed when the message is written? */
    private boolean connectionClose = true;
    /** Buffer position indicator while writing the header */
    private int pos = 0;

    /** The http request or response being written */
    private HttpMessage httpMessage;
    /** The piped output stream into the message body */
    private OutputStream bodyOutStream;

    public void setStreamingBody(boolean streamingBody) {
        this.streamingBody = streamingBody;
    }

    public boolean isStreamingBody() {
        return streamingBody;
    }

    public MessageWriter(boolean requestMode, HttpMessage httpMessage) {
        this.requestMode = requestMode;
        this.httpMessage = httpMessage;
        this.connectionClose = httpMessage.isConnectionClose();
    }

    public boolean isConnectionClose() {
        return connectionClose;
    }

    public InputStream getInputStream() {
        return httpMessage.getInputStream();
    }

    public int availableForRead() throws IOException {
        return httpMessage.getInputStream().available();
    }

    public byte[] getMessageHeader() {
        return httpMessage.toString().getBytes();
    }
}
