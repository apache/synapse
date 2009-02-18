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

import org.apache.synapse.commons.util.datasource.DataSourceInformation;
import org.apache.synapse.commons.util.datasource.DataSourceInformationRepository;
import org.apache.synapse.commons.util.datasource.DataSourceInformationRepositoryListener;
import org.apache.synapse.commons.util.datasource.DataSourceRepositoryManager;

import java.util.List;
import java.util.Properties;

/**
 * Contains Factory methods that use to create DataSourceInformationRepository
 */
public class DataSourceInformationRepositoryFactory {

    /**
     * Factory method to create a DataSourceInformationRepository
     * Use 'DataSourceRepositoryManager' as RepositoryListener
     *
     * @param properties DataSource properties
     * @return DataSourceInformationRepository instance
     */
    public static DataSourceInformationRepository createDataSourceInformationRepository(
            Properties properties) {

        return createDataSourceInformationRepository(properties,
                DataSourceRepositoryManager.getInstance());
    }

    /**
     * Factory method to create a DataSourceInformationRepository
     *
     * @param properties DataSource properties
     * @param listener   DataSourceInformationRepositoryListener
     * @return DataSourceInformationRepository instance
     */
    public static DataSourceInformationRepository createDataSourceInformationRepository(
            Properties properties, DataSourceInformationRepositoryListener listener) {

        List<DataSourceInformation> sourceInformationList =
                DataSourceInformationListFactory.createDataSourceInformationList(properties);
        DataSourceInformationRepository repository = new DataSourceInformationRepository();
        repository.setRepositoryListener(listener);
        if (properties != null && !properties.isEmpty()) {
            repository.setConfigurationProperties(properties);
        }
        for (DataSourceInformation information : sourceInformationList) {
            if (information != null) {
                repository.addDataSourceInformation(information);
            }
        }
        return repository;
    }
}
