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
import org.apache.synapse.config.Configuration;
import org.apache.synapse.config.SpringConfiguration;

import javax.xml.namespace.QName;
import java.util.Iterator;

public class SpringConfigurationFactory implements ConfigurationFactory {

    private static final Log log = LogFactory.getLog(SpringConfigurationFactory.class);

    private static final String CONFIG_NAME = "config_name";

    /**
     * <configuration name="string" type="spring">
     *    <property name="config_src" value="string"/>
     *    <property>*
     * </configuration>
     *
     * @param elem
     * @return A named Spring Configuration
     */
    public Configuration createConfiguration(OMElement elem) {

        Iterator iter = elem.getChildrenWithName(new QName(Constants.NULL_NAMESPACE, "property"));
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof OMElement) {
                OMElement prop = (OMElement) o;
                OMAttribute name = prop.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
                OMAttribute value = prop.getAttribute(new QName(Constants.NULL_NAMESPACE, "value"));

                if (name != null && value != null && CONFIG_NAME.equals(name.getAttributeValue())) {
                    return new SpringConfiguration(name.getAttributeValue(), value.getAttributeValue());
                }
            }
        }
        return null;
    }

    public String getType() {
        return SpringConfiguration.SPRING_TYPE;
    }
}
