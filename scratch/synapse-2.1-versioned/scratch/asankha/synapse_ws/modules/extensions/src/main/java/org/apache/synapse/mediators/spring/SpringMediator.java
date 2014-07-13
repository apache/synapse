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

package org.apache.synapse.mediators.spring;

import org.apache.synapse.MessageContext;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.Entry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;

/**
 * This mediator allows Spring beans implementing the org.apache.synapse.Mediator
 * interface to mediate messages passing through Synapse.
 *
 * A Spring mediator is instantiated by Spring (see www.springframework.org). The mediator
 * refers to a Spring bean name, and also either a Spring configuration defined to Synapse
 * or an inlined Spring configuration.
 */
public class SpringMediator extends AbstractMediator {

    /**
     * The Spring bean ref to be used
     */
    private String beanName = null;
    /**
     * The named Spring config to be used
     */
    private String configKey = null;
    /**
     * The Spring ApplicationContext to be used
     */
    private ApplicationContext appContext = null;

    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Spring mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        Entry entry = synCtx.getConfiguration().getEntryDefinition(configKey);

        // if the configKey refers to a dynamic property
        if (entry != null && entry.isDynamic()) {
            if (!entry.isCached() || entry.isExpired()) {
                buildAppContext(synCtx, traceOrDebugOn, traceOn);
            }
        // if the property is not a DynamicProperty, we will create an ApplicationContext only once
        } else {
            if (appContext == null) {
                buildAppContext(synCtx, traceOrDebugOn, traceOn);
            }
        }

        if (appContext != null) {

            Object o = appContext.getBean(beanName);    
            if (o != null && Mediator.class.isAssignableFrom(o.getClass())) {
                Mediator m = (Mediator) o;
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Loaded mediator from bean : " + beanName + " executing...");
                }
                return m.mediate(synCtx);

            } else {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Unable to load mediator from bean : " + beanName);
                }
                handleException("Could not load bean named : " + beanName +
                    " from the Spring configuration with key : " + configKey, synCtx);
            }
        } else {
            handleException("Cannot reference application context with key : " + configKey, synCtx);
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Spring mediator");
        }
        return true;
    }

    private synchronized void buildAppContext(MessageContext synCtx,
        boolean traceOrDebugOn, boolean traceOn) {

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Creating Spring ApplicationContext from key : " + configKey);
        }
        GenericApplicationContext appContext = new GenericApplicationContext();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(appContext);
        xbdr.setValidating(false);
        xbdr.loadBeanDefinitions(
            new InputStreamResource(
                SynapseConfigUtils.getStreamSource(synCtx.getEntry(configKey)).getInputStream()));
        appContext.refresh();
        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Spring ApplicationContext from key : " + configKey + " created");
        }
        this.appContext = appContext;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
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
