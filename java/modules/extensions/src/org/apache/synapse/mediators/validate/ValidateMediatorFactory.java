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
package org.apache.synapse.mediators.validate;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Util;
import org.apache.synapse.config.xml.AbstractListMediatorFactory;
import org.apache.synapse.config.xml.Constants;
import org.apache.synapse.config.xml.MediatorPropertyFactory;
import org.apache.synapse.config.DynamicProperty;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.validate.ValidateMediator;
import org.apache.ws.commons.schema.XmlSchema;
import org.jaxen.JaxenException;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import javax.xml.namespace.QName;

/**
 * Creates a validation mediator from the XML configuration
 * <p/>
 * <validate [source="xpath"]>
 *   <schema key="string">+
 *   <property name="<validation-feature-id>" value="true|false"/> *
 *   <on-fail>
 *     mediator+
 *   </on-fail>
 * </validate>
 */
public class ValidateMediatorFactory extends AbstractListMediatorFactory {

    private static final Log log = LogFactory.getLog(ValidateMediatorFactory.class);

    private static final QName VALIDATE_Q = new QName(Constants.SYNAPSE_NAMESPACE, "validate");
    private static final QName ON_FAIL_Q  = new QName(Constants.SYNAPSE_NAMESPACE, "on-fail");
    private static final QName SCHEMA_Q   = new QName(Constants.SYNAPSE_NAMESPACE, "schema");
    private static final QName KEY_Q      = new QName(Constants.NULL_NAMESPACE, "key");
    private static final QName SOURCE_Q   = new QName(Constants.NULL_NAMESPACE, "source");

    private static final String STR_SCHEMA =
        Constants.SCHEMA_PROLOG +
        "\t<xs:element name=\"validate\" type=\"validate_type\"/>\n" +
        "\t<xs:complexType name=\"validate_type\">\n" +
        "\t\t<xs:sequence minOccurs=\"0\" maxOccurs=\"unbounded\">\n" +
        "\t\t\t<xs:element name=\"property\" type=\"synapse:property_type\"/>\n" +
        "\t\t</xs:sequence>\n" +
        "\t\t<xs:element name=\"on-fail\" type=\"on-fail_type\"/>" +
        "\t\t<xs:attribute name=\"url\" type=\"xs:string\" use=\"required\"/>\n" +
        "\t\t<xs:attribute name=\"source\" type=\"xs:string\"/>\n" +
        "\t</xs:complexType>" +
        "\t<xs:complexType name=\"on-fail_type\">\n" +
        "\t\t<xs:complexContent>\n" +
        "\t\t\t<xs:extension base=\"synapse:mediator_type\"/>\n" +
        "\t\t</xs:complexContent>\n" +
        "\t</xs:complexType>" +
        Constants.SCHEMA_EPILOG;

    private static final XmlSchema SCHEMA =
        org.apache.synapse.config.xml.Util.getSchema(STR_SCHEMA, VALIDATE_Q);


    public Mediator createMediator(OMElement elem) {

        ValidateMediator validateMediator = new ValidateMediator();

        // process schema element definitions and create DynamicProperties
        List schemaKeys = new ArrayList();
        Iterator schemas = elem.getChildrenWithName(SCHEMA_Q);

        while (schemas.hasNext()) {
            Object o = schemas.next();
            if (o instanceof OMElement) {
                OMElement omElem = (OMElement) o;
                OMAttribute keyAtt = omElem.getAttribute(KEY_Q);
                if (keyAtt != null) {
                    schemaKeys.add(new DynamicProperty(keyAtt.getAttributeValue()));
                } else {
                    handleException("A 'schema' definition must contain the registry 'key'");
                }
            } else {
                handleException("Invalid 'schema' declaration for validate mediator");
            }
        }

        if (schemaKeys.size() == 0) {
            handleException("No schemas specified for the validate mediator");
        } else {
            validateMediator.setSchemaKeys(schemaKeys);
        }

        // process source XPath attribute if present
        OMAttribute attSource = elem.getAttribute(SOURCE_Q);

        if (attSource != null) {
            try {
                AXIOMXPath xp = new AXIOMXPath(attSource.getAttributeValue());
                validateMediator.setSource(xp);
                Util.addNameSpaces(xp, elem, log);
            } catch (JaxenException e) {
                handleException("Invalid XPath expression specified for attribute 'source'", e);
            }
        }

        // process on-fail
        OMElement onFail = null;
        Iterator iter = elem.getChildrenWithName(ON_FAIL_Q);
        if (iter.hasNext()) {
            onFail = (OMElement)iter.next();
        }

        if (onFail != null && onFail.getChildElements().hasNext()) {
            super.addChildren(onFail, validateMediator);
        } else {
            handleException("A non-empty <on-fail> child element is required for " +
                "the <validate> mediator");
        }

        // process properties
        validateMediator.addAllProperties(
            MediatorPropertyFactory.getMediatorProperties(elem));

        return validateMediator;
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public QName getTagQName() {
        return VALIDATE_Q;
    }

    public XmlSchema getTagSchema() {
        return SCHEMA;
    }
}
