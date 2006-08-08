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
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;

import javax.xml.namespace.QName;

/**
 * Build an Apache Rampart OutflowSecurity Parameter using the given
 * XML fragment from a Synapse configuration file.
 */
public class RampartSecurityBuilder {

    private static final Log log = LogFactory.getLog(RampartSecurityBuilder.class);

    /**
     * Return a Rampart OutflowSecurity 'Parameter', by scanning the children of the
     * given element.
     * @param elem the source element to be used
     * @return a Rampart OutflowSecurity 'Parameter'
     */
    public static Parameter getSecurityParameter(OMElement elem, String name) {
        OMElement paramElt = elem.getFirstChildWithName(
            new QName(Constants.NULL_NAMESPACE, "parameter"));
        if (paramElt != null) {
            Parameter param = new Parameter();
            param.setParameterElement(paramElt);
            param.setValue(paramElt);
            param.setName(name);
            return param;
        } else {
            return null;
        }
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
