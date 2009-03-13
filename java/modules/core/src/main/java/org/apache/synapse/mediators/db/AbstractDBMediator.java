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

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.datasources.PerUserPoolDataSource;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.util.datasource.DBPoolView;
import org.apache.synapse.config.xml.AbstractDBMediatorFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;

import javax.sql.DataSource;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This abstract DB mediator will perform common DB connection pooling etc. for all DB mediators
 */
public abstract class AbstractDBMediator extends AbstractMediator {

    /** Hold JDBC properties */
    protected final Map dataSourceProps = new HashMap();
    /** The DataSource to get DB connections */
    private DataSource dataSource = null;
    /**
     * MBean for DBPool monitoring
     */
    private DBPoolView dbPoolView;
    /** Statements */
    private final List<Statement> statementList = new ArrayList<Statement>();

    /**
     * Initializes the mediator. Does nothing right now. If DataSource lookup is supported, could
     * do the IC lookup here
     * @param se the Synapse environment reference
     */
    @Override
    public void init(SynapseEnvironment se) {
        // do nothing
    }

    /**
     * Destroys the mediator. If we are using our custom DataSource, then shut down the connections
     */
    @Override
    public void destroy() {
        if (this.dataSource instanceof BasicDataSource) {
            try {
                ((BasicDataSource) this.dataSource).close();
                log.info("Successfully shut down DB connection pool for URL : " + getDSName());
            } catch (SQLException e) {
                log.warn("Error shutting down DB connection pool for URL : " + getDSName());
            }
        } else if (this.dataSource instanceof PerUserPoolDataSource) {
            ((PerUserPoolDataSource) this.dataSource).close();
            log.info("Successfully shut down DB connection pool for URL : " + getDSName());
        }
    }

    /**
     * Process each SQL statement against the current message
     * @param synCtx the current message
     * @return true, always
     */
    public boolean mediate(MessageContext synCtx) {

        String name = (this instanceof DBLookupMediator ? "DBLookup" : "DBReport");
        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : " + name + " mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        for (Statement aStatement : statementList) {
            if (aStatement != null) {
                processStatement(aStatement, synCtx);
            }
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("End : " + name + " mediator");
        }
        return true;
    }

    /**
     * Subclasses must specify how each SQL statement is processed
     * @param query the SQL statement
     * @param msgCtx current message
     */
    abstract protected void processStatement(Statement query, MessageContext msgCtx);

    /**
     * Return the name or (hopefully) unique connection URL specific to the DataSource being used
     * This is used for logging purposes only
     * @return a unique name or URL to refer to the DataSource being used
     */
    protected String getDSName() {
        String name = (String) dataSourceProps.get(AbstractDBMediatorFactory.URL_Q);
        if (name == null) {
            name = (String) dataSourceProps.get(AbstractDBMediatorFactory.DSNAME_Q);
        }
        return name;
    }

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
    protected PreparedStatement getPreparedStatement(Statement stmnt,
                                                     MessageContext msgCtx) throws SQLException {

        SynapseLog synLog = getLog(msgCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Getting a connection from DataSource " + getDSName() +
                " and preparing statement : " + stmnt.getRawStatement());
        }

        Connection con = getDataSource().getConnection();
        if (con == null) {
            String msg = "Connection from DataSource " + getDSName() + " is null.";
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (dataSource instanceof BasicDataSource) {

            BasicDataSource basicDataSource = (BasicDataSource) dataSource;
            int numActive = basicDataSource.getNumActive();
            int numIdle = basicDataSource.getNumIdle();
            String connectionId = Integer.toHexString(con.hashCode());


            DBPoolView dbPoolView = getDbPoolView();
            if (dbPoolView != null) {
                dbPoolView.setNumActive(numActive);
                dbPoolView.setNumIdle(numIdle);
                dbPoolView.updateConnectionUsage(connectionId);
            }

            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("[ DB Connection : " + con + " ]");
                synLog.traceOrDebug("[ DB Connection instance identifier : " +
                        connectionId + " ]");
                synLog.traceOrDebug("[ Number of Active Connection : " + numActive + " ]");
                synLog.traceOrDebug("[ Number of Idle Connection : " + numIdle + " ]");
            }
        }

        PreparedStatement ps = con.prepareStatement(stmnt.getRawStatement());

        // set parameters if any
        List<Statement.Parameter> params = stmnt.getParameters();
        int column = 1;

        for (Statement.Parameter param : params) {
            if (param == null) {
                continue;
            }
            String value = (param.getPropertyName() != null ?
                    param.getPropertyName() : param.getXpath().stringValueOf(msgCtx));

            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Setting as parameter : " + column + " value : " + value +
                        " as JDBC Type : " + param.getType() + "(see java.sql.Types for valid " +
                        "types)");
            }

            switch (param.getType()) {
                // according to J2SE 1.5 /docs/guide/jdbc/getstart/mapping.html
                case Types.CHAR:
                case Types.VARCHAR:
                case Types.LONGVARCHAR: {
                    ps.setString(column++, value);
                    break;
                }
                case Types.NUMERIC:
                case Types.DECIMAL: {
                    ps.setBigDecimal(column++, new BigDecimal(value));
                    break;
                }
                case Types.BIT: {
                    ps.setBoolean(column++, Boolean.parseBoolean(value));
                    break;
                }
                case Types.TINYINT: {
                    ps.setByte(column++, Byte.parseByte(value));
                    break;
                }
                case Types.SMALLINT: {
                    ps.setShort(column++, Short.parseShort(value));
                    break;
                }
                case Types.INTEGER: {
                    ps.setInt(column++, Integer.parseInt(value));
                    break;
                }
                case Types.BIGINT: {
                    ps.setLong(column++, Long.parseLong(value));
                    break;
                }
                case Types.REAL: {
                    ps.setFloat(column++, Float.parseFloat(value));
                    break;
                }
                case Types.FLOAT: {
                    ps.setDouble(column++, Double.parseDouble(value));
                    break;
                }
                case Types.DOUBLE: {
                    ps.setDouble(column++, Double.parseDouble(value));
                    break;
                }
                // skip BINARY, VARBINARY and LONGVARBINARY
                case Types.DATE: {
                    ps.setDate(column++, Date.valueOf(value));
                    break;
                }
                case Types.TIME: {
                    ps.setTime(column++, Time.valueOf(value));
                    break;
                }
                case Types.TIMESTAMP: {
                    ps.setTimestamp(column++, Timestamp.valueOf(value));
                    break;
                }
                // skip CLOB, BLOB, ARRAY, DISTINCT, STRUCT, REF, JAVA_OBJECT
                default: {
                    String msg = "Trying to set an un-supported JDBC Type : " + param.getType() +
                            " against column : " + column + " and statement : " +
                            stmnt.getRawStatement() +
                            " used by a DB mediator against DataSource : " + getDSName() +
                            " (see java.sql.Types for valid type values)";
                    handleException(msg, msgCtx);
                }
            }
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Successfully prepared statement : " + stmnt.getRawStatement() +
                " against DataSource : " + getDSName());
        }
        return ps;
    }

    public DBPoolView getDbPoolView() {
        return dbPoolView;
    }

    public void setDbPoolView(DBPoolView dbPoolView) {
        this.dbPoolView = dbPoolView;
    }
}
