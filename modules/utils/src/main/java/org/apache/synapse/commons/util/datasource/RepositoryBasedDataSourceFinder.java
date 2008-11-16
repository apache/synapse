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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.util.SynapseUtilException;

import javax.sql.DataSource;

/**
 * Finds a DataSource from  DataSourceRepositories
 */
public class RepositoryBasedDataSourceFinder {

    private final static Log log = LogFactory.getLog(InMemoryDataSourceRepository.class);
    private DataSourceRepositoryManager dataSourceRepositoryManager;
    private boolean initialized;
    private static final RepositoryBasedDataSourceFinder REPOSITORY_BASED_DATA_SOURCE_FINDER = new RepositoryBasedDataSourceFinder();

    private RepositoryBasedDataSourceFinder() {
    }

    public static RepositoryBasedDataSourceFinder getInstance() {
        return REPOSITORY_BASED_DATA_SOURCE_FINDER;
    }

    public void init(DataSourceRepositoryManager dataSourceRepositoryManager) {
        this.dataSourceRepositoryManager = dataSourceRepositoryManager;
        this.initialized = true;
    }

    /**
     * Find a DataSource using given name
     *
     * @param name Name of the DataSource to be found
     * @return DataSource if found , otherwise null
     */
    public DataSource find(String name) {
        assertInitialized();
        if (name == null || "".equals(name)) {
            handleException("DataSource name cannot be found.");
        }

        return dataSourceRepositoryManager.getDataSource(name);
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

    private void assertInitialized() {
        if (!initialized) {
            handleException("RepositoryBasedDataSourceFinder has not been initialized with a 'DataSourceRepositoryManager' instance ");
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
