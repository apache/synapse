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
package org.apache.synapse.util.datasource.factory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.util.MiscellaneousUtil;
import org.apache.synapse.util.datasource.DataSourceInformation;

import java.util.Properties;

/**
 * Factory to create a DataSourceInformation based on given properties
 */

public class DataSourceInformationFactory {

    private static final Log log = LogFactory.getLog(DataSourceInformationFactory.class);

    public static final String PROP_ICFACTORY = "icFactory";
    public static final String PROP_PROVIDER_URL = "providerUrl";
    public static final String PROP_PROVIDER_PORT = "providerPort";
    public static final String DOT_STRING = ".";
    public static final String PROP_USER_NAME = "username";
    public static final String PROP_PASSWORD = "password";
    public static final String PROP_DRIVER_CLS_NAME = "driverClassName";
    public static final String PROP_DSNAME = "dsName";
    public static final String PROP_URL = "url";
    public static final String PROP_DRIVER = "driver";
    public static final String PROP_USER = "user";

    public static final String PROP_CPDSADAPTER = "cpdsadapter";
    public static final String PROP_JNDI_ENV = "jndiEnvironment";
    public static final String PROP_DEFAULTMAXACTIVE = "defaultMaxActive";
    public static final String PROP_DEFAULTMAXIDLE = "defaultMaxIdle";
    public static final String PROP_DEFAULTMAXWAIT = "defaultMaxWait";
    public static final String PROP_DATA_SOURCE_NAME = "dataSourceName";
    public static final String PROP_CPDS_CLASS_NAME = "className";
    public static final String PROP_CPDS_FACTORY = "factory";
    public static final String PROP_CPDS_NAME = "name";

    public final static String PROP_DEFAULTAUTOCOMMIT = "defaultAutoCommit";
    public final static String PROP_DEFAULTREADONLY = "defaultReadOnly";
    public final static String PROP_TESTONBORROW = "testOnBorrow";
    public final static String PROP_TESTONRETURN = "testOnReturn";
    public final static String PROP_TIMEBETWEENEVICTIONRUNSMILLIS =
            "timeBetweenEvictionRunsMillis";
    public final static String PROP_NUMTESTSPEREVICTIONRUN = "numTestsPerEvictionRun";
    public final static String PROP_MINEVICTABLEIDLETIMEMILLIS = "minEvictableIdleTimeMillis";
    public final static String PROP_TESTWHILEIDLE = "testWhileIdle";
    public final static String PROP_VALIDATIONQUERY = "validationQuery";
    public final static String PROP_MAXACTIVE = "maxActive";
    public final static String PROP_MAXIDLE = "maxIdle";
    public final static String PROP_MAXWAIT = "maxWait";

    public final static String PROP_MINIDLE = "minIdle";
    public final static String PROP_INITIALSIZE = "initialSize";
    public final static String PROP_DEFAULTTRANSACTIONISOLATION = "defaultTransactionIsolation";
    public final static String PROP_DEFAULTCATALOG = "defaultCatalog";
    public final static String PROP_ACCESSTOUNDERLYINGCONNECTIONALLOWED =
            "accessToUnderlyingConnectionAllowed";
    public final static String PROP_REMOVEABANDONED = "removeAbandoned";
    public final static String PROP_REMOVEABANDONEDTIMEOUT = "removeAbandonedTimeout";
    public final static String PROP_LOGABANDONED = "logAbandoned";
    public final static String PROP_POOLPREPAREDSTATEMENTS = "poolPreparedStatements";
    public final static String PROP_MAXOPENPREPAREDSTATEMENTS = "maxOpenPreparedStatements";
    public final static String PROP_CONNECTIONPROPERTIES = "connectionProperties";

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
        buffer.append(SynapseConstants.SYNAPSE_DATASOURCES);
        buffer.append(DOT_STRING);
        buffer.append(dsName);
        buffer.append(DOT_STRING);

        // Prefix for getting particular data source's properties
        String prefix = buffer.toString();

        String driver = MiscellaneousUtil.getProperty(
                properties, prefix + PROP_DRIVER_CLS_NAME, null);
        if (driver == null) {
            handleException(prefix + PROP_DRIVER_CLS_NAME + " cannot be found.");
        }

        String url = MiscellaneousUtil.getProperty(properties, prefix + PROP_URL, null);
        if (url == null) {
            handleException(prefix + PROP_URL + " cannot be found.");
        }

        DataSourceInformation information = new DataSourceInformation();

        information.setDriver(driver);
        information.setUrl(url);

        // get other required properties
        String user = (String) MiscellaneousUtil.getProperty(
                properties, prefix + PROP_USER_NAME, "synapse", String.class);
        information.setUser(user);

        String password = (String) MiscellaneousUtil.getProperty(
                properties, prefix + PROP_PASSWORD, "synapse", String.class);

        information.setPassword(password);

        String dataSourceName = (String) MiscellaneousUtil.getProperty(
                properties, prefix + PROP_DSNAME, dsName, String.class);
        information.setName(dataSourceName);

        String dsType = (String) MiscellaneousUtil.getProperty(
                properties, prefix + "type", "BasicDataSource", String.class);

        information.setType(dsType);

        Integer maxActive = (Integer) MiscellaneousUtil.getProperty(
                properties, prefix + PROP_MAXACTIVE,
                GenericObjectPool.DEFAULT_MAX_ACTIVE, Integer.class);
        information.setMaxActive(maxActive);

        Integer maxIdle = (Integer) MiscellaneousUtil.getProperty(
                properties, prefix + PROP_MAXIDLE,
                GenericObjectPool.DEFAULT_MAX_IDLE, Integer.class);
        information.setMaxIdle(maxIdle);

        Long maxWait = (Long) MiscellaneousUtil.getProperty(
                properties, prefix + PROP_MAXWAIT,
                GenericObjectPool.DEFAULT_MAX_WAIT, Long.class);

        information.setMaxWait(maxWait);

        // Construct DriverAdapterCPDS reference
        String suffix = PROP_CPDSADAPTER +
                DOT_STRING + "className";
        String className = MiscellaneousUtil.getProperty(properties, prefix + suffix,
                "org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS");
        information.addParameter(suffix, className);
        suffix = PROP_CPDSADAPTER +
                DOT_STRING + "factory";
        String factory = MiscellaneousUtil.getProperty(properties, prefix + suffix,
                "org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS");
        information.addParameter(suffix, factory);
        suffix = PROP_CPDSADAPTER +
                DOT_STRING + "name";
        String name = MiscellaneousUtil.getProperty(properties, prefix + suffix,
                "cpds");
        information.addParameter(suffix, name);

        boolean defaultAutoCommit = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_DEFAULTAUTOCOMMIT, true, Boolean.class);

        boolean defaultReadOnly = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_DEFAULTREADONLY, false, Boolean.class);

        boolean testOnBorrow = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_TESTONBORROW, true, Boolean.class);

        boolean testOnReturn = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_TESTONRETURN, false, Boolean.class);

        long timeBetweenEvictionRunsMillis = (Long) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_TIMEBETWEENEVICTIONRUNSMILLIS,
                GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS, Long.class);

        int numTestsPerEvictionRun = (Integer) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_NUMTESTSPEREVICTIONRUN,
                GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN, Integer.class);

        long minEvictableIdleTimeMillis = (Long) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_MINEVICTABLEIDLETIMEMILLIS,
                GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS, Long.class);

        boolean testWhileIdle = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_TESTWHILEIDLE, false, Boolean.class);

        String validationQuery = MiscellaneousUtil.getProperty(properties,
                prefix + PROP_VALIDATIONQUERY, null);

        int minIdle = (Integer) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_MINIDLE, GenericObjectPool.DEFAULT_MIN_IDLE, Integer.class);

        int initialSize = (Integer) MiscellaneousUtil.getProperty(
                properties, prefix + PROP_INITIALSIZE, 0, Integer.class);

        int defaultTransactionIsolation = (Integer) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_DEFAULTTRANSACTIONISOLATION, -1, Integer.class);

        String defaultCatalog = MiscellaneousUtil.getProperty(
                properties, prefix + PROP_DEFAULTCATALOG, null);

        boolean accessToUnderlyingConnectionAllowed =
                (Boolean) MiscellaneousUtil.getProperty(properties,
                        prefix + PROP_ACCESSTOUNDERLYINGCONNECTIONALLOWED, false, Boolean.class);

        boolean removeAbandoned = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_REMOVEABANDONED, false, Boolean.class);

        int removeAbandonedTimeout = (Integer) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_REMOVEABANDONEDTIMEOUT, 300, Integer.class);

        boolean logAbandoned = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_LOGABANDONED, false, Boolean.class);

        boolean poolPreparedStatements = (Boolean) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_POOLPREPAREDSTATEMENTS, false, Boolean.class);

        int maxOpenPreparedStatements = (Integer) MiscellaneousUtil.getProperty(properties,
                prefix + PROP_MAXOPENPREPAREDSTATEMENTS,
                GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, Integer.class);

        information.setDefaultAutoCommit(defaultAutoCommit);
        information.setDefaultReadOnly(defaultReadOnly);
        information.setTestOnBorrow(testOnBorrow);
        information.setTestOnReturn(testOnReturn);
        information.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        information.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        information.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        information.setTestWhileIdle(testWhileIdle);
        information.setValidationQuery(validationQuery);
        information.setMinIdle(minIdle);
        information.setDefaultTransactionIsolation(defaultTransactionIsolation);
        information.setAccessToUnderlyingConnectionAllowed(accessToUnderlyingConnectionAllowed);
        information.setRemoveAbandoned(removeAbandoned);
        information.setRemoveAbandonedTimeout(removeAbandonedTimeout);
        information.setLogAbandoned(logAbandoned);
        information.setPoolPreparedStatements(poolPreparedStatements);
        information.setMaxOpenPreparedStatements(maxOpenPreparedStatements);
        information.setInitialSize(initialSize);
        information.setDefaultCatalog(defaultCatalog);

        return information;
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
}
