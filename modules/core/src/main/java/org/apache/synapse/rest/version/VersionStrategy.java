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
package org.apache.synapse.rest.version;

/**
 * This class is responsible for version delegation for API resources
 */
public interface VersionStrategy {

    /**
     * check if version matches from the version ifo object.
     * @param versionInfoObj object that can be used to extract data about the version
     * @return
     */
    public boolean isMatchingVersion(Object versionInfoObj);

    /**
     * get the assigned version for the delegated object
     * @return string representation for the version
     */
    public String getVersion();

    /**
     * returns version type information. ie:- version can be embedded in request URL , HTTP header
     * , etc
     * @return version type String
     */
    public String getVersionType();

    /**
     * returns type information ie:- location regex/string that can be used t extract version..
     * @return version param
     */
    public String getVersionParam();


}
