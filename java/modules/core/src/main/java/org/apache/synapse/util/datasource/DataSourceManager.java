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
package org.apache.synapse.util.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * Utility class to handle data source registration
 */
public class DataSourceManager implements DataSourceInformationRepositoryListener, DataSourceFinder {

    private static final Log log = LogFactory.getLog(DataSourceManager.class);

    private static final DataSourceManager DATA_SOURCE_MANAGER = new DataSourceManager();

    private static final DataSourceRepository IN_MEMORY_REPOSITORY = InMemoryDataSourceRepository.getInstance();
    private static final DataSourceRepository JNDI_REPOSITORY = JNDIBasedDataSourceRepository.getInstance();

    private DataSourceManager() {
    }

    public static DataSourceManager getInstance() {
        return DATA_SOURCE_MANAGER;
    }

    /**
     * Find a DataSource using given name
     *
     * @param name Name of the DataSource to be found
     * @return DataSource if found , otherwise null
     */
    public DataSource find(String name) {

        if (name == null || "".equals(name)) {
            handleException("DataSource name cannot be found.");
        }

        DataSource result = IN_MEMORY_REPOSITORY.lookUp(name);

        if (result != null) {
            return result;
        }
        if (JNDI_REPOSITORY.isInitialized()) {
            return IN_MEMORY_REPOSITORY.lookUp(name);
        }
        return null;
    }

    /**
     * Find a DataSource using the given name and JNDI environment properties
     *
     * @param dsName  Name of the DataSource to be found
     * @param jndiEnv JNDI environment properties
     * @return DataSource if found , otherwise null
     */
    public DataSource find(String dsName, Properties jndiEnv) {

        try {

            Context context = new InitialContext(jndiEnv);
            return find(dsName, context);

        } catch (NamingException e) {
            handleException("Error looking up DataSource : " + dsName +
                    " using JNDI properties : " + jndiEnv, e);
        }
        return null;
    }

    /**
     * Find a DataSource using the given name and naming context
     *
     * @param dsName  Name of the DataSource to be found
     * @param context Naming Context
     * @return DataSource if found , otherwise null
     */
    public DataSource find(String dsName, Context context) {

        try {
            Object dataSourceO = context.lookup(dsName);
            if (dataSourceO != null && dataSourceO instanceof DataSource) {
                return (DataSource) dataSourceO;
            } else {
                handleException("DataSource : " + dsName + " not found when looking up" +
                        " using JNDI properties : " + context.getEnvironment());
            }

        } catch (NamingException e) {
            handleException(new StringBuilder().append("Error looking up DataSource : ")
                    .append(dsName).append(" using JNDI properties : ").
                    append(context).toString(), e);
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

    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     */
    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     * @param e   The exception
     */
    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

}
