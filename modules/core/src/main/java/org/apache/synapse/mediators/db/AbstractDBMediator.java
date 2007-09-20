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

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.xml.AbstractDBMediatorFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.dbcp.dbcp.BasicDataSource;

import javax.xml.namespace.QName;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.*;

/**
 * This abstract DB mediator will perform common DB connection pooling etc. for all DB mediators
 */
public abstract class AbstractDBMediator extends AbstractMediator implements ManagedLifecycle {

    private static final Log log = LogFactory.getLog(AbstractDBMediator.class);
    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    /** Hold JDBC properties */
    protected Map dataSourceProps = new HashMap();
    /** The DataSource to get DB connections */
    private DataSource dataSource = null;
    /** Statements */
    List statementList = new ArrayList();

    /** For logging purposes refer to*/
    private String url;

    public void init(SynapseEnvironment se) {
        url = (String) dataSourceProps.get(AbstractDBMediatorFactory.URL_Q);
    }

    public void destroy() {
        try {
            ((BasicDataSource) getDataSource()).close();
            log.info("");
        } catch (SQLException e) {
            log.warn("Error shutting down DB connection pool for URL : " + url);
        }
    }

    public boolean mediate(MessageContext synCtx) {
        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        
        for (Iterator iter = statementList.iterator(); iter.hasNext(); ) {
            processStatement((Statement) iter.next(), synCtx);
        }
        return true;
    }

    abstract protected void processStatement(Statement query, MessageContext msgCtx);

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void addDataSourceProperty(QName name, String value) {
        dataSourceProps.put(name, value);
    }

    public void addDataSourceProperty(String name, String value) {
        dataSourceProps.put(name, value);
    }

    public Map getDataSourceProps() {
        return dataSourceProps;
    }

    public void addStatement(Statement stmnt) {
        statementList.add(stmnt);
    }

    public List getStatementList() {
        return statementList;
    }

    /**
     * Return a Prepared statement for the given Statement object, which is ready to be executed
     * @param stmnt
     * @param msgCtx
     * @return
     * @throws SQLException
     */
    protected PreparedStatement getPreparedStatement(Statement stmnt, MessageContext msgCtx) throws SQLException {
        Connection con = getDataSource().getConnection();
        PreparedStatement ps = con.prepareStatement(stmnt.getRawStatement());

        // set parameters if any
        List params = stmnt.getParameters();
        int column = 1;

        for (Iterator pi = params.iterator(); pi.hasNext(); ) {

            Statement.Parameter param = (Statement.Parameter) pi.next();
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
        return ps;
    }
}
