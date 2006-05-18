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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Extension;
import org.apache.synapse.config.SpringConfigExtension;

import javax.xml.namespace.QName;

/**
 * Creates a Spring configuration extension from XML configuration. A Spring
 * configuration extension keeps Spring away from the core of synapse
 */
public class SpringConfigExtensionFactory implements ExtensionFactory {

    private static final Log log = LogFactory.getLog(SpringConfigExtensionFactory.class);

    private static final QName SPRING_CFG_Q = new QName(Constants.SYNAPSE_NAMESPACE + "/spring", "config");

    /**
     * <spring:config name="string" src="file"/>
     *
     * @param elem the XML configuration element
     * @return A named Spring Configuration
     */
    public Extension createExtension(OMElement elem) {

        SpringConfigExtension springCfgExt = null;
        OMAttribute name = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        OMAttribute src  = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "src"));

        if (name == null) {
            handleException("The 'name' attribute is required for a Spring configuration definition");
        } else if (src == null) {
            handleException("The 'src' attribute is required for a Spring configuration definition");
        } else {
            springCfgExt = new SpringConfigExtension(name.getAttributeValue(), src.getAttributeValue());
        }
        return springCfgExt;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public QName getTagQName() {
        return SPRING_CFG_Q;
    }
}
