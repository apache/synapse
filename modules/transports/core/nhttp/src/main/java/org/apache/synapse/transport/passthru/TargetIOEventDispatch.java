/*
 *  Copyright 2013 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru;

import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpClientIOTarget;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.params.HttpParams;
import org.apache.synapse.transport.passthru.logging.LoggingUtils;

/**
 * This is a factory for creating the logging sessions or non-logging sessions.
 */
public class TargetIOEventDispatch extends DefaultClientIOEventDispatch {

    public TargetIOEventDispatch(final NHttpClientHandler handler, final HttpParams params) {
        super(LoggingUtils.decorate(handler), params);
    }

    @Override
    protected NHttpClientIOTarget createConnection(IOSession session) {
        session = LoggingUtils.decorate(session, "client");
        return LoggingUtils.createClientConnection(
                session,
                createHttpResponseFactory(),
                this.allocator,
                this.params);
    }
}
