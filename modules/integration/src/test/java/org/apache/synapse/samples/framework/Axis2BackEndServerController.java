package org.apache.synapse.samples.framework;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.ListenerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.config.Axis2ServerConfiguration;

import java.util.concurrent.CountDownLatch;

/**
 * Responsible for programatically starting up and shutting down
 * an Axis2 server instance in order to run a sample test.
 */
public class Axis2BackEndServerController implements BackEndServerController {

    private static final Log log = LogFactory.getLog(Axis2BackEndServerController.class);

    private String serverName;
    private BackEndServerThread serverThread;
    private ConfigurationContext configContext;
    private ListenerManager listenerManager;
    private Axis2ServerConfiguration configuration;
    private CountDownLatch cdLatch;
    private Exception processException;

    public Axis2BackEndServerController(String serverName,
                                        Axis2ServerConfiguration configuration) {
        this.serverName = serverName;
        this.configuration = configuration;
        serverThread = new BackEndServerThread();
        serverThread.setName(configuration.getServerName()+ " thread");
        cdLatch = new CountDownLatch(1);
    }

    public String getServerName() {
        return serverName;
    }

    public boolean start() {
        log.info("Preparing to start Axis2 Server: " + serverName);
        serverThread.start();
        try {
            log.info("Waiting for Axis2 to start");
            cdLatch.await();
            if (processException == null) {
                log.info("Axis2 is successfully started. continuing tests");
                return true;
            } else {
                log.warn("There was an error starting Axis2 server: " + serverName, processException);
                return false;
            }
        } catch (InterruptedException e) {
            return false;
        }
    }

    public boolean stop() {
        if (serverThread.isRunning) {
            serverThread.isRunning = false;
            try {
                cdLatch = new CountDownLatch(1);
                cdLatch.await();
            } catch (InterruptedException e) {
                 return false;
            }
        }
        return true;
    }

    class BackEndServerThread extends Thread {

        public boolean isRunning = false;

        public void run() {processException = null;
            log.info("ServerThread: Initializing Axis2 Server: " + serverName);
            processException = null;
            try {
                listenerManager = new ListenerManager();

                configContext = ConfigurationContextFactory
                        .createConfigurationContextFromFileSystem(configuration.getAxis2Repo(),
                                configuration.getAxis2Xml());

                // setting System.setProperty does not work since this all servers are run on same jvm
                configContext.setProperty("server_name", serverName);

                TransportInDescription httpTrsIn = configContext.getAxisConfiguration().
                        getTransportsIn().get("http");
                String httpPort = configuration.getHttpPort();
                if (httpPort != null && httpPort.length() > 0) {
                    try {
                        new Integer(httpPort);
                        httpTrsIn.getParameter("port").setValue(httpPort);
                    } catch (NumberFormatException e) {
                        log.error("Given port is not a valid integer. Using default value.");
                    }
                }

                TransportInDescription httpsTrsIn = configContext.getAxisConfiguration().
                        getTransportsIn().get("https");
                String httpsPort = configuration.getHttpsPort();
                if (httpsPort != null && httpsPort.length() > 0) {
                    try {
                        new Integer(httpsPort);
                        httpsTrsIn.getParameter("port").setValue(httpsPort);
                    } catch (NumberFormatException e) {
                        log.error("Given port is not a valid integer. Using default value.");
                    }
                }

                log.info("ServerThread: Starting Axis2 Server: " + serverName);


                ClusteringAgent clusteringAgent =
                        configContext.getAxisConfiguration().getClusteringAgent();
                String avoidInit = ClusteringConstants.Parameters.AVOID_INITIATION;
                if (clusteringAgent != null && clusteringAgent.getParameter(avoidInit) != null &&
                        ((String) clusteringAgent.getParameter(avoidInit).getValue()).
                                equalsIgnoreCase("true")) {
                    clusteringAgent.setConfigurationContext(configContext);
                    clusteringAgent.init();
                }


                listenerManager.startSystem(configContext);
                isRunning = true;

            } catch (Exception e) {
                processException = e;
            }
            cdLatch.countDown();

            log.info("ServerThread: Wait until test are finished");
            while (isRunning) {
                //wait
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    log.info("Thread interrupted");
                }
            }
            log.info("ServerThread: Shutting down Axis2 Server...");
            try {
                listenerManager.stop();
                configContext.terminate();
            } catch (Exception e) {
                log.warn("Error while shutting down Axis2 server", e);
            }
            cdLatch.countDown();
        }
    }
}
