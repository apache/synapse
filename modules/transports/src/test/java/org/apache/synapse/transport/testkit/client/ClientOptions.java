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

package org.apache.synapse.transport.testkit.client;

import javax.mail.internet.ContentType;

import org.apache.axiom.om.util.UUIDGenerator;

public class ClientOptions {
    private final ContentType baseContentType;
    private final String charset;
    private String mimeBoundary;
    private String rootContentId;

    public ClientOptions(ContentType baseContentType, String charset) {
        this.baseContentType = baseContentType;
        this.charset = charset;
    }

    public ContentType getBaseContentType() {
        return baseContentType;
    }

    public String getCharset() {
        return charset;
    }
    
    public String getMimeBoundary() {
        if (mimeBoundary == null) {
            mimeBoundary =
                    "MIMEBoundary"
                            + UUIDGenerator.getUUID().replace(':', '_');

        }
        return mimeBoundary;
    }

    public String getRootContentId() {
        if (rootContentId == null) {
            rootContentId =
                    "0."
                            + UUIDGenerator.getUUID()
                            + "@apache.org";
        }
        return rootContentId;
    }
}
