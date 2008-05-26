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

/**
 * Keeps the states of the load balance algorithm.This hides where those states are kept.For a
 * cluster environment ,all states are kept in the axis2 configuration context in order to replicate
 * those states so that other synapse instance in the same cluster can see those changes .
 * This class can be evolved to keep any run time states related to the endpoint .
 * For a non-clustered environment , all data are kept locally.
 * <p/>
 * This class provide the abstraction need to separate the dynamic data from the static data
 * and improve the  high cohesion and provides capability to replicate only required state at
 * a given time. This improves the performance when replicate data.
 */
public class AlgorithmContext {

    private static final Log log = LogFactory.getLog(AlgorithmContext.class);

    /* The  static constant only for construct key prefix for each property in a dispatcher context
     * as it is need when those property state going to replicate in a cluster env. */
    private static final String UNDERSCORE_STRING = "_";
    private static final String CURRENT_EPR = "currentEPR";

    /* The axis configuration context-  this will hold the all callers states
     * when doing throttling in a clustered environment. */
    private ConfigurationContext configCtx;

    /* Is this env. support clustering*/
    private boolean isClusteringEnable = false;

    /* The key for 'currentEPR' attribute and this is used when this attribute value being
     * replicated */
    private String currentEPRPropertyKey;

    /* The pointer to current epr - The position of the current EPR */
    private int currentEPR = 0;

    /**
     * To get the  position of the current EPR
     * If there is no value and if there will not appear any errors , then '0' will be returned.
     *
     * @return The  position of the current EPR
     */
    public int getCurrentEndpointIndex() {

        if (this.isClusteringEnable) {  // if this is a clustering env.

            if (this.currentEPRPropertyKey == null || "".equals(this.currentEPRPropertyKey)) {
                handleException("Cannot find the required key to find the " +
                        "shared state of the 'currentEPR' attribute");
            }

            Object value = this.configCtx.getPropertyNonReplicable(this.currentEPRPropertyKey);
            if (value == null) {
                return 0;
            }
            try {
                if (value instanceof Integer) {
                    return ((Integer) value).intValue();
                } else if (value instanceof String) {
                    return Integer.parseInt((String) value);
                }
            } catch (NumberFormatException e) {
                handleException("The invalid value for the 'currentEPR' attribute");
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
    public void setCurrentEPR(int currentEPR) {

        if (isClusteringEnable) {  // if this is a clustering env.

            if (currentEPRPropertyKey != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Setting the current EPR " + currentEPR
                            + " with the key " + currentEPRPropertyKey);
                }
                // Sets the property and  replicates the current state  so that all instances
                setAndReplicateState(currentEPRPropertyKey, currentEPR);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Setting the current EPR " + currentEPR);
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
        return configCtx;
    }

    /**
     * Sets the  ConfigurationContext instance . This is only used for cluster env.
     * By setting this , indicates that this is a cluster env.
     *
     * @param configCtx The ConfigurationContext instance
     */
    public void setConfigurationContext(ConfigurationContext configCtx) {

        if (configCtx == null) {
            handleException("The ConfigurationContext cannot be null when system " +
                    "in a cluster environment");
        }

        this.configCtx = configCtx;
        this.isClusteringEnable = true; // Now, the environment is considered as a cluster
    }

    /**
     * Sets the identifier for this algorithm context , so that , this can be identified
     * uniquely across the cluster. The id will be the name of the endpoint
     *
     * @param contextID The Id for this algorithm context
     */
    public void setContextID(String contextID) {

        if (contextID == null || "".equals(contextID)) {
            handleException("The Context ID cannot be null when system in a cluster environment");
        }

        //Making required key for each property in the algorithm context- Those will be used when
        //replicating states
        StringBuffer buffer = new StringBuffer();
        buffer.append(contextID);
        buffer.append(UNDERSCORE_STRING);
        buffer.append(CURRENT_EPR);
        currentEPRPropertyKey = buffer.toString();
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

        if (configCtx != null && key != null && value != null) {

            try {
                if (log.isDebugEnabled()) {
                    log.debug("Start replicating the property with key : " + key
                            + " value : " + value);
                }

                configCtx.setProperty(key, value);
                Replicator.replicate(configCtx, new String[]{key});

                if (log.isDebugEnabled()) {
                    log.debug("Completed replication of the property with key : " + key);
                }

            } catch (ClusteringFault clusteringFault) {
                handleException("Error during the replicating states ", clusteringFault);
            }
        }
    }

}