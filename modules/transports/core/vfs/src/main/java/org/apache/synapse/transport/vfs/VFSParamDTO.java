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
package org.apache.synapse.transport.vfs;

/**
 * Carries the auto lock release settings of a poll table entry down to
 * {@link VFSUtils#acquireLock(org.apache.commons.vfs2.FileSystemManager,
 * org.apache.commons.vfs2.FileObject, VFSParamDTO)}.
 */
public class VFSParamDTO {

    private boolean autoLockRelease;
    private Long autoLockReleaseInterval;

    public VFSParamDTO() {
        autoLockRelease = false;
        autoLockReleaseInterval = VFSConstants.DEFAULT_AUTO_LOCK_RELEASE_INTERVAL;
    }

    /**
     * @return the autoLockRelease
     */
    public boolean isAutoLockRelease() {
        return autoLockRelease;
    }

    /**
     * @param autoLockRelease the autoLockRelease to set
     */
    public void setAutoLockRelease(boolean autoLockRelease) {
        this.autoLockRelease = autoLockRelease;
    }

    /**
     * @return the autoLockReleaseInterval
     */
    public Long getAutoLockReleaseInterval() {
        return autoLockReleaseInterval;
    }

    /**
     * @param autoLockReleaseInterval the autoLockReleaseInterval to set
     */
    public void setAutoLockReleaseInterval(Long autoLockReleaseInterval) {
        this.autoLockReleaseInterval = autoLockReleaseInterval;
    }
}
