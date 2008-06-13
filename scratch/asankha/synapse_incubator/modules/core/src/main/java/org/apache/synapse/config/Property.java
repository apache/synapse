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

package org.apache.synapse.config;

import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMElement;

import java.net.URL;

public class Property {

    private static final Log log = LogFactory.getLog(Property.class);

    /**
     * The name of the registry to which this key applies
     */
    private String registryName;
    /**
     * Name of the property
     */
    private String name;
    /**
     * The type of the static proprerty
     */
    private int type;
    /**
     * Source URL of the property if it is a SRC_TYPE
     */
    private URL src;
    /**
     * The registry key
     */
    private String key;
    /**
     * The value of the property
     * This can be either an OMElement or an String
     */
    private Object value;
    /**
     * An XML to Object mapper - if one is available
     * @deprecated
     */
    private XMLToObjectMapper mapper;
    /**
     * The version of the cached resource
     */
    private long version;
    /**
     * The local expiry time for the cached resource
     */
    private long expiryTime;

    public String getRegistryName() {
        return this.registryName;
    }

    public void setRegistryName(String registryName) {
        this.registryName = registryName;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        if (type <= 4 && type >= 0)
            this.type = type;
        else
            handleException("Invalid property type for the static property");
    }

    public URL getSrc() {
        return src;
    }

    public void setSrc(URL src) {
        this.src = src;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the value of the property. String if the type is INLINE_STRING_TYPE or VALUE_TYPE,
     * OMElement otherwise.
     * @return Either an OMElement or a String
     */
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     *
     * @return mapper
     * @deprecated
     */
    public XMLToObjectMapper getMapper() {
        return mapper;
    }

    /**
     *
     * @param mapper
     * @deprecated
     */
    public void setMapper(XMLToObjectMapper mapper) {
        this.mapper = mapper;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean isExpired() {
        if(getType() == DYNAMIC_TYPE) {
            return System.currentTimeMillis() > expiryTime;
        } else {
            return false;
        }
    }

    public boolean isCached() {
        return value != null;
    }

    public boolean isDynamic() {
        return type == DYNAMIC_TYPE;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public static final int INLINE_STRING_TYPE = 0;
    public static final int INLINE_XML_TYPE = 1;
    public static final int VALUE_TYPE = 2;
    public static final int SRC_TYPE = 3;
    public static final int DYNAMIC_TYPE = 4;
}
