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

package org.apache.synapse;

import javax.xml.namespace.QName;

/**
 * This startup interface will be instantiated to create startup tasks.
 */
public interface Startup extends ManagedLifecycle {

    /**
     * This will return the configuration tag QName of the implemented startup
     *
     * @return QName representing the configuraiton element for the startup
     */
    public abstract QName getTagQName();

    /**
     * This will return the name of the startup
     *
     * @return String representing the name
     */
    public String getName();

    /**
     * This will set the name of a Startup
     *
     * @param id String name to be set to the startup
     */
    public void setName(String id);
}
