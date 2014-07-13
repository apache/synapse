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

package org.apache.synapse.transport.nhttp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpClientIOTarget;
import org.apache.http.nio.NHttpServerIOTarget;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOSession;

public class LoggingUtils {
    
    public static String HEADER_LOG_ID = "org.apache.synapse.transport.nhttp.headers"; 

    public static IOSession decorate(IOSession session, final String id) {
        Log log = LogFactory.getLog(session.getClass());
        if (log.isDebugEnabled()) {
            session = new LoggingIOSession(log, session, id);
        }
        return session;
    }
    
    public static NHttpClientIOTarget decorate(NHttpClientIOTarget target) {
        Log log = LogFactory.getLog(target.getClass());
        Log headerlog = LogFactory.getLog(HEADER_LOG_ID);
        if (log.isDebugEnabled() || headerlog.isDebugEnabled()) {
            target = new LoggingNHttpClientIOTarget(log, headerlog, target);
        }
        return target;
    }

    public static NHttpServerIOTarget decorate(NHttpServerIOTarget target) {
        Log log = LogFactory.getLog(target.getClass());
        Log headerlog = LogFactory.getLog(HEADER_LOG_ID);
        if (log.isDebugEnabled() || headerlog.isDebugEnabled()) {
            target = new LoggingNHttpServerIOTarget(log, headerlog, target);
        }
        return target;
    }

    public static NHttpClientHandler decorate(NHttpClientHandler handler) {
        Log log = LogFactory.getLog(handler.getClass());
        Log headerlog = LogFactory.getLog(HEADER_LOG_ID);
        if (log.isDebugEnabled() || headerlog.isDebugEnabled()) {
            handler = new LoggingNHttpClientHandler(log, headerlog, handler);
        }
        return handler;
    }

    public static NHttpServiceHandler decorate(NHttpServiceHandler handler) {
        Log log = LogFactory.getLog(handler.getClass());
        Log headerlog = LogFactory.getLog(HEADER_LOG_ID);
        if (log.isDebugEnabled() || headerlog.isDebugEnabled()) {
            handler = new LoggingNHttpServiceHandler(log, headerlog, handler);
        }
        return handler;
    }

}