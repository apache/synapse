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
package org.apache.synapse.commons.util.datasource;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.synapse.commons.util.secret.SecretCallback;
import org.apache.synapse.commons.util.secret.SecretCallbackHandler;
import org.apache.synapse.commons.util.secret.SecretLoadingModule;
import org.apache.synapse.commons.util.secret.SingleSecretCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Encapsulates the All information related to a DataSource
 */
public class DataSourceInformation {

    public static final String BASIC_DATA_SOURCE = "BasicDataSource";
    public static final String PER_USER_POOL_DATA_SOURCE = "PerUserPoolDataSource";
    private String user;
    private String aliasPassword;
    private String datasourceName;
    private int maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;
    private int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;
    private long maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;
    private String driver;
    private String url;
    private String type = BASIC_DATA_SOURCE;
    private boolean defaultAutoCommit = true;
    private boolean defaultReadOnly = false;
    private boolean testOnBorrow = true;
    private boolean testOnReturn = false;
    private int minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;
    private int initialSize;
    private int defaultTransactionIsolation = -1;
    private String defaultCatalog;
    private boolean accessToUnderlyingConnectionAllowed = false;
    private boolean removeAbandoned = false;
    private long removeAbandonedTimeout;
    private boolean logAbandoned = true;
    private boolean poolPreparedStatements = true;
    private int maxOpenPreparedStatements;
    private final Properties properties = new Properties();
    private String repositoryType = DataSourceConfigurationConstants.PROP_REGISTRY_MEMORY;
    private String alias;
    private SecretCallbackHandler passwordProvider;

    private long timeBetweenEvictionRunsMillis =
            GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

    private int numTestsPerEvictionRun =
            GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;

    private long minEvictableIdleTimeMillis =
            GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    private boolean testWhileIdle = false;
    private String validationQuery;

    private final Map<String, Object> parameters = new HashMap<String, Object>();


    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }


    public int getDefaultTransactionIsolation() {
        return defaultTransactionIsolation;
    }

    public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
        this.defaultTransactionIsolation = defaultTransactionIsolation;
    }

    public String getDefaultCatalog() {
        return defaultCatalog;
    }

    public void setDefaultCatalog(String defaultCatalog) {
        this.defaultCatalog = defaultCatalog;
    }

    public boolean isAccessToUnderlyingConnectionAllowed() {
        return accessToUnderlyingConnectionAllowed;
    }

    public void setAccessToUnderlyingConnectionAllowed(
            boolean accessToUnderlyingConnectionAllowed) {
        this.accessToUnderlyingConnectionAllowed = accessToUnderlyingConnectionAllowed;
    }

    public boolean isRemoveAbandoned() {
        return removeAbandoned;
    }

    public void setRemoveAbandoned(boolean removeAbandoned) {
        this.removeAbandoned = removeAbandoned;
    }

    public long getRemoveAbandonedTimeout() {
        return removeAbandonedTimeout;
    }

    public void setRemoveAbandonedTimeout(long removeAbandonedTimeout) {
        this.removeAbandonedTimeout = removeAbandonedTimeout;
    }

    public boolean isLogAbandoned() {
        return logAbandoned;
    }

    public void setLogAbandoned(boolean logAbandoned) {
        this.logAbandoned = logAbandoned;
    }

    public boolean isPoolPreparedStatements() {
        return poolPreparedStatements;
    }

    public void setPoolPreparedStatements(boolean poolPreparedStatements) {
        this.poolPreparedStatements = poolPreparedStatements;
    }

    public int getMaxOpenPreparedStatements() {
        return maxOpenPreparedStatements;
    }

    public void setMaxOpenPreparedStatements(int maxOpenPreparedStatements) {
        this.maxOpenPreparedStatements = maxOpenPreparedStatements;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAliasPassword() {
        return aliasPassword;
    }

    /**
     * Get actual password based on SecretCallbackHandler and alias password
     * If SecretCallbackHandler is null, then returns alias password
     * @return  Actual password
     */
    public String getResolvedPassword() {

        if (passwordProvider != null) {
            if (aliasPassword != null && !"".equals(aliasPassword)) {

                SecretLoadingModule secretLoadingModule = new SecretLoadingModule();
                secretLoadingModule.init(new SecretCallbackHandler[]{passwordProvider});
                SingleSecretCallback secretCallback =
                        new SingleSecretCallback(DataSourceConfigurationConstants.PROMPT,
                                aliasPassword);
                SecretCallback[] secretCallbacks = new SecretCallback[]{secretCallback};
                secretLoadingModule.load(secretCallbacks);
                return secretCallback.getSecret();
            }
        }
        return aliasPassword;
    }

    public void setAliasPassword(String aliasPassword) {
        this.aliasPassword = aliasPassword;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void addParameter(String name, Object value) {
        parameters.put(name, value);
    }

    public Object getParameter(String name) {
        return parameters.get(name);
    }

    public boolean isDefaultAutoCommit() {
        return defaultAutoCommit;
    }

    public void setDefaultAutoCommit(boolean defaultAutoCommit) {
        this.defaultAutoCommit = defaultAutoCommit;
    }

    public boolean isDefaultReadOnly() {
        return defaultReadOnly;
    }

    public void setDefaultReadOnly(boolean defaultReadOnly) {
        this.defaultReadOnly = defaultReadOnly;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    public int getNumTestsPerEvictionRun() {
        return numTestsPerEvictionRun;
    }

    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
    }

    public long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    public boolean isTestWhileIdle() {
        return testWhileIdle;
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties.putAll(properties);
    }

    public void addProperty(String name, String value) {
        if (name != null && value != null && !"".equals(name) && !"".equals(value)) {
            this.properties.put(name, value);
        }
    }

    public String getRepositoryType() {
        return repositoryType;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Map<String, Object> getAllParameters() {
        return this.parameters;
    }

    public SecretCallbackHandler getPasswordProvider() {
        return passwordProvider;
    }

    public void setPasswordProvider(SecretCallbackHandler passwordProvider) {
        this.passwordProvider = passwordProvider;
    }
}
