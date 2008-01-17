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

package org.apache.synapse.mediators.db;

import org.apache.synapse.MessageContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.Iterator;

/**
 * Simple database table lookup mediator. Designed only for read/lookup
 */
public class DBLookupMediator extends AbstractDBMediator {

    protected void processStatement(Statement stmnt, MessageContext msgCtx) {

        boolean traceOn = isTraceOn(msgCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        // execute the prepared statement, and extract the first result row and
        // set as message context properties, any results that have been specified
        Connection con = null;
        try {
            PreparedStatement ps = getPreparedStatement(stmnt, msgCtx);
            con = ps.getConnection();
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn,
                        "Processing the first row returned : " + stmnt.getRawStatement());
                }

                Iterator propNameIter = stmnt.getResultsMap().keySet().iterator();
                while (propNameIter.hasNext()) {

                    String propName = (String) propNameIter.next();
                    String columnStr = (String) stmnt.getResultsMap().get(propName);

                    Object obj = null;
                    try {
                        int colNum = Integer.parseInt(columnStr);
                        obj = rs.getObject(colNum);
                    } catch (NumberFormatException ignore) {
                        obj = rs.getObject(columnStr);
                    }

                    if (obj != null) {
                        if (traceOrDebugOn) {
                            traceOrDebug(traceOn, "Column : " + columnStr +
                                " returned value : " + obj +
                                " Setting this as the message property : " + propName);
                        }
                        msgCtx.setProperty(propName, obj.toString());
                    } else {
                        if (traceOrDebugOn) {
                            traceOrDebugWarn(traceOn, "Column : " + columnStr +
                                " returned null Skip setting message property : " + propName);
                        }
                    }
                }
            } else {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Statement : "
                        + stmnt.getRawStatement() + " returned 0 rows");
                }
            }
            
        } catch (SQLException e) {
            handleException("Error executing statement : " + stmnt.getRawStatement() +
                " against DataSource : " + getDSName(), e, msgCtx);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignore) {}
            }
        }
    }

}
