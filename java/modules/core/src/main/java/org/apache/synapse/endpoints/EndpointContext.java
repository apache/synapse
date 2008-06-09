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
package org.apache.synapse.endpoints;

import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.context.Replicator;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;

/**
 * Keeps the states of the endpoint.This hides where those states are kept .For a cluster
 * environment,all states are kept in the axis2 configuration context in order to replicate those
 * states so that other synapse instance in the same cluster can see those changes . This class can
 * be evolved to keep any run time states related to the endpoint .For a non-clustered environment,
 * all data are kept locally.
 * <p/>
 * This class provide the abstraction need to separate the dynamic data from the static data and
 * improve the  high cohesion and provides capability to replicate only required state at
 * a given time. This improves the performance when replicate data.
 */
public class EndpointContext {

    private static final Log log = LogFactory.getLog(EndpointContext.class);

    /* The  static constant only for construct key prefix for each property in endpoint context
     * as it is need when those property state going to replicate in a cluster env. */
    private static final String ACTIVE = "active";
    private static final String RECOVER_ON = "recover_on";
    private static final String UNDERSCORE_STRING = "_";

    /* Determines if this endpoint is active or not. This variable have to be loaded always from the
     * memory as multiple threads could access it.*/
    private boolean active = true;

    /* Time to recover a failed endpoint.*/
    private long recoverOn = Long.MAX_VALUE;

    /* The axis configuration context-  this will hold the all callers states
     * when doing throttling in a clustered environment. */
    private ConfigurationContext configCtx;

    /* The key for 'active' attribute and this is used when this attribute value being replicated */
    private String activePropertyKey;
    /* The key for 'recoverOn' attribute and this is used when this attribute value being
     * replicated */
    private String recoverOnPropertyKey;

    /* Is this env. support clustering*/
    private boolean isClusteringEnable = false;

    /**
     * Checks if the endpoint is active (failed or not)
     *
     * @return Returns true if the endpoint is active , otherwise , false will be returned
     */
    public boolean isActive() {

        if (this.isClusteringEnable) {  // if this is a clustering env.

            if (this.activePropertyKey == null || "".equals(this.activePropertyKey)) {
                handleException("Cannot find the required key to find the " +
                        "shared state of 'active' attribute");
            }

            // gets the value from configuration context (The shared state across all instances )
            Object value = this.configCtx.getPropertyNonReplicable(this.activePropertyKey);
            if (value == null) {
                return true;
            }
            if (value instanceof Boolean) {
                return ((Boolean) value).booleanValue();
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            } else {
                handleException("Unsupported object type for value" + value);
            }

        } else {
            return active;
        }

        throw new SynapseException("Invalid states in endpoint context");

    }

    /**
     * Sets if endpoint active or not.
     *
     * @param active True for make endpoint active , false for make it inactive
     */
    public synchronized void setActive(boolean active) {

        if (this.isClusteringEnable) {  // if this is a clustering env.
            // replicates the state so that all instances across cluster can see this state
            setAndReplicateState(this.activePropertyKey, active);
        } else {
            this.active = active;
        }

    }

    /**
     * Time to recover a failed endpoint.
     *
     * @return Returns time to recover a failed endpoint.
     */
    public long getRecoverOn() {

        if (this.isClusteringEnable) {    // if this is a clustering env.

            if (this.recoverOnPropertyKey == null || "".equals(this.recoverOnPropertyKey)) {
                handleException("Cannot find the required key to find the " +
                        "shared state of 'recoveOn' attribute");
            }

            // gets the value from configuration context (The shared state across all instances )
            Object value = this.configCtx.getPropertyNonReplicable(this.recoverOnPropertyKey);
            if (value == null) {
                return Long.MAX_VALUE;
            }
            if (value instanceof Long) {
                return ((Long) value).longValue();
            } else if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    return Long.MAX_VALUE;
                }
            } else {
                handleException("Unsupported object type for value" + value);
            }

        } else {
            return recoverOn;
        }
        throw new SynapseException("Invalid states in endpoint context");
    }

    /**
     * Sets time to recover a failed endpoint.
     *
     * @param recoverOn The value for recover time
     */
    public void setRecoverOn(long recoverOn) {

        if (this.isClusteringEnable) { // if this is a clustering env.
            // replicates the state so that all instances across cluster can see this state
            setAndReplicateState(this.recoverOnPropertyKey, recoverOn);
        } else {
            this.recoverOn = recoverOn;
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
            handleException("The ConfigurationContext cannot be null" +
                    " when system in a cluster environment");
        }

        this.configCtx = configCtx;
        this.isClusteringEnable = true; // Now, the environment is considered as a cluster
    }

    /**
     * Sets the identifier for this endpoint context , so that , this can be identified
     * uniquely across the cluster. The id will be the name of the endpoint
     *
     * @param contextID The Id for this endpoint context
     */
    public void setContextID(String contextID) {

        if (contextID == null || "".equals(contextID)) {
            handleException("The Context ID cannot be null when system in a cluster environment");
        }

        //Making required key for each property in the endpoint context - Those will be used when
        //replicating states
        StringBuffer buffer = new StringBuffer();
        buffer.append(contextID);
        buffer.append(UNDERSCORE_STRING);
        String prefix = buffer.toString();

        this.recoverOnPropertyKey = prefix + RECOVER_ON;
        this.activePropertyKey = prefix + ACTIVE;

    }


    /**
     * Helper method to replicates states of the property with given key
     * replicates  the given state so that all instances across cluster can see this state
     *
     * @param key   The key of the property
     * @param value The value of the property
     */
    private void setAndReplicateState(String key, Object value) {

        if (configCtx != null && key != null && value != null) {

            try {
                if (log.isDebugEnabled()) {
                    log.debug("Start replicating the property with key : " + key +
                            " value : " + value);
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
}