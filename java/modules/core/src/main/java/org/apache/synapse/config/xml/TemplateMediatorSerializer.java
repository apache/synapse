/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.template.TemplateMediator;

import java.util.Collection;
import java.util.Iterator;

/**
 *  Serializer class for Template to serialize into a  configuration as follows
 * <template name="simple_func">
	    <parameter name="p1"/>
        <parameter name="p2"/>*
        <mediator/>+
    </template>
 */
public class TemplateMediatorSerializer extends AbstractListMediatorSerializer {
    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {
        if (!(m instanceof TemplateMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        TemplateMediator mediator = (TemplateMediator) m;
        OMElement templateElem = fac.createOMElement("template", synNS);

        if (mediator.getName() != null) {
            templateElem.addAttribute(fac.createOMAttribute(
                    "name", nullNS, mediator.getName()));

            if (mediator.getErrorHandler() != null) {
                templateElem.addAttribute(fac.createOMAttribute(
                        "onError", nullNS, mediator.getErrorHandler()));
            }
            serializeParams(templateElem,mediator);
            saveTracingState(templateElem, mediator);
            serializeChildren(templateElem, mediator.getList());
        }

        return templateElem;
    }

    private void serializeParams(OMElement templateElem, TemplateMediator mediator) {
        Collection<String> params = mediator.getParameters();
        Iterator<String> paramIterator = params.iterator();
        while (paramIterator.hasNext()){
            String paramName = paramIterator.next();
            if(!"".equals(paramName)){
                OMElement paramEl = fac.createOMElement("parameter", synNS);
                paramEl.addAttribute(fac.createOMAttribute("name",nullNS,paramName));
                templateElem.addChild(paramEl);
            }
        }
    }

    public String getMediatorClassName() {
        return TemplateMediator.class.getName();
    }
}
