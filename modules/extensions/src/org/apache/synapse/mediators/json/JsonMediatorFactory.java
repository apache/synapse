/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.mediators.json;

import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.json.JsonMediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.Constants;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.synapse.config.xml.Util;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.commons.schema.XmlSchema;

import javax.xml.namespace.QName;

/**
 * Creates an instance of JsonMediator.
 * <x:json/> mediator belongs to the http://ws.apache.org/ns/synapse/json namespace.
 * <p/>
 * <x:json (direction="JTX"|"XTJ)"/>
 * JTX is Json to XML
 * XTJ is XML to Json
 */
public class JsonMediatorFactory implements MediatorFactory {

    private static final Log log = LogFactory.getLog(JsonMediatorFactory.class);

    private static final QName TAG_NAME = new QName(Constants.SYNAPSE_NAMESPACE+"/json", "json");

    private static final String STR_SCHEMA =
        org.apache.synapse.config.xml.Constants.SCHEMA_PROLOG +
        "\t<xs:element name=\"json\" type=\"synapse:json_type\"/>\n" +
        "\t<xs:complexType name=\"json_type\">\n" +
        "\t\t<xs:attribute name=\"direction\" type=\"xs:string\"/>" +
        "\t</xs:complexType>" +
        org.apache.synapse.config.xml.Constants.SCHEMA_EPILOG;

    private static final XmlSchema SCHEMA = Util.getSchema(STR_SCHEMA, TAG_NAME);

    public Mediator createMediator(OMElement elem) {
        JsonMediator jsonMediator = new JsonMediator();
        OMAttribute direction = elem.getAttribute(new QName("direction"));
        if (direction == null) {
            handleException("JSON element doesnot contain 'direction' attribute.");
        }else {
            jsonMediator.setDirection(direction.getAttributeValue().trim());
        }
        return jsonMediator;
    }

    public QName getTagQName() {
        return TAG_NAME;
    }

    public QName getTagSchemaType() {
        return new QName(Constants.SYNAPSE_NAMESPACE,
            getTagQName().getLocalPart() + "_type", "json");
    }

    public XmlSchema getTagSchema() {
        return SCHEMA;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
