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
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;

import javax.naming.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Hashtable;
import java.io.*;

/**
 * Utility class to handle data source registration
 */
public class DataSourceRegistrar {

    public static final Log log = LogFactory.getLog(DataSourceRegistrar.class);

    /**
     * The  static constants only for constructing key prefix for each property
     */
    private static final String PROP_ICFACTORY = "icFactory";
    private static final String PROP_PROVIDER_URL = "providerUrl";
    private static final String PROP_PROVIDER_PORT = "providerPort";
    private static final String DOT_STRING = ".";
    private static final String PROP_USER_NAME = "username";
    private static final String PROP_PASSWORD = "password";
    private static final String PROP_DRIVER_CLS_NAME = "driverClassName";
    private static final String PROP_DSNAME = "dsName";
    private static final String PROP_URL = "url";
    private static final String PROP_DRIVER = "driver";
    private static final String PROP_USER = "user";

    private static final String PROP_CPDSADAPTER = "cpdsadapter";
    private static final String PROP_JNDI_ENV = "jndiEnvironment";
    private static final String PROP_DEFAULTMAXACTIVE = "defaultMaxActive";
    private static final String PROP_DEFAULTMAXIDLE = "defaultMaxIdle";
    private static final String PROP_DEFAULTMAXWAIT = "defaultMaxWait";
    private static final String PROP_DATA_SOURCE_NAME = "dataSourceName";

    private final static String PROP_DEFAULTAUTOCOMMIT = "defaultAutoCommit";
    private final static String PROP_DEFAULTREADONLY = "defaultReadOnly";
    private final static String PROP_TESTONBORROW = "testOnBorrow";
    private final static String PROP_TESTONRETURN = "testOnReturn";
    private final static String PROP_TIMEBETWEENEVICTIONRUNSMILLIS =
            "timeBetweenEvictionRunsMillis";
    private final static String PROP_NUMTESTSPEREVICTIONRUN = "numTestsPerEvictionRun";
    private final static String PROP_MINEVICTABLEIDLETIMEMILLIS = "minEvictableIdleTimeMillis";
    private final static String PROP_TESTWHILEIDLE = "testWhileIdle";
    private final static String PROP_VALIDATIONQUERY = "validationQuery";
    private final static String PROP_MAXACTIVE = "maxActive";
    private final static String PROP_MAXIDLE = "maxIdle";
    private final static String PROP_MAXWAIT = "maxWait";

    private final static String PROP_MINIDLE = "minIdle";
    private final static String PROP_INITIALSIZE = "initialSize";
    private final static String PROP_DEFAULTTRANSACTIONISOLATION = "defaultTransactionIsolation";
    private final static String PROP_DEFAULTCATALOG = "defaultCatalog";
    private final static String PROP_ACCESSTOUNDERLYINGCONNECTIONALLOWED =
            "accessToUnderlyingConnectionAllowed";
    private final static String PROP_REMOVEABANDONED = "removeAbandoned";
    private final static String PROP_REMOVEABANDONEDTIMEOUT = "removeAbandonedTimeout";
    private final static String PROP_LOGABANDONED = "logAbandoned";
    private final static String PROP_POOLPREPAREDSTATEMENTS = "poolPreparedStatements";
    private final static String PROP_MAXOPENPREPAREDSTATEMENTS = "maxOpenPreparedStatements";
    private final static String PROP_CONNECTIONPROPERTIES = "connectionProperties";

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

        // setting naming provider
        Hashtable props = new Hashtable();
        Properties jndiEvn = new Properties();  //This is needed for PerUserPoolDatasource

        String namingFactory = getProperty(dsProperties, rootPrefix + PROP_ICFACTORY,
                "com.sun.jndi.rmi.registry.RegistryContextFactory");

        props.put(Context.INITIAL_CONTEXT_FACTORY, namingFactory);
        jndiEvn.put(Context.INITIAL_CONTEXT_FACTORY, namingFactory);

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
                getProperty(dsProperties, rootPrefix + PROP_PROVIDER_PORT, String.valueOf(port));
        try {
            port = Integer.parseInt(providerPort);
        } catch (NumberFormatException ignored) {
        }

        // Create a RMI local registry
        RMIRegistryController.getInstance().createLocalRegistry(port);

        String providerUrl = getProperty(dsProperties, rootPrefix + PROP_PROVIDER_URL,
                "rmi://" + providerHost + ":" + providerPort);

        props.put(Context.PROVIDER_URL, providerUrl);
        jndiEvn.put(Context.PROVIDER_URL, providerUrl);

        log.info("DataSources will be registered in the JNDI context with provider PROP_URL : " +
                providerUrl);

        try {
            InitialContext initialContext = new InitialContext(props);
            //Registering data sources with the initial context
            for (int i = 0; i < dataSourcesNames.length; i++) {
                registerDataSource(dataSourcesNames[i], dsProperties, initialContext, jndiEvn);
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
     * @param jndiEnv        The JNDI environment properties
     */
    private static void registerDataSource(String dsName, Properties dsProperties, InitialContext initialContext, Properties jndiEnv) {

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

        String driver = getProperty(dsProperties, prefix + PROP_DRIVER_CLS_NAME, null);
        if (driver == null) {
            handleException(prefix + PROP_DRIVER_CLS_NAME + " cannot be found.");
        }

        String url = getProperty(dsProperties, prefix + PROP_URL, null);
        if (url == null) {
            handleException(prefix + PROP_URL + " cannot be found.");
        }

        // get other required properties
        String user = getProperty(dsProperties, prefix + PROP_USER_NAME, "synapse");
        String password = getProperty(dsProperties, prefix + PROP_PASSWORD, "synapse");
        String dataSourceName = getProperty(dsProperties, prefix + PROP_DSNAME, dsName);

        //populates context tree
        populateContextTree(initialContext, dataSourceName);

        String dsType = getProperty(dsProperties, prefix + "type", "BasicDataSource");

        String maxActive = getProperty(dsProperties, prefix + PROP_MAXACTIVE,
                String.valueOf(GenericObjectPool.DEFAULT_MAX_ACTIVE));
        String maxIdle = getProperty(dsProperties, prefix + PROP_MAXIDLE,
                String.valueOf(GenericObjectPool.DEFAULT_MAX_IDLE));
        String maxWait = getProperty(dsProperties, prefix + PROP_MAXWAIT,
                String.valueOf(GenericObjectPool.DEFAULT_MAX_WAIT));

        if ("BasicDataSource".equals(dsType)) {

            Reference ref = new Reference("javax.sql.DataSource",
                    "org.apache.commons.dbcp.BasicDataSourceFactory", null);

            ref.add(new StringRefAddr(PROP_DRIVER_CLS_NAME, driver));
            ref.add(new StringRefAddr(PROP_URL, url));
            ref.add(new StringRefAddr(PROP_USER_NAME, user));
            ref.add(new StringRefAddr(PROP_PASSWORD, password));
            ref.add(new StringRefAddr(PROP_MAXACTIVE, maxActive));
            ref.add(new StringRefAddr(PROP_MAXIDLE, maxIdle));
            ref.add(new StringRefAddr(PROP_MAXWAIT, maxWait));

            //set BasicDataSource specific parameters
            setBasicDataSourceParameters(ref, dsProperties, prefix);
            //set default properties for reference
            setCommonParameters(ref, dsProperties, prefix);

            try {
                initialContext.rebind(dataSourceName, ref);
            } catch (NamingException e) {
                String msg = " Error binding name ' " + dataSourceName + " ' to " +
                        "the DataSource(BasicDataSource) reference";
                handleException(msg, e);
            }

        } else if ("PerUserPoolDataSource".equals(dsType)) {

            // Construct DriverAdapterCPDS reference
            String className = getProperty(dsProperties, prefix + PROP_CPDSADAPTER +
                    DOT_STRING + "className",
                    "org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS");
            String factory = getProperty(dsProperties, prefix + PROP_CPDSADAPTER +
                    DOT_STRING + "factory",
                    "org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS");
            String name = getProperty(dsProperties, prefix + PROP_CPDSADAPTER +
                    DOT_STRING + "name",
                    "cpds");

            Reference cpdsRef =
                    new Reference(className, factory, null);

            cpdsRef.add(new StringRefAddr(PROP_DRIVER, driver));
            cpdsRef.add(new StringRefAddr(PROP_URL, url));
            cpdsRef.add(new StringRefAddr(PROP_USER, user));
            cpdsRef.add(new StringRefAddr(PROP_PASSWORD, password));

            try {
                initialContext.rebind(name, cpdsRef);
            } catch (NamingException e) {
                String msg = "Error binding name '" + name + "' to " +
                        "the DriverAdapterCPDS reference";
                handleException(msg, e);
            }

            // Construct PerUserPoolDataSource reference
            Reference ref =
                    new Reference("org.apache.commons.dbcp.datasources.PerUserPoolDataSource",
                            "org.apache.commons.dbcp.datasources.PerUserPoolDataSourceFactory", null);

            ref.add(new BinaryRefAddr(PROP_JNDI_ENV, serialize(jndiEnv)));
            ref.add(new StringRefAddr(PROP_DATA_SOURCE_NAME, name));
            ref.add(new StringRefAddr(PROP_DEFAULTMAXACTIVE, maxActive));
            ref.add(new StringRefAddr(PROP_DEFAULTMAXIDLE, maxIdle));
            ref.add(new StringRefAddr(PROP_DEFAULTMAXWAIT, maxWait));

            //set default properties for reference
            setCommonParameters(ref, dsProperties, prefix);

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
     * Helper method to serialize object into a byte array
     *
     * @param data The object to be serialized
     * @return The byte array representation of the provided object
     */
    private static byte[] serialize(Object data) {

        ObjectOutputStream outputStream = null;
        ByteArrayOutputStream binOut = null;
        byte[] result = null;
        try {
            binOut = new ByteArrayOutputStream();
            outputStream = new ObjectOutputStream(binOut);
            outputStream.writeObject(data);
            result = binOut.toByteArray();
        } catch (IOException e) {
            handleException("Error serializing object :" + data);
        } finally {
            if (binOut != null) {
                try {
                    binOut.close();
                } catch (IOException ex) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ex) {
                }
            }
        }
        return result;
    }

    /**
     * Helper method to set all default parameter for naming reference of data source
     *
     * @param reference  The naming reference instance
     * @param properties The properties which contains required parameter value
     * @param prefix     The key prefix for which is used to get data from given properties
     */
    private static void setCommonParameters(Reference reference, Properties properties, String prefix) {
        String defaultAutoCommit = getProperty(properties,
                prefix + PROP_DEFAULTAUTOCOMMIT, String.valueOf(true));
        String defaultReadOnly = getProperty(properties,
                prefix + PROP_DEFAULTREADONLY, String.valueOf(false));
        String testOnBorrow = getProperty(properties,
                prefix + PROP_TESTONBORROW, String.valueOf(true));
        String testOnReturn = getProperty(properties,
                prefix + PROP_TESTONRETURN, String.valueOf(false));
        String timeBetweenEvictionRunsMillis = getProperty(properties,
                prefix + PROP_TIMEBETWEENEVICTIONRUNSMILLIS,
                String.valueOf(GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS));
        String numTestsPerEvictionRun = getProperty(properties,
                prefix + PROP_NUMTESTSPEREVICTIONRUN,
                String.valueOf(GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN));
        String minEvictableIdleTimeMillis = getProperty(properties,
                prefix + PROP_MINEVICTABLEIDLETIMEMILLIS,
                String.valueOf(GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS));
        String testWhileIdle = getProperty(properties,
                prefix + PROP_TESTWHILEIDLE, String.valueOf(false));
        String validationQuery = getProperty(properties,
                prefix + PROP_VALIDATIONQUERY, null);

        reference.add(new StringRefAddr(PROP_DEFAULTAUTOCOMMIT, defaultAutoCommit));
        reference.add(new StringRefAddr(PROP_DEFAULTREADONLY, defaultReadOnly));
        reference.add(new StringRefAddr(PROP_TESTONBORROW, testOnBorrow));
        reference.add(new StringRefAddr(PROP_TESTONRETURN, testOnReturn));
        reference.add(new StringRefAddr(PROP_TIMEBETWEENEVICTIONRUNSMILLIS,
                timeBetweenEvictionRunsMillis));
        reference.add(new StringRefAddr(PROP_NUMTESTSPEREVICTIONRUN, numTestsPerEvictionRun));
        reference.add(new StringRefAddr(PROP_MINEVICTABLEIDLETIMEMILLIS,
                minEvictableIdleTimeMillis));
        reference.add(new StringRefAddr(PROP_TESTWHILEIDLE, testWhileIdle));
        if (validationQuery != null && !"".equals(validationQuery)) {
            reference.add(new StringRefAddr(PROP_VALIDATIONQUERY, validationQuery));
        }
    }

    /**
     * Helper method to set all BasicDataSource specific parameter
     *
     * @param reference  The naming reference instance
     * @param properties The properties which contains required parameter value
     * @param prefix     The key prefix for which is used to get data from given properties
     */
    private static void setBasicDataSourceParameters(Reference reference, Properties properties, String prefix) {
        String minIdle = getProperty(properties,
                prefix + PROP_MINIDLE, String.valueOf(GenericObjectPool.DEFAULT_MIN_IDLE));
        String initialSize = getProperty(properties, prefix + PROP_INITIALSIZE, String.valueOf(0));
        String defaultTransactionIsolation = getProperty(properties,
                prefix + PROP_DEFAULTTRANSACTIONISOLATION, null);
        String defaultCatalog = getProperty(properties, prefix + PROP_DEFAULTCATALOG, null);
        String accessToUnderlyingConnectionAllowed = getProperty(properties,
                prefix + PROP_ACCESSTOUNDERLYINGCONNECTIONALLOWED, String.valueOf(false));
        String removeAbandoned = getProperty(properties,
                prefix + PROP_REMOVEABANDONED, String.valueOf(false));
        String removeAbandonedTimeout = getProperty(properties,
                prefix + PROP_REMOVEABANDONEDTIMEOUT, String.valueOf(300));
        String logAbandoned = getProperty(properties,
                prefix + PROP_LOGABANDONED, String.valueOf(false));
        String poolPreparedStatements = getProperty(properties,
                prefix + PROP_POOLPREPAREDSTATEMENTS, String.valueOf(false));
        String maxOpenPreparedStatements = getProperty(properties,
                prefix + PROP_MAXOPENPREPAREDSTATEMENTS,
                String.valueOf(GenericKeyedObjectPool.DEFAULT_MAX_TOTAL));

        reference.add(new StringRefAddr(PROP_MINIDLE, minIdle));
        if (defaultTransactionIsolation != null && !"".equals(defaultTransactionIsolation)) {
            reference.add(new StringRefAddr(PROP_DEFAULTTRANSACTIONISOLATION,
                    defaultTransactionIsolation));
        }
        reference.add(new StringRefAddr(PROP_ACCESSTOUNDERLYINGCONNECTIONALLOWED,
                accessToUnderlyingConnectionAllowed));
        reference.add(new StringRefAddr(PROP_REMOVEABANDONED, removeAbandoned));
        reference.add(new StringRefAddr(PROP_REMOVEABANDONEDTIMEOUT, removeAbandonedTimeout));
        reference.add(new StringRefAddr(PROP_LOGABANDONED, logAbandoned));
        reference.add(new StringRefAddr(PROP_POOLPREPAREDSTATEMENTS, poolPreparedStatements));
        reference.add(new StringRefAddr(PROP_MAXOPENPREPAREDSTATEMENTS,
                maxOpenPreparedStatements));
        reference.add(new StringRefAddr(PROP_INITIALSIZE, initialSize));
        if (defaultCatalog != null && !"".equals(defaultCatalog)) {
            reference.add(new StringRefAddr(PROP_DEFAULTCATALOG, defaultCatalog));
        }
    }

    /**
     * Helper method to create context tree for a given path
     *
     * @param initialContext The root context
     * @param path           The path of the resource
     */
    private static void populateContextTree(InitialContext initialContext, String path) {

        String[] paths = path.split("/");
        if (paths != null && paths.length > 1) {

            Context context = initialContext;
            for (int i = 0; i < paths.length; i++) {

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
                        "Using default value " + def);
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
