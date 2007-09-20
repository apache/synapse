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

import org.apache.synapse.config.xml.AbstractMediatorSerializer;
import org.apache.synapse.Mediator;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.db.DBLookupMediator;
import org.apache.synapse.mediators.db.Statement;

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
public class DBLookupMediatorSerializer extends AbstractDBMediatorSerializer {

    private static final Log log = LogFactory.getLog(DBLookupMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof DBLookupMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        DBLookupMediator mediator = (DBLookupMediator) m;
        OMElement dbLookup = fac.createOMElement("dblookup", synNS);
        finalizeSerialization(dbLookup,mediator);
        serializeDBInformation(mediator, dbLookup);

        if (parent != null) {
            parent.addChild(dbLookup);
        }
        return dbLookup;
    }

    public String getMediatorClassName() {
        return DBLookupMediator.class.getName();
    }
}
