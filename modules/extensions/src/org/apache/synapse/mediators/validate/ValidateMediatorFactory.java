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
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.validate.ValidateMediator;
import org.apache.ws.commons.schema.XmlSchema;
import org.jaxen.JaxenException;

import java.util.Iterator;
import javax.xml.namespace.QName;

/**
 * Creates a validation mediator from the XML configuration
 * <p/>
 * <validate schema="url" [source="xpath"]>
 *   <property name="<validation-feature-id>" value="true|false"/> *
 *   <on-fail>
 *     mediator+
 *   </on-fail>
 * </validate>
 */
public class ValidateMediatorFactory extends AbstractListMediatorFactory {

    private static final Log log = LogFactory.getLog(ValidateMediatorFactory.class);

    private static final QName VALIDATE_Q = new QName(Constants.SYNAPSE_NAMESPACE, "validate");
    private static final QName ON_FAIL_Q = new QName(Constants.SYNAPSE_NAMESPACE, "on-fail");
    private static final QName SCHEMA_Q = new QName(Constants.NULL_NAMESPACE, "schema");
    private static final QName SOURCE_Q = new QName(Constants.NULL_NAMESPACE, "source");

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
        OMAttribute attSchema = elem.getAttribute(SCHEMA_Q);
        OMAttribute attSource = elem.getAttribute(SOURCE_Q);

        if (attSchema != null) {
            validateMediator.setSchemaUrl(attSchema.getAttributeValue());
        } else {
            String msg = "The 'schema' attribute is required for the validate mediator configuration";
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (attSource != null) {
            try {
                AXIOMXPath xp = new AXIOMXPath(attSource.getAttributeValue());
                validateMediator.setSource(xp);
                Util.addNameSpaces(xp, elem, log);
            } catch (JaxenException e) {
                String msg = "Invalid XPath expression specified for attribute 'source'";
                log.error(msg);
                throw new SynapseException(msg, e);
            }
        }

        OMElement onFail = null;
        Iterator iter = elem.getChildrenWithName(ON_FAIL_Q);
        if (iter.hasNext()) {
            onFail = (OMElement)iter.next();
        }

        if (onFail != null && onFail.getChildElements().hasNext()) {
            super.addChildren(onFail, validateMediator);
        } else {
            String msg = "A non-empty <on-fail> child element is required for the <validate> mediator";
            log.error(msg);
            throw new SynapseException(msg);
        }

        validateMediator.addAllProperties(
            MediatorPropertyFactory.getMediatorProperties(elem));

        return validateMediator;
    }

    public QName getTagQName() {
        return VALIDATE_Q;
    }

    public XmlSchema getTagSchema() {
        return SCHEMA;
    }
}
