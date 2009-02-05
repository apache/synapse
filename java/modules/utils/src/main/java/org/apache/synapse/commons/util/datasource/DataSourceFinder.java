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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.Properties;

/**
 *
 */
public class DataSourceFinder {

    private static Log log = LogFactory.getLog(DataSourceFinder.class);

    /**
     * Find a DataSource using the given name and JNDI environment properties
     *
     * @param dsName  Name of the DataSource to be found
     * @param jndiEnv JNDI environment properties
     * @return DataSource if found , otherwise null
     */
    public static DataSource find(String dsName, Properties jndiEnv) {

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
    public static DataSource find(String dsName, Context context) {

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

    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     */
    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseUtilException(msg);
    }

    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     * @param e   The exception
     */
    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseUtilException(msg, e);
    }
}
