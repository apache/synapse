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
/**
 * To change this template use File | Settings | File Templates.
 */
package org.apache.synapse.util.datasource;

import org.apache.synapse.util.datasource.factory.DataSourceFactory;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps all DataSources in the memory
 */
public class InMemoryDataSourceRegistry implements DataSourceRegistry {

    private static final InMemoryDataSourceRegistry ourInstance = new InMemoryDataSourceRegistry();
    private final static Map<String, DataSource> dataSources = new HashMap<String, DataSource>();

    public static InMemoryDataSourceRegistry getInstance() {
        return ourInstance;
    }

    private InMemoryDataSourceRegistry() {
    }

    public void register(DataSourceInformation information) {
        DataSource dataSource = DataSourceFactory.createDataSource(information);
        String name = information.getName();
        dataSources.put(name, dataSource);
    }

    public DataSource lookUp(String name) {
        return dataSources.get(name);
    }
}
