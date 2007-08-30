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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.ListenerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.net.ServerSocket;
import java.util.Iterator;

/**
 * To manage the Synapse Server  instances. This class is responsible for
 * the staring and stopping listeners
 */

public class ServerManager {

    private static ServerManager instance;
    private static Log log = LogFactory.getLog(ServerManager.class);
    public static String axis2Repolocation;
    private ListenerManager listenerManager;
    private ConfigurationContext configctx;

    /**
     * To ensure that there is a only one Manager
     * @return  Server Manager Instance
     */
    public static ServerManager getInstance() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    /**
     * starting all the listeners
     */
    public void start() {

        if (axis2Repolocation == null) {
            System.out.println("The Axis2 Repository must be provided");
            return;
        }
        System.out.println("[SynapseServer] Using the Axis2 Repository "
                           + new File(axis2Repolocation).getAbsolutePath());
        try {
            configctx = ConfigurationContextFactory.
                    createConfigurationContextFromFileSystem(axis2Repolocation, null);
            
            listenerManager = configctx.getListenerManager();
            if (listenerManager == null) {
                listenerManager = new ListenerManager();
                listenerManager.init(configctx);
            }
            // decide on HTTP port to execute
            selectPort(configctx);

            Iterator iter = configctx.getAxisConfiguration().
                    getTransportsIn().keySet().iterator();
            while (iter.hasNext()) {
                String trp = (String) iter.next();
                TransportInDescription trsIn = (TransportInDescription)
                        configctx.getAxisConfiguration().getTransportsIn().get(trp);
                listenerManager.addListener(trsIn, false);
                String msg = "[SynapseServer] Starting transport " + trsIn.getName();
                if (trsIn.getParameter("port") != null) {
                    msg += " on port " + trsIn.getParameter("port").getValue();
                }
                System.out.println(msg);
            }
            System.out.println("[SynapseServer] Ready");

        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("[SynapseServer] Startup failed...");
        }
    }

    /**
     * stop all the listeners
     */
    public void stop() {
        try {
            if (listenerManager != null) {
                listenerManager.stop();
                listenerManager.destroy();
            }
            //we need to call this method to clean the team fils we created.
            if (configctx != null) {
                configctx.terminate();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Helper method to select a alternate port if the currently selected port is in use
     * @param configCtx configuration context 
     */
    private static void selectPort(ConfigurationContext configCtx) {
        // check if configured port is available
        TransportInDescription trsIn = (TransportInDescription)
                configCtx.getAxisConfiguration().getTransportsIn().get("http");

        if (trsIn != null) {

            int port = 8080;

            String strPort = System.getProperty("port");
            if (strPort != null) {
                // port is specified as a VM parameter
                try {
                    port = new Integer(strPort).intValue();
                } catch (NumberFormatException e) {
                    // user supplied parameter is not a valid integer. so use the port in configuration.
                    log.error("Given port is not a valid integer. Port specified in the configuration is used for the server.");
                    port = Integer.parseInt(trsIn.getParameter("port").getValue().toString());
                }

            } else {
                port = Integer.parseInt(trsIn.getParameter("port").getValue().toString());
            }

            while (true) {
                ServerSocket sock = null;
                try {
                    sock = new ServerSocket(port);
                    trsIn.getParameter("port").setValue(Integer.toString(port));
                    break;
                } catch (Exception e) {
                    System.out.println("[SynapseServer] Port " + port + " already in use. Trying alternate");
                    if (port == 8080) {
                        port = 8008;
                    } else {
                        port++;
                    }
                } finally {
                    if (sock != null) {
                        try {
                            sock.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }
}