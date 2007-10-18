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
import java.sql.SQLException;

/**
 * A mediator that writes (i.e. inserts one row) to a table using message information
 */
public class DBReportMediator extends AbstractDBMediator {

    protected void processStatement(Statement stmnt, MessageContext msgCtx) {

        boolean traceOn = isTraceOn(msgCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        try {
            PreparedStatement ps = getPreparedStatement(stmnt, msgCtx);
            int count = ps.executeUpdate();

            if (count > 0) {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn,
                        "Inserted " + count + " row/s using statement : " + stmnt.getRawStatement());
                }
            } else {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn,
                        "No rows were inserted for statement : " + stmnt.getRawStatement());
                }
            }
        } catch (SQLException e) {
            handleException("Error execuring insert statement : " + stmnt.getRawStatement() +
                " against DataSource : " + getDSName(), e, msgCtx);
        }
    }
}
