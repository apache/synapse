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
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * dynamic buffer expansion on need - done
 * TODO socket timeouts, 100 continue, 202 processing - asap
 * TODO ompression/mtom & mime/SSL - sometime soon
 * TODO http sessions - do we need?
 * TODO ByteBuffer pools and reuse of buffers, multiple selectors/IO threads - advanced
 */
public class Reactor implements Runnable {

    private static final Log log = LogFactory.getLog(Reactor.class);

    /**
     * The [single] main selector
     */
    final Selector selector;
    /**
     * The server socket channel
     */
    final ServerSocketChannel serverSocketChannel;

    /**
     * The maximum number of milli secs a select call would block for
     */
    private int selectTimeout = 500;
    /**
     * variable to be set when a shutdown is required
     */
    private boolean shutdownRequested = false;
    /**
     * The HttpService on which events are fired upon when new messages are received
     */
    private HttpService httpService;

    private static Reactor _httpReactor, _httpsReactor;

    public static synchronized Reactor getInstance(boolean secure) {
        if (secure) {
            return _httpsReactor;
        } else {
            return _httpReactor;
        }
    }

    public static synchronized Reactor createReactor(
        String host, int port, boolean secure, HttpService httpService) throws IOException {
        if (secure) {
            if (_httpsReactor != null) {
                _httpsReactor.setShutdownRequested(true);
            }
            _httpsReactor = new Reactor(host, port, secure, httpService);
            return _httpsReactor;
        } else {
            if (_httpReactor != null) {
                _httpReactor.setShutdownRequested(true);
            }
            _httpReactor = new Reactor(host, port, secure, httpService);
            return _httpReactor;
        }
    }

    /**
     * Starts a new Reactor on the specified port and prepares to
     * accept new connections
     *
     * @param port the server listen port
     * @throws IOException
     */
    private Reactor(String host, int port, boolean secure, HttpService httpService) throws IOException {

        this.httpService = httpService;
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(
            new InetSocketAddress(port));
            /*new InetSocketAddress(
                host == null ? InetAddress.getLocalHost() : InetAddress.getByName(host), port));*/
        serverSocketChannel.configureBlocking(false);

        SelectionKey sk = serverSocketChannel.register(
            selector, SelectionKey.OP_ACCEPT);
        sk.attach(new Acceptor());
        log.info("Reactor Created for host : " + host + " on port : " + port);
    }

    /**
     * This is the main routine of the Reactor, which will
     */
    public void run() {
        try {
            while (!shutdownRequested) {
                int keys = selector.select(selectTimeout);
                if (keys > 0) {
                    Set selected = selector.selectedKeys();
                    Iterator it = selected.iterator();
                    while (it.hasNext())
                        dispatch((SelectionKey) (it.next()));
                    selected.clear();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The main dispatch routine. This currently does not delegate to a thread pool
     * and leave this to the user
     *
     * @param k the selection key for the event
     */
    void dispatch(SelectionKey k) {
        Runnable r = (Runnable) (k.attachment());
        if (r != null)
            r.run();
    }

    /**
     * Accepts a new connection and hands it off to a new IncomingHandler instance
     */
    class Acceptor implements Runnable {

        public void run() {
            SocketChannel socket;
            try {
                socket = serverSocketChannel.accept();
                if (socket != null) {
                    log.info("Accepting new connection...");
                    // we have a *new* HTTP connection here
                    new IncomingHandler(socket, selector, httpService);
                }
            } catch (IOException e) {
                handleException("Exception while accepting a connection : " + e.getMessage(), e);
            }
        }
    }

    public void send(HttpRequest request, Runnable callback) {
        try {
            InetSocketAddress addr = new InetSocketAddress(
                request.getHost(), request.getPort());

            SocketChannel socket = SocketChannel.open();
            socket.configureBlocking(false);
            socket.connect(addr);

            SelectionKey sk = socket.register(selector, SelectionKey.OP_CONNECT);
            OutgoingHandler outHandler = new OutgoingHandler(socket, sk, request, httpService);
            if (callback != null) {
                outHandler.setCallback(callback);
            }
            sk.attach(outHandler);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        e.printStackTrace(); // TODO
        // throw new xxxx TODO
    }

    public void setShutdownRequested(boolean shutdownRequested) {
        this.shutdownRequested = shutdownRequested;
    }
}
