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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.utils.sslcert.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Cache Manager takes care of and maintains an LRU cache which implements ManageableCache Interface.
 * Duration should be configured such that cacheManager is not too much involved with the cache,
 * but manages it optimally.
 */
public class CacheManager {

    private static final Log log = LogFactory.getLog(CacheManager.class);

    private final boolean DO_NOT_INTERRUPT_IF_RUNNING = false;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture scheduledFuture = null;
    private ManageableCache cache;
    private int cacheMaxSize;
    private int duration;
    private CacheManagingTask cacheManagingTask;

    /**
     * A new cacheManager will be started on the given ManageableCache object.
     *
     * @param cache        a Manageable Cache which could be managed by this cache manager.
     * @param cacheMaxSize Maximum size of the cache. If the cache exceeds this size, LRU values
     *                     will be removed
     */
    public CacheManager(ManageableCache cache, int cacheMaxSize, int duration) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        this.cache = cache;
        this.cacheMaxSize = cacheMaxSize;
        this.cacheManagingTask = new CacheManagingTask();
        this.duration = duration;
        start();
    }

    /**
     * To Start the CacheManager. Should be called only once per CacheManager hence called in
     * constructor. CacheManager will run its scheduled task every "duration" number of minutes.
     */
    private boolean start() {
        if (scheduledFuture == null || (scheduledFuture.isCancelled())) {
            scheduledFuture = scheduler.scheduleWithFixedDelay(cacheManagingTask,
                    duration, duration, TimeUnit.MINUTES);
            log.info(cache.getClass().getSimpleName()+" Cache Manager Started");
            return true;
        }
        return false;
    }

    /**
     * Used to wake cacheManager up at will. If this method is called while its task is running, it
     * will run its task again soon after its done. CacheManagerTask will be rescheduled as before.
     * @return true if successfully waken up. false otherwise.
     */
    public boolean wakeUpNow(){
        if (scheduledFuture !=null) {
            if (!scheduledFuture.isCancelled()) {
                scheduledFuture.cancel(DO_NOT_INTERRUPT_IF_RUNNING);
            }
            scheduledFuture = scheduler.scheduleWithFixedDelay(cacheManagingTask,
                    0, duration,TimeUnit.MINUTES);
            log.info(cache.getClass().getSimpleName()+" Cache Manager Wakened Up.....");
            return true;
        }
        return false;
    }

    /**
     * Change the cacheManager duration (schedule period) to given value.
     * @param duration new duration to which the cacheManager schedule period should change.
     * @return true if successfully changed. false otherwise.
     * @throws IllegalArgumentException if given duration is not between the allowed limit.
     */
    public boolean changeDuration(int duration) throws IllegalArgumentException {
        int min = Constants.CACHE_MIN_DURATION_MINS;
        int max = Constants.CACHE_MAX_DURATION_MINS;
        if (duration < min || duration > max) {
            throw new IllegalArgumentException("Duration time should should be between " + min +
                    " and " + max + " minutes");
        }
        this.duration = duration;
        return wakeUpNow();
    }

    public int getDuration(){
        return duration;
    }

    /**
     * Gracefully stop cacheManager.
     */
    public boolean stop(){
        if (scheduledFuture !=null && !scheduledFuture.isCancelled()){
            scheduledFuture.cancel(DO_NOT_INTERRUPT_IF_RUNNING);
            log.info(cache.getClass().getSimpleName()+" Cache Manager Stopped.....");
            return true;
        }
        return false;
    }

    public boolean isRunning() {
        return !scheduledFuture.isCancelled();
    }

    /**
     * This is the Scheduled Task the CacheManager uses in order to remove invalid cache values and
     * to remove LRU values if the cache reaches cacheMaxSize.
     */
    private class CacheManagingTask implements Runnable {

        public void run() {
            long start = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug(cache.getClass().getSimpleName() + " Cache Manager Task Started.");
            }

            ManageableCacheValue nextCacheValue;
            //cache.getCacheSize() can vary when new entries are added. So get cache size at this point
            int cacheSize = cache.getCacheSize();
            int numberToRemove = (cacheSize>cacheMaxSize)?  cacheSize - cacheMaxSize: 0;

            List<ManageableCacheValue> entriesToRemove = new ArrayList<ManageableCacheValue>();
            LRUEntryCollector lruEntryCollector = new LRUEntryCollector(entriesToRemove, numberToRemove);

            //Start looking at cache entries from the beginning.
            cache.resetIterator();
            //Iteration through the cache entries.
            while ((cacheSize--) > 0) {
                nextCacheValue = cache.getNextCacheValue();
                if (nextCacheValue == null) {
                    log.debug("Cache manager iteration through Cache values done");
                    break;
                }

                //Updating invalid cache values
                if (!nextCacheValue.isValid()) {
                    log.debug("Updating Invalid Cache Value by Manager");
                    nextCacheValue.updateCacheWithNewValue();
                }

                //There are LRU entries to be removed since cacheSize > maxCacheSize. So collect them.
                if (numberToRemove > 0) {
                    lruEntryCollector.collectEntriesToRemove(nextCacheValue);
                }
            }

            //LRU entries removing
            for (ManageableCacheValue oldCacheValue: entriesToRemove) {
                log.debug("Removing LRU value from cache");
                oldCacheValue.removeThisCacheValue();
            }

            if (log.isDebugEnabled()) {
                log.debug(cache.getClass().getSimpleName()+" Cache Manager Task Done. Took " +
                        (System.currentTimeMillis() - start) + " ms.");
            }
        }

        private class LRUEntryCollector {

            private List<ManageableCacheValue> entriesToRemove;
            private int listMaxSize;

            LRUEntryCollector(List<ManageableCacheValue> entriesToRemove, int numberToRemove){
                this.entriesToRemove = entriesToRemove;
                this.listMaxSize = numberToRemove;
            }

            /**
             * This method collects the listMaxSize number of LRU values from the Cache. This is
             * called for all the entries in the cache. But only listMaxSize number of LRU entries
             * will be collected in entriesToRemove list. These collected values will be removed from
             * the cache. This uses a part of the Logic in Insertion Sort.
             *
             * @param value to be collected if LRU.
             */
            private void collectEntriesToRemove(ManageableCacheValue value) {

                entriesToRemove.add(value);
                int i = entriesToRemove.size() - 1;

                for(; i > 0 && (value.getTimeStamp() < entriesToRemove.get(i - 1).getTimeStamp()); i--) {
                    entriesToRemove.remove(i);
                    entriesToRemove.add(i,(entriesToRemove.get(i - 1)));
                }
                entriesToRemove.remove(i);
                entriesToRemove.add(i,value);
                /*
                 * First entry in the list will be the oldest. Last will be the earliest in the list.
                 * So remove the earliest since we need to collect the old (LRU) values to remove
                 * from cache later
                 */
                if (entriesToRemove.size() > listMaxSize) {
                    entriesToRemove.remove(entriesToRemove.size() - 1);
                }
            }

        }
    }
}
