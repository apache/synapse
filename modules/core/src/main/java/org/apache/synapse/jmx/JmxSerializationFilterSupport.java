/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.jmx;

import org.apache.commons.logging.Log;

import java.io.ObjectInputFilter;
import java.util.function.BinaryOperator;

/**
 * Installs JEP 415 context-specific deserialization control for remote JMX without using
 * {@code ObjectInputFilter.Config.setSerialFilter} for the entire JVM.
 * <p>
 * Synapse targets Java 17+; the configured JMX pattern is merged with the stream filter only when
 * the stack indicates deserialization for {@code javax.management.remote} / {@code com.sun.jmx.remote}.
 */
public final class JmxSerializationFilterSupport {

    private JmxSerializationFilterSupport() {
    }

    /**
     * Installs a {@link ObjectInputFilter.Config#setSerialFilterFactory serial filter factory}
     * that merges the given pattern for JMX/RMI deserialization stacks.
     *
     * @param pattern non-empty filter pattern (same string as {@code synapse.jmx.remote.serial.filter.pattern})
     * @param log     logger (typically {@link org.apache.synapse.JmxAdapter})
     */
    public static void configureForJmxPattern(String pattern, Log log) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return;
        }
        try {
            ObjectInputFilter jmxFilter = ObjectInputFilter.Config.createFilter(pattern);
            BinaryOperator<ObjectInputFilter> factory = (curr, next) -> {
                ObjectInputFilter base = next != null ? next : curr;
                if (base == null) {
                    base = ObjectInputFilter.Config.getSerialFilter();
                }
                if (!isJmxRemoteDeserializationStack()) {
                    return base;
                }
                return ObjectInputFilter.merge(jmxFilter, base);
            };
            ObjectInputFilter.Config.setSerialFilterFactory(factory);
            log.info("Installed JEP 415 serial filter factory for JMX deserialization");
            if (log.isDebugEnabled()) {
                log.debug("JEP 415 serial filter factory installed for JMX/RMI stacks");
            }
        } catch (IllegalStateException e) {
            log.warn("Serial filter factory already set; leaving existing factory in place: "
                    + e.getMessage());
        } catch (RuntimeException e) {
            if (log.isDebugEnabled()) {
                log.debug("JEP 415 serial filter factory failed: " + e.getMessage(), e);
            }
            log.warn("Could not install JEP 415 serial filter factory; check JDK configuration.");
        }
    }

    /**
     * Detect JMX-over-RMI deserialization by stack frames.
     */
    static boolean isJmxRemoteDeserializationStack() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < trace.length && i < 40; i++) {
            String name = trace[i].getClassName();
            if (name.startsWith("javax.management.remote.")
                    || name.startsWith("com.sun.jmx.remote")) {
                return true;
            }
        }
        return false;
    }
}
