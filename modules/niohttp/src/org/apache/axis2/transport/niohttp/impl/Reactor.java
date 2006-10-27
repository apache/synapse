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
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

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
     * The maximum number of threads used for the worker thread pool
     */
    private static final int WORKERS_MAX_THREADS = 4;
    /**
     * The keep alive time of an idle worker thread
     */
    private static final long WORKER_KEEP_ALIVE = 60L;
    /**
     * The worker thread timeout time unit
     */
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

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

    private ExecutorService workerPool = null;

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
            _httpsReactor = new Reactor(host, port, secure, httpService);
            return _httpsReactor;
        } else {
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
        sk.attach(new Acceptor(this.httpService));
        log.info("Reactor Created for host : " + host + " on port : " + port);

        // create thread pool of workers
        workerPool = new ThreadPoolExecutor(
            4,
            WORKERS_MAX_THREADS, WORKER_KEEP_ALIVE, TIME_UNIT,
            new LinkedBlockingQueue(),
            new org.apache.axis2.util.threadpool.DefaultThreadFactory(
                    new ThreadGroup("NioHttp Worker thread group"),
                    "NioHttpWorker"));
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
            log.info("Reactor shutting down as a shutdown has been requested..");

        } catch (IOException e) {
            log.fatal("Reactor encountered an error while selecting : " + e.getMessage(), e);

        } finally {
            if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
                try {
                    serverSocketChannel.close();
                } catch (IOException e) {}
                try {
                    selector.close();
                } catch (IOException e) {}
            }
            log.info("Reactor shutdown. Server socket channel and the main selector closed..");
        }
    }

    /**
     * The main dispatch routine. This currently does not delegate to a thread pool
     * and leave this to the user
     *
     * @param k the selection key for the event
     */
    void dispatch(SelectionKey k) {
        Runnable r = (Runnable) k.attachment();
        if (r != null && r instanceof IOHandler) {
            IOHandler h = (IOHandler) r;
            if (!h.isBeingProcessed()) {
                h.lock();
                workerPool.execute(r);
            }
        }
    }

    /**
     * Accepts a new connection and hands it off to a new IncomingHandler instance
     */
    class Acceptor extends AbstractIOHandler {
        private HttpService httpService = null;

        public Acceptor(HttpService httpService) {
            this.httpService = httpService;
        }

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
                log.warn("Error accepting a connection : " + e.getMessage(), e);
            }
        }
    }

    /**
     * Queue this HttpRequest to be sent to its destination, and invoke the Callback
     * on recepit of a response
     * @param request the HttpRequest which is to be sent. The body of this message could
     *      be streamed before or after the invocation of this method, and the end of the
     *      body would be determined when the output stream is closed.
     * @param callback an optional call back to be invoked when a response is received
     *      for this request
     */
    public void send(HttpRequest request, Runnable callback) {
                
        SocketChannel socket = null;
        try {
            InetSocketAddress addr = new InetSocketAddress(
                request.getHost(), request.getPort());

            socket = SocketChannel.open();
            socket.configureBlocking(false);
            socket.connect(addr);

            OutgoingHandler outHandler = new OutgoingHandler(socket, selector, httpService);

            outHandler.setRequest(request);
            outHandler.setCallback(callback);

        } catch (IOException e) {
            // close the socket if we opened it, selection key would be cancelled if invoked
            if (socket != null && socket.isOpen()) {
                try {
                    socket.close();
                } catch (IOException ioe) {}
            }
            log.error("IO Exception : " + e.getMessage() +
                " sending request : " + request + " : " , e);
        }
    }

    /**
     * Request to shutdown the reactor
     * @param shutdownRequested if true, will request the reactor to shutdown
     */
    public void setShutdownRequested(boolean shutdownRequested) {
        log.info("reactor shudown requested..");
        this.shutdownRequested = shutdownRequested;
    }
}
