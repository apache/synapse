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

import java.util.*;

/**
 *
 */
public class DataSourceInformationRepository {

    private static final Log log = LogFactory.getLog(DataSourceInformationRepository.class);

    private final Map<String, DataSourceInformation> dataSourceInformationMap =
            new HashMap<String, DataSourceInformation>();

    private final List<DataSourceInformationRepositoryListener> listeners =
            new ArrayList<DataSourceInformationRepositoryListener>();

    public void setConfigurationProperties(Properties congurationProperties) {

        for (DataSourceInformationRepositoryListener listener : listeners) {
            listener.reConfigure(congurationProperties);
        }
    }

    public void addDataSourceInformation(DataSourceInformation dataSourceInformation) {

        assertNull(dataSourceInformation, "DataSource information is null");

        dataSourceInformationMap.put(dataSourceInformation.getAlias(), dataSourceInformation);
        for (DataSourceInformationRepositoryListener listener : listeners) {
            listener.addDataSourceInformation(dataSourceInformation);
        }
    }

    public DataSourceInformation getDataSourceInformation(String name) {

        assertNull(name, "Name of the datasource  information instance to be returned is null");

        return dataSourceInformationMap.get(name);
    }

    public DataSourceInformation removeDataSourceInformation(String name) {

        assertNull(name, "Name of the datasource information instance to be removed is null");

        DataSourceInformation information = dataSourceInformationMap.remove(name);

        assertNull(information, "There is no datasource information instance for given name :" + name);

        for (DataSourceInformationRepositoryListener listener : listeners) {
            listener.removeDataSourceInformation(information);
        }
        return information;
    }

    public Iterator<DataSourceInformation> getAllDataSourceInformation() {

        return dataSourceInformationMap.values().iterator();
    }

    public void registerDataSourceInformationRepositoryListener(DataSourceInformationRepositoryListener listener) {

        assertNull(listener, "Provided 'DataSourceInformationRepositoryListener' instance is null");

        listeners.add(listener);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseUtilException(msg);
    }

    private void assertNull(String name, String msg) {
        if (name == null || "".equals(name)) {
            handleException(msg);
        }
    }

    private void assertNull(Object object, String msg) {
        if (object == null) {
            handleException(msg);
        }
    }
}