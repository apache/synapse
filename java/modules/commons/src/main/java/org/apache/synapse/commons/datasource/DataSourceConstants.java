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
package org.apache.synapse.commons.datasource;

/**
 * 
 */
public class DataSourceConstants {

    public static final String PROP_SYNAPSE_PREFIX_DS = "synapse.datasources";
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
    public static final String PROP_PROVIDER_PORT = "providerPort";
    public final static String PROP_REGISTRY = "registry";
    public final static String PROP_REGISTRY_MEMORY = "memory";
    public final static String PROP_REGISTRY_JNDI = "JNDI";
    public static final String PROP_ICFACTORY = "icFactory";
    public static final String PROP_PROVIDER_URL = "providerUrl";
    public static final String DOT_STRING = ".";
    public static final String COMMA_STRING = ",";
    public static final String PROP_TYPE = "type";
    public static final String PROP_BASIC_DATA_SOURCE = "BasicDataSource";
    public static final String PROP_CLASS_NAME = "className";
    public static final String PROP_CPDSADAPTER_DRIVER
            = "org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS";
    public static final String PROP_FACTORY = "factory";
    public static final String PROP_NAME = "name";
    public static final String DATASOURCE_INFORMATION_REPOSITORY =
            "DataSourceInformationRepository";
    public static final String DEFAULT_IC_FACTORY =
            "com.sun.jndi.rmi.registry.RegistryContextFactory";
    public static final int DEFAULT_PROVIDER_PORT = 2199;

}
