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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.spring.SpringConfigExtension;
import org.springframework.context.ApplicationContext;

/**
 * This mediator allows Spring beans implementing the org.apache.synapse.api.Mediator
 * interface to mediate messages passing through Synapse.
 *
 * A Spring mediator is instantiated by Spring (see www.springframework.org). The mediator
 * refers to a Spring bean name, and also either a Spring configuration defined to Synapse
 * or an inlined Spring configuration.
 */
public class SpringMediator implements Mediator {

    private static final Log log = LogFactory.getLog(SpringMediator.class);

    /**
     * The Spring bean ref to be used
     */
    private String beanName = null;
    /**
     * The named Spring configName to be used
     */
    private String configName = null;
    /**
     * The Spring ApplicationContext to be used
     */
    private ApplicationContext appContext = null;

    public boolean mediate(MessageContext synCtx) {

        if (beanName == null) {
            handleException("The bean name for the Spring mediator has not been specified");
        }

        // if a named configuration is referenced, use it
        if (configName != null) {
            // get named Spring configuration
            Object cfg = synCtx.getConfiguration().getProperty(configName);

            if (cfg != null && cfg instanceof SpringConfigExtension) {

                ApplicationContext appContext = ((SpringConfigExtension) cfg).getAppContext();
                log.debug("Loading bean : " + beanName + " from Spring configuration named : " + configName);
                Object o = appContext.getBean(beanName);

                if (o != null && Mediator.class.isAssignableFrom(o.getClass())) {
                    Mediator m = (Mediator) o;
                    return m.mediate(synCtx);

                } else {
                    handleException("Could not find the bean named : " + beanName +
                        " from the Spring configuration named : " + configName);
                }
            } else {
                handleException("Could not get a reference to a valid Spring configuration named : " + configName);
            }

        } else if (appContext != null) {

            log.debug("Loading bean : " + beanName + " from inline Spring configuration");
            Object o = appContext.getBean(beanName);

            if (o != null && Mediator.class.isAssignableFrom(o.getClass())) {
                Mediator m = (Mediator) o;
                return m.mediate(synCtx);

            } else {
                handleException("Could not find the bean named : " + beanName +
                    " from the inline Spring configuration");
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
