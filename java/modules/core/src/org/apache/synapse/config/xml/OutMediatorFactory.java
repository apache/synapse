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

import org.apache.axiom.om.OMElement;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.filters.OutMediator;
import org.apache.ws.commons.schema.XmlSchema;

import javax.xml.namespace.QName;

/**
 * Creates an Out mediator instance
 *
 * <pre>
 * &lt;out&gt;
 *    mediator+
 * &lt;/out&gt;
 * </pre>
 */
public class OutMediatorFactory extends AbstractListMediatorFactory {

    private static final QName OUT_Q = new QName(Constants.SYNAPSE_NAMESPACE, "out");

    private static final String STR_SCHEMA =
        Constants.SCHEMA_PROLOG +
        "\t<xs:element name=\"out\" type=\"out_type\"/>\n" +
        "\t<xs:complexType name=\"out_type\">\n" +
        "\t\t<xs:complexContent>\n" +
        "\t\t\t<xs:extension base=\"synapse:mediator_type\"/>\n" +
        "\t\t</xs:complexContent>\n" +
        "\t</xs:complexType>" +
        Constants.SCHEMA_EPILOG;

    private static final XmlSchema SCHEMA =
        org.apache.synapse.config.xml.Util.getSchema(STR_SCHEMA, OUT_Q);

    public Mediator createMediator(OMElement elem) {
        OutMediator filter = new OutMediator();
        super.addChildren(elem, filter);
        return filter;
    }

    public QName getTagQName() {
        return OUT_Q;
    }

    public XmlSchema getTagSchema() {
        return SCHEMA;
    }
}
