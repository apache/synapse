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

package samples.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.axis2.util.CommandLineOptionParser;
import org.apache.axis2.util.OptionsValidator;
import org.apache.axis2.util.CommandLineOption;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.engine.ListenerManager;
import org.apache.axis2.description.TransportInDescription;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.net.ServerSocket;

public class SampleAxis2Server {

    private static final Log log = LogFactory.getLog(SimpleHTTPServer.class);

    int port = -1;

    public static int DEFAULT_PORT = 9000;


    /**
     * Expected system properties
     *      http_port: Port to bind HTTP transport (default is 9000)
     *      https_port: Port to bind HTTPS transport (default is 9002)
     *      server_name: Name of this instance of the server (optional)
     *
     * @param args  1: Axis2 repository
     *              2: Axis2 configuration file (axis2.xml)
     *
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        startServer(args);
    }

    public static ListenerManager startServer(String[] args) {
        String repoLocation = null;
		String confLocation = null;

		CommandLineOptionParser optionsParser = new CommandLineOptionParser(args);
		List invalidOptionsList = optionsParser.getInvalidOptions(new OptionsValidator() {
			public boolean isInvalid(CommandLineOption option) {
				String optionType = option.getOptionType();
				return !("repo".equalsIgnoreCase(optionType) || "conf"
						.equalsIgnoreCase(optionType));
			}
		});

		if ((invalidOptionsList.size()>0)||(args.length>4))
		{
			printUsage();
			return null;
		}

		Map optionsMap = optionsParser.getAllOptions();

		CommandLineOption repoOption = (CommandLineOption) optionsMap
				.get("repo");
		CommandLineOption confOption = (CommandLineOption) optionsMap
				.get("conf");

		log.info("[SimpleAxisServer] Starting");
		if (repoOption != null) {
			repoLocation = repoOption.getOptionValue();
			System.out.println("[SimpleAxisServer] Using the Axis2 Repository : "
					+ new File(repoLocation).getAbsolutePath());
		}
		if (confOption != null) {
			confLocation = confOption.getOptionValue();
			System.out
					.println("[SimpleAxisServer] Using the Axis2 Configuration File : "
							+ new File(confLocation).getAbsolutePath());
		}

		try {
			ConfigurationContext configctx = ConfigurationContextFactory
					.createConfigurationContextFromFileSystem(repoLocation,
							confLocation);

            configurePort(configctx);

            ListenerManager listenerManager =  new ListenerManager();
				listenerManager.init(configctx);
			listenerManager.start();
            log.info("[SimpleAxisServer] Started");
            return listenerManager;
        } catch (Throwable t) {
            log.fatal("[SimpleAxisServer] Shutting down. Error starting SimpleAxisServer", t);
        }
        return null;
    }

    private static void configurePort(ConfigurationContext configCtx) {

        TransportInDescription trsIn = (TransportInDescription)
            configCtx.getAxisConfiguration().getTransportsIn().get("http");

        if(trsIn != null) {
            String port = System.getProperty("http_port");
            if(port != null) {
                try {
                    new Integer(port);
                    trsIn.getParameter("port").setValue(port);
                } catch (NumberFormatException e) {
                    log.error("Given port is not a valid integer. Using 9000 for port.");
                    trsIn.getParameter("port").setValue("9000");
                }
            } else {
                trsIn.getParameter("port").setValue("9000");
            }
        }

        TransportInDescription httpsTrsIn = (TransportInDescription)
            configCtx.getAxisConfiguration().getTransportsIn().get("https");

        if(httpsTrsIn != null) {
            String port = System.getProperty("https_port");
            if(port != null) {
                try {
                    new Integer(port);
                    httpsTrsIn.getParameter("port").setValue(port);
                } catch (NumberFormatException e) {
                    log.error("Given port is not a valid integer. Using 9000 for port.");
                    httpsTrsIn.getParameter("port").setValue("9002");
                }
            } else {
                httpsTrsIn.getParameter("port").setValue("9002");
            }
        }
    }

    public static void printUsage() {
        System.out.println("Usage: SampleAxisServer -repo <repository>  -conf <axis2 configuration file>");
        System.out.println();
        System.exit(1);
    }
}
