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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.TemplateParameter;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * A utility class for serializing instances of TemplateParameter objects by reading
 * through a given XML configuration
 *
 * <pre>
 * &lt;element&gt;
 *    &lt;parameter name="p1" [default="value|expression"] [optional=(true|false)]/&gt;*
 * &lt;/element&gt;
 * </pre>
 */
public class TemplateParameterSerializer {
    private static final Log log = LogFactory.getLog(TemplateParameterSerializer.class);

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = SynapseConstants.SYNAPSE_OMNAMESPACE;
    protected static final OMNamespace nullNS
            = fac.createOMNamespace(XMLConfigConstants.NULL_NAMESPACE, "");

    protected static final QName PARAMETER_Q
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "parameter");

    /**
     * Serialize all the parameters to the given parent element. For each and every
     * parameter there will be a separate parameter element created inside the parent element.
     *
     * @param parent element to which parameter elements should be added
     * @param params the list of  TemplateParameter objects
     */
    public static void serializeTemplateParameters(OMElement parent,
                                               Collection<TemplateParameter> params) {
        serializeTemplateParameters(parent, params, PARAMETER_Q);
    }

    /**
     * Serialize all the parameters to the given parent element. For each and every
     * parameter there will be a separate parameter element created inside the parent element.
     *
     * @param parent element to which parameter elements should be added
     * @param params the list of  TemplateParameter objects
     * @param childElementName of the parameter element to be created
     */
    public static void serializeTemplateParameters(OMElement parent,
                                               Collection<TemplateParameter> params,
                                               QName childElementName) {
        for (TemplateParameter tp : params) {
            serializeTemplateParameter(parent,tp, childElementName);
        }
    }

    /**
     * Serialize the parameter to the given parent element. There will be a element created with
     * given configuration inside the parent element.
     *
     * @param parent element to which parameter elements should be added
     * @param tp a property to be serialized
     * @param childElementName of the parameter element to be created
     */
    public static void serializeTemplateParameter(OMElement parent,
                                             TemplateParameter tp,
                                             QName childElementName) {
        OMElement param = fac.createOMElement(childElementName, parent);
        if (tp.getName() != null) {
            param.addAttribute(fac.createOMAttribute("name", nullNS, tp.getName()));
        } else {
            String msg = "Template parameter name missing";
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (tp.getDefaultValue() != null) {
            new ValueSerializer().serializeValue(tp.getDefaultValue(), "default", param);

        if (tp.isOptional()) {
            param.addAttribute(fac.createOMAttribute("optional", nullNS, "true"));
        }
    }
}
}