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
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;
import java.util.*;

/**
 * Simple database table lookup mediator. Designed only for read/lookup
 */
public class DBLookupMediator extends AbstractDBMediator {

    private static final Log log = LogFactory.getLog(DBLookupMediator.class);

    /** Result cache */
    Map cacheMap = new HashMap();

    protected void processStatement(Statement stmnt, MessageContext msgCtx) {

/*
        // if available in cache, serve from cache and return
        String result = (String) cacheMap.get(attName);
        if (result != null) {
            msgCtx.setProperty(attName, result);
            return;
        }
*/
        // execute the prepared statement, and extract the first result row and
        // set as message context properties, any results that have been specified
        try {
            PreparedStatement ps = getPreparedStatement(stmnt, msgCtx);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
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
                        msgCtx.setProperty(propName, obj.toString());
                        cacheMap.put(propName, obj.toString());
                    } else {
                        // todo handle this
                    }
                }
            } else {
                // todo
            }
        } catch (SQLException e) {
            // todo handle this
            e.printStackTrace();
        }
    }

}
