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
package org.apache.synapse.mediators.transform;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Util;
import org.apache.synapse.config.xml.Constants;
import org.apache.synapse.config.xml.AbstractMediatorFactory;
import org.apache.synapse.config.xml.MediatorPropertyFactory;
import org.apache.synapse.mediators.transform.XSLTMediator;
import org.apache.synapse.api.Mediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.commons.schema.XmlSchema;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Creates a XSLT mediator from the given XML
 *
 * <pre>
 * &lt;xslt key="property-key" [source="xpath"]&gt;
 *   &lt;property name="string" (value="literal" | expression="xpath")/&gt;*
 * &lt;/transform&gt;
 * </pre>
 */
public class XSLTMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(XSLTMediatorFactory.class);
    private static final QName TAG_NAME    = new QName(Constants.SYNAPSE_NAMESPACE, "xslt");

    private static final String STR_SCHEMA =
        Constants.SCHEMA_PROLOG +
        "\t<xs:element name=\"transform\" type=\"transform_type\"/>\n" +
        "\t<xs:complexType name=\"transform_type\">\n" +
        "\t\t<xs:sequence minOccurs=\"0\" maxOccurs=\"unbounded\">\n" +
        "\t\t\t<xs:element name=\"property\" type=\"synapse:property_type\"/>\n" +
        "\t\t</xs:sequence>\n" +
        "\t\t<xs:attribute name=\"xslt\" type=\"xs:string\" use=\"required\"/>\n" +
        "\t\t<xs:attribute name=\"source\" type=\"xs:string\"/>\n" +
            "\t</xs:complexType>" +
        Constants.SCHEMA_EPILOG;

    private static final XmlSchema SCHEMA =
        org.apache.synapse.config.xml.Util.getSchema(STR_SCHEMA, TAG_NAME);

    public QName getTagQName() {
        return TAG_NAME;
    }

    public Mediator createMediator(OMElement elem) {

        XSLTMediator transformMediator = new XSLTMediator();

        OMAttribute attXslt   = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
        OMAttribute attSource = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "source"));

        if (attXslt != null) {
            transformMediator.setXsltKey(attXslt.getAttributeValue());
        } else {
            handleException("The 'key' attribute is required for the XSLT mediator");
        }

        if (attSource != null) {
            try {
                AXIOMXPath xp = new AXIOMXPath(attSource.getAttributeValue());
                Util.addNameSpaces(xp, elem, log);
                transformMediator.setSource(xp);

            } catch (JaxenException e) {
                handleException("Invalid XPath specified for the source attribute : " +
                    attSource.getAttributeValue());
            }
        }

        transformMediator.addAllProperties(
            MediatorPropertyFactory.getMediatorProperties(elem));

        return transformMediator;
    }

    public XmlSchema getTagSchema() {
        return SCHEMA;
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}