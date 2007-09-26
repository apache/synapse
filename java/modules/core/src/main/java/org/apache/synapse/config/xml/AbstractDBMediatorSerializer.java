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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.mediators.db.Statement;
import org.apache.synapse.mediators.db.AbstractDBMediator;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.sql.Types;

/**
 * <dbreport | dblookup | .. etc>
 *   <connection>
 *     <pool>
 *       <driver/>
 *       <url/>
 *       <user/>
 *       <password/>
 *       <property name="name" value="value"/>*
 *     </pool>
 *   </connection>
 *   <statement>
 *     <sql>insert into table values (?, ?, ..)</sql>
 *     <parameter [value="" | expression=""] type="INTEGER|VARCHAR"/>*
 *     <result name="string" column="int|string"/>*
 *   </statement>+
 * </dbreport | dblookup | .. etc>
 *
 * Supported properties
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
public abstract class AbstractDBMediatorSerializer extends AbstractMediatorSerializer {

    protected void serializeDBInformation(AbstractDBMediator mediator, OMElement dbParent) {

        OMElement connElt = fac.createOMElement("connection", synNS);
        OMElement poolElt = fac.createOMElement("pool", synNS);

        Iterator iter = mediator.getDataSourceProps().keySet().iterator();
        while (iter.hasNext()) {

            Object o = iter.next();
            String value = (String) mediator.getDataSourceProps().get(o);

            if (o instanceof QName) {
                QName name = (QName) o;
                OMElement elt = fac.createOMElement(name);
                elt.setText(value);
                poolElt.addChild(elt);

            } else if (o instanceof String) {
                OMElement elt = fac.createOMElement(AbstractDBMediatorFactory.PROP_Q);
                elt.addAttribute(fac.createOMAttribute("name", nullNS, (String) o));
                elt.addAttribute(fac.createOMAttribute("value", nullNS, value));
                poolElt.addChild(elt);
            }
        }

        connElt.addChild(poolElt);
        dbParent.addChild(connElt);

        // process statements
        Iterator statementIter = mediator.getStatementList().iterator();
        while (statementIter.hasNext()) {

            Statement statement = (Statement) statementIter.next();
            OMElement stmntElt = fac.createOMElement(AbstractDBMediatorFactory.STMNT_Q);

            OMElement sqlElt = fac.createOMElement(AbstractDBMediatorFactory.SQL_Q);
            sqlElt.setText(statement.getRawStatement());
            stmntElt.addChild(sqlElt);

            // serialize parameters of the statement
            for (Iterator it = statement.getParameters().iterator(); it.hasNext(); ) {

                Statement.Parameter param = (Statement.Parameter) it.next();
                OMElement paramElt = fac.createOMElement(AbstractDBMediatorFactory.PARAM_Q);

                if (param.getPropertyName() != null) {
                    paramElt.addAttribute(
                        fac.createOMAttribute("value", nullNS, param.getPropertyName()));
                }
                if (param.getXpath() != null) {
                    paramElt.addAttribute(
                        fac.createOMAttribute("expression", nullNS, param.getXpath().toString()));
                    serializeNamespaces(paramElt, param.getXpath());
                }

                switch (param.getType()) {
                    case Types.VARCHAR: {
                        paramElt.addAttribute(fac.createOMAttribute("type", nullNS, "string"));
                        break;
                    }
                    case Types.INTEGER: {
                        paramElt.addAttribute(fac.createOMAttribute("type", nullNS, "int"));
                        break;
                    }
                    default:
                        // TODO handle
                }

                stmntElt.addChild(paramElt);
            }

            // serialize any optional results of the statement
            for (Iterator it = statement.getResultsMap().keySet().iterator(); it.hasNext(); ) {

                String name = (String) it.next();
                String columnStr = (String) statement.getResultsMap().get(name);

                OMElement resultElt = fac.createOMElement(AbstractDBMediatorFactory.RESULT_Q);

                resultElt.addAttribute(
                    fac.createOMAttribute("name", nullNS, columnStr));
                resultElt.addAttribute(
                    fac.createOMAttribute("column", nullNS, columnStr));

                stmntElt.addChild(resultElt);
            }

            dbParent.addChild(stmntElt);
        }
    }
}
