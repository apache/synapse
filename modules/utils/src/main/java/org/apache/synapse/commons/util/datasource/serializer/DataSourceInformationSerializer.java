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
package org.apache.synapse.commons.util.datasource.serializer;

import org.apache.synapse.commons.util.datasource.DataSourceConstants;
import org.apache.synapse.commons.util.datasource.DataSourceInformation;
import org.apache.synapse.commons.util.secret.SecretConfigurationConstants;

import java.util.Properties;

/**
 * Serialize  a  DataSourceInformation to a Properties
 */
public class DataSourceInformationSerializer {

    /**
     * Serialize  a  DataSourceInformation to a Properties
     *
     * @param information DataSourceInformation instance
     * @return DataSource configuration properties
     */
    public static Properties serialize(DataSourceInformation information) {

        final Properties properties = new Properties();

        String alias = information.getAlias();
        StringBuffer buffer = new StringBuffer();
        buffer.append(DataSourceConstants.PROP_SYNAPSE_PREFIX_DS);
        buffer.append(DataSourceConstants.DOT_STRING);
        buffer.append(alias);
        buffer.append(DataSourceConstants.DOT_STRING);

        // Prefix for getting particular data source's properties
        String prefix = buffer.toString();
        addProperty(properties, prefix + DataSourceConstants.PROP_DSNAME,
                information.getDatasourceName());
        addProperty(properties, prefix + SecretConfigurationConstants.PROP_USER_NAME,
                information.getSecretInformation().getUser());
        addProperty(properties, prefix + SecretConfigurationConstants.PROP_PASSWORD,
                information.getSecretInformation().getAliasPassword());
        addProperty(properties, prefix + DataSourceConstants.PROP_MAXACTIVE,
                String.valueOf(information.getMaxActive()));
        addProperty(properties, prefix + DataSourceConstants.PROP_MAXIDLE,
                String.valueOf(information.getMaxIdle()));

        addProperty(properties, prefix + DataSourceConstants.PROP_MAXWAIT,
                String.valueOf(information.getMaxWait()));

        addProperty(properties, prefix + DataSourceConstants.PROP_DRIVER_CLS_NAME,
                String.valueOf(information.getDriver()));

        addProperty(properties, prefix + DataSourceConstants.PROP_URL,
                String.valueOf(information.getUrl()));

        addProperty(properties, prefix + DataSourceConstants.PROP_TYPE,
                String.valueOf(information.getType()));

        addProperty(properties, prefix + DataSourceConstants.PROP_DEFAULTAUTOCOMMIT,
                String.valueOf(information.isDefaultAutoCommit()));

        addProperty(properties, prefix + DataSourceConstants.PROP_DEFAULTREADONLY,
                String.valueOf(information.isDefaultReadOnly()));

        addProperty(properties, prefix + DataSourceConstants.PROP_TESTONBORROW,
                String.valueOf(information.isTestOnBorrow()));

        addProperty(properties, prefix + DataSourceConstants.PROP_TESTONRETURN,
                String.valueOf(information.isTestOnReturn()));

        addProperty(properties, prefix + DataSourceConstants.PROP_MINIDLE,
                String.valueOf(information.getMinIdle()));

        addProperty(properties, prefix + DataSourceConstants.PROP_INITIALSIZE,
                String.valueOf(information.getInitialSize()));

        addProperty(properties, prefix +
                DataSourceConstants.PROP_DEFAULTTRANSACTIONISOLATION,
                String.valueOf(information.getDefaultTransactionIsolation()));

        String defaultCatalog = information.getDefaultCatalog();
        if (defaultCatalog != null && !"".equals(defaultCatalog)) {
            addProperty(properties, prefix + DataSourceConstants.PROP_DEFAULTCATALOG,
                    String.valueOf(defaultCatalog));
        }

        addProperty(properties, prefix +
                DataSourceConstants.PROP_ACCESSTOUNDERLYINGCONNECTIONALLOWED,
                String.valueOf(information.isAccessToUnderlyingConnectionAllowed()));

        addProperty(properties, prefix + DataSourceConstants.PROP_REMOVEABANDONED,
                String.valueOf(information.isRemoveAbandoned()));

        addProperty(properties, prefix +
                DataSourceConstants.PROP_REMOVEABANDONEDTIMEOUT,
                String.valueOf(information.getRemoveAbandonedTimeout()));

        addProperty(properties, prefix + DataSourceConstants.PROP_LOGABANDONED,
                String.valueOf(information.isLogAbandoned()));

        addProperty(properties, prefix +
                DataSourceConstants.PROP_POOLPREPAREDSTATEMENTS,
                String.valueOf(information.isPoolPreparedStatements()));

        addProperty(properties, prefix +
                DataSourceConstants.PROP_MAXOPENPREPAREDSTATEMENTS,
                String.valueOf(information.getMaxOpenPreparedStatements()));

        addProperty(properties, prefix + DataSourceConstants.PROP_REGISTRY,
                String.valueOf(information.getRepositoryType()));

        addProperty(properties, prefix +
                DataSourceConstants.PROP_TIMEBETWEENEVICTIONRUNSMILLIS,
                String.valueOf(information.getTimeBetweenEvictionRunsMillis()));

        addProperty(properties, prefix +
                DataSourceConstants.PROP_NUMTESTSPEREVICTIONRUN,
                String.valueOf(information.getNumTestsPerEvictionRun()));

        addProperty(properties, prefix +
                DataSourceConstants.PROP_MINEVICTABLEIDLETIMEMILLIS,
                String.valueOf(information.getMinEvictableIdleTimeMillis()));

        addProperty(properties, prefix + DataSourceConstants.PROP_TESTWHILEIDLE,
                String.valueOf(information.isTestWhileIdle()));

        String validationQ = information.getValidationQuery();
        if (validationQ != null && !"".equals(validationQ)) {
            addProperty(properties, prefix + DataSourceConstants.PROP_VALIDATIONQUERY,
                    String.valueOf(validationQ));
        }

        properties.putAll(information.getAllParameters());
        properties.putAll(information.getProperties());

        return properties;

    }

    private static void addProperty(Properties properties, String key, String value) {
        if (value != null && !"".equals(value)) {
            properties.setProperty(key, value);
        }
    }
}
