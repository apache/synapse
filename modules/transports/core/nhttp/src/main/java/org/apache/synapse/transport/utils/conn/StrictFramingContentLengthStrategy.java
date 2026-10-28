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

package org.apache.synapse.transport.utils.conn;

import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.ProtocolException;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.protocol.HTTP;

/**
 * Inbound {@link ContentLengthStrategy} that rejects ambiguous message framing. A message with
 * more than one {@code Content-Length}, more than one {@code Transfer-Encoding}, or both
 * {@code Content-Length} and {@code Transfer-Encoding} has no single body boundary, so it is
 * refused with a {@link ProtocolException} rather than resolved to one interpretation.
 * Otherwise length is computed by the delegate {@link StrictContentLengthStrategy}.
 */
public class StrictFramingContentLengthStrategy implements ContentLengthStrategy {

    public static final StrictFramingContentLengthStrategy INSTANCE =
            new StrictFramingContentLengthStrategy();

    private final ContentLengthStrategy delegate = StrictContentLengthStrategy.INSTANCE;

    @Override
    public long determineLength(final HttpMessage message) throws HttpException {
        if (message.getHeaders(HTTP.CONTENT_LEN).length > 1) {
            throw new ProtocolException("Multiple Content-Length headers");
        }
        if (message.getHeaders(HTTP.TRANSFER_ENCODING).length > 1) {
            throw new ProtocolException("Multiple Transfer-Encoding headers");
        }
        if (message.containsHeader(HTTP.TRANSFER_ENCODING)
                && message.containsHeader(HTTP.CONTENT_LEN)) {
            throw new ProtocolException(
                    "Content-Length and Transfer-Encoding headers are both present");
        }
        return delegate.determineLength(message);
    }
}
