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

package org.apache.synapse.transport.utils.sslcert;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.description.Parameter;

import javax.xml.namespace.QName;

public class CertificateVerificationConfig {

    private Integer cacheSize = Constants.CACHE_DEFAULT_ALLOCATED_SIZE;
    private Integer cacheDuration = Constants.CACHE_DEFAULT_DURATION_MINS;

    public CertificateVerificationConfig(Parameter param) {
        if (param == null) {
            throw new IllegalArgumentException("Parameter must not be null");
        }
        OMElement element = param.getParameterElement();
        OMElement sizeElement = element.getFirstChildWithName(new QName("CacheSize"));
        if (sizeElement != null) {
            cacheSize = new Integer(sizeElement.getText());
        }
        OMElement durationElement = element.getFirstChildWithName(new QName("CacheDurationMins"));
        if (durationElement != null) {
            cacheDuration = new Integer(durationElement.getText());
        }
    }

    public Integer getCacheSize() {
        return cacheSize;
    }

    public Integer getCacheDuration() {
        return cacheDuration;
    }
}
