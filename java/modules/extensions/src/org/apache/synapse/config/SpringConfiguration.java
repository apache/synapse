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
package org.apache.synapse.config;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;
import org.apache.synapse.config.xml.SpringConfigurationFactory;

public class SpringConfiguration extends Configuration {

    /** This is the Spring ApplicationContext/BeanFactory */
    GenericApplicationContext appContext = null;

    public static final String SPRING_TYPE = "spring";

    /**
     * Create a Spring configuration from the given configuration
     * @param configFile the configuration file to be used
     */
    public SpringConfiguration(String name, String configFile) {
        super.setName(name);
        super.setType(SPRING_TYPE);
        appContext = new GenericApplicationContext();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(appContext);
        xbdr.setValidating(false);
        xbdr.loadBeanDefinitions(new FileSystemResource(configFile));
        appContext.refresh();
    }

    public GenericApplicationContext getAppContext() {
        return appContext;
    }

}
