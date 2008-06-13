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

package org.apache.synapse.mediators.builtin;

import org.apache.axis2.AxisFault;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.context.Replicator;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.saaj.util.SAAJUtil;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.util.FixedByteArrayOutputStream;
import org.apache.synapse.util.MessageHelper;
import org.wso2.caching.CacheManager;
import org.wso2.caching.CachedObject;
import org.wso2.caching.CachingConstants;
import org.wso2.caching.CachingException;
import org.wso2.caching.digest.DigestGenerator;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * CacheMediator will cache the response messages indexed using the hash value of the
 * request message, and subsequent messages with the same request (request hash will be
 * generated and checked for the equality) within the cache expiration period will be served
 * from the stored responses in the cache
 *
 * @see org.apache.synapse.Mediator
 */
public class CacheMediator extends AbstractMediator {

    private String id = null;
    private String scope = CachingConstants.SCOPE_PER_HOST;// global
    private boolean collector = false;
    private DigestGenerator digestGenerator = CachingConstants.DEFAULT_XML_IDENTIFIER;
    private int inMemoryCacheSize = CachingConstants.DEFAULT_CACHE_SIZE;
    // if this is 0 then no disk cache, and if there is no size specified in the config
    // factory will asign a default value to enable disk based caching
    private int diskCacheSize = 0;
    private long timeout = 0L;
    private SequenceMediator onCacheHitSequence = null;
    private String onCacheHitRef = null;
    private int maxMessageSize = 0;
    private String cacheManagerKey = CachingConstants.CACHE_MANAGER; // default per-host
    private static final String CACHE_MANAGER_PREFIX = "synapse.cache_manager_";

    public boolean mediate(MessageContext synCtx) {

        // tracing and debuggin related mediation initiation
        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Cache mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        // if maxMessageSize is specified check for the message size before processing
        FixedByteArrayOutputStream fbaos = null;
        if (maxMessageSize > 0) {
            fbaos = new FixedByteArrayOutputStream(maxMessageSize);
            try {
                MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope()).serialize(fbaos);
            } catch (XMLStreamException e) {
                handleException("Error in checking the message size", e, synCtx);
            } catch (SynapseException syne) {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Message size exceeds the upper bound for caching, " +
                            "request will not be cached");
                    return true;
                }
            }
        }

        ConfigurationContext cfgCtx =
            ((Axis2MessageContext) synCtx).getAxis2MessageContext().getConfigurationContext();
        if (cfgCtx == null) {
            handleException("Unable to perform caching, "
                + " ConfigurationContext cannot be found", synCtx);
            return false; // never executes.. but keeps IDE happy
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn,
                "Looking up cache at scope : " + scope + " with ID : " + cacheManagerKey);
        }

        // look up cache
        Object prop = cfgCtx.getPropertyNonReplicable(cacheManagerKey);
        CacheManager cacheManager;
        if (prop != null && prop instanceof CacheManager) {
            cacheManager = (CacheManager) prop;
        } else {
            synchronized (cfgCtx) {
                // check again after taking the lock to make sure no one else did it before us
                prop = cfgCtx.getPropertyNonReplicable(cacheManagerKey);
                if (prop != null && prop instanceof CacheManager) {
                    cacheManager = (CacheManager) prop;

                } else {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Creating/recreating the cache object");
                    }
                    cacheManager = new CacheManager();
                    cfgCtx.setProperty(cacheManagerKey, cacheManager);
                }
            }
        }

        boolean result = true;
        try {
            
            if (synCtx.isResponse()) {
                processResponseMessage(synCtx, cfgCtx, traceOrDebugOn, traceOn, cacheManager);

            } else {
                result = processRequestMessage(
                    synCtx, cfgCtx, traceOrDebugOn, traceOn, cacheManager);
            }
            
        } catch (ClusteringFault clusteringFault) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Unable to replicate Cache mediator state among the cluster");
            }
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Cache mediator");
        }
        
        return result;
    }

    /**
     * Process a response message through this cache mediator. This finds the Cache used, and
     * updates it for the corresponding request hash
     *
     * @param traceOrDebugOn is trace or debug logging on?
     * @param traceOn        is tracing on?
     * @param synCtx         the current message (response)
     * @param cfgCtx         the abstract context in which the cache will be kept
     * @param cacheManager   the cache manager
     * @throws ClusteringFault is there is an error in replicating the cfgCtx
     */
    private void processResponseMessage(MessageContext synCtx, ConfigurationContext cfgCtx,
        boolean traceOrDebugOn, boolean traceOn, CacheManager cacheManager) throws ClusteringFault {

        if (!collector) {
            handleException("Response messages cannot be handled in a non collector cache", synCtx);
        }

        String requestHash = (String) synCtx.getProperty(CachingConstants.REQUEST_HASH);

        if (requestHash != null) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Storing the response message into the cache at scope : " +
                    scope + " with ID : " + cacheManagerKey + " for request hash : " + requestHash);
            }

            CachedObject cachedObj = cacheManager.getResponseForKey(requestHash, cfgCtx);
            if (cachedObj != null) {

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Storing the response for the message with ID : " +
                        synCtx.getMessageID() + " with request hash ID : " +
                        cachedObj.getRequestHash() + " in the cache : " + cacheManagerKey);
                }

                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                try {
                    MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope()).serialize(outStream);
                    cachedObj.setResponseEnvelope(outStream.toByteArray());
                } catch (XMLStreamException e) {
                    handleException("Unable to set the response to the Cache", e, synCtx);
                }

                /* this is not required yet, can commented this for perf improvements
                   in the future there can be a situation where user sends the request
                   with the response hash (if client side caching is on) in which case
                   we can compare that response hash with the given response hash and
                   respond with not-modified http header */
                // cachedObj.setResponseHash(cache.getGenerator().getDigest(
                //     ((Axis2MessageContext) synCtx).getAxis2MessageContext()));

                if (cachedObj.getTimeout() > 0) {
                    cachedObj.setExpireTimeMillis(System.currentTimeMillis() + cachedObj.getTimeout());
                }

                cfgCtx.setProperty(cacheManagerKey, cacheManager);
                Replicator.replicate(cfgCtx);

            } else {
                auditWarn("A response message without a valid mapping to the " +
                    "request hash found. Unable to store the response in cache", synCtx);
            }

        } else {
            auditWarn("A response message without a mapping to the " +
                "request hash found. Unable to store the response in cache", synCtx);
        }
    }

    /**
     * Processes a request message through the cache mediator. Generates the request hash and looks
     * up for a hit, if found; then the specified named or anonymous sequence is executed or marks
     * this message as a response and sends back directly to client.
     *
     * @param synCtx         incoming request message
     * @param cfgCtx         the AbstractContext in which the cache will be kept
     * @param traceOrDebugOn is tracing or debug logging on?
     * @param traceOn        is tracing on?
     * @param cacheManager   the cache manager
     * @return should this mediator terminate further processing?
     * @throws ClusteringFault if there is an error in replicating the cfgCtx
     */
    private boolean processRequestMessage(MessageContext synCtx, ConfigurationContext cfgCtx,
        boolean traceOrDebugOn, boolean traceOn, CacheManager cacheManager) throws ClusteringFault {

        if (collector) {
            handleException("Request messages cannot be handled in a collector cache", synCtx);
        }

        String requestHash = null;
        try {  
            requestHash = digestGenerator.getDigest(
                ((Axis2MessageContext) synCtx).getAxis2MessageContext());
            synCtx.setProperty(CachingConstants.REQUEST_HASH, requestHash);
        } catch (CachingException e) {
            handleException("Error in calculating the hash value of the request", e, synCtx);
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Generated request hash : " + requestHash);
        }

        if (cacheManager.containsKey(requestHash) &&
            cacheManager.getResponseForKey(requestHash, cfgCtx) != null) {

            // get the response from the cache and attach to the context and change the
            // direction of the message
            CachedObject cachedObj = cacheManager.getResponseForKey(requestHash, cfgCtx);

            if (!cachedObj.isExpired() && cachedObj.getResponseEnvelope() != null) {

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Cache-hit for message ID : " + synCtx.getMessageID());
                }

                // mark as a response and replace envelope from cache
                synCtx.setResponse(true);
                try {
                    MessageFactory mf = MessageFactory.newInstance();
                    SOAPMessage smsg = mf.createMessage(new MimeHeaders(),
                        new ByteArrayInputStream(cachedObj.getResponseEnvelope()));

                    org.apache.axiom.soap.SOAPEnvelope omSOAPEnv =
                        SAAJUtil.toOMSOAPEnvelope(smsg.getSOAPPart().getDocumentElement());

                    // todo: if there is a WSA messageID in the response, is that need to be unique on each and every resp

                    synCtx.setEnvelope(omSOAPEnv);
                } catch (AxisFault axisFault) {
                    handleException("Error setting response envelope from cache : "
                        + cacheManagerKey, synCtx);
                } catch (IOException ioe) {
                    handleException("Error setting response envelope from cache : "
                        + cacheManagerKey, ioe, synCtx);
                } catch (SOAPException soape) {
                    handleException("Error setting response envelope from cache : "
                        + cacheManagerKey, soape, synCtx);
                }

                // take specified action on cache hit
                if (onCacheHitSequence != null) {
                    // if there is an onCacheHit use that for the mediation
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn,
                            "Delegating message to the onCachingHit " + "Anonymous sequence");
                    }
                    onCacheHitSequence.mediate(synCtx);

                } else if (onCacheHitRef != null) {

                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Delegating message to the onCachingHit " +
                            "sequence : " + onCacheHitRef);
                    }
                    synCtx.getSequence(onCacheHitRef).mediate(synCtx);

                } else {

                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Request message " + synCtx.getMessageID() +
                            " was served from the cache : " + cacheManagerKey);
                    }
                    // send the response back if there is not onCacheHit is specified
                    synCtx.setTo(null);
                    Axis2Sender.sendBack(synCtx);
                }
                // stop any following mediators from executing
                return false;

            } else {
                // cache exists, but has expired...
                cachedObj.expire();
                cachedObj.setTimeout(timeout);
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn,
                        "Existing cached response has expired. Reset cache element");
                }

                cfgCtx.setProperty(cacheManagerKey, cacheManager);
                Replicator.replicate(cfgCtx);
            }

        } else {

            // if not found in cache, check if we can cache this request
            if (cacheManager.getCacheKeys().size() == inMemoryCacheSize) {
                cacheManager.removeExpiredResponses(cfgCtx);
                if (cacheManager.getCacheKeys().size() == inMemoryCacheSize) {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "In-memory cache is full. Unable to cache");
                    }
                } else {
                    storeRequestToCache(cfgCtx, requestHash, cacheManager);
                }
            } else {
                storeRequestToCache(cfgCtx, requestHash, cacheManager);
            }
        }
        return true;
    }

    /**
     * Store request message to the cache
     *
     * @param cfgCtx        - the Abstract context in which the cache will be kept
     * @param requestHash   - the request hash that has already been computed
     * @param cacheManager  - the cache
     * @throws ClusteringFault if there is an error in replicating the cfgCtx
     */
    private void storeRequestToCache(ConfigurationContext cfgCtx,
        String requestHash, CacheManager cacheManager) throws ClusteringFault {
        
        CachedObject cachedObj = new CachedObject();
        cachedObj.setRequestHash(requestHash);
        // this does not set the expiretime but just sets the timeout and the espiretime will
        // be set when the response is availabel
        cachedObj.setTimeout(timeout);
        cacheManager.addResponseWithKey(requestHash, cachedObj, cfgCtx);

        cfgCtx.setProperty(cacheManagerKey, cacheManager);
        Replicator.replicate(cfgCtx);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
        if (CachingConstants.SCOPE_PER_MEDIATOR.equals(scope)) {
            cacheManagerKey = CACHE_MANAGER_PREFIX + id;
        }
    }

    public boolean isCollector() {
        return collector;
    }

    public void setCollector(boolean collector) {
        this.collector = collector;
    }

    public DigestGenerator getDigestGenerator() {
        return digestGenerator;
    }

    public void setDigestGenerator(DigestGenerator digestGenerator) {
        this.digestGenerator = digestGenerator;
    }

    public int getInMemoryCacheSize() {
        return inMemoryCacheSize;
    }

    public void setInMemoryCacheSize(int inMemoryCacheSize) {
        this.inMemoryCacheSize = inMemoryCacheSize;
    }

    public int getDiskCacheSize() {
        return diskCacheSize;
    }

    public void setDiskCacheSize(int diskCacheSize) {
        this.diskCacheSize = diskCacheSize;
    }

    // change the variable to Timeout milis seconds
    public long getTimeout() {
        return timeout / 1000;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout * 1000;
    }

    public SequenceMediator getOnCacheHitSequence() {
        return onCacheHitSequence;
    }

    public void setOnCacheHitSequence(SequenceMediator onCacheHitSequence) {
        this.onCacheHitSequence = onCacheHitSequence;
    }

    public String getOnCacheHitRef() {
        return onCacheHitRef;
    }

    public void setOnCacheHitRef(String onCacheHitRef) {
        this.onCacheHitRef = onCacheHitRef;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }
}
