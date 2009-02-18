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
import java.util.Properties;

/**
 * Utility class to handle data source registration
 */
public class DataSourceRepositoryManager implements DataSourceInformationRepositoryListener {

    private static final Log log = LogFactory.getLog(DataSourceRepositoryManager.class);

    private static final DataSourceRepositoryManager DATA_SOURCE_REPOSITORY_MANAGER
            = new DataSourceRepositoryManager();

    private static final DataSourceRepository IN_MEMORY_REPOSITORY
            = InMemoryDataSourceRepository.getInstance();
    private static final DataSourceRepository JNDI_REPOSITORY
            = JNDIBasedDataSourceRepository.getInstance();

    public DataSourceRepositoryManager() {
    }

    public static DataSourceRepositoryManager getInstance() {
        return DATA_SOURCE_REPOSITORY_MANAGER;
    }

    /**
     * Find a DataSource using given name
     *
     * @param name Name of the DataSource to be found
     * @return DataSource if found , otherwise null
     */
    public DataSource getDataSource(String name) {

        if (name == null || "".equals(name)) {
            handleException("DataSource name cannot be found.");
        }

        DataSource result = IN_MEMORY_REPOSITORY.lookUp(name);

        if (result != null) {
            return result;
        }
        if (JNDI_REPOSITORY.isInitialized()) {
            return JNDI_REPOSITORY.lookUp(name);
        }
        return null;
    }

    public void addDataSourceInformation(DataSourceInformation dataSourceInformation) {

        if (dataSourceInformation == null) {
            return;
        }

        String repositoryType = dataSourceInformation.getRepositoryType();
        if (DataSourceConfigurationConstants.PROP_REGISTRY_JNDI.equals(repositoryType)) {
            JNDI_REPOSITORY.register(dataSourceInformation);
        } else {
            IN_MEMORY_REPOSITORY.register(dataSourceInformation);
        }
    }

    public void removeDataSourceInformation(DataSourceInformation dataSourceInformation) {

        String repositoryType = dataSourceInformation.getRepositoryType();

        if (DataSourceConfigurationConstants.PROP_REGISTRY_JNDI.equals(repositoryType)) {
            JNDI_REPOSITORY.unRegister(dataSourceInformation.getDatasourceName());
        } else {
            IN_MEMORY_REPOSITORY.unRegister(dataSourceInformation.getDatasourceName());
        }
    }

    public void reConfigure(Properties confProperties) {

        JNDI_REPOSITORY.init(confProperties);
        IN_MEMORY_REPOSITORY.init(confProperties);
    }

    public void clear() {
        IN_MEMORY_REPOSITORY.clear();
        JNDI_REPOSITORY.clear();
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
