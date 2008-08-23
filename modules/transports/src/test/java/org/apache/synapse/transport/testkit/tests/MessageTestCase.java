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

package org.apache.synapse.transport.testkit.tests;

import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.name.Key;

public class MessageTestCase extends TransportTestCase {
    protected final ContentTypeMode contentTypeMode;
    protected final String contentType;

    public MessageTestCase(ContentTypeMode contentTypeMode, String contentType, Object... resources) {
        super(resources);
        this.contentTypeMode = contentTypeMode;
        this.contentType = contentType;
    }

    @Key("contentTypeMode")
    public ContentTypeMode getContentTypeMode() {
        return contentTypeMode;
    }
}
