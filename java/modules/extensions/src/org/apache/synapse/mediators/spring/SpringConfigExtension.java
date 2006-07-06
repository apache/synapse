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

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.apache.synapse.config.Extension;

/**
 * This defines an extension to Synapse to process a Spring Configuration.
 * This keeps the Spring dependency out from the Synapse core, and the
 * dependent Jars from the core distribution.
 *
 * A Spring configuration is usually named, but this class allows an
 * inlined configuration to be built up as well, where the Spring mediator
 * defines an inline Spring configuration
 */
public class SpringConfigExtension implements Extension {

    /**
     * The name of this Spring configuration
     */
    private String name = null;

    /**
     * This is the Spring ApplicationContext/BeanFactory
     */
    private GenericApplicationContext appContext = null;

    /**
     * Create a Spring configuration from the given configuration
     *
     * @param configFile the configuration file to be used
     */
    public SpringConfigExtension(String name, String configFile) {
        setName(name);
        appContext = new GenericApplicationContext();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(appContext);
        xbdr.setValidating(false);
        xbdr.loadBeanDefinitions(new FileSystemResource(configFile));
        appContext.refresh();
    }

    public GenericApplicationContext getAppContext() {
        return appContext;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
