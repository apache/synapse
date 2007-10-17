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
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.description.Parameter;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.util.MessageHelper;
import org.wso2.caching.Cache;
import org.wso2.caching.CachedObject;
import org.wso2.caching.CachingConstants;
import org.wso2.caching.digest.DigestGenerator;

/**
 *
 */
public class CacheMediator extends AbstractMediator {

    private String id = null;
    private String scope = CachingConstants.SCOPE_PER_HOST;
    private DigestGenerator digestGenerator = CachingConstants.DEFAULT_XML_IDENTIFIER;
    private int inMemoryCacheSize = CachingConstants.DEFAULT_CACHE_SIZE;
    // if this is 0 then no disk cache, and if there is no size specified in the config
    // factory will asign a default value to enable disk based caching
    private int diskCacheSize = 0;
    private long timeout = 0L;
    private SequenceMediator onCacheHitSequence = null;
    private String onCacheHitRef = null;
    private String cacheObjKey = CachingConstants.CACHE_OBJECT; // default per-host
    private static final String CACHE_OBJ_PREFIX = "synapse.cache_obj_";

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

        AxisConfiguration axisCfg = synCtx.getConfiguration().getAxisConfiguration();
        if (axisCfg == null) {
            handleException("Unable to perform caching, "
                + " AxisConfiguration cannot be found", synCtx);
            return false; // never executes.. but keeps IDE happy
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Looking up cache at scope : " +
                scope + " with ID : " + cacheObjKey);
        }

        // look up cache
        Parameter param = axisCfg.getParameter(cacheObjKey);
        Cache cache = null;
        if (param != null && param.getValue() instanceof Cache) {
            cache = (Cache) param.getValue();

        } else {
            synchronized (axisCfg) {
                // check again after taking the lock to make sure no one else did it before us
                param = axisCfg.getParameter(cacheObjKey);
                if (param != null && param.getValue() instanceof Cache) {
                    cache = (Cache) param.getValue();

                } else {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Creating/recreating the cache object");
                    }
                    cache = new Cache();
                    try {
                        axisCfg.addParameter(cacheObjKey, cache);
                    } catch (AxisFault af) {
                        auditWarn("Unable to create a cache with ID : " + cacheObjKey, synCtx);
                    }
                }
            }
        }

        boolean result = true;
        if (synCtx.isResponse()) {
            processResponseMessage(traceOrDebugOn, traceOn, synCtx, cache);

        } else {
            result = processRequestMessage(synCtx, traceOrDebugOn, traceOn, cache);
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Cache mediator");
        }
        return result;
    }

    /**
     * Process a response message through this cache mediator. This finds the Cache used, and
     * updates it for the corresponding request hash
     * @param traceOrDebugOn is trace or debug logging on?
     * @param traceOn is tracing on?
     * @param synCtx the current message (response)
     * @param cache the cache
     */
    private void processResponseMessage(boolean traceOrDebugOn, boolean traceOn,
        MessageContext synCtx, Cache cache) {

        Object requestHash = synCtx.getProperty(CachingConstants.REQUEST_HASH_KEY);

        if (requestHash != null) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Storing the response message into the cache at scope : "
                    + scope + " with ID : " + cacheObjKey + " for request hash : " + requestHash);
            }

            Object obj = cache.getResponseForKey(requestHash);

            if (obj != null && obj instanceof CachedObject) {

                CachedObject cachedObj = (CachedObject) obj;
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Storing the response for the message with ID : "
                        + synCtx.getMessageID() + " with request hash ID : " +
                        cachedObj.getRequestHash() + " in the cache : " + cacheObjKey);
                }

                cachedObj.setResponseEnvelope(
                    MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope()));

                // this is not required yet can commented this for perf improvements
                // in the future there can be a situation where user sends the request with the
                // response hash (if client side caching is on) in which case we can compare that
                // response hash with the given response hash and respond with not-modified http header
                cachedObj.setResponseHash(digestGenerator.getDigest(
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext()));

                cachedObj.setExpireTime(
                    System.currentTimeMillis() + cachedObj.getTimeout());

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
     * Processes a request message through the cache mediator. Generates the request hash and
     * looks up for a hit, if found; then the specified named or anonymous sequence is executed
     * or marks this message as a response and sends back directly to client.
     * @param synCtx incoming request message
     * @param traceOrDebugOn is tracing or debug logging on?
     * @param traceOn is tracing on?
     * @param cache the cache
     * @return should this mediator terminate further processing?
     */
    private boolean processRequestMessage(MessageContext synCtx, boolean traceOrDebugOn, boolean traceOn, Cache cache) {

        Object requestHash = digestGenerator
            .getDigest(((Axis2MessageContext) synCtx).getAxis2MessageContext());
        synCtx.setProperty(CachingConstants.REQUEST_HASH_KEY, requestHash);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Generated request hash : " + requestHash);
        }

        if (cache.containsKey(requestHash) &&
            cache.getResponseForKey(requestHash) instanceof CachedObject) {

            // get the response from the cache and attach to the context and change the
            // direction of the message
            CachedObject cachedObj = (CachedObject) cache.getResponseForKey(requestHash);

            if (!cachedObj.isExpired() && cachedObj.getResponseEnvelope() != null) {

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn,
                        "Cache-hit for message ID : " + synCtx.getMessageID());
                }

                // mark as a response and replace envelope from cache
                synCtx.setResponse(true);
                try {
                    synCtx.setEnvelope(cachedObj.getResponseEnvelope());
                } catch (AxisFault axisFault) {
                    handleException(
                        "Error setting response envelope from cache : " + cacheObjKey, synCtx);
                }

                // take specified action on cache hit
                if (onCacheHitSequence != null) {
                    // if there is an onCacheHit use that for the mediation
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Delegating message to the onCachingHit " +
                            "Anonymous sequence");
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
                            " has served from the cache : " + cacheObjKey);
                    }
                    // send the response back if there is not onCacheHit is specified
                    synCtx.setTo(null);
                    Axis2Sender.sendBack(synCtx);
                }
                // stop any following mediators from executing
                return false;

            } else {
                // cache exists, but has expired...
                cachedObj.clearCache();
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Existing cached response has expired. Reset cache element");
                }
            }

        } else {

            // if not found in cache, check if we can cache this request
            if (cache.getCache().size() == inMemoryCacheSize) {
                cache.removeExpiredResponses();
                if (cache.getCache().size() == inMemoryCacheSize) {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "In-memory cache is full. Unable to cache");
                    }
                } else {
                    storeRequestToCache(synCtx, requestHash, cache);
                }
            } else {
                storeRequestToCache(synCtx, requestHash, cache);
            }
        }
        return true;
    }

    /**
     * Store request message to the cache
     * @param synCtx the request message
     * @param requestHash the request hash that has already been computed
     * @param cache the cache
     */
    private void storeRequestToCache(MessageContext synCtx, Object requestHash, Cache cache) {
        CachedObject cachedObj = new CachedObject();
        cachedObj.setRequestEnvelope(MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope()));
        cachedObj.setRequestHash(requestHash);
        cachedObj.setTimeout(timeout);
        cache.addResponseWithKey(requestHash, cachedObj);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.cacheObjKey = CACHE_OBJ_PREFIX + id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
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

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
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
}
