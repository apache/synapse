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
package org.apache.synapse.util.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.util.MiscellaneousUtil;
import org.apache.synapse.util.datasource.factory.DataSourceInformationFactory;

import javax.naming.*;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * Keep all DataSources in the JNDI Tree
 */
public class JNDIBasedDataSourceRegistry implements DataSourceRegistry {

    private static Log log = LogFactory.getLog(JNDIBasedDataSourceRegistry.class);

    private static final JNDIBasedDataSourceRegistry ourInstance =
            new JNDIBasedDataSourceRegistry();
    private static InitialContext initialContext;
    private static final Properties indiEnv = new Properties();
    private static boolean initialize = false;

    public static JNDIBasedDataSourceRegistry getInstance(Properties jndiEnv) {

        if (!initialize) {

            if (jndiEnv == null) {
                handleException("JNDI environment properties cannot be found");
            }

            indiEnv.putAll(jndiEnv);

            try {

                if (log.isDebugEnabled()) {
                    log.debug("Initilating a Naming conext with JNDI " +
                            "environment properties :  " + jndiEnv);
                }

                initialContext = new InitialContext(jndiEnv);
                initialize = true;

            } catch (NamingException e) {
                handleException("Error creating a InitialConext" +
                        " with JNDI env properties : " + jndiEnv);
            }
        }
        return ourInstance;
    }

    private JNDIBasedDataSourceRegistry() {
    }

    /**
     * Register a DataSource in the JNDI tree
     *
     * @see org.apache.synapse.util.datasource.DataSourceRegistry#register(DataSourceInformation)
     */
    public void register(DataSourceInformation information) {

        String dsType = information.getType();
        String driver = information.getDriver();
        String url = information.getUrl();
        String user = information.getUser();
        String password = information.getPassword();
        String maxActive = String.valueOf(information.getMaxActive());
        String maxIdle = String.valueOf(information.getMaxIdle());
        String maxWait = String.valueOf(information.getMaxWait());
        String dataSourceName = information.getName();

        if (dataSourceName == null || "".equals(dataSourceName)) {
            handleException("Invalid DataSource configuration !! -" +
                    "DataSource Name cannot be found ");
        }

        //populates context tree
        populateContextTree(initialContext, dataSourceName);

        if (DataSourceInformation.BASIC_DATA_SOURCE.equals(dsType)) {

            Reference ref = new Reference("javax.sql.DataSource",
                    "org.apache.commons.dbcp.BasicDataSourceFactory", null);

            ref.add(new StringRefAddr(DataSourceInformationFactory.PROP_DRIVER_CLS_NAME, driver));
            ref.add(new StringRefAddr(DataSourceInformationFactory.PROP_URL, url));
            ref.add(new StringRefAddr(DataSourceInformationFactory.PROP_USER_NAME, user));
            ref.add(new StringRefAddr(DataSourceInformationFactory.PROP_PASSWORD, password));
            ref.add(new StringRefAddr(DataSourceInformationFactory.PROP_MAXACTIVE, maxActive));
            ref.add(new StringRefAddr(DataSourceInformationFactory.PROP_MAXIDLE, maxIdle));
            ref.add(new StringRefAddr(DataSourceInformationFactory.PROP_MAXWAIT, maxWait));

            // set BasicDataSource specific parameters
            setBasicDataSourceParameters(ref, information);
            //set default properties for reference
            setCommonParameters(ref, information);

            try {

                if (log.isDebugEnabled()) {
                    log.debug("Registering a DataSource with name : " +
                            dataSourceName + " in the JNDI tree with properties : " + indiEnv);
                }

                initialContext.rebind(dataSourceName, ref);
            } catch (NamingException e) {
                String msg = " Error binding name ' " + dataSourceName + " ' to " +
                        "the DataSource(BasicDataSource) reference";
                handleException(msg, e);
            }

        } else if (DataSourceInformation.PER_USER_POOL_DATA_SOURCE.equals(dsType)) {

            // Construct DriverAdapterCPDS reference
            String className = (String) information.getParameter(
                    DataSourceInformationFactory.PROP_CPDSADAPTER +
                            DataSourceInformationFactory.DOT_STRING +
                            DataSourceInformationFactory.PROP_CPDS_CLASS_NAME);
            String factory = (String) information.getParameter(
                    DataSourceInformationFactory.PROP_CPDSADAPTER +
                            DataSourceInformationFactory.DOT_STRING +
                            DataSourceInformationFactory.PROP_CPDS_FACTORY);
            String name = (String) information.getParameter(
                    DataSourceInformationFactory.PROP_CPDSADAPTER +
                            DataSourceInformationFactory.DOT_STRING +
                            DataSourceInformationFactory.PROP_CPDS_NAME);

            Reference cpdsRef =
                    new Reference(className, factory, null);

            cpdsRef.add(new StringRefAddr(DataSourceInformationFactory.PROP_DRIVER, driver));
            cpdsRef.add(new StringRefAddr(DataSourceInformationFactory.PROP_URL, url));
            cpdsRef.add(new StringRefAddr(DataSourceInformationFactory.PROP_USER, user));
            cpdsRef.add(new StringRefAddr(DataSourceInformationFactory.PROP_PASSWORD, password));

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
                            "org.apache.commons.dbcp.datasources.PerUserPoolDataSourceFactory",
                            null);

            ref.add(new BinaryRefAddr(
                    DataSourceInformationFactory.PROP_JNDI_ENV,
                    MiscellaneousUtil.serialize(indiEnv)));
            ref.add(new StringRefAddr(
                    DataSourceInformationFactory.PROP_DATA_SOURCE_NAME, name));
            ref.add(new StringRefAddr(
                    DataSourceInformationFactory.PROP_DEFAULTMAXACTIVE, maxActive));
            ref.add(new StringRefAddr(
                    DataSourceInformationFactory.PROP_DEFAULTMAXIDLE, maxIdle));
            ref.add(new StringRefAddr(
                    DataSourceInformationFactory.PROP_DEFAULTMAXWAIT, maxWait));

            //set default properties for reference
            setCommonParameters(ref, information);

            try {

                if (log.isDebugEnabled()) {
                    log.debug("Registering a DataSource with name : " +
                            dataSourceName + " in the JNDI tree with properties : " + indiEnv);
                }

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
     * Get a DatSource which has been registered in the JNDI tree
     *
     * @see org.apache.synapse.util.datasource.DataSourceRegistry#lookUp(String)
     */
    public DataSource lookUp(String dsName) {

        if (log.isDebugEnabled()) {
            log.debug("Getting a DataSource with name : " + dsName + " from the JNDI tree.");
        }
        return DataSourceFinder.find(dsName, initialContext);
    }

    /**
     * Helper method to set all default parameter for naming reference of data source
     *
     * @param reference   The naming reference instance
     * @param information DataSourceInformation instance
     */
    private static void setCommonParameters(Reference reference, DataSourceInformation information) {

        reference.add(new StringRefAddr(DataSourceInformationFactory.PROP_DEFAULTAUTOCOMMIT,
                String.valueOf(information.isDefaultAutoCommit())));
        reference.add(new StringRefAddr(DataSourceInformationFactory.PROP_DEFAULTREADONLY,
                String.valueOf(information.isDefaultReadOnly())));
        reference.add(new StringRefAddr(DataSourceInformationFactory.PROP_TESTONBORROW,
                String.valueOf(information.isTestOnBorrow())));
        reference.add(new StringRefAddr(DataSourceInformationFactory.PROP_TESTONRETURN,
                String.valueOf(information.isTestOnReturn())));
        reference.add(new StringRefAddr(
                DataSourceInformationFactory.PROP_TIMEBETWEENEVICTIONRUNSMILLIS,
                String.valueOf(information.getTimeBetweenEvictionRunsMillis())));
        reference.add(new StringRefAddr(DataSourceInformationFactory.PROP_NUMTESTSPEREVICTIONRUN,
                String.valueOf(information.getNumTestsPerEvictionRun())));
        reference.add(new StringRefAddr(
                DataSourceInformationFactory.PROP_MINEVICTABLEIDLETIMEMILLIS,
                String.valueOf(information.getMinEvictableIdleTimeMillis())));
        reference.add(new StringRefAddr(
                DataSourceInformationFactory.PROP_TESTWHILEIDLE,
                String.valueOf(information.isTestWhileIdle())));

        String validationQuery = information.getValidationQuery();

        if (validationQuery != null && !"".equals(validationQuery)) {
            reference.add(new StringRefAddr(
                    DataSourceInformationFactory.PROP_VALIDATIONQUERY, validationQuery));
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
            ref.add(
                    new StringRefAddr(
                            DataSourceInformationFactory.PROP_DEFAULTTRANSACTIONISOLATION,
                            String.valueOf(defaultTransactionIsolation)));
        }

        ref.add(
                new StringRefAddr(DataSourceInformationFactory.PROP_MINIDLE,
                        String.valueOf(information.getMaxIdle())));
        ref.add(
                new StringRefAddr(
                        DataSourceInformationFactory.PROP_ACCESSTOUNDERLYINGCONNECTIONALLOWED,
                        String.valueOf(information.isAccessToUnderlyingConnectionAllowed())));
        ref.add(
                new StringRefAddr(
                        DataSourceInformationFactory.PROP_REMOVEABANDONED,
                        String.valueOf(information.isRemoveAbandoned())));
        ref.add
                (new StringRefAddr(DataSourceInformationFactory.PROP_REMOVEABANDONEDTIMEOUT,
                        String.valueOf(information.getRemoveAbandonedTimeout())));
        ref.add
                (new StringRefAddr(
                        DataSourceInformationFactory.PROP_LOGABANDONED,
                        String.valueOf(information.isLogAbandoned())));
        ref.add(
                new StringRefAddr(
                        DataSourceInformationFactory.PROP_POOLPREPAREDSTATEMENTS,
                        String.valueOf(information.isPoolPreparedStatements())));
        ref.add(
                new StringRefAddr(DataSourceInformationFactory.PROP_MAXOPENPREPAREDSTATEMENTS,
                        String.valueOf(information.getMaxOpenPreparedStatements())));
        ref.add(
                new StringRefAddr(
                        DataSourceInformationFactory.PROP_INITIALSIZE, String.valueOf(
                        information.getInitialSize())));

        if (defaultCatalog != null && !"".equals(defaultCatalog)) {
            ref.add(new StringRefAddr
                    (DataSourceInformationFactory.PROP_DEFAULTCATALOG, defaultCatalog));
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
}
