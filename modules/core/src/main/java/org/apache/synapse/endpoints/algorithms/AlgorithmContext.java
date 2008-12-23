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
package org.apache.synapse.endpoints.algorithms;

import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.context.Replicator;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps the runtime state of the algorithm
 */
public class AlgorithmContext {

    private static final Log log = LogFactory.getLog(AlgorithmContext.class);

    private static final String KEY_PREFIX = "synapse.endpoint.lb.algorithm.";
    private static final String CURRENT_EPR = ".current_epr";

    /* The axis2 configuration context - this hold state in a clustered environment. */
    private ConfigurationContext cfgCtx;

    /* Are we supporting clustering ? */
    private Boolean isClusteringEnabled = null;

    /* The key for 'currentEPR' attribute when replicated in a clsuter */
    private String CURRENT_EPR_PROP_KEY;

    /* The pointer to current epr - The position of the current EPR */
    private int currentEPR = 0;

    private Map<String, Object> parameters;

    public AlgorithmContext(boolean clusteringEnabled, ConfigurationContext cfgCtx, String endpointName) {
        this.cfgCtx = cfgCtx;
        if (clusteringEnabled) {
            isClusteringEnabled = Boolean.TRUE;
        }
        CURRENT_EPR_PROP_KEY = KEY_PREFIX + endpointName + CURRENT_EPR;
        parameters = new HashMap<String, Object>();
    }

    /**
     * To get the position of the current EPR for use. Default to 0 - i.e. first endpoint
     *
     * @return The  position of the current EPR
     */
    public int getCurrentEndpointIndex() {

        if (Boolean.TRUE.equals(isClusteringEnabled)) {

            Object value = cfgCtx.getPropertyNonReplicable(this.CURRENT_EPR_PROP_KEY);
            if (value == null) {
                return 0;
            } else if (value instanceof Integer) {
                return ((Integer) value);
            }
        } else {
            return currentEPR;
        }
        return 0;
    }

    /**
     * The  position of the current EPR
     *
     * @param currentEPR The current position
     */
    public void setCurrentEndpointIndex(int currentEPR) {

        if (Boolean.TRUE.equals(isClusteringEnabled)) {

            if (log.isDebugEnabled()) {
                log.debug("Set EPR with key : " + CURRENT_EPR_PROP_KEY + " as : " + currentEPR);
            }
            setAndReplicateState(CURRENT_EPR_PROP_KEY, currentEPR);

        } else {
            if (log.isDebugEnabled()) {
                log.debug("Setting the current EPR as : " + currentEPR);
            }
            this.currentEPR = currentEPR;
        }
    }

    /**
     * Get the configuration context instance . This is only available for cluster env.
     *
     * @return Returns the ConfigurationContext instance
     */
    public ConfigurationContext getConfigurationContext() {
        return cfgCtx;
    }

    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     */
    protected void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     * @param e   The exception
     */
    protected void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    /**
     * Helper method to replicates states of the property with given key
     * Sets property and  replicates the current state  so that all instances
     * across cluster can see this state
     *
     * @param key   The key of the property
     * @param value The value of the property
     */
    private void setAndReplicateState(String key, Object value) {

        if (cfgCtx != null && key != null && value != null) {

            try {
                if (log.isDebugEnabled()) {
                    log.debug("Replicating property key : " + key + " as : " + value);
                }
                cfgCtx.setProperty(key, value);
                Replicator.replicate(cfgCtx, new String[]{key});

            } catch (ClusteringFault clusteringFault) {
                handleException("Error replicating property : " + key + " as : " +
                    value, clusteringFault);
            }
        }
    }

    public Object getParameter(String key) {
        return parameters.get(key);
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

}