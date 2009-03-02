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

package org.apache.synapse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.CountDownLatch;

/**
 * This is the class invoked by the command line scripts synapse.sh and synapse-daemon.sh to
 * start an instance of Synapse. This class calls on the ServerManager to start up the instance
 *
 * TODO Switch to using commons-cli and move all command line parameter processing etc from the
 * .sh and .bat into this.. for 1.3 release :)
 */
public class SynapseServer {

    private static final Log log = LogFactory.getLog(SynapseServer.class);

    private static final String USAGE_TXT =
            "Usage: SynapseServer <axis2_repository> <axis2_xml> <synapse_home> <synapse_xml> " +
                    "<resolve_root> <deployment mode>" +
                    "\n Opts: -? this message";

    public static void printUsage() {
        System.out.println(USAGE_TXT);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {

        // first check if we should print usage
        if (args.length != 1 && args.length != 4 && args.length != 5 && args.length != 6
                && args.length != 6 && args.length != 7) {
            printUsage();
        }

        ServerConfigurationInformation configurationInformation =
                ServerConfigurationInformationFactory.createServerConfigurationInformation(args);
        ServerManager serverManager = ServerManager.getInstance();
        serverManager.init(configurationInformation, null);
        serverManager.start();
        addShutdownHook();
        
        // Put the main thread into wait state. This makes sure that the Synapse server
        // doesn't stop immediately if ServerManager#start doesn't create any non daemon
        // threads (see also SYNAPSE-425).
        new CountDownLatch(1).await();
    }

    private static void addShutdownHook() {
        Thread shutdownHook = new Thread() {
            public void run() {
                log.info("Shutting down Apache Synapse ...");
                try {
                    ServerManager.getInstance().stop();
                    log.info("Shutdown complete");
                    log.info("Halting JVM");
                } catch (Exception e) {
                    log.warn("Error occurred while shutting down Apache Synapse : " + e);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
}
