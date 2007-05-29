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

package org.apache.axis2.transport.nhttp.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.channels.Pipe;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.ByteBuffer;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.IOException;
import java.io.File;

/**
 * Create a Pipe suitable for the runtime platform. The java.nio.channels.Pipe implementation
 * on Windows uses TCP ports bound to the loopback interface to implement a Pipe. In Linux and
 * Solaris this is passed to a native method.
 */
public class PipeImpl {

    private static final Log log = LogFactory.getLog(PipeImpl.class);

    private ReadableByteChannel source;
    private WritableByteChannel sink;

    private PipedOutputStream pipedOut;
    protected static boolean useNative;

    static {
        // platfom default - Unix - native, Windows - Piped Streams
        if ("/".equals(File.separator)) {
            useNative = true;
        }

        // has this been overridden?
        String option = System.getProperty("native_pipes");
        if (option != null) {
            // if an option is specified, use it
            if ("true".equals(option)) {
                useNative = true;
            } else if ("false".equals(option)) {
                useNative = false;
            }
        }

        if (useNative) {
            log.info("Using native OS Pipes for event-driven to stream IO bridging");
        } else {
            log.info("Using simulated buffered Pipes for event-driven to stream IO bridging");
        }
    }

    public PipeImpl() throws IOException {
        if (useNative) {
            Pipe pipe = Pipe.open();
            source = pipe.source();
            sink = pipe.sink();

        } else {
            PipedInputStream pipedIn = new PipedInputStream();
            try {
                pipedOut = new PipedOutputStream(pipedIn);
            } catch (IOException e) {
                e.printStackTrace();
            }

            source = Channels.newChannel(pipedIn);
            sink = Channels.newChannel(pipedOut);
        }
    }

    public ReadableByteChannel source() {
        return source;
    }

    public WritableByteChannel sink() {
        return sink;
    }
}
