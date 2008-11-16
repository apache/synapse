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

import javax.naming.Context;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * Finds a DataSource based on various criteria
 */
public interface DataSourceFinder {

    /**
     * Find a DataSource using given name
     *
     * @param name Name of the DataSource to be found
     * @return DataSource if found , otherwise null
     */
    DataSource find(String name);

    /**
     * Find a DataSource using the given name and JNDI environment properties
     *
     * @param dsName  Name of the DataSource to be found
     * @param jndiEnv JNDI environment properties
     * @return DataSource if found , otherwise null
     */
    DataSource find(String dsName, Properties jndiEnv);

    /**
     * Find a DataSource using the given name and naming context
     *
     * @param dsName  Name of the DataSource to be found
     * @param context Naming Context
     * @return DataSource if found , otherwise null
     */
    DataSource find(String dsName, Context context);

}
