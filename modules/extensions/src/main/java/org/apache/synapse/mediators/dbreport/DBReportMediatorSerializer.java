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

import org.apache.synapse.config.xml.AbstractMediatorSerializer;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.dblookup.Query;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;
import java.sql.Types;

/**
 * <dbreport>
 *   <connection>
 *     <jdbc>
 *       <driver/>
 *       <url/>
 *       <user/>
 *       <password/>
 *     </jdbc>
 *   </connection>
 *   <insert>
 *     <sql>insert into table values (?, ?, ..)</sql>
 *     <parameter [property="" | xpath=""] type="int|string"/>*
 *   </insert>+
 * </dbreport>
 */
public class DBReportMediatorSerializer extends AbstractMediatorSerializer {

    private static final Log log = LogFactory.getLog(DBReportMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof DBReportMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        DBReportMediator mediator = (DBReportMediator) m;
        OMElement dbReport = fac.createOMElement("dbreport", synNS);
        finalizeSerialization(dbReport, mediator);

        // process jdbc info
        OMElement connElt = fac.createOMElement("connection", synNS);
        OMElement jdbcElt = fac.createOMElement("jdbc", synNS);

        Iterator iter = mediator.getJdbcProps().keySet().iterator();
        while (iter.hasNext()) {
            QName name = (QName) iter.next();
            OMElement elt = fac.createOMElement(name);
            elt.setText((String) mediator.getJdbcProps().get(name));
            jdbcElt.addChild(elt);
        }
        connElt.addChild(jdbcElt);
        dbReport.addChild(connElt);

        // process queries
        iter = mediator.getQueryList().iterator();

        while (iter.hasNext()) {
            Query query = (Query) iter.next();

            OMElement insertElt = fac.createOMElement("insert", synNS);

            OMElement sqlElt = fac.createOMElement("sql", synNS);
            sqlElt.setText(query.getRawStatement());
            insertElt.addChild(sqlElt);

            for (Iterator it = query.getParameters().iterator(); it.hasNext(); ) {

                Query.Parameter param = (Query.Parameter) it.next();
                OMElement paramElt = fac.createOMElement("parameter", synNS);

                if (param.getPropertyName() != null) {
                    paramElt.addAttribute(
                        fac.createOMAttribute("property", nullNS, param.getPropertyName()));
                }
                if (param.getXpath() != null) {
                    paramElt.addAttribute(
                        fac.createOMAttribute("xpath", nullNS, param.getXpath().toString()));
                    super.serializeNamespaces(paramElt, param.getXpath());
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
                insertElt.addChild(paramElt);
            }
            dbReport.addChild(insertElt);
        }

        if (parent != null) {
            parent.addChild(dbReport);
        }
        return dbReport;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public String getMediatorClassName() {
        return DBReportMediator.class.getName();
    }
}
