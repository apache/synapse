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

package org.apache.synapse.mediators.dblookup;

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.sql.*;
import java.util.*;

/**
 * Simple database table lookup mediator. Designed only for read/lookup
 */
public class DBLookupMediator extends AbstractMediator implements ManagedLifecycle {

    private static final Log log = LogFactory.getLog(DBLookupMediator.class);

    /** Hold JDBC properties */
    private Map jdbcProps = new HashMap();
    /** The connection to the database */
    Connection conn = null;
    /** Query map */
    Map queryMap = new HashMap();
    /** Result cache */
    Map cacheMap = new HashMap();

    public boolean mediate(MessageContext msgCtx) {

        for (Iterator iter = queryMap.keySet().iterator(); iter.hasNext(); ) {
            processQuery((String) iter.next(), msgCtx);
        }
        return true;
    }

    private void processQuery(String attName, MessageContext msgCtx) {

        // if available in cache, serve from cache and return
        String result = (String) cacheMap.get(attName);
        if (result != null) {
            msgCtx.setProperty(attName, result);
            return;
        }

        Query query = (Query) queryMap.get(attName);

        try {
            PreparedStatement ps = query.getStatement();

            // set parameters if any
            List params = query.getParameters();
            int column = 1;
            for (Iterator pi = params.iterator(); pi.hasNext(); ) {
                Query.Parameter param = (Query.Parameter) pi.next();
                switch (param.getType()) {
                    case Types.VARCHAR: {
                        ps.setString(column++,
                            param.getPropertyName() != null ?
                                (String) msgCtx.getProperty(param.getPropertyName()) :
                                Axis2MessageContext.getStringValue(param.getXpath(), msgCtx));
                        break;
                    }
                    case Types.INTEGER: {
                        ps.setInt(column++,
                            Integer.parseInt(param.getPropertyName() != null ?
                                (String) msgCtx.getProperty(param.getPropertyName()) :
                                Axis2MessageContext.getStringValue(param.getXpath(), msgCtx)));
                        break;
                    }
                    default: {
                        // todo handle this
                    }
                }
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Object obj = rs.getObject(1);
                if (obj != null) {
                    msgCtx.setProperty(attName, obj.toString());
                    cacheMap.put(attName, obj.toString());
                } else {
                    // todo handle this
                }
            }
        } catch (SQLException e) {
            // todo handle this
            e.printStackTrace();
        }
    }

    public void init(SynapseEnvironment se) {
        // establish database connection
    }

    public void destroy() {
        // disconnect from the database
        log.debug("Shutting down database connection of the DB Lookup mediator");
        try {
            conn.close();
        } catch (SQLException e) {
            log.warn("Error shutting down the database connection", e);
        }
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public void addQuery(String name, Query q) {
        queryMap.put(name, q);
    }

    public void addJDBCProperty(QName name, String value) {
        jdbcProps.put(name, value);
    }

    public Map getJdbcProps() {
        return jdbcProps;
    }

    public Map getQueryMap() {
        return queryMap;
    }
}
