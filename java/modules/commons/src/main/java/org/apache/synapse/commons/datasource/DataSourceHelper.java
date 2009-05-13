/**
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

import org.apache.synapse.commons.datasource.factory.DataSourceInformationRepositoryFactory;
import org.apache.synapse.commons.SynapseCommonsException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

/**
 * Some help functions related with DataSourceInformationRepository
 */
public class DataSourceHelper {

    private static final Log log = LogFactory.getLog(DataSourceHelper.class);
    private final static DataSourceHelper REPOSITORY_HELPER =
            new DataSourceHelper();

    private DataSourceInformationRepository dataSourceInformationRepository;
    private boolean initialized = false;
    private DataSourceRepositoryManager dataSourceRepositoryManager;
    private RepositoryBasedDataSourceFinder repositoryBasedDataSourceFinder;

    private DataSourceHelper() {
    }

    public static DataSourceHelper getInstance() {
        return REPOSITORY_HELPER;
    }

    /**
     * Initialize DataSourceInformationRepository.
     *
     * @param repository to be initialized
     * @param properties DataSources configuration properties
     */
    public void init(
            DataSourceInformationRepository repository,
            Properties properties) {

        if (initialized) {
            if (log.isDebugEnabled()) {
                log.debug("DataSourceHelper has been already initialized.");
            }
            return;
        }

        DataSourceInformationRepositoryListener repositoryListener = null;
        if (repository != null) {
            repositoryListener = repository.getRepositoryListener();
        }

        if (repositoryListener == null) {
            repositoryListener = new DataSourceRepositoryManager(
                    new InMemoryDataSourceRepository(),
                    new JNDIBasedDataSourceRepository());
            if (repository != null) {
                repository.setRepositoryListener(repositoryListener);
            }
        }

        if (repositoryListener instanceof DataSourceRepositoryManager) {
            dataSourceRepositoryManager = (DataSourceRepositoryManager) repositoryListener;
            repositoryBasedDataSourceFinder = new RepositoryBasedDataSourceFinder();
            repositoryBasedDataSourceFinder.init(dataSourceRepositoryManager);
        }

        if (repository == null) {
            repository =
                    DataSourceInformationRepositoryFactory.createDataSourceInformationRepository(
                            repositoryListener, properties);
        } else {
            DataSourceInformationRepositoryFactory.setupDatasourceInformationRepository(
                    repository, properties);
        }
        dataSourceInformationRepository = repository;
        initialized = true;
    }

    public DataSourceInformationRepository getDataSourceInformationRepository() {
        assertInitialized();
        return dataSourceInformationRepository;
    }

    private void assertInitialized() {
        if (!initialized) {
            String msg = "DataSourceHelper has not been initialized, " +
                    "it requires to be initialized";
            log.error(msg);
            throw new SynapseCommonsException(msg);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public DataSourceRepositoryManager getDataSourceRepositoryManager() {
        assertInitialized();
        return dataSourceRepositoryManager;
    }

    public RepositoryBasedDataSourceFinder getRepositoryBasedDataSourceFinder() {
        assertInitialized();
        return repositoryBasedDataSourceFinder;
    }
}
