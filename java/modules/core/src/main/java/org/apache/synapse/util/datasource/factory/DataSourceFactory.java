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

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.util.datasource.DataSourceInformation;

import javax.sql.DataSource;

/**
 * Factory for creating a DataSource based on information in DataSourceInformation
 */
public class DataSourceFactory {

    private final static Log log = LogFactory.getLog(DataSourceFactory.class);

    private DataSourceFactory() {
    }

    /**
     * Factory method to create a DataSource based on provided information
     * which is encapsulated in the  DataSourceInformation object
     *
     * @param information Information about DataSource
     * @return DataSource Instance if one can be created ,
     *         otherwise null or exception if provided details are not valid or enough to create
     *         a DataSource
     */
    public static DataSource createDataSource(DataSourceInformation information) {

        String dsType = information.getType();
        String driver = information.getDriver();

        if (driver == null || "".equals(driver)) {
            handleException("Database driver class name cannot be found.");
        }

        String url = information.getUrl();

        if (url == null || "".equals(url)) {
            handleException("Database connection URL cannot be found.");
        }

        String user = information.getUser();
        String password = information.getPassword();

        if (DataSourceInformation.BASIC_DATA_SOURCE.equals(dsType)) {

            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setDriverClassName(driver);
            basicDataSource.setUrl(url);

            if (user != null && !"".equals(user)) {
                basicDataSource.setUsername(user);
            }

            if (password != null && !"".equals(password)) {
                basicDataSource.setPassword(password);
            }

            basicDataSource.setMaxActive(information.getMaxActive());
            basicDataSource.setMaxIdle(information.getMaxIdle());
            basicDataSource.setMaxWait(information.getMaxWait());
            basicDataSource.setDefaultAutoCommit(information.isDefaultAutoCommit());
            basicDataSource.setDefaultReadOnly(information.isDefaultReadOnly());
            basicDataSource.setTestOnBorrow(information.isTestOnBorrow());
            basicDataSource.setTestOnReturn(information.isTestOnReturn());
            basicDataSource.setTestWhileIdle(information.isTestWhileIdle());

            String validationQuery = information.getValidationQuery();

            if (validationQuery != null && !"".equals(validationQuery)) {
                basicDataSource.setValidationQuery(validationQuery);
            }
            return basicDataSource;

        } else {
            handleException("Unsupported DataSorce : " + dsType);
        }
        return null;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
