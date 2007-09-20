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

import org.apache.synapse.mediators.db.Statement;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.sql.*;

public class DBReportMediator extends AbstractDBMediator {

    private static final Log log = LogFactory.getLog(DBReportMediator.class);

    protected void processStatement(Statement stmnt, MessageContext msgCtx) {

        try {
            PreparedStatement ps = getPreparedStatement(stmnt, msgCtx);
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
}
