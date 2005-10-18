package org.apache.synapse.transport.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.server.SimpleHttpServer;
import org.apache.axis2.transport.http.HTTPWorker;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.synapse.transport.http.SynapseWorker;

import java.io.IOException;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 12, 2005
 * Time: 2:01:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleSynapseServer {
    /**
     * Field log
     */
    protected Log log = LogFactory.getLog(SimpleSynapseServer.class.getName());

    /**
     * Field systemContext
     */
    protected ConfigurationContext configurationContext;

    /**
     * Embedded commons http client based server
     */
    SimpleHttpServer embedded = null;

    int port = -1;

    /**
     * Constructor SimpleHTTPServer
     */
    public SimpleSynapseServer() {
    }

    /**
     * Constructor SimpleHTTPServer
     *
     * @param systemContext
     */
    public SimpleSynapseServer(ConfigurationContext systemContext,
                            int port) throws IOException {
        this.configurationContext = systemContext;
        this.port = port;
    }

    /**
     * Constructor SimpleHTTPServer
     *
     * @param dir
     * @throws org.apache.axis2.AxisFault
     */
    public SimpleSynapseServer(String dir, int port) throws AxisFault {
        try {
            this.port = port;
            ConfigurationContextFactory erfac = new ConfigurationContextFactory();
            this.configurationContext = erfac.buildConfigurationContext(dir);
            Thread.sleep(2000);
        } catch (Exception e1) {
            throw new AxisFault(e1);
        }
    }

    /**
     * Checks if this HTTP server instance is running.
     *
     * @return  true/false
     */
    public boolean isRunning() {
        if(embedded == null) {
            return false;
        }
        return embedded.isRunning();
    }

    /**
     * stop the server if not already told to.
     *
     * @throws Throwable
     */
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }

    /**
     * Start this server as a NON-daemon.
     */
    public void start() throws AxisFault {
        try {
            embedded = new SimpleHttpServer(port);
            embedded.setRequestHandler(new SynapseWorker(configurationContext));
        } catch (IOException e) {
            log.error(e);
            throw new AxisFault(e);
        }
    }

    /**
     * Stop this server. Can be called safely if the system is already stopped,
     * or if it was never started.
     * This will interrupt any pending accept().
     */
    public void stop() {
        log.info("stop called");
        if(embedded != null) {
            embedded.destroy();
        }
        log.info("Simple Axis Server Quits");
    }

    /**
     * Method getSystemContext
     *
     * @return the system context
     */
    public ConfigurationContext getSystemContext() {
        return configurationContext;
    }

}
