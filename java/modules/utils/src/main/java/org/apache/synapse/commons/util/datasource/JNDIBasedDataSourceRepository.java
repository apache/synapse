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
/**
 * To change this template use File | Settings | File Templates.
 */
package org.apache.synapse.commons.util.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.util.MiscellaneousUtil;
import org.apache.synapse.commons.util.RMIRegistryController;
import org.apache.synapse.commons.util.SynapseUtilException;

import javax.naming.*;
import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Keep all DataSources in the JNDI Tree
 */
public class JNDIBasedDataSourceRepository implements DataSourceRepository {

    private static Log log = LogFactory.getLog(JNDIBasedDataSourceRepository.class);

    private static final JNDIBasedDataSourceRepository ourInstance =
            new JNDIBasedDataSourceRepository();
    private InitialContext initialContext;
    private Properties jndiProperties;
    private static final Map<String, InitialContext> perDataSourceICMap = new HashMap<String, InitialContext>();
    private boolean initialized = false;

    public static JNDIBasedDataSourceRepository getInstance() {
        return ourInstance;
    }

    public void init(Properties jndiEnv) {

        initialized = true;
        if (jndiEnv == null || jndiEnv.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Provided global JNDI environment properties is empty or null.");
            }
            return;
        }

        if (isValid(jndiEnv)) {
            jndiProperties = createJNDIEnvironment(jndiEnv, null);
            initialContext = createInitialContext(jndiProperties);
        }
    }

    private JNDIBasedDataSourceRepository() {
    }

    /**
     * Register a DataSource in the JNDI tree
     *
     * @see DataSourceRepository#register(DataSourceInformation)
     */
    public void register(DataSourceInformation information) {

        validateInitialized();
        String dataSourceName = information.getDatasourceName();
        validateDSName(dataSourceName);
        Properties properties = information.getProperties();

        InitialContext context = null;
        Properties jndiEvn = null;

        if (properties == null || properties.isEmpty()) {
            if (initialContext != null) {
                context = initialContext;
                if (log.isDebugEnabled()) {
                    log.debug("Empty JNDI properties for datasource " + dataSourceName);
                    log.debug("Using system-wide jndi properties : " + jndiProperties);
                }

                jndiEvn = jndiProperties;
            }
        }

        if (context == null) {

            jndiEvn = createJNDIEnvironment(properties, information.getAlias());
            context = createInitialContext(jndiEvn);

            if (context == null) {

                validateInitialContext(initialContext);
                context = initialContext;

                if (log.isDebugEnabled()) {
                    log.debug("Cannot create a name context with provided jndi properties : " + jndiEvn);
                    log.debug("Using system-wide JNDI properties : " + jndiProperties);
                }

                jndiEvn = jndiProperties;
            } else {
                perDataSourceICMap.put(dataSourceName, context);
            }
        }

        String dsType = information.getType();
        String driver = information.getDriver();
        String url = information.getUrl();
        String user = information.getUser();
        String password = information.getPassword();
        String maxActive = String.valueOf(information.getMaxActive());
        String maxIdle = String.valueOf(information.getMaxIdle());
        String maxWait = String.valueOf(information.getMaxWait());

        //populates context tree
        populateContextTree(context, dataSourceName);

        if (DataSourceInformation.BASIC_DATA_SOURCE.equals(dsType)) {

            Reference ref = new Reference("javax.sql.DataSource",
                    "org.apache.commons.dbcp.BasicDataSourceFactory", null);

            ref.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_DRIVER_CLS_NAME, driver));
            ref.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_URL, url));
            ref.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_USER_NAME, user));
            ref.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_PASSWORD, password));
            ref.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_MAXACTIVE, maxActive));
            ref.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_MAXIDLE, maxIdle));
            ref.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_MAXWAIT, maxWait));

            // set BasicDataSource specific parameters
            setBasicDataSourceParameters(ref, information);
            //set default jndiProperties for reference
            setCommonParameters(ref, information);

            try {

                if (log.isDebugEnabled()) {
                    log.debug("Registering a DataSource with name : " +
                            dataSourceName + " in the JNDI tree with jndiProperties : " + jndiEvn);
                }

                context.rebind(dataSourceName, ref);
            } catch (NamingException e) {
                String msg = " Error binding name ' " + dataSourceName + " ' to " +
                        "the DataSource(BasicDataSource) reference";
                handleException(msg, e);
            }

        } else if (DataSourceInformation.PER_USER_POOL_DATA_SOURCE.equals(dsType)) {

            // Construct DriverAdapterCPDS reference
            String className = (String) information.getParameter(
                    DataSourceConfigurationConstants.PROP_CPDSADAPTER +
                            DataSourceConfigurationConstants.DOT_STRING +
                            DataSourceConfigurationConstants.PROP_CPDS_CLASS_NAME);
            String factory = (String) information.getParameter(
                    DataSourceConfigurationConstants.PROP_CPDSADAPTER +
                            DataSourceConfigurationConstants.DOT_STRING +
                            DataSourceConfigurationConstants.PROP_CPDS_FACTORY);
            String name = (String) information.getParameter(
                    DataSourceConfigurationConstants.PROP_CPDSADAPTER +
                            DataSourceConfigurationConstants.DOT_STRING +
                            DataSourceConfigurationConstants.PROP_CPDS_NAME);

            Reference cpdsRef =
                    new Reference(className, factory, null);

            cpdsRef.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_DRIVER, driver));
            cpdsRef.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_URL, url));
            cpdsRef.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_USER, user));
            cpdsRef.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_PASSWORD, password));

            try {
                context.rebind(name, cpdsRef);
            } catch (NamingException e) {
                String msg = "Error binding name '" + name + "' to " +
                        "the DriverAdapterCPDS reference";
                handleException(msg, e);
            }

            // Construct PerUserPoolDataSource reference
            Reference ref =
                    new Reference("org.apache.commons.dbcp.datasources.PerUserPoolDataSource",
                            "org.apache.commons.dbcp.datasources.PerUserPoolDataSourceFactory",
                            null);

            ref.add(new BinaryRefAddr(
                    DataSourceConfigurationConstants.PROP_JNDI_ENV,
                    MiscellaneousUtil.serialize(jndiEvn)));
            ref.add(new StringRefAddr(
                    DataSourceConfigurationConstants.PROP_DATA_SOURCE_NAME, name));
            ref.add(new StringRefAddr(
                    DataSourceConfigurationConstants.PROP_DEFAULTMAXACTIVE, maxActive));
            ref.add(new StringRefAddr(
                    DataSourceConfigurationConstants.PROP_DEFAULTMAXIDLE, maxIdle));
            ref.add(new StringRefAddr(
                    DataSourceConfigurationConstants.PROP_DEFAULTMAXWAIT, maxWait));

            //set default jndiProperties for reference
            setCommonParameters(ref, information);

            try {

                if (log.isDebugEnabled()) {
                    log.debug("Registering a DataSource with name : " +
                            dataSourceName + " in the JNDI tree with jndiProperties : " + jndiEvn);
                }

                context.rebind(dataSourceName, ref);
            } catch (NamingException e) {
                String msg = "Error binding name ' " + dataSourceName + " ' to " +
                        "the PerUserPoolDataSource reference";
                handleException(msg, e);
            }

        } else {
            handleException("Unsupported data source type : " + dsType);
        }
    }

    public void unRegister(String name) {

        InitialContext context = getCachedInitialContext(name);
        try {
            context.unbind(name);
        } catch (NamingException e) {
            handleException("Error removing a Datasource with name : " +
                    name + " from the JNDI context : " + initialContext, e);
        }
    }

    /**
     * Get a DatSource which has been registered in the JNDI tree
     *
     * @see DataSourceRepository#lookUp(String)
     */
    public DataSource lookUp(String dsName) {

        validateInitialized();
        validateDSName(dsName);
        if (log.isDebugEnabled()) {
            log.debug("Getting a DataSource with name : " + dsName + " from the JNDI tree.");
        }

        InitialContext context = getCachedInitialContext(dsName);
        return DataSourceFinder.find(dsName, context);
    }

    public void clear() {
        initialized = false;
        initialContext = null;
        jndiProperties.clear();
        perDataSourceICMap.clear();
    }

    private InitialContext getCachedInitialContext(String name) {
        InitialContext context = perDataSourceICMap.get(name);
        if (context == null) {
            validateInitialContext(initialContext);
            context = initialContext;
        }
        return context;
    }

    /**
     * Helper method to set all default parameter for naming reference of data source
     *
     * @param reference   The naming reference instance
     * @param information DataSourceInformation instance
     */
    private static void setCommonParameters(Reference reference, DataSourceInformation information) {

        reference.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_DEFAULTAUTOCOMMIT,
                String.valueOf(information.isDefaultAutoCommit())));
        reference.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_DEFAULTREADONLY,
                String.valueOf(information.isDefaultReadOnly())));
        reference.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_TESTONBORROW,
                String.valueOf(information.isTestOnBorrow())));
        reference.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_TESTONRETURN,
                String.valueOf(information.isTestOnReturn())));
        reference.add(new StringRefAddr(
                DataSourceConfigurationConstants.PROP_TIMEBETWEENEVICTIONRUNSMILLIS,
                String.valueOf(information.getTimeBetweenEvictionRunsMillis())));
        reference.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_NUMTESTSPEREVICTIONRUN,
                String.valueOf(information.getNumTestsPerEvictionRun())));
        reference.add(new StringRefAddr(
                DataSourceConfigurationConstants.PROP_MINEVICTABLEIDLETIMEMILLIS,
                String.valueOf(information.getMinEvictableIdleTimeMillis())));
        reference.add(new StringRefAddr(
                DataSourceConfigurationConstants.PROP_TESTWHILEIDLE,
                String.valueOf(information.isTestWhileIdle())));

        String validationQuery = information.getValidationQuery();

        if (validationQuery != null && !"".equals(validationQuery)) {
            reference.add(new StringRefAddr(
                    DataSourceConfigurationConstants.PROP_VALIDATIONQUERY, validationQuery));
        }
    }

    /**
     * Helper method to set all BasicDataSource specific parameter
     *
     * @param ref         The naming reference instance
     * @param information DataSourceInformation instance
     */
    private static void setBasicDataSourceParameters(Reference ref, DataSourceInformation information) {

        int defaultTransactionIsolation = information.getDefaultTransactionIsolation();
        String defaultCatalog = information.getDefaultCatalog();


        if (defaultTransactionIsolation != -1) {
            ref.add(new StringRefAddr(
                    DataSourceConfigurationConstants.PROP_DEFAULTTRANSACTIONISOLATION,
                    String.valueOf(defaultTransactionIsolation)));
        }

        ref.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_MINIDLE,
                String.valueOf(information.getMaxIdle())));
        ref.add(new StringRefAddr(
                DataSourceConfigurationConstants.PROP_ACCESSTOUNDERLYINGCONNECTIONALLOWED,
                String.valueOf(information.isAccessToUnderlyingConnectionAllowed())));
        ref.add(new StringRefAddr(
                DataSourceConfigurationConstants.PROP_REMOVEABANDONED,
                String.valueOf(information.isRemoveAbandoned())));
        ref.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_REMOVEABANDONEDTIMEOUT,
                String.valueOf(information.getRemoveAbandonedTimeout())));
        ref.add(new StringRefAddr(
                DataSourceConfigurationConstants.PROP_LOGABANDONED,
                String.valueOf(information.isLogAbandoned())));
        ref.add(new StringRefAddr(
                DataSourceConfigurationConstants.PROP_POOLPREPAREDSTATEMENTS,
                String.valueOf(information.isPoolPreparedStatements())));
        ref.add(new StringRefAddr(DataSourceConfigurationConstants.PROP_MAXOPENPREPAREDSTATEMENTS,
                String.valueOf(information.getMaxOpenPreparedStatements())));
        ref.add(new StringRefAddr(
                DataSourceConfigurationConstants.PROP_INITIALSIZE, String.valueOf(
                information.getInitialSize())));

        if (defaultCatalog != null && !"".equals(defaultCatalog)) {
            ref.add(new StringRefAddr
                    (DataSourceConfigurationConstants.PROP_DEFAULTCATALOG, defaultCatalog));
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
            for (String path1 : paths) {

                try {
                    assert context != null;
                    context = context.createSubcontext(path1);
                    if (context == null) {
                        handleException("sub context " + path1 + " could not be created");
                    }

                } catch (NamingException e) {
                    handleException("Unable to create sub context : " + path1, e);
                }
            }
        }
    }

    private static Properties createJNDIEnvironment(Properties dsProperties, String name) {

        String namingFactory = DataSourceConfigurationConstants.DEFAULT_IC_FACTORY;
        String providerUrl = null;
        int port = DataSourceConfigurationConstants.DEFAULT_PROVIDER_PORT;
        String providerPort = null;
        // setting naming provider
        Properties jndiEvn = new Properties();  //This is needed for PerUserPoolDatasource

        if (dsProperties != null && !dsProperties.isEmpty()) {

            if (log.isDebugEnabled()) {
                log.debug("Using properties " + dsProperties + " to create JNDI Environment");
            }

            StringBuffer buffer = new StringBuffer();
            buffer.append(DataSourceConfigurationConstants.PROP_SYNAPSE_DATASOURCES);
            buffer.append(DataSourceConfigurationConstants.DOT_STRING);
            if (name != null && !"".equals(name)) {
                buffer.append(name);
                buffer.append(DataSourceConfigurationConstants.DOT_STRING);
            }
            // The prefix for root level jndiProperties
            String rootPrefix = buffer.toString();


            namingFactory = MiscellaneousUtil.getProperty(
                    dsProperties, rootPrefix + DataSourceConfigurationConstants.PROP_ICFACTORY,
                    DataSourceConfigurationConstants.DEFAULT_IC_FACTORY);

            //Provider URL
            providerUrl = MiscellaneousUtil.getProperty(
                    dsProperties, rootPrefix + DataSourceConfigurationConstants.PROP_PROVIDER_URL, null);
            providerPort =
                    MiscellaneousUtil.getProperty(dsProperties, rootPrefix + DataSourceConfigurationConstants.PROP_PROVIDER_PORT,
                            String.valueOf(DataSourceConfigurationConstants.DEFAULT_PROVIDER_PORT));

        }

        jndiEvn.put(Context.INITIAL_CONTEXT_FACTORY, namingFactory);

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

            if (providerPort != null) {
                try {
                    port = Integer.parseInt(providerPort);
                } catch (NumberFormatException ignored) {
                }
            }

            // Create a RMI local registry
            RMIRegistryController.getInstance().createLocalRegistry(port);
            providerUrl = "rmi://" + providerHost + ":" + port;

        }

        jndiEvn.put(Context.PROVIDER_URL, providerUrl);

        log.info("DataSources will be registered in the JNDI context with provider PROP_URL : " +
                providerUrl);
        return jndiEvn;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     */
    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseUtilException(msg);
    }

    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     * @param e   The exception
     */
    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseUtilException(msg, e);
    }

    private void validateInitialized() {
        if (!isInitialized()) {
            handleException("Datasource registry has not been initialized yet");
        }
    }

    private void validateDSName(String dataSourceName) {
        if (dataSourceName == null || "".equals(dataSourceName)) {
            handleException("Invalid DataSource configuration !! -" +
                    "DataSource Name cannot be found ");
        }
    }

    private void validateInitialContext(InitialContext initialContext) {
        if (initialContext == null) {
            handleException("InitialContext cannot be found.");
        }
    }

    private InitialContext createInitialContext(Properties jndiEnv) {

        if (jndiEnv == null || jndiEnv.isEmpty()) {
            return null;
        }
        try {

            if (log.isDebugEnabled()) {
                log.debug("Initiating a Naming context with JNDI " +
                        "environment jndiProperties :  " + jndiEnv);
            }

            return new InitialContext(jndiEnv);

        } catch (NamingException e) {
            handleException("Error creating a InitialConext" +
                    " with JNDI env jndiProperties : " + jndiEnv);
        }
        return null;
    }

    private boolean isValid(Properties dsProperties) {

        String dataSources = MiscellaneousUtil.getProperty(dsProperties,
                DataSourceConfigurationConstants.PROP_SYNAPSE_DATASOURCES, null);

        if (dataSources != null && !"".equals(dataSources)) {
            String[] dataSourcesNames = dataSources.split(",");
            return !(dataSourcesNames == null || dataSourcesNames.length == 0);
        }

        return false;
    }
}
