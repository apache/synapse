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
package org.apache.synapse.mediators.ext.spring;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Configuration;
import org.apache.synapse.config.SpringConfiguration;
import org.apache.synapse.api.Mediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

/**
 * This mediator allows
 * <p> This class is the class that "plugs" Spring-based mediators into Synapse.
 * <p> A spring based mediator is any object that implements mediator and can be instantiated by
 * Spring (see www.springframework.org). The mediator definition is set up using the 
 *  SpringMediatorProcessorConfigurator class.
 *  
 * This class simply has a Context property which is set with a Spring GenericApplicationContext and 
 * a BeanName property, which is set with the name of the bean  
 *
 */
public class SpringMediator implements Mediator {

    private static final Log log = LogFactory.getLog(SpringMediator.class);

    /** The Spring bean ref to be used */
    private String beanName = null;
    /** The named Spring configName to be used */
    private String configName = null;
    /** The Spring ApplicationContext to be used */
    private ApplicationContext appContext = null;

    public boolean mediate(MessageContext synCtx) {

        if (beanName == null) {
            handleException("The bean name for the Spring mediator has not been specified");
        }

        // if a named configuration is referenced, use it
        if (configName != null) {
            // get named Spring configName
            Configuration config = synCtx.getConfiguration().getNamedConfiguration(configName);

            if (config != null && config instanceof SpringConfiguration) {

                ApplicationContext appContext = ((SpringConfiguration) config).getAppContext();
                Object o = appContext.getBean(beanName);

                if (o != null && Mediator.class.isAssignableFrom(o.getClass())) {
                    Mediator m = (Mediator) o;
                    return m.mediate(synCtx);

                } else {
                    handleException("Could not find the bean named : " + beanName +
                        " from the Spring configuration named : " + configName);
                }
            } else {
                handleException("Could not get a reference to the Spring configuration named : " + configName);
            }

        } else if (appContext != null) {

            Object o = appContext.getBean(beanName);

            if (o != null && Mediator.class.isAssignableFrom(o.getClass())) {
                Mediator m = (Mediator) o;
                return m.mediate(synCtx);

            } else {
                handleException("Could not find the bean named : " + beanName +
                    " from the anonymous Spring configuration");
            }

        } else {
            handleException("A named Spring configuration or an ApplicationContext has not been specified");
        }
        return true;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public ApplicationContext getAppContext() {
        return appContext;
    }

    public void setAppContext(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    public String getType() {
        return "SpringMediator";
    }
}
