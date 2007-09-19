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

import org.apache.synapse.config.xml.AbstractMediatorFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

/**
 * <dblookup>
 *   <connection>
 *     <jdbc>
 *       <driver/>
 *       <url/>
 *       <user/>
 *       <password/>
 *     </jdbc>
 *   </connection>
 *   <query setAttribute="queue_name">
 *     <sql>select queue from table where device_id = ?</sql>
 *     <parameter [property="" | xpath=""] type="int|string"/>*
 *   </query>+
 * </dblookup>
 */
public class DBLookupMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(DBLookupMediatorFactory.class);

    private static final QName DBLOOKUP_Q = new QName(SynapseConstants.SYNAPSE_NAMESPACE, "dblookup");

    private static final QName DRIVER_Q   =
        new QName(org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "driver");
    private static final QName URL_Q      =
        new QName(org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "url");
    private static final QName USER_Q     =
        new QName(org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "user");
    private static final QName PASS_Q     =
        new QName(org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "password");

    private static final QName QUERY_Q    =
        new QName(org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "query");
    private static final QName Q_ATT_Q    = new QName("setAttribute");
    private static final QName SQL_Q      =
        new QName(org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "sql");
    private static final QName PARAM_Q    =
        new QName(org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "parameter");
    private static final QName PROPERTY_Q = new QName("property");
    private static final QName XPATH_Q    = new QName("xpath");
    private static final QName TYPE_Q     = new QName("type");

    public Mediator createMediator(OMElement elem) {

        DBLookupMediator mediator = new DBLookupMediator();
        Connection conn = null;

        try {
            AXIOMXPath xpath = new AXIOMXPath("//syn:connection/syn:jdbc");
            xpath.addNamespace("syn", org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE);
            OMElement jdbc = (OMElement) xpath.selectSingleNode(elem);

            try {
                Class.forName(getValue(jdbc, DRIVER_Q));
                conn = DriverManager.getConnection(
                            getValue(jdbc, URL_Q), getValue(jdbc, USER_Q), getValue(jdbc, PASS_Q));
                mediator.setConn(conn);
                mediator.addJDBCProperty(DRIVER_Q, getValue(jdbc, DRIVER_Q));
                mediator.addJDBCProperty(URL_Q,  getValue(jdbc, URL_Q));
                mediator.addJDBCProperty(USER_Q, getValue(jdbc, USER_Q));
                mediator.addJDBCProperty(PASS_Q, getValue(jdbc, PASS_Q));

            } catch (SQLException e) {
                handleException("Error connecting to Database using : " + jdbc, e);
            } catch (ClassNotFoundException e) {
                handleException("Error loading JDBC driver using : " + jdbc, e);
            }

        } catch (JaxenException e) {
            // in future handle connection pools and data sources, but for now fail if not jdbc
            handleException("JDBC Database connection information must be specified");
        }

        Iterator iter = elem.getChildrenWithName(QUERY_Q);
        while (iter.hasNext()) {

            OMElement qryElt = (OMElement) iter.next();
            Query query = new Query();
            if (qryElt.getAttribute(Q_ATT_Q) != null) {
                try {
                    query.setStatement(conn.prepareStatement(getValue(qryElt, SQL_Q)));
                    query.setRawStatement(getValue(qryElt, SQL_Q));
                } catch (SQLException e) {
                    handleException("Invalid SQL query for Lookup : " + getValue(qryElt, SQL_Q), e);
                }

                Iterator paramIter = qryElt.getChildrenWithName(PARAM_Q);
                while (paramIter.hasNext()) {

                    OMElement paramElt = (OMElement) paramIter.next();
                    try {
                        query.addParameter(
                            getAttribute(paramElt, PROPERTY_Q),
                            getAttribute(paramElt, XPATH_Q),
                            getAttribute(paramElt, TYPE_Q));
                    } catch (JaxenException e) {
                        handleException("Invalid XPath expression for query : "
                            + getAttribute(paramElt, XPATH_Q));
                    }
                }
                mediator.addQuery(qryElt.getAttributeValue(Q_ATT_Q), query);
            }
        }

        return mediator;
    }

    private String getValue(OMElement elt, QName qName) {
        OMElement e = elt.getFirstChildWithName(qName);
        if (e != null) {
            return e.getText();
        } else {
            handleException("Unable to read configuration value for : " + qName);
        }
        return null;
    }

    private String getAttribute(OMElement elt, QName qName) {
        OMAttribute a = elt.getAttribute(qName);
        if (a != null) {
            return a.getAttributeValue();
        }
        return null;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    public QName getTagQName() {
        return DBLOOKUP_Q;
    }
}
