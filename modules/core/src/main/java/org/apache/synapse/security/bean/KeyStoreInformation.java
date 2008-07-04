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
package org.apache.synapse.security.bean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;

/**
 * Encapsulates the keyStore related information
 */
public class KeyStoreInformation {

    private static final Log log = LogFactory.getLog(KeyStoreInformation.class);

    private String storeType = "JKS";
    private String alias;
    private String location;


    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        if (storeType == null || "".equals(storeType)) {
            if (log.isDebugEnabled()) {
                log.debug("Given store type is null , using default type : JKS");
            }
        }
        this.storeType = storeType;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        if (alias == null || "".equals(alias)) {
            handleException("Alias for a key entry or a certificate cannot be null");
        }
        this.alias = alias;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        if (location != null && "".equals(location)) {
            handleException("KeyStore location can not be null");
        }
        this.location = location;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
