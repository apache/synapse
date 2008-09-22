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

package org.apache.synapse.transport.testkit.axis2.client;

import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.testkit.util.LogManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class LogAspect {
    private static final Log log = LogFactory.getLog(LogAspect.class);
    
    @Around("call(void org.apache.axis2.transport.MessageFormatter.writeTo(" +
    		"       org.apache.axis2.context.MessageContext, org.apache.axiom.om.OMOutputFormat," +
    		"       java.io.OutputStream, boolean))" +
    		" && args(msgContext, format, out, preserve)")
    public void aroundWriteTo(ProceedingJoinPoint proceedingJoinPoint,
            MessageContext msgContext, OMOutputFormat format, OutputStream out, boolean preserve)
            throws Throwable {
        OutputStream log = LogManager.INSTANCE.createLog("formatter");
        try {
            OutputStream tee = new TeeOutputStream(out, log);
            proceedingJoinPoint.proceed(new Object[] { msgContext, format, tee, preserve });
        } finally {
            log.close();
        }
    }
    
    @AfterReturning(
        pointcut="call(javax.activation.DataSource org.apache.axis2.format.MessageFormatterEx.getDataSource(..))",
        returning="dataSource")
    public void afterGetDataSource(DataSource dataSource) {
        try {
            OutputStream out = LogManager.INSTANCE.createLog("formatter");
            try {
                InputStream in = dataSource.getInputStream();
                try {
                    IOUtils.copy(in, out);
                } finally {
                    in.close();
                }
            } finally {
                out.close();
            }
        } catch (Throwable ex) {
            log.error("Unable to dump message", ex);
        }
    }
}
