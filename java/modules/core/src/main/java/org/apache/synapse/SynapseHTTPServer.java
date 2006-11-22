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

import org.apache.axis2.util.OptionsParser;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.engine.ListenerManager;
import org.apache.axis2.description.TransportInDescription;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.Iterator;

/**
 * Starts all transports as specified on the axis2.xml
 */
public class SynapseHTTPServer {

    public static void printUsage() {
        System.out.println("Usage: SynapseHTTPServer <repository>");
        System.out.println(" Opts: -? this message");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {

        // first check if we should print usage
        if (args.length != 1 || !new File(args[0]).exists()) {
            printUsage();
        }

        System.out.println("[SynapseHTTPServer] Using the Axis2 Repository "
                + new File(args[0]).getAbsolutePath());

        try {
            ConfigurationContext configctx = ConfigurationContextFactory.
                createConfigurationContextFromFileSystem(args[0], null);

            ListenerManager listenerManager = configctx.getListenerManager();
            if (listenerManager == null) {
                listenerManager = new ListenerManager();
                listenerManager.init(configctx);
            }

            Iterator iter = configctx.getAxisConfiguration().
                getTransportsIn().keySet().iterator();
            while (iter.hasNext()) {
                QName trp = (QName) iter.next();
                TransportInDescription trsIn = (TransportInDescription)
                    configctx.getAxisConfiguration().getTransportsIn().get(trp);
                listenerManager.addListener(trsIn, false);
                if (new QName("http").equals(trsIn.getName())) {
                    System.out.println("[SynapseHTTPServer] Started HTTP on port : " +
                        trsIn.getParameter("port").getValue());
                }
            }
            System.out.println("[SynapseHTTPServer] Ready");

        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("[SynapseHTTPServer] Startup failed...");
        }
    }

}
