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

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.synapse.config.xml.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.ext.ClassMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;

import java.util.Iterator;
import java.io.StringReader;

/**
 * Creates an instance of a Class mediator using XML configuration specified
 *
 * <pre>
 * &lt;class name="class-name"&gt;
 *   &lt;property name="string" (value="literal" | expression="xpath")/&gt;*
 * &lt;/class&gt;
 * </pre>
 */
public class ClassMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(LogMediatorFactory.class);

    private static final QName CLASS_Q = new QName(Constants.SYNAPSE_NAMESPACE, "class");

    private static final String STR_SCHEMA =
        Constants.SCHEMA_PROLOG +
        "\t<xs:element name=\"class\" type=\"class_type\"/>\n" +
        "\t<xs:complexType name=\"class_type\">\n" +
        "\t\t<xs:sequence minOccurs=\"0\" maxOccurs=\"unbounded\">\n" +
        "\t\t\t<xs:element name=\"property\" type=\"synapse:property_type\"/>\n" +
        "\t\t</xs:sequence>\n" +
        "\t\t<xs:attribute name=\"name\"/>\n" +
        "\t</xs:complexType>" +
        Constants.SCHEMA_EPILOG;

    private static final XmlSchema SCHEMA = Util.getSchema(STR_SCHEMA, CLASS_Q);        

    public Mediator createMediator(OMElement elem) {

        ClassMediator classMediator = new ClassMediator();

        OMAttribute name = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        if (name == null) {
            String msg = "The name of the actual mediator class is a required attribute";
            log.error(msg);
            throw new SynapseException(msg);
        }

        try {
            Class clazz = getClass().getClassLoader().loadClass(name.getAttributeValue());
            classMediator.setClazz(clazz);
        } catch (ClassNotFoundException e) {
            String msg = "Cannot find class : " + name.getAttributeValue();
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }

        classMediator.addAllProperties(MediatorPropertyFactory.getMediatorProperties(elem));

        return classMediator;
    }


    public QName getTagQName() {
        return CLASS_Q;
    }

    public XmlSchema getTagSchema() {
        return SCHEMA;
    }
}
