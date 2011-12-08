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
package org.apache.synapse.samples.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ServerConfigurationInformation;
import org.apache.synapse.ServerManager;
import org.apache.synapse.samples.framework.config.SynapseServerConfiguration;

import java.util.concurrent.CountDownLatch;

/**
 * Responsible for starting up and shutting down
 * a synapse server instance in order to run a sample test.
 */
public class SynapseProcessController implements ProcessController {

    private static final Log log = LogFactory.getLog(SynapseProcessController.class);

    private ServerThread serverThread;
    private SynapseServerConfiguration configuration;
    private ServerConfigurationInformation information;
    private ServerManager manager;
    private CountDownLatch cdLatch;
    private Exception processException;

    public SynapseProcessController(SynapseServerConfiguration configuration) {
        this.configuration = configuration;
        information = new ServerConfigurationInformation();
        manager = new ServerManager();
        cdLatch = new CountDownLatch(1);
        serverThread = new ServerThread();
        serverThread.setName(configuration.getServerName() + " thread");
    }

    public boolean startProcess() {
        information.setSynapseHome(configuration.getSynapseHome());
        information.setSynapseXMLLocation(configuration.getSynapseXml());
        information.setServerName(configuration.getServerName());
        information.setAxis2Xml(configuration.getAxis2Xml());
        information.setResolveRoot(configuration.getAxis2Repo());
        information.setAxis2RepoLocation(configuration.getAxis2Repo());

        log.info("SynapseProcessController: Preparing to start synapse server");
        serverThread.start();

        try {
            log.info("SynapseProcessController: Waiting for synapse to start");
            cdLatch.await();
            if (processException == null) {
                log.info("SynapseProcessController: synapse is started. continuing tests");
                return true;
            } else {
                log.warn("SynapseProcessController: There was an error starting synapse", processException);
                return false;
            }
        } catch (InterruptedException e) {
            return false;
        }
    }

    public boolean stopProcess() {
        if (serverThread.isRunning) {
            serverThread.isRunning = false;
            try {
                cdLatch = new CountDownLatch(1);
                cdLatch.await();
            } catch (InterruptedException e) {
                log.warn("Thread interrupted");
            }
        }
        return true;
    }

    private class ServerThread extends Thread {

        public boolean isRunning = false;

        public void run() {
            processException = null;
            log.info("SynapseProcessController.ServerThread: Initializing Synapse Server...");
            try {
                manager.init(information, null);
                log.info("SynapseProcessController.ServerThread: Starting Synapse Server...");
                manager.start();
                isRunning = true;
            } catch (Exception e) {
                processException = e;
            }
            cdLatch.countDown();

            log.info("SynapseProcessController.ServerThread: Await until test are finished");
            while (isRunning) {
                //wait
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    log.warn("Thread interrupted");
                }
            }
            log.info("SynapseProcessController.ServerThread:Shutting down Synapse Server...");
            manager.shutdown();
            cdLatch.countDown();
        }
    }

}