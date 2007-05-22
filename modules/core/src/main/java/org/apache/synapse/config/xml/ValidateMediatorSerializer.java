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

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.ValidateMediator;
import org.apache.synapse.config.xml.MediatorSerializer;
import org.apache.synapse.config.xml.AbstractListMediatorSerializer;

import java.util.Iterator;

/**
 * <validate [source="xpath"]>
 *   <schema key="string">+
 *   <property name="<validation-feature-id>" value="true|false"/> *
 *   <on-fail>
 *     mediator+
 *   </on-fail>
 * </validate>
 */
public class ValidateMediatorSerializer extends AbstractListMediatorSerializer
    implements MediatorSerializer {

    private static final Log log = LogFactory.getLog(ValidateMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof ValidateMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        ValidateMediator mediator = (ValidateMediator) m;
        OMElement validate = fac.createOMElement("validate", synNS);
        finalizeSerialization(validate,mediator);

        if (mediator.getSource() != null) {
            validate.addAttribute(fac.createOMAttribute(
                "source", nullNS, mediator.getSource().toString()));
            serializeNamespaces(validate, mediator.getSource());
        }

        Iterator iter = mediator.getSchemaKeys().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            OMElement schema = fac.createOMElement("schema", synNS, validate);
            schema.addAttribute(fac.createOMAttribute("key", nullNS, key));
        }

        serializeProperties(validate, mediator.getProperties());

        OMElement onFail = fac.createOMElement("on-fail", synNS, validate);
        serializeChildren(onFail, mediator.getList());        

        if (parent != null) {
            parent.addChild(validate);
        }
        return validate;
    }

    public String getMediatorClassName() {
        return ValidateMediator.class.getName();
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
