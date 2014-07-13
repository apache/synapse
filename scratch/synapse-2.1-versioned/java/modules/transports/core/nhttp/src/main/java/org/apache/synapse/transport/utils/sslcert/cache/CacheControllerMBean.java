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

package org.apache.synapse.transport.utils.sslcert.cache;

public interface CacheControllerMBean {

    /**
     * Gracefully stops a cacheManager scheduled thread.
     * @return true if successfully stopped. False otherwise.
     */
    public boolean stopCacheManager();

    /**
     * Wakes up a stopped cacheManager thread.
     * @return true if cacheManager is waken up. False otherwise.
     */
    public boolean wakeUpCacheManager();

    /**
     * Changes cacheManager task scheduled period.
     * @param duration Duration which cacheManager thread waits to start its task again.
     * @return true if successfully changed duration. False otherwise.
     */
    public boolean changeCacheManagerDurationMins(int duration);

    /**
     * @return true if CacheManager is running. False if its stopped.
     */
    public boolean isCacheManagerRunning();

    /**
     * @return Number of cacheEntries in the cache.
     */
    public int getCacheSize();

    /**
     * @return cacheManager duration in minutes.
     */
    public int getCacheManagerDurationMins();
}
