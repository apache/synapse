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

import java.util.*;

/**
 *
 */
public class DataSourceInformationRepository {

    private final Map<String, DataSourceInformation> dataSourceInformationMap =
            new HashMap<String, DataSourceInformation>();
    private final List<DataSourceInformationRepositoryListener> listeners =
            new ArrayList<DataSourceInformationRepositoryListener>();

    public void setConfigurationProperties(Properties congurationProperties) {
        for (DataSourceInformationRepositoryListener listener : listeners) {
            if (listener != null) {
                listener.reConfigure(congurationProperties);
            }
        }
    }

    public void addDataSourceInformation(DataSourceInformation dataSourceInformation) {
        dataSourceInformationMap.put(dataSourceInformation.getAlias(), dataSourceInformation);
        for (DataSourceInformationRepositoryListener listener : listeners) {
            if (listener != null) {
                listener.addDataSourceInformation(dataSourceInformation);
            }
        }
    }

    public DataSourceInformation getDataSourceInformation(String name) {
        return dataSourceInformationMap.get(name);
    }

    public DataSourceInformation removeDataSourceInformation(String name) {
        DataSourceInformation information = dataSourceInformationMap.remove(name);
        for (DataSourceInformationRepositoryListener listener : listeners) {
            if (listener != null) {
                listener.removeDataSourceInformation(information);
            }
        }
        return information;
    }

    public Iterator<DataSourceInformation> getAllDataSourceInformation() {
        return dataSourceInformationMap.values().iterator();
    }

    public void registerDataSourceInformationRepositoryListener(DataSourceInformationRepositoryListener listener) {
        listeners.add(listener);
    }
}