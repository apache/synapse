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
package org.apache.synapse.util.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.util.MiscellaneousUtil;
import org.apache.synapse.util.RMIRegistryController;
import org.apache.synapse.util.datasource.factory.DataSourceInformationFactory;

import javax.naming.Context;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Utility class to handle data source registration
 */
public class DataSourceRegistrar {

    private static final Log log = LogFactory.getLog(DataSourceRegistrar.class);

    /**
     * The  static constants only for constructing key prefix for each property
     */
    private static final String PROP_ICFACTORY = "icFactory";
    private static final String PROP_PROVIDER_URL = "providerUrl";
    private static final String PROP_PROVIDER_PORT = "providerPort";
    private static final String DOT_STRING = ".";
    private final static String PROP_REGISTRY = "registry";
    private final static String PROP_REGISTRY_MEMORY = "memory";
    private final static String PROP_REGISTRY_JNDI = "JNDI";

    /**
     * Register data sources in the JNDI context
     * Given properties should contains all the properties need for construct JNDI naming references
     *
     * @param dsProperties The source properties
     */
    public static void registerDataSources(Properties dsProperties) {

        if (dsProperties == null) {
            if (log.isDebugEnabled()) {
                log.debug("DataSource properties cannot be found..");
            }
            return;
        }

        String dataSources = MiscellaneousUtil.getProperty(dsProperties,
                SynapseConstants.SYNAPSE_DATASOURCES, null);

        if (dataSources == null || "".equals(dataSources)) {
            if (log.isDebugEnabled()) {
                log.debug("No DataSources defined for initialization..");
            }
            return;
        }

        String[] dataSourcesNames = dataSources.split(",");
        if (dataSourcesNames == null || dataSourcesNames.length == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No DataSource definitions found for initialization..");
            }
            return;
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append(SynapseConstants.SYNAPSE_DATASOURCES);
        buffer.append(DOT_STRING);
        // The prefix for root level properties
        String rootPrefix = buffer.toString();

        Properties jndiEvn = null;
        //Registering data sources with the initial context
        for (String dsName : dataSourcesNames) {

            if (dsName == null) {
                continue;
            }

            StringBuffer registryBuffer = new StringBuffer();
            registryBuffer.append(rootPrefix);
            registryBuffer.append(dsName);
            registryBuffer.append(DOT_STRING);
            registryBuffer.append(PROP_REGISTRY);
            String registryKey = registryBuffer.toString();

            String registry = MiscellaneousUtil.getProperty(dsProperties,
                    registryKey, PROP_REGISTRY_MEMORY);


            DataSourceInformation information =
                    DataSourceInformationFactory.
                            createDataSourceInformation(dsName, dsProperties);

            DataSourceRegistry dataSourceRegistry;

            if (PROP_REGISTRY_JNDI.equals(registry)) {
                if (jndiEvn == null) {
                    jndiEvn = createJNDIEnvironment(dsProperties, rootPrefix);
                }
                dataSourceRegistry = JNDIBasedDataSourceRegistry.getInstance(jndiEvn);
            } else {
                dataSourceRegistry = InMemoryDataSourceRegistry.getInstance();
            }
            dataSourceRegistry.register(information);
        }
    }

    private static Properties createJNDIEnvironment(Properties dsProperties, String rootPrefix) {

        // setting naming provider
        Properties jndiEvn = new Properties();  //This is needed for PerUserPoolDatasource

        String namingFactory = MiscellaneousUtil.getProperty(
                dsProperties, rootPrefix + PROP_ICFACTORY,
                "com.sun.jndi.rmi.registry.RegistryContextFactory");

        jndiEvn.put(Context.INITIAL_CONTEXT_FACTORY, namingFactory);

        //Provider URL
        String providerUrl = MiscellaneousUtil.getProperty(
                dsProperties, rootPrefix + PROP_PROVIDER_URL, null);

        if (providerUrl != null && !"".equals(providerUrl)) {
            if (log.isDebugEnabled()) {
                log.debug("Using provided initial context provider url :" + providerUrl);
            }

        } else {
            if (log.isDebugEnabled()) {
                log.debug("No initial context provider url...creaeting a new one");
            }
            String providerHost = "localhost";
            try {
                InetAddress addr = InetAddress.getLocalHost();
                if (addr != null) {
                    String hostname = addr.getHostName();
                    if (hostname == null) {
                        String ipAddr = addr.getHostAddress();
                        if (ipAddr != null) {
                            providerHost = ipAddr;
                        }
                    } else {
                        providerHost = hostname;
                    }
                }
            } catch (UnknownHostException e) {
                log.warn("Unable to determine hostname or IP address.. Using localhost", e);
            }

            // default port for RMI registry
            int port = 2199;
            String providerPort =
                    MiscellaneousUtil.getProperty(dsProperties, rootPrefix + PROP_PROVIDER_PORT,
                            String.valueOf(port));
            try {
                port = Integer.parseInt(providerPort);
            } catch (NumberFormatException ignored) {
            }

            // Create a RMI local registry
            RMIRegistryController.getInstance().createLocalRegistry(port);

            providerUrl = MiscellaneousUtil.getProperty(dsProperties,
                    rootPrefix + PROP_PROVIDER_URL,
                    "rmi://" + providerHost + ":" + providerPort);
        }

        jndiEvn.put(Context.PROVIDER_URL, providerUrl);

        log.info("DataSources will be registered in the JNDI context with provider PROP_URL : " +
                providerUrl);
        return jndiEvn;
    }
}
