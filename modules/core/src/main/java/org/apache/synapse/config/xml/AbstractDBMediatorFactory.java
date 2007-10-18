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

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.synapse.mediators.db.AbstractDBMediator;
import org.apache.synapse.mediators.db.Statement;
import org.jaxen.JaxenException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.namespace.QName;
import java.sql.Connection;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * <dbreport | dblookup | .. etc>
 *   <connection>
 *     <pool>
 *     (
 *       <driver/>
 *       <url/>
 *       <user/>
 *       <password/>
 *     | 
 *       <dsName/>
 *       <icClass/>
 *       <url/>
 *       <user/>
 *       <password/>
 *     )
 *       <property name="name" value="value"/>*
 *     </pool>
 *   </connection>
 *   <statement>
 *     <sql>insert into table values (?, ?, ..) OR select target from destinations where src = ?</sql>
 *     <parameter (value="const" | expression="xpath") type="INTEGER|VARCHAR|..."/>*
 *     <result name="propName" column="target | number"/>*
 *   </statement>+
 * </dbreport | dblookup | .. etc>
 *
 * Supported properties for custom DataSources
 * autocommit = true | false
 * isolation = Connection.TRANSACTION_NONE
 *           | Connection.TRANSACTION_READ_COMMITTED
 *           | Connection.TRANSACTION_READ_UNCOMMITTED 
 *           | Connection.TRANSACTION_REPEATABLE_READ 
 *           | Connection.TRANSACTION_SERIALIZABLE
 * initialsize = int
 * maxactive = int
 * maxidle = int
 * maxopenstatements = int
 * maxwait = long
 * minidle = int
 * poolstatements = true | false
 * testonborrow = true | false
 * testonreturn = true | false
 * testwhileidle = true | false
 * validationquery = String
 */
public abstract class AbstractDBMediatorFactory extends AbstractMediatorFactory {

    public static final QName URL_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "url");
    static final QName DRIVER_Q   = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "driver");
    static final QName USER_Q     = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "user");
    static final QName PASS_Q     = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "password");

    static final QName DSNAME_Q   = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "dsName");
    static final QName ICCLASS_Q  = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "icClass");

    static final QName STMNT_Q    = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "statement");
    static final QName SQL_Q      = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "sql");
    static final QName PARAM_Q    = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "parameter");
    static final QName RESULT_Q   = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "result");

    static final QName ATT_COLUMN = new QName("column");
    static final QName ATT_TYPE   = new QName("type");

    protected void buildDataSource(OMElement elem, AbstractDBMediator mediator) {

        OMElement pool = null;
        // get the 'pool' element and determine if we need to create a DataSource or
        // look up using JNDI
        try {
            AXIOMXPath xpath = new AXIOMXPath("//syn:connection/syn:pool");
            xpath.addNamespace("syn", XMLConfigConstants.SYNAPSE_NAMESPACE);
            pool = (OMElement) xpath.selectSingleNode(elem);

            if (pool.getFirstChildWithName(DRIVER_Q) != null) {
                mediator.setDataSource(createCustomDataSource(pool, mediator));

            } else if (
                pool.getFirstChildWithName(ICCLASS_Q) != null &&
                pool.getFirstChildWithName(DSNAME_Q) != null) {
                mediator.setDataSource(lookupDataSource(pool, mediator));
            } else {
                handleException("The DataSource connection information must be specified for " +
                    "using a custom DataSource connection pool or for a JNDI lookup");
            }

        } catch (JaxenException e) {
            handleException("Error looking up DataSource connection information", e);
        }
    }

    /**
     * Lookup the DataSource on JNDI using the specified properties
     * @param pool the toplevel 'pool' element that holds DataSource information
     * @param mediator the mediator to store properties for serialization
     * @return a DataSource looked up using specified properties
     */
    private DataSource lookupDataSource(OMElement pool, AbstractDBMediator mediator) {

        Hashtable props = new Hashtable();
        // load the minimum required properties
        props.put(Context.INITIAL_CONTEXT_FACTORY, (getValue(pool, ICCLASS_Q)));
        props.put(Context.SECURITY_PRINCIPAL, getValue(pool, USER_Q));
        props.put(Context.SECURITY_CREDENTIALS, getValue(pool, PASS_Q));
        props.put(Context.PROVIDER_URL, getValue(pool, URL_Q));
        String dsName = getValue(pool, DSNAME_Q);

        //save loaded properties for later
        mediator.addDataSourceProperty(ICCLASS_Q, getValue(pool, ICCLASS_Q));
        mediator.addDataSourceProperty(DSNAME_Q,  getValue(pool, DSNAME_Q));
        mediator.addDataSourceProperty(URL_Q,  getValue(pool, URL_Q));
        mediator.addDataSourceProperty(USER_Q, getValue(pool, USER_Q));
        mediator.addDataSourceProperty(PASS_Q, getValue(pool, PASS_Q));

        try {
            Context ctx = new InitialContext(props);
            if (ctx != null) {
                Object ds = ctx.lookup(dsName);
                if (ds != null && ds instanceof DataSource) {
                    return (DataSource) ds;
                } else {
                    handleException("DataSource : " + dsName + " not found when looking up" +
                        " using JNDI properties : " + props);
                }
            } else {
                handleException("Error getting InitialContext using JNDI properties : " + props);
            }
        } catch (NamingException e) {
            handleException("Error looking up DataSource : " + dsName +
                " using JNDI properties : " + props, e);
        }
        return null;
    }

    /**
     * Create a custom DataSource using the specified properties and Apache DBCP
     * @param pool the toplevel 'pool' element that holds DataSource information
     * @param mediator the mediator to store properties for serialization
     * @return a DataSource created using specified properties
     */
    private DataSource createCustomDataSource(OMElement pool, AbstractDBMediator mediator) {

        BasicDataSource ds = new BasicDataSource();

        // load the minimum required properties
        ds.setDriverClassName(getValue(pool, DRIVER_Q));
        ds.setUsername(getValue(pool, USER_Q));
        ds.setPassword(getValue(pool, PASS_Q));
        ds.setUrl(getValue(pool, URL_Q));

        //save loaded properties for later
        mediator.addDataSourceProperty(DRIVER_Q, getValue(pool, DRIVER_Q));
        mediator.addDataSourceProperty(URL_Q,  getValue(pool, URL_Q));
        mediator.addDataSourceProperty(USER_Q, getValue(pool, USER_Q));
        mediator.addDataSourceProperty(PASS_Q, getValue(pool, PASS_Q));

        Iterator props = pool.getChildrenWithName(PROP_Q);
        while (props.hasNext()) {

            OMElement prop = (OMElement) props.next();
            String name  = prop.getAttribute(ATT_NAME).getAttributeValue();
            String value = prop.getAttribute(ATT_VALUE).getAttributeValue();
            // save property for later
            mediator.addDataSourceProperty(name, value);

            if ("autocommit".equals(name)) {
                if ("true".equals(value)) {
                    ds.setDefaultAutoCommit(true);
                } else if ("false".equals(value)) {
                    ds.setDefaultAutoCommit(false);
                }
            } else if ("isolation".equals(name)) {
                try {
                    if ("Connection.TRANSACTION_NONE".equals(value)) {
                        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_NONE);
                    } else if ("Connection.TRANSACTION_READ_COMMITTED".equals(value)) {
                        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    } else if ("Connection.TRANSACTION_READ_UNCOMMITTED".equals(value)) {
                        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                    } else if ("Connection.TRANSACTION_REPEATABLE_READ".equals(value)) {
                        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                    } else if ("Connection.TRANSACTION_SERIALIZABLE".equals(value)) {
                        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                    }
                } catch (NumberFormatException ignore) {}
            } else if ("initialsize".equals(name)) {
                try {
                    ds.setInitialSize(Integer.parseInt(value));
                } catch (NumberFormatException ignore) {}
            } else if ("maxactive".equals(name)) {
                try {
                    ds.setMaxActive(Integer.parseInt(value));
                } catch (NumberFormatException ignore) {}
            } else if ("maxidle".equals(name)) {
                try {
                    ds.setMaxIdle(Integer.parseInt(value));
                } catch (NumberFormatException ignore) {}
            } else if ("maxopenstatements".equals(name)) {
                try {
                    ds.setMaxOpenPreparedStatements(Integer.parseInt(value));
                } catch (NumberFormatException ignore) {}
            } else if ("maxwait".equals(name)) {
                try {
                    ds.setMaxWait(Long.parseLong(value));
                } catch (NumberFormatException ignore) {}
            } else if ("minidle".equals(name)) {
                try {
                    ds.setMinIdle(Integer.parseInt(value));
                } catch (NumberFormatException ignore) {}
            } else if ("poolstatements".equals(name)) {
                if ("true".equals(value)) {
                    ds.setPoolPreparedStatements(true);
                } else if ("false".equals(value)) {
                    ds.setPoolPreparedStatements(false);
                }
            } else if ("testonborrow".equals(name)) {
                if ("true".equals(value)) {
                    ds.setTestOnBorrow(true);
                } else if ("false".equals(value)) {
                    ds.setTestOnBorrow(false);
                }
            } else if ("testonreturn".equals(name)) {
                if ("true".equals(value)) {
                    ds.setTestOnReturn(true);
                } else if ("false".equals(value)) {
                    ds.setTestOnReturn(false);
                }
            } else if ("testwhileidle".equals(name)) {
                if ("true".equals(value)) {
                    ds.setTestWhileIdle(true);
                } else if ("false".equals(value)) {
                    ds.setTestWhileIdle(false);
                }
            } else if ("validationquery".equals(name)) {
                ds.setValidationQuery(value);
            }
        }
        return ds;
    }

    protected void processStatements(OMElement elem, AbstractDBMediator mediator) {

        Iterator iter = elem.getChildrenWithName(STMNT_Q);
        while (iter.hasNext()) {

            OMElement stmntElt = (OMElement) iter.next();
            Statement statement = new Statement(getValue(stmntElt, SQL_Q));

            Iterator paramIter = stmntElt.getChildrenWithName(PARAM_Q);
            while (paramIter.hasNext()) {

                OMElement paramElt = (OMElement) paramIter.next();
                String xpath = getAttribute(paramElt, ATT_EXPRN);
                String value = getAttribute(paramElt, ATT_VALUE);

                if (xpath != null || value != null) {
                    
                    AXIOMXPath xp = null;
                    if (xpath != null) {
                        try {
                            xp = new AXIOMXPath(xpath);
                            OMElementUtils.addNameSpaces(xp, paramElt, log);

                        } catch (JaxenException e) {
                            handleException("Invalid XPath specified for the source attribute : " +
                                    xpath);
                        }
                    }
                    statement.addParameter(
                            value,
                            xp,
                            getAttribute(paramElt, ATT_TYPE));
                }
            }

            Iterator resultIter = stmntElt.getChildrenWithName(RESULT_Q);
            while (resultIter.hasNext()) {

                OMElement resultElt = (OMElement) resultIter.next();
                statement.addResult(
                    getAttribute(resultElt, ATT_NAME),
                    getAttribute(resultElt, ATT_COLUMN));
            }

            mediator.addStatement(statement);
        }
    }

    protected String getValue(OMElement elt, QName qName) {
        OMElement e = elt.getFirstChildWithName(qName);
        if (e != null) {
            return e.getText();
        } else {
            handleException("Unable to read configuration value for : " + qName);
        }
        return null;
    }

    protected String getAttribute(OMElement elt, QName qName) {
        OMAttribute a = elt.getAttribute(qName);
        if (a != null) {
            return a.getAttributeValue();
        }
        return null;
    }
}

