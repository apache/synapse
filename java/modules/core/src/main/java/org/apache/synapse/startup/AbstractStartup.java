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

package org.apache.synapse.startup;

import org.apache.synapse.Startup;

/**
 * 
 */
public abstract class AbstractStartup implements Startup {

    /**
     * Holds the id of a Startup
     */
    private String id = null;

    /**
     * This will return the id of the startup
     *
     * @return String representing the id
     */
    public String getId() {
        return this.id;
    }

    /**
     * This will set the id of a Startup
     *
     * @param id
     *          String id to be set to the startup
     */
    public void setId(String id) {
        this.id = id;
    }
}
