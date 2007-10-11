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
    private SequenceMediator onCacheHit = null;
    private String onCacheHitRef = null;
    private static final String CACHE_OBJ_PREFIX = "chache_obj_";

    public boolean mediate(MessageContext synCtx) {

        // tracing and debuggin related mediation initiation
        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Cache mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx);
            }
        }

        if (synCtx.getConfiguration().getAxisConfiguration() == null) {
            handleException("Unable to mediate the message in the cache "
                + ": AxisConfiguration not found", synCtx);
        }

        try {

            String cacheObjKey = CachingConstants.CACHE_OBJECT;
            if (CachingConstants.SCOPE_PER_HOST.equals(scope)) {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Looking up the global cache object : scope = " + scope);
                }
                cacheObjKey = CachingConstants.CACHE_OBJECT;
            } else if (CachingConstants.SCOPE_PER_MEDIATOR.equals(scope)) {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Looking up the mediator " +
                        "specific cache object : scope = " + scope);
                }
                cacheObjKey = CACHE_OBJ_PREFIX + id;
            } else {
                handleException("Scope for the cache mediator "
                    + scope + " is not supported yet", synCtx);
            }

            Parameter param =
                synCtx.getConfiguration().getAxisConfiguration().getParameter(cacheObjKey);
            Cache cache;
            if (param != null && param.getValue() instanceof Cache) {
                cache = (Cache) param.getValue();
            } else {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Creating/Recreating the cache object");
                }
                cache = new Cache();
                synCtx.getConfiguration().getAxisConfiguration().addParameter(cacheObjKey, cache);
            }

            if (synCtx.isResponse()) {

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Starting the response message store in the cache");
                }

                Object obj;
                if (synCtx.getProperty(CachingConstants.REQUEST_HASH_KEY) != null) {
                    obj = cache.getResponseForKey(
                        synCtx.getProperty(CachingConstants.REQUEST_HASH_KEY));
                } else {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Response message with no mapping to the " +
                            "request hash found : Unable to store the response for caching");
                        traceOrDebug(traceOn, "End : Clone mediator");
                    }
                    return true;
                }
                
                if (obj != null && obj instanceof CachedObject) {
                    
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Storing the response for the message "
                            + synCtx.getMessageID() + " in the cache");
                    }
                    
                    CachedObject cachedObj = (CachedObject) obj;
                    cachedObj.setResponseEnvelope(
                        MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope()));
                    // todo: there seems to be a problem with the digest generation of the response
                    // this is not required for the moment
                    // cachedObj.setResponseHash(digestGenerator.getDigest(
                    //     ((Axis2MessageContext) synCtx).getAxis2MessageContext()));
                    cachedObj.setExpireTime(
                        System.currentTimeMillis() + cachedObj.getTimeout());
                } else {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Response message with a mapping in " +
                            "the cache for the request no found : " +
                            "Unable to store the response for caching");
                        traceOrDebug(traceOn, "End : Clone mediator");
                    }
                    return true;
                }
            } else {

                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Starting the request message cache lookup");
                }

                Object requestHash = digestGenerator
                    .getDigest(((Axis2MessageContext) synCtx).getAxis2MessageContext());
                synCtx.setProperty(CachingConstants.REQUEST_HASH_KEY, requestHash);

                if (cache.containsKey(requestHash) &&
                    cache.getResponseForKey(requestHash) instanceof CachedObject) {
                    
                    // get the response from the cache and attach to the context and change the
                    // direction of the message
                    CachedObject cachedObj
                        = (CachedObject) cache.getResponseForKey(requestHash);
                    
                    if (!cachedObj.isExpired() && cachedObj.getResponseEnvelope() != null) {

                        if (traceOrDebugOn) {
                            traceOrDebug(traceOn,
                                "Cache-hit occures for the message : " + synCtx.getMessageID());
                        }
                        synCtx.setResponse(true);
                        synCtx.setEnvelope(cachedObj.getResponseEnvelope());
                        if (onCacheHit != null) {
                            // if there is an onCacheHit use that for the mediation
                            if (traceOrDebugOn) {
                                traceOrDebug(traceOn, "Mediating the message using the " +
                                    "onCachingHit Anonymous sequence");
                            }
                            onCacheHit.mediate(synCtx);
                        } else if (onCacheHitRef != null) {
                            
                            if (traceOrDebugOn) {
                                traceOrDebug(traceOn, "Mediating the message using the " +
                                    "onCachingHit sequence : " + onCacheHitRef);
                            }
                            synCtx.getSequence(onCacheHitRef).mediate(synCtx);

                        } else {
                            
                            if (traceOrDebugOn) {
                                traceOrDebug(traceOn, "Request message "
                                    + synCtx.getMessageID() + " has surved from the cache");
                            }
                            // send the response back if there is not onCacheHit is specified
                            synCtx.setTo(null);
                            Axis2Sender.sendBack(synCtx);
                        }
                    } else {

                        cachedObj.clearCache();

                        if (traceOrDebugOn) {
                            traceOrDebug(
                                traceOn, "Cached response has expired and hence cleared");
                            traceOrDebug(traceOn, "End : Clone mediator");
                        }
                        return true;
                    }

                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "End : Clone mediator");
                    }
                    return false;
                    
                } else {
                    
                    if (cache.getCache().size() == inMemoryCacheSize) {
                        cache.removeExpiredResponses();
                        if (cache.getCache().size() == inMemoryCacheSize) {
                            if (log.isDebugEnabled()) {
                                log.debug("In-Memory cache size exceeded and there are no " +
                                    "expired caches unable to store the cache");
                            }

                            // finalize tracing and debugging
                            if (traceOrDebugOn) {
                                traceOrDebug(traceOn, "End : Clone mediator");
                            }
                            
                            return true;
                        }
                    }
                    
                    CachedObject cachedObj = new CachedObject();
                    cachedObj.setRequestEnvelope(
                        MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope()));
                    cachedObj.setRequestHash(requestHash);
                    cachedObj.setTimeout(timeout);
                    cache.addResponseWithKey(requestHash, cachedObj);
                }
            }

        } catch (AxisFault fault) {
            handleException("Error occured in the caching mediator processing", fault, synCtx);
        }

        // finalize tracing and debugging
        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Clone mediator");
        }

        return true;
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

    public SequenceMediator getOnCacheHit() {
        return onCacheHit;
    }

    public void setOnCacheHit(SequenceMediator onCacheHit) {
        this.onCacheHit = onCacheHit;
    }

    public String getOnCacheHitRef() {
        return onCacheHitRef;
    }

    public void setOnCacheHitRef(String onCacheHitRef) {
        this.onCacheHitRef = onCacheHitRef;
    }
}
