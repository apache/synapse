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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.template.TemplateMediator;

import javax.xml.namespace.QName;
import java.util.Properties;

/**
 * Factory class for Template configuration as follows
 * <template name="simple_func">
	    <parameter name="p1" [default="value|expression"] [optional=(true|false)]/>
        <parameter name="p2" [default="value|expression"] [optional=(true|false)]/>*
        <mediator/>+
    </template>
 */
public class TemplateMediatorFactory extends AbstractListMediatorFactory {

    private static final QName TEMPLATE_Q
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "template");
    private static final QName TEMPLATE_BODY_Q
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "sequence");

    protected Mediator createSpecificMediator(OMElement elem, Properties properties) {
        TemplateMediator templateTemplateMediator = new TemplateMediator();
        OMAttribute nameAttr = elem.getAttribute(ATT_NAME);
        if (nameAttr != null) {
            templateTemplateMediator.setName(nameAttr.getAttributeValue());
            processAuditStatus(templateTemplateMediator, elem);
            //set template parameters
            templateTemplateMediator.setParameters(TemplateParameterFactory.getTemplateParameters(elem));
            OMElement templateBodyElem = elem.getFirstChildWithName(TEMPLATE_BODY_Q);
            addChildren(templateBodyElem, templateTemplateMediator, properties);
        } else {
            String msg = "An EIP template must be a named mediator .";
            log.error(msg);
            throw new SynapseException(msg);
        }
        return templateTemplateMediator;
    }

    public QName getTagQName() {
        return TEMPLATE_Q;
    }

}
