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
package org.apache.synapse.commons.util.datasource.factory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.synapse.commons.util.MiscellaneousUtil;
import org.apache.synapse.commons.util.SynapseUtilException;
import org.apache.synapse.commons.util.datasource.DataSourceConfigurationConstants;
import org.apache.synapse.commons.util.datasource.DataSourceInformation;

import java.util.Properties;

/**
 * Factory to create a DataSourceInformation based on given properties
 */

public class DataSourceInformationFactory {

    private static final Log log = LogFactory.getLog(DataSourceInformationFactory.class);


    private DataSourceInformationFactory() {
    }

    /**
     * Factory method to create a DataSourceInformation instance based on given properties
     *
     * @param dsName     DataSource Name
     * @param properties Properties to create and configure DataSource
     * @return DataSourceInformation instance
     */
    public static DataSourceInformation createDataSourceInformation(String dsName, Properties properties) {

        if (dsName == null || "".equals(dsName)) {
            if (log.isDebugEnabled()) {
                log.debug("DataSource name is either empty or null, ignoring..");
            }
            return null;
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append(DataSourceConfigurationConstants.PROP_SYNAPSE_DATASOURCES);
        buffer.append(DataSourceConfigurationConstants.DOT_STRING);
        buffer.append(dsName);
        buffer.append(DataSourceConfigurationConstants.DOT_STRING);

        // Prefix for getting particular data source's properties
        String prefix = buffer.toString();

        String driver = MiscellaneousUtil.getProperty(
                properties, prefix + DataSourceConfigurationConstants.PROP_DRIVER_CLS_NAME, null);
        if (driver == null) {
            handleException(prefix + DataSourceConfigurationConstants.PROP_DRIVER_CLS_NAME + " cannot be found.");
        }

        String url = MiscellaneousUtil.getProperty(properties, 
                prefix + DataSourceConfigurationConstants.PROP_URL, null);
        if (url == null) {
            handleException(prefix + DataSourceConfigurationConstants.PROP_URL + " cannot be found.");
        }

        DataSourceInformation information = new DataSourceInformation();
        information.setAlias(dsName);

        information.setDriver(driver);
        information.setUrl(url);

        // get other required properties
        String user = (String) MiscellaneousUtil.getProperty(
                properties, prefix + DataSourceConfigurationConstants.PROP_USER_NAME, null, String.class);
        if (user != null && !"".equals(user)) {
            information.setUser(user);
        }

        String password = (String) MiscellaneousUtil.getProperty(
                properties, prefix + DataSourceConfigurationConstants.PROP_PASSWORD, null, String.class);

        if (password != null && !"".equals(password)) {
            information.setPassword(password);
        }

        String dataSourceName = (String) MiscellaneousUtil.getProperty(
                properties, prefix + DataSourceConfigurationConstants.PROP_DSNAME, dsName, String.class);
        information.setDatasourceName(dataSourceName);

        String dsType = (String) MiscellaneousUtil.getProperty(
                properties, prefix + DataSourceConfigurationConstants.PROP_TYPE,
                DataSourceConfigurationConstants.PROP_BASIC_DATA_SOURCE, String.class);

        information.setType(dsType);

        String repositoryType = (String) MiscellaneousUtil.getProperty(
                properties, prefix + DataSourceConfigurationConstants.PROP_REGISTRY,
                DataSourceConfigurationConstants.PROP_REGISTRY_MEMORY, String.class);

        information.setRepositoryType(repositoryType);

        Integer maxActive = (Integer) MiscellaneousUtil.getProperty(
                properties, prefix + DataSourceConfigurationConstants.PROP_MAXACTIVE,
                GenericObjectPool.DEFAULT_MAX_ACTIVE, Integer.class);
        information.setMaxActive(maxActive);

        Integer maxIdle = (Integer) MiscellaneousUtil.getProperty(
                properties, prefix + DataSourceConfigurationConstants.PROP_MAXIDLE,
                GenericObjectPool.DEFAULT_MAX_IDLE, Integer.class);
        information.setMaxIdle(maxIdle);

        Long maxWait = (Long) MiscellaneousUtil.getProperty(
                properties, prefix + DataSourceConfigurationConstants.PROP_MAXWAIT,
                GenericObjectPool.DEFAULT_MAX_WAIT, Long.class);

        information.setMaxWait(maxWait);

        // Construct DriverAdapterCPDS reference
        String suffix = DataSourceConfigurationConstants.PROP_CPDSADAPTER +
                DataSourceConfigurationConstants.DOT_STRING + DataSourceConfigurationConstants.PROP_CLASS_NAME;
        String className = MiscellaneousUtil.getProperty(properties, prefix + suffix,
                DataSourceConfigurationConstants.PROP_CPDSADAPTER_DRIVER);
        information.addParameter(suffix, className);
        suffix = DataSourceConfigurationConstants.PROP_CPDSADAPTER +
                DataSourceConfigurationConstants.DOT_STRING + DataSourceConfigurationConstants.PROP_FACTORY;
        String factory = MiscellaneousUtil.getProperty(properties, prefix + suffix,
                DataSourceConfigurationConstants.PROP_CPDSADAPTER_DRIVER);
        information.addParameter(suffix, factory);
        suffix = DataSourceConfigurationConstants.PROP_CPDSADAPTER +
                DataSourceConfigurationConstants.DOT_STRING + DataSourceConfigurationConstants.PROP_NAME;
        String name = MiscellaneousUtil.getProperty(properties, prefix + suffix,
                "cpds");
        information.addParameter(suffix, name);

        boolean defaultAutoCommit = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_DEFAULTAUTOCOMMIT, true, Boolean.class);

        boolean defaultReadOnly = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_DEFAULTREADONLY, false, Boolean.class);

        boolean testOnBorrow = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_TESTONBORROW, true, Boolean.class);

        boolean testOnReturn = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_TESTONRETURN, false, Boolean.class);

        long timeBetweenEvictionRunsMillis = (Long) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_TIMEBETWEENEVICTIONRUNSMILLIS,
                GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS, Long.class);

        int numTestsPerEvictionRun = (Integer) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_NUMTESTSPEREVICTIONRUN,
                GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN, Integer.class);

        long minEvictableIdleTimeMillis = (Long) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_MINEVICTABLEIDLETIMEMILLIS,
                GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS, Long.class);

        boolean testWhileIdle = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_TESTWHILEIDLE, false, Boolean.class);

        String validationQuery = MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_VALIDATIONQUERY, null);

        int minIdle = (Integer) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_MINIDLE, GenericObjectPool.DEFAULT_MIN_IDLE,
                Integer.class);

        int initialSize = (Integer) MiscellaneousUtil.getProperty(
                properties, prefix + DataSourceConfigurationConstants.PROP_INITIALSIZE, 0, Integer.class);

        int defaultTransactionIsolation = (Integer) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_DEFAULTTRANSACTIONISOLATION, -1, Integer.class);

        String defaultCatalog = MiscellaneousUtil.getProperty(
                properties, prefix + DataSourceConfigurationConstants.PROP_DEFAULTCATALOG, null);

        boolean accessToUnderlyingConnectionAllowed =
                (Boolean) MiscellaneousUtil.getProperty(properties,
                        prefix + DataSourceConfigurationConstants.PROP_ACCESSTOUNDERLYINGCONNECTIONALLOWED,
                        false, Boolean.class);

        boolean removeAbandoned = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_REMOVEABANDONED, false, Boolean.class);

        int removeAbandonedTimeout = (Integer) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_REMOVEABANDONEDTIMEOUT, 300, Integer.class);

        boolean logAbandoned = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_LOGABANDONED, false, Boolean.class);

        boolean poolPreparedStatements = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_POOLPREPAREDSTATEMENTS, false, Boolean.class);

        int maxOpenPreparedStatements = (Integer) MiscellaneousUtil.getProperty(properties,
                prefix + DataSourceConfigurationConstants.PROP_MAXOPENPREPAREDSTATEMENTS,
                GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, Integer.class);

        information.setDefaultAutoCommit(defaultAutoCommit);
        information.setDefaultReadOnly(defaultReadOnly);
        information.setTestOnBorrow(testOnBorrow);
        information.setTestOnReturn(testOnReturn);
        information.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        information.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        information.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        information.setTestWhileIdle(testWhileIdle);
        information.setMinIdle(minIdle);
        information.setDefaultTransactionIsolation(defaultTransactionIsolation);
        information.setAccessToUnderlyingConnectionAllowed(accessToUnderlyingConnectionAllowed);
        information.setRemoveAbandoned(removeAbandoned);
        information.setRemoveAbandonedTimeout(removeAbandonedTimeout);
        information.setLogAbandoned(logAbandoned);
        information.setPoolPreparedStatements(poolPreparedStatements);
        information.setMaxOpenPreparedStatements(maxOpenPreparedStatements);
        information.setInitialSize(initialSize);

        if (validationQuery != null && !"".equals(validationQuery)) {
            information.setValidationQuery(validationQuery);
        }

        if (defaultCatalog != null && !"".equals(defaultCatalog)) {
            information.setDefaultCatalog(defaultCatalog);
        }

        information.addProperty(prefix + DataSourceConfigurationConstants.PROP_ICFACTORY,
                MiscellaneousUtil.getProperty(
                        properties, prefix + DataSourceConfigurationConstants.PROP_ICFACTORY,
                        null));
        //Provider URL
        information.addProperty(prefix + DataSourceConfigurationConstants.PROP_PROVIDER_URL,
                MiscellaneousUtil.getProperty(
                        properties, prefix + DataSourceConfigurationConstants.PROP_PROVIDER_URL, null));

        information.addProperty(prefix + DataSourceConfigurationConstants.PROP_PROVIDER_PORT,
                MiscellaneousUtil.getProperty(
                        properties, prefix + DataSourceConfigurationConstants.PROP_PROVIDER_PORT, null));
        
        return information;
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
}
