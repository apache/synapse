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
package org.apache.synapse.transport.jms;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.base.threads.WorkerPool;

/**
 * Class managing a set of {@link JMSConnectionFactory} objects.
 */
public class JMSConnectionFactoryManager {
    private static final Log log = LogFactory.getLog(JMSConnectionFactoryManager.class);
    
    /** A Map containing the JMS connection factories managed by this, keyed by name */
    private final Map<String,JMSConnectionFactory> connectionFactories = new HashMap<String,JMSConnectionFactory>();
    
    private final ConfigurationContext cfgCtx;
    
    private final JMSListener jmsListener;
    
    private final WorkerPool workerPool;
    
    public JMSConnectionFactoryManager(ConfigurationContext cfgCtx) {
        this.cfgCtx = cfgCtx;
        jmsListener = null;
        workerPool = null;
    }
    
    public JMSConnectionFactoryManager(ConfigurationContext cfgCtx, JMSListener jmsListener, WorkerPool workerPool) {
        this.cfgCtx = cfgCtx;
        this.jmsListener = jmsListener;
        this.workerPool = workerPool;
    }
    
    /**
     * Create JMSConnectionFactory instances for the definitions in the transport configuration,
     * and add these into our collection of connectionFactories map keyed by name
     *
     * @param trpDesc the transport description for JMS
     */
    public void loadConnectionFactoryDefinitions(ParameterInclude trpDesc) {

        // iterate through all defined connection factories
        Iterator<?> conFacIter = trpDesc.getParameters().iterator();

        while (conFacIter.hasNext()) {
            Parameter conFacParams = (Parameter) conFacIter.next();

            JMSConnectionFactory jmsConFactory =
                new JMSConnectionFactory(conFacParams.getName(), jmsListener, workerPool, cfgCtx);
            JMSUtils.setConnectionFactoryParameters(conFacParams, jmsConFactory);

            connectionFactories.put(jmsConFactory.getName(), jmsConFactory);
        }
    }
    
    /**
     * Get the names of the defined connection factories.
     * @return
     */
    public String[] getNames() {
        Collection<String> result = connectionFactories.keySet();
        return result.toArray(new String[result.size()]);
    }
    
    /**
     * Start all connection factories.
     * 
     * @throws AxisFault
     */
    public void start() throws AxisFault {
        for (JMSConnectionFactory conFac : connectionFactories.values()) {
            try {
                conFac.connectAndListen();
            } catch (JMSException e) {
                handleException("Error starting connection factory : " + conFac.getName(), e);
            } catch (NamingException e) {
                handleException("Error starting connection factory : " + conFac.getName(), e);
            }
        }
    }

    /**
     * Get the JMS connection factory with the given name.
     * 
     * @param name the name of the JMS connection factory
     * @return the JMS connection factory or null if no connection factory with
     *         the given name exists
     */
    public JMSConnectionFactory getJMSConnectionFactory(String name) {
        return connectionFactories.get(name);
    }
    
    /**
     * Get the JMS connection factory that matches the given properties, i.e. referring to
     * the same underlying connection factory.
     * 
     * @param props
     * @return the JMS connection factory or null if no connection factory compatible
     *         with the given properties exists
     */
    public JMSConnectionFactory getJMSConnectionFactory(Map<String,String> props) {
        for (JMSConnectionFactory cf : connectionFactories.values()) {
            Map<String,String> jndiProperties = cf.getJndiProperties();
            if (equals(props.get(JMSConstants.CONFAC_JNDI_NAME_PARAM), jndiProperties.get(JMSConstants.CONFAC_JNDI_NAME_PARAM))
                &&
                equals(props.get(Context.INITIAL_CONTEXT_FACTORY), jndiProperties.get(Context.INITIAL_CONTEXT_FACTORY))
                &&
                equals(props.get(Context.PROVIDER_URL), jndiProperties.get(Context.PROVIDER_URL))
                &&
                equals(props.get(Context.SECURITY_PRINCIPAL), jndiProperties.get(Context.SECURITY_PRINCIPAL))
                &&
                equals(props.get(Context.SECURITY_CREDENTIALS), jndiProperties.get(Context.SECURITY_CREDENTIALS))) {
                return cf;
            }
        }
        return null;
    }
    
    /**
     *     Prevents NullPointerException when s1 is null.
     *     If both values are null this returns true 
     */
    private static boolean equals(Object s1, Object s2) {
        if(s1 == s2) {
            return true;
        } else if(s1 != null && s1.equals(s2)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Pause all connection factories.
     */
    public void pause() {
        for (JMSConnectionFactory conFac : connectionFactories.values()) {
            conFac.pause();
        }
    }
    
    /**
     * Resume all connection factories.
     */
    public void resume() {
        for (JMSConnectionFactory conFac : connectionFactories.values()) {
            conFac.resume();
        }
    }
    
    /**
     * Stop all connection factories.
     */
    public void stop() {
        for (JMSConnectionFactory conFac : connectionFactories.values()) {
            conFac.stop();
        }
    }

    protected void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }
}
