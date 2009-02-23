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

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.util.SynapseUtilException;
import org.apache.synapse.commons.util.datasource.factory.DataSourceInformationRepositoryFactory;

import java.util.Properties;

/**
 * Some help functions related with DataSourceInformationRepository
 */
public class DataSourceInformationRepositoryHelper {

    private static final Log log = LogFactory.getLog(DataSourceInformationRepositoryHelper.class);

    /**
     * Initialize and register DataSourceInformationRepository with AxisConfiguration
     *
     * @param axisConfiguration AxisConfiguration instance
     * @param properties        DataSources configuration properties
     */
    public static void initializeDataSourceInformationRepository(
            AxisConfiguration axisConfiguration,
            Properties properties) {

        DataSourceInformationRepository repository =
                getDataSourceInformationRepository(axisConfiguration);
        DataSourceInformationRepositoryListener listener = null;

        if (repository != null) {
            listener = repository.getRepositoryListener();
        }

        if (listener == null) {
            if (log.isDebugEnabled()) {
                log.debug("Creating a new DataSourceInformationRepositoryListener instance ");
            }
            listener = DataSourceRepositoryManager.getInstance();
        }

        if (listener instanceof DataSourceRepositoryManager) {
            RepositoryBasedDataSourceFinder finder = RepositoryBasedDataSourceFinder.getInstance();
            finder.init((DataSourceRepositoryManager) listener);
        }

        if (repository == null) {
            if (log.isDebugEnabled()) {
                log.debug("Initiating a new DataSourceInformationRepository");
            }
            initializeDataSourceInformationRepository(axisConfiguration, properties, listener);
        }
    }

    /**
     * Initialize and register DataSourceInformationRepository with AxisConfiguration
     *
     * @param axisConfiguration AxisConfiguration instance
     * @param properties        DataSources configuration properties
     * @param listener          DataSourceInformationRepositoryListener instance
     */
    public static void initializeDataSourceInformationRepository(
            AxisConfiguration axisConfiguration,
            Properties properties,
            DataSourceInformationRepositoryListener listener) {

        DataSourceInformationRepository repository =
                DataSourceInformationRepositoryFactory.createDataSourceInformationRepository(
                        properties, listener);
        Parameter parameter = new Parameter(
                DataSourceConfigurationConstants.DATASOURCE_INFORMATION_REPOSITORY, repository);
        try {
            axisConfiguration.addParameter(parameter);
        } catch (AxisFault axisFault) {
            handleException("Error setting 'DataSourceInformationRepository' as" +
                    " a parameter to axis2 configuration ", axisFault);
        }
    }

    /**
     * Gets DataSourceInformationRepository that is kept in  AxisConfiguration
     *
     * @param axisConfiguration AxisConfiguration instance
     * @return DataSourceInformationRepository instance if there any , otherwise null
     */
    public static DataSourceInformationRepository getDataSourceInformationRepository(
            AxisConfiguration axisConfiguration) {

        Parameter parameter = axisConfiguration.getParameter(
                DataSourceConfigurationConstants.DATASOURCE_INFORMATION_REPOSITORY);
        if (parameter != null) {
            Object result = parameter.getValue();
            if (!(result instanceof DataSourceInformationRepository)) {
                handleException("Invalid type  '" + result.getClass().getName()
                        + "' , expected : 'DataSourceInformationRepository'");
            }
            return (DataSourceInformationRepository) result;
        }
        return null;
    }

    private static void handleException(String msg, Throwable error) {
        log.error(msg, error);
        throw new SynapseUtilException(msg, error);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseUtilException(msg);
    }
}
