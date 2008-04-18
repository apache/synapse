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
package org.apache.synapse.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;

import javax.naming.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Hashtable;

/**
 * Utility class to handle data source registration
 */
public class DataSourceRegistrar {

    public static final Log log = LogFactory.getLog(DataSourceRegistrar.class);

    /**
     * The  static constants only for constructing key prefix for each property
     */
    private static final String ICFACTORY = "icFactory";
    private static final String PROVIDER_URL = "providerUrl";
    private static final String PROVIDER_PORT = "providerPort";
    private static final String DOT_STRING = ".";
    private static final String USER_NAME = "username";
    private static final String PASSWORD = "password";
    private static final String DRIVER_CLS_NAME = "driverClassName";
    private static final String DSNAME = "dsName";
    private static final String URL = "url";
    private static final String MAX_ACTIVE = "maxActive";
    private static final String MAX_IDLE = "maxIdle";
    private static final String MAX_WAIT = "maxWait";
    private static final String DATA_SOURCE_NAME = "dataSourceName";
    private static final String DRIVER = "driver";
    private static final String USER = "user";

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

        String dataSources = getProperty(dsProperties,
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

        // setting naming provider URL
        Hashtable props = new Hashtable();
        props.put(Context.INITIAL_CONTEXT_FACTORY,
            getProperty(dsProperties, rootPrefix + ICFACTORY,
                "com.sun.jndi.rmi.registry.RegistryContextFactory"));

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
            getProperty(dsProperties, rootPrefix + PROVIDER_PORT, String.valueOf(port));
        try {
            port = Integer.parseInt(providerPort);
        } catch (NumberFormatException ignored) {}

        // Create a RMI local registry
        RMIRegistryController.getInstance().createLocalRegistry(port);

        String providerUrl = getProperty(dsProperties, rootPrefix + PROVIDER_URL,
                "rmi://" + providerHost + ":" + providerPort);
        props.put(Context.PROVIDER_URL, providerUrl);

        log.info("DataSources will be registered in the JNDI context with provider URL : " +
                providerUrl);

        try {
            InitialContext initialContext = new InitialContext(props);
            //Registering data sources with the initial context
            for (int i = 0; i < dataSourcesNames.length; i++) {
                registerDataSource(dataSourcesNames[i], dsProperties, initialContext);
            }

        } catch (NamingException e) {
            String msg = "Error constructing an InitialContext to register DataSources";
            handleException(msg, e);
        }
    }

    /**
     * Helper method to register a single data source .The given data source name is used ,
     * if there is no property with name dsName ,when,data source is binding to the initial context,
     *
     * @param dsName         The name of the data source
     * @param dsProperties   The property bag
     * @param initialContext The initial context instance
     */
    private static void registerDataSource(String dsName, Properties dsProperties, InitialContext initialContext) {

        if (dsName == null || "".equals(dsName)) {
            if (log.isDebugEnabled()) {
                log.debug("DataSource name is either empty or null, ignoring..");
            }
            return;
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append(SynapseConstants.SYNAPSE_DATASOURCES);
        buffer.append(DOT_STRING);
        buffer.append(dsName);
        buffer.append(DOT_STRING);

        // Prefix for getting particular data source's properties
        String prefix = buffer.toString();

        String driver = getProperty(dsProperties, prefix + DRIVER_CLS_NAME, null);
        if (driver == null) {
            handleException(prefix + DRIVER_CLS_NAME + " cannot be found.");
        }

        String url = getProperty(dsProperties, prefix + URL, null);
        if (url == null) {
            handleException(prefix + URL + " cannot be found.");
        }

        // get other required properties
        String user = getProperty(dsProperties, prefix + USER_NAME, "synapse");
        String password = getProperty(dsProperties, prefix + PASSWORD, "synapse");
        String dataSourceName = getProperty(dsProperties, prefix + DSNAME, dsName);

        //populates context tree
        populateContextTree(initialContext, dataSourceName);

        String dsType = getProperty(dsProperties, prefix + "type", "BasicDataSource");

        if ("BasicDataSource".equals(dsType)) {

            Reference ref = new Reference("javax.sql.DataSource",
                    "org.apache.commons.dbcp.BasicDataSourceFactory", null);

            ref.add(new StringRefAddr(DRIVER_CLS_NAME, driver));
            ref.add(new StringRefAddr(URL, url));
            ref.add(new StringRefAddr(USER_NAME, user));
            ref.add(new StringRefAddr(PASSWORD, password));

            //set default properties for reference
            setDefaultParameters(ref, dsProperties, prefix);

            try {
                initialContext.rebind(dataSourceName, ref);
            } catch (NamingException e) {
                String msg = " Error when binds a name ' " + dataSourceName + " ' to " +
                        "the DataSource(BasicDataSource) reference";
                handleException(msg, e);
            }

        } else if ("PerUserPoolDataSource".equals(dsType)) {

            // Construct DriverAdapterCPDS reference
            String className = getProperty(dsProperties, prefix + DOT_STRING + "className",
                    "org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS");
            String factory = getProperty(dsProperties, prefix + DOT_STRING + "factory",
                    "org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS");
            String name = getProperty(dsProperties, prefix + DOT_STRING + "name",
                    "cpds");
            Reference cpdsRef =
                    new Reference(className, factory, null);

            cpdsRef.add(new StringRefAddr(DRIVER, driver));
            cpdsRef.add(new StringRefAddr(URL, url));
            cpdsRef.add(new StringRefAddr(USER, user));
            cpdsRef.add(new StringRefAddr(PASSWORD, password));

            try {
                initialContext.rebind(name, cpdsRef);
            } catch (NamingException e) {
                String msg = "Error binding name '" + name + "' to " +
                        "the DriverAdapterCPDS reference";
                handleException(msg, e);
            }

            // Construct PerUserPoolDataSource reference
            Reference ref = new Reference("org.apache.commons.dbcp.datasources.PerUserPoolDataSource",
                    "org.apache.commons.dbcp.datasources.PerUserPoolDataSourceFactory", null);

            ref.add(new StringRefAddr(DRIVER_CLS_NAME, driver));
            ref.add(new StringRefAddr(URL, url));
            ref.add(new StringRefAddr(USER_NAME, user));
            ref.add(new StringRefAddr(PASSWORD, password));
            ref.add(new StringRefAddr(DATA_SOURCE_NAME, name));

            //set default properties for reference
            setDefaultParameters(ref, dsProperties, prefix);

            try {
                initialContext.rebind(dataSourceName, ref);
            } catch (NamingException e) {
                String msg = "Error binding name ' " + dataSourceName + " ' to " +
                        "the PerUserPoolDataSource reference";
                handleException(msg, e);
            }

        } else {
            handleException("Unsupported data source type : " + dsType);
        }
    }

    /**
     * Helper method to set all default parameter for naming reference of data source
     *
     * @param reference  The naming reference instance
     * @param properties The properties which contains required parameter value
     * @param prefix     The key prefix for which is used to get data from given properties
     */
    private static void setDefaultParameters(Reference reference, Properties properties, String prefix) {

        String maxActive = getProperty(properties, prefix + MAX_ACTIVE, "100");
        String maxIdle = getProperty(properties, prefix + MAX_IDLE, "30");
        String maxWait = getProperty(properties, prefix + MAX_WAIT, "10000");

        reference.add(new StringRefAddr("defaultMaxActive", maxActive));
        reference.add(new StringRefAddr("defaultMaxIdle", maxIdle));
        reference.add(new StringRefAddr("defaultMaxWait", maxWait));
    }

    /**
     * Helper method to create context tree for a given path
     *
     * @param initialContext The root context
     * @param path           The path of the resource
     */
    private static void populateContextTree(InitialContext initialContext, String path) {

        String[] paths = path.split("/");
        if (paths != null && paths.length != 0) {

            Context context = initialContext;
            for (int i=0; i < paths.length; i++) {

                try {
                    context = context.createSubcontext(paths[i]);
                    if (context == null) {
                        handleException("sub context " + paths[i] + " could not be created");
                    }

                } catch (NamingException e) {
                    handleException("Unable to create sub context : " + paths[i], e);
                }
            }
        }
    }

    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     */
    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     * @param e   The exception
     */
    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    /**
     * Helper method to get the value of the property from a given property bag
     *
     * @param dsProperties The property collection
     * @param name         The name of the property
     * @param def          The default value for the property
     * @return The value of the property if it is found , otherwise , default value
     */
    private static String getProperty(Properties dsProperties, String name, String def) {

        String result = dsProperties.getProperty(name);
        if ((result == null || result.length() == 0 || "".equals(result)) && def != null) {
            if (log.isDebugEnabled()) {
                log.debug("The name with ' " + name + " ' cannot be found. " +
                        "Using default vale " + def);
            }
            result = def;
        }
        if (result != null) {
            return result.trim();
        } else {
            return def;
        }
    }
}
