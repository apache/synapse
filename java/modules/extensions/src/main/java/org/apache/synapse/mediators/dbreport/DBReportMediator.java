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

package org.apache.synapse.mediators.dbreport;

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.dblookup.Query;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.util.*;
import java.sql.*;

public class DBReportMediator extends AbstractMediator implements ManagedLifecycle {

    private static final Log log = LogFactory.getLog(DBReportMediator.class);

    /** Hold JDBC properties */
    private Map jdbcProps = new HashMap();
    /** The connection to the database */
    Connection conn = null;
    /** Query map */
    List queryList = new ArrayList();

    public boolean mediate(MessageContext msgCtx) {
        for (Iterator iter = queryList.iterator(); iter.hasNext(); ) {
            processQuery((Query) iter.next(), msgCtx);
        }
        return true;
    }

    private void processQuery(Query query, MessageContext msgCtx) {

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

            if (ps.executeUpdate() > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Added a row to the table");
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
        log.debug("Shutting down database connection of the DB Report mediator");
        try {
            conn.close();
        } catch (SQLException e) {
            log.warn("Error shutting down the database connection", e);
        }
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public void addQuery(Query q) {
        queryList.add(q);
    }

    public void addJDBCProperty(QName name, String value) {
        jdbcProps.put(name, value);
    }

    public Map getJdbcProps() {
        return jdbcProps;
    }

    public List getQueryList() {
        return queryList;
    }
}
