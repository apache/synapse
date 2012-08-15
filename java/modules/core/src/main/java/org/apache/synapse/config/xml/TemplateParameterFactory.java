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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.TemplateParameter;
import org.apache.synapse.mediators.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A utility class capable of creating instances of TemplateParameter objects by reading
 * through a given XML configuration
 *
 * <pre>
 * &lt;element&gt;
 *    &lt;parameter name="p1" [default="value|expression"] [optional=(true|false)]/&gt;*
 * &lt;/element&gt;
 * </pre>
 */
public class TemplateParameterFactory {
    private static final Log log = LogFactory.getLog(TemplateParameterFactory.class);

     /**
     * Creates a list of parameters in a given template
     *
     * @param elem the OMElement that specifies the template configuration
     * @return the list of TemplateParameter instances created
     */
    public static List<TemplateParameter> getTemplateParameters(OMElement elem) {

        List<TemplateParameter> parameterList = new ArrayList<TemplateParameter>();

        Iterator iter = elem.getChildrenWithName(TemplateParameter.PARAMETER_Q);

        while (iter.hasNext()) {

            OMElement paramEle = (OMElement) iter.next();
            OMAttribute attName = paramEle.getAttribute(TemplateParameter.ATT_NAME_Q);
            OMAttribute attDefault = paramEle.getAttribute(TemplateParameter.ATT_DEFAULT_Q);
            OMAttribute attOptional = paramEle.getAttribute(TemplateParameter.ATT_OPTIONAL_Q);

            TemplateParameter param = new TemplateParameter();

            if (attName == null || attName.getAttributeValue() == null ||
                    attName.getAttributeValue().trim().length() == 0) {
                String msg = "Parameter name is a required attribute for a Template Parameter";
                log.error(msg);
                throw new SynapseException(msg);
            } else {
                param.setName(attName.getAttributeValue());
            }


            if (attDefault == null || attDefault.getAttributeValue() == null ||
                    attDefault.getAttributeValue().trim().length() == 0) {
                String msg = "Default value is not specified for " + param.getName() + " Parameter";
                log.warn(msg);
            } else {
                Value paramValue = new ValueFactory().createValue("default", paramEle);
                param.setDefaultValue(paramValue);
            }

            if (attOptional == null || attOptional.getAttributeValue() == null ||
                    attOptional.getAttributeValue().trim().length() == 0) {
            } else {
                param.setOptional(Boolean.valueOf(attOptional.getAttributeValue()));
            }

            parameterList.add(param);
        }

        return parameterList;
    }

}
