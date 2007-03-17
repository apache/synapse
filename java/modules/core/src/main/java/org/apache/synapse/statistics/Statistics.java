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
package org.apache.synapse.statistics;

/**
 * The statistics data structure
 */

public class Statistics {

    /**  Maximum Processing time for a one way flow  */
    private long maxProcessingTime = 0;
    /** Minmum Processing time for a one way flow   */
    private long minProcessingTime = -1;
    /** Avarage Processing time for a one way flow */
    private double avgProcessingTime = 0;
    /** Total Processing time for a one way flow  */
    private double totalProcessingTime;
    /** The number of access count for a one way flow  */
    private int count = 0;
    /** The number of fault count for a one way flow  */
    private int faultCount = 0;

    /**
     * Update the statistics
     *
     * @param inTime  - The processing start time
     * @param outTime - The processing end time
     * @param isFault - A boolean value that indicate whether falut has occured or not
     */
    public void update(long inTime, long outTime, boolean isFault) {
        count++;
        if (isFault) {
            faultCount++;
        }
        long responseTime = outTime - inTime;
        if (maxProcessingTime < responseTime) {
            maxProcessingTime = responseTime;
        }
        if (minProcessingTime > responseTime) {
            minProcessingTime = responseTime;
        }
        if (minProcessingTime == -1) {
            minProcessingTime = responseTime;
        }
        totalProcessingTime = totalProcessingTime + responseTime;
        avgProcessingTime = totalProcessingTime / count;
    }

    /**
     * @return Returns the Maximum processing time
     */
    public long getMaxProcessingTime() {
        return maxProcessingTime;
    }

    /**
     * @return Returns the Avarage processing time
     */
    public double getAvgProcessingTime() {
        return avgProcessingTime;
    }

    /**
     * @return Returns the minimum processing time
     */
    public long getMinProcessingTime() {
        return minProcessingTime;
    }

    /**
     * @return Returns the fault count
     */
    public int getFaultCount() {
        return faultCount;
    }

    /**
     * @return Returns the total count that represents number of access in a one way flow
     */
    public int getCount() {
        return count;
    }
}
