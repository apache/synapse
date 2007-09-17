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

import org.apache.synapse.config.xml.AbstractMediatorSerializer;
import org.apache.synapse.Mediator;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.synapse.SynapseException;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Map;
import java.sql.Types;

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
public class DBLookupMediatorSerializer extends AbstractMediatorSerializer {

    private static final Log log = LogFactory.getLog(DBLookupMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof DBLookupMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        DBLookupMediator mediator = (DBLookupMediator) m;
        OMElement dbLookup = fac.createOMElement("dblookup", synNS);
        finalizeSerialization(dbLookup,mediator);

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
        dbLookup.addChild(connElt);

        // process queries
        Map queryMap = mediator.getQueryMap();
        iter = queryMap.keySet().iterator();

        while (iter.hasNext()) {

            String attName = (String) iter.next();
            Query query = (Query) queryMap.get(attName);

            OMElement queryElt = fac.createOMElement("query", synNS);
            queryElt.addAttribute(fac.createOMAttribute("setAttribute", nullNS, attName));

            OMElement sqlElt = fac.createOMElement("sql", synNS);
            sqlElt.setText(query.getRawStatement());
            queryElt.addChild(sqlElt);

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
                
                queryElt.addChild(paramElt);
                dbLookup.addChild(queryElt);
            }
        }

        if (parent != null) {
            parent.addChild(dbLookup);
        }
        return dbLookup;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public String getMediatorClassName() {
        return DBLookupMediator.class.getName();
    }
}
