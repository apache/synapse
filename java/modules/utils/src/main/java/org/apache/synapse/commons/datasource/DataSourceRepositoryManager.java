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


    private InMemoryDataSourceRepository  inMemoryDataSourceRepository;
    private JNDIBasedDataSourceRepository  jndiBasedDataSourceRepository;

    public DataSourceRepositoryManager(InMemoryDataSourceRepository inMemoryDataSourceRepository,
                                       JNDIBasedDataSourceRepository jndiBasedDataSourceRepository) {
        this.inMemoryDataSourceRepository = inMemoryDataSourceRepository;
        this.jndiBasedDataSourceRepository = jndiBasedDataSourceRepository;
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

        DataSource result = inMemoryDataSourceRepository.lookUp(name);

        if (result != null) {
            return result;
        }
        if (jndiBasedDataSourceRepository.isInitialized()) {
            return jndiBasedDataSourceRepository.lookUp(name);
        }
        return null;
    }

    public void addDataSourceInformation(DataSourceInformation dataSourceInformation) {

        if (dataSourceInformation == null) {
            return;
        }

        String repositoryType = dataSourceInformation.getRepositoryType();
        if (DataSourceConstants.PROP_REGISTRY_JNDI.equals(repositoryType)) {
            jndiBasedDataSourceRepository.register(dataSourceInformation);
        } else {
            inMemoryDataSourceRepository.register(dataSourceInformation);
        }
    }

    public void removeDataSourceInformation(DataSourceInformation dataSourceInformation) {

        String repositoryType = dataSourceInformation.getRepositoryType();

        if (DataSourceConstants.PROP_REGISTRY_JNDI.equals(repositoryType)) {
            jndiBasedDataSourceRepository.unRegister(dataSourceInformation.getDatasourceName());
        } else {
            inMemoryDataSourceRepository.unRegister(dataSourceInformation.getDatasourceName());
        }
    }

    public void reConfigure(Properties confProperties) {

        jndiBasedDataSourceRepository.init(confProperties);
        inMemoryDataSourceRepository.init(confProperties);
    }

    public void clear() {
        if (inMemoryDataSourceRepository.isInitialized()) {
            inMemoryDataSourceRepository.clear();
        }
        if (jndiBasedDataSourceRepository.isInitialized()) {
            jndiBasedDataSourceRepository.clear();
        }
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
