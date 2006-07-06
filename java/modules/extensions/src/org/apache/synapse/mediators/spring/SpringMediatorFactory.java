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
package org.apache.synapse.mediators.spring;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.spring.SpringConfigExtension;
import org.apache.synapse.config.xml.Constants;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.synapse.config.xml.Util;
import org.apache.synapse.mediators.spring.SpringMediator;
import org.apache.ws.commons.schema.XmlSchema;

import javax.xml.namespace.QName;

/**
 * Creates an instance of a Spring mediator that refers to the given Spring
 * configuration and bean. Optionally, one could specify an inlined Spring
 * configuration as opposed to a globally defined Spring configuration
 * <p/>
 * <spring bean="exampleBean1" (config="spring1" | src="spring.xml)"/>
 */
public class SpringMediatorFactory implements MediatorFactory {

    private static final Log log = LogFactory.getLog(SpringMediatorFactory.class);

    private static final QName TAG_NAME = new QName(Constants.SYNAPSE_NAMESPACE + "/spring", "spring");

    private static final String STR_SCHEMA =
        org.apache.synapse.config.xml.Constants.SCHEMA_PROLOG +
        "\t<xs:element name=\"spring\" type=\"synapse:spring_type\"/>\n" +
        "\t<xs:complexType name=\"spring_type\">\n" +
        "\t\t<xs:attribute name=\"bean\" type=\"xs:string\" use=\"required\"/>\n" +
        "\t\t<xs:attribute name=\"config\" type=\"xs:string\"/>\n" +
        "\t\t<xs:attribute name=\"src\" type=\"xs:string\"/>\n" +
        "\t</xs:complexType>" +
        org.apache.synapse.config.xml.Constants.SCHEMA_EPILOG;

    private static final XmlSchema SCHEMA = Util.getSchema(STR_SCHEMA, TAG_NAME);

    /**
     * Create a Spring mediator instance referring to the bean and configuration given
     * by the OMElement declaration
     *
     * @param elem the OMElement that specifies the Spring mediator configuration
     * @return the Spring mediator instance created
     */
    public Mediator createMediator(OMElement elem) {

        SpringMediator sm = new SpringMediator();
        OMAttribute bean = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "bean"));
        OMAttribute cfg = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "config"));
        OMAttribute src = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "src"));

        if (bean == null) {
            handleException("The 'bean' attribute is required for a Spring mediator definition");
        } else if (cfg == null && src == null) {
            handleException("A 'config' or 'src' attribute is required for a Spring mediator definition");

        } else {
            sm.setBeanName(bean.getAttributeValue());
            if (cfg != null) {
                log.debug("Creating a Spring mediator using configuration named : " + cfg.getAttributeValue());
                sm.setConfigName(cfg.getAttributeValue());

            } else {
                log.debug("Creating an inline Spring configuration using source : " + src.getAttributeValue());
                SpringConfigExtension sce = new SpringConfigExtension("inline", src.getAttributeValue());
                sm.setAppContext(sce.getAppContext());
            }
            return sm;
        }
        return null;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public QName getTagQName() {
        return TAG_NAME;
    }

    public QName getTagSchemaType() {
        return new QName(Constants.SYNAPSE_NAMESPACE,
            getTagQName().getLocalPart() + "_type", "spring");
    }

    public XmlSchema getTagSchema() {
        return SCHEMA;
    }
}
