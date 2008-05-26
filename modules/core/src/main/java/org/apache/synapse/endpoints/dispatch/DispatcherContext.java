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
package org.apache.synapse.endpoints.dispatch;

import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.context.Replicator;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.IndirectEndpoint;
import org.apache.synapse.endpoints.SALoadbalanceEndpoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the states of the dispatcher . This hides where those states are kept . For a cluster
 * environment , all states are kept in the axis2 configuration context in order to replicate
 * those states so that other synapse instance in the same cluster can see those changes .
 * This class can be evolved to keep any run time states related to the endpoint .
 * For a non-clustered environment , all data are kept locally.
 * <p/>
 * This class provide the abstraction need to separate the dynamic data from the static data and
 * improve the  high cohesion and provides capability to replicate only required state at a given
 * time. This improves the performance when replicate data.
 */
public class DispatcherContext {

    private static final Log log = LogFactory.getLog(DispatcherContext.class);

    /* The  static constant only for construct key prefix for each property in a dispatcher context
     *as it is need when those property state going to replicate in a cluster env. */
    private static final String SESSION = "session";
    private static final String UNDERSCORE_STRING = "_";

    /* Map to store session -> endpoint mappings. Synchronized map is used as this is accessed by
     * multiple threads (e.g. multiple clients different sessions).*/
    private final Map<String, Endpoint> sessionMap
            = Collections.synchronizedMap(new HashMap<String, Endpoint>());

    /*The axis configuration context-  this will hold the all callers states
     *when doing throttling in a clustered environment. */
    private ConfigurationContext configCtx;

    /* Is this env. support clustering*/
    private boolean isClusteringEnable = false;

    /*The key prefix for each session and this is used when this attribute value being replicated */
    private String keyPrefix;

    /*To keep all defined child endpoints  */
    private final Map<String, Endpoint> endpointsMap = new HashMap<String, Endpoint>();

    /**
     * return the endpoint  for the given session.
     * Null will be returned , if there is no endpoint for given session.
     *
     * @param sessionID The session identifier
     * @return Returns the endpoint for the given session.
     */
    public Endpoint getEndpoint(String sessionID) {

        if (isClusteringEnable) {    // if this is a clustering env.

            if (keyPrefix == null || "".equals(keyPrefix)) {
                handleException("Cannot find the required key prefix to find the " +
                        "shared state of one of  'session'");
            }
            // gets the value from configuration context (The shared state across all instances)
            Object value = this.configCtx.getPropertyNonReplicable(this.keyPrefix + sessionID);
            if (value != null && value instanceof String) {
                if (log.isDebugEnabled()) {
                    log.debug("Retrieving the endpoint from the session id " + value);
                }
                return endpointsMap.get(value.toString());
            }

        } else {

            synchronized (sessionMap) {
                if (log.isDebugEnabled()) {
                    log.debug("Retrieving the endpoint from the session id " + sessionID);
                }
                return sessionMap.get(sessionID);
            }
        }

        return null;
    }

    /**
     * Sets the given endpoint mapping with given the session id.
     *
     * @param sessionID The session identifier
     * @param endpoint  The endpoint
     */
    public void setEndpoint(String sessionID, Endpoint endpoint) {

        if (isClusteringEnable) {  // if this is a clustering env.

            String endpointName;
            if (endpoint instanceof IndirectEndpoint) {
                endpointName = ((IndirectEndpoint) endpoint).getKey();
            } else {
                endpointName = endpoint.getName();
            }

            if (endpointName == null) {
                if (log.isDebugEnabled() && isClusteringEnable()) {
                    log.warn(SALoadbalanceEndpoint.WARN_MESSAGE);
                }
                endpointName = SynapseConstants.ANONYMOUS_ENDPOINT;
            }
            
            if (keyPrefix != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding the enpoint " + endpointName + " with the session id "
                            + keyPrefix + sessionID + " for replication to the session");
                }
                // replicates the state so that all instances across cluster can see this state
                setAndReplicateState(keyPrefix + sessionID, endpointName);
            }

        } else {

            synchronized (sessionMap) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding the endpoint " + endpoint
                            + " with the session id " + sessionID + " to the session");
                }
                sessionMap.put(sessionID, endpoint);
            }
        }

    }

    /**
     * Removes the endpoint for the given session.
     *
     * @param id The session identifier
     */
    public void removeSession(String id) {

        if (isClusteringEnable) {   // if this is a clustering env.

            if (keyPrefix != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Removing and replicating " +
                            "the session with the session id " + keyPrefix + id);
                }
                //Removes the endpoint name and then replicates the current
                //state so that all instances
                removeAndReplicateState(keyPrefix + id);
            }

        } else {

            synchronized (sessionMap) {
                if (log.isDebugEnabled()) {
                    log.debug("Removing the session with the session id " + id);
                }
                sessionMap.remove(id);
            }
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
     * Sets the identifier for this dispatcher context , so that , this can be identified
     * uniquely across the cluster. The id will be the name of the endpoint
     *
     * @param contextID The Id for this dispatcher context
     */
    public void setContextID(String contextID) {

        if (contextID == null || "".equals(contextID)) {
            handleException("The Context ID cannot be null when system in a cluster environment");
        }

        //Making required key for each property in the dispatcher context - Those will be used when
        //replicating states
        StringBuffer buffer = new StringBuffer();
        buffer.append(contextID);
        buffer.append(UNDERSCORE_STRING);
        buffer.append(SESSION);
        buffer.append(UNDERSCORE_STRING);
        keyPrefix = buffer.toString();

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
                    log.debug("Start replicating the property with key : " +
                            key + " value : " + value);
                }

                Object prop = configCtx.getPropertyNonReplicable(key);
                if (prop == null) {
                    configCtx.setProperty(key, value);
                    Replicator.replicate(configCtx, new String[]{key});
                }

                if (log.isDebugEnabled()) {
                    log.debug("Completed replication of the property with key: " + key);
                }

            } catch (ClusteringFault clusteringFault) {
                handleException("Error during the replicating states ", clusteringFault);
            }
        }
    }

    /**
     * Helper method to replicates states of the property with given key
     * Removes the property and then replicates the current state so that all instances
     * across cluster can see this state
     *
     * @param key The key of the property
     */
    private void removeAndReplicateState(String key) {

        if (configCtx != null && key != null) {

            try {
                if (log.isDebugEnabled()) {
                    log.debug("Start replicating the property removal with key : " + key);
                }

                configCtx.removeProperty(key);
                Replicator.replicate(configCtx, new String[]{key});

                if (log.isDebugEnabled()) {
                    log.debug("Completed replication of the property removal with key : " + key);
                }

            } catch (ClusteringFault clusteringFault) {
                handleException("Error during the replicating states ", clusteringFault);
            }
        }
    }

    /**
     * Returns whether clustering is enable or not
     *
     * @return True - enable , false -> this is not a cluster env.
     */
    public boolean isClusteringEnable() {
        return isClusteringEnable;
    }

    /**
     * Sets the defined child endpoints
     *
     * @param endpoints The endpoint list
     */
    public void setEndpoints(List<Endpoint> endpoints) {

        if (endpoints != null) {

            for (Endpoint endpoint : endpoints) {

                String endpointName;
                if (endpoint instanceof IndirectEndpoint) {
                    endpointName = ((IndirectEndpoint) endpoint).getKey();
                } else {
                    endpointName = endpoint.getName();
                }
                
                if (endpointName == null) {
                    if (log.isDebugEnabled() && isClusteringEnable()) {
                        log.warn(SALoadbalanceEndpoint.WARN_MESSAGE);
                    }
                    endpointName = SynapseConstants.ANONYMOUS_ENDPOINT;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Adding an endpoint with the name/key "
                            + endpointName + " to the endpoints map");
                }
                endpointsMap.put(endpointName, endpoint);
            }
        }
    }
}