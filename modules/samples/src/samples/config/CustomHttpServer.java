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
package samples.config;

import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.axis2.util.OptionsParser;
import org.apache.axis2.util.threadpool.ThreadFactory;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.SynapseConfigurationBuilder;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;

import java.io.File;

public class CustomHttpServer extends SimpleHTTPServer {

    private static final Log log = LogFactory.getLog(CustomHttpServer.class);

    public CustomHttpServer(ConfigurationContext cfgCtx, int port, ThreadFactory o) throws AxisFault {
        super(cfgCtx, port, o);
    }

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        OptionsParser optionsParser = new OptionsParser(args);

        args = optionsParser.getRemainingArgs();
        // first check if we should print usage
        if ((optionsParser.isFlagSet('?') > 0) || (optionsParser.isFlagSet('h') > 0) ||
                args == null || args.length == 0 || args.length > 2) {
            printUsage();
        }
        String paramPort = optionsParser.isValueSet('p');
        if (paramPort != null) {
            port = Integer.parseInt(paramPort);
        }
        args = optionsParser.getRemainingArgs();

        System.out.println("[CustomHttpServer] Starting");
        System.out.println("[CustomHttpServer] Using the Axis2 Repository "
                + new File(args[0]).getAbsolutePath());
        System.out.println("[CustomHttpServer] Listening on port " + port);
        try {
            CustomHttpServer receiver = new CustomHttpServer(
                    ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                            args[0], null), port, null);
            Runtime.getRuntime().addShutdownHook(new ShutdownThread(receiver));
            receiver.start();
            System.out.println("[CustomHttpServer] Started");
            // now initialize Synapse into the started Axis configuration
            receiver.initializeSynapse();

        } catch (Throwable t) {
            log.fatal("Error starting CustomHttpServer", t);
            System.out.println("[CustomHttpServer] Shutting down");
        }
    }

    /**
     * Perform the custom initialization of Synapse
     */
    private void initializeSynapse() {

        log.info("Initializing Synapse...");

        AxisConfiguration axisCfg = getConfigurationContext().getAxisConfiguration();

        // set the Synapse configuration and environment into the Axis2 configuration
        Parameter synapseCtxParam = new Parameter(Constants.SYNAPSE_CONFIG, null);
        synapseCtxParam.setValue(getSynapseConfiguration());

        Parameter synapseEnvParam = new Parameter(Constants.SYNAPSE_ENV, null);
        synapseEnvParam.setValue(new Axis2SynapseEnvironment(axisCfg));

        try {
            axisCfg.addParameter(synapseCtxParam);
            axisCfg.addParameter(synapseEnvParam);

        } catch (AxisFault e) {
            handleException(
                "Could not set parameters '" + Constants.SYNAPSE_CONFIG + "' and/or '" + Constants.SYNAPSE_ENV +
                "'to the Axis2 configuration : " + e.getMessage(), e);
        }

        log.info("Synapse initialized...");
    }

    /**
     * Creates the required SynapseConfiguration to be used by the custom server, programmatically
     * @return the synapse configuration created programmatically
     */
    private SynapseConfiguration getSynapseConfiguration() {
        // One may create an instance of SynapseConfiguration purely programmatically here.. and not call
        // back to SynapseConfigurationBuilder.getDefaultConfiguration()
        // see SynapseConfigurationBuilder.getDefaultConfiguration() and Unit tests for examples
        return SynapseConfigurationBuilder.getDefaultConfiguration();
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    static class ShutdownThread extends Thread {
        private SimpleHTTPServer server = null;

        public ShutdownThread(SimpleHTTPServer server) {
            super();
            this.server = server;
        }

        public void run() {
            System.out.println("[CustomHttpServer] Shutting down");
            server.stop();
            System.out.println("[CustomHttpServer] Shutdown complete");
        }
    }

}
