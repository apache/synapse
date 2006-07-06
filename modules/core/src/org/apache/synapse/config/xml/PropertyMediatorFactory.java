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
package org.apache.synapse.config.xml;

import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.builtin.PropertyMediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Util;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.commons.schema.XmlSchema;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;

/**
 * Creates a set-property mediator through the supplied XML configuration
 *
 * <pre>
 * &lt;set-property name="string" (value="literal" | expression="xpath")/&gt;
 * </pre>
 */
public class PropertyMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(LogMediatorFactory.class);

    private static final QName PROP_Q    = new QName(Constants.SYNAPSE_NAMESPACE, "set-property");

    private static final String STR_SCHEMA =
        Constants.SCHEMA_PROLOG +
        "\t<xs:element name=\"set-property\" type=\"set-property_type\"/>\n" +
        "\t<xs:complexType name=\"set-property_type\">\n" +
        "\t\t<xs:attribute name=\"name\"/>\n" +
        "\t\t<xs:attribute name=\"value\"/>\n" +
        "\t\t<xs:attribute name=\"expression\"/>\n" +
        "\t</xs:complexType>" +
        Constants.SCHEMA_EPILOG;

    private static final XmlSchema SCHEMA =
        org.apache.synapse.config.xml.Util.getSchema(STR_SCHEMA, PROP_Q);

    public Mediator createMediator(OMElement elem) {

        PropertyMediator propMediator = new PropertyMediator();
        OMAttribute name = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        OMAttribute value = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "value"));
        OMAttribute expression = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "expression"));

        if (name == null) {
            String msg = "The 'name' attribute is required for the configuration of a property mediator";
            log.error(msg);
            throw new SynapseException(msg);
        } else if (value == null && expression == null) {
            String msg = "Either an 'value' or 'expression' attribute is required for a property mediator";
            log.error(msg);
            throw new SynapseException(msg);
        }

        propMediator.setName(name.getAttributeValue());
        if (value != null) {
            propMediator.setValue(value.getAttributeValue());
        } else {
            try {
                AXIOMXPath xp = new AXIOMXPath(expression.getAttributeValue());
                Util.addNameSpaces(xp, elem, log);
                propMediator.setExpression(xp);

            } catch (JaxenException e) {
                String msg = "Invalid XPath expression for attribute 'expression' : " + expression.getAttributeValue();
                log.error(msg);
                throw new SynapseException(msg);
            }
        }

        return propMediator;
    }

    public QName getTagQName() {
        return PROP_Q;
    }

    public XmlSchema getTagSchema() {
        return SCHEMA;
    }
}
