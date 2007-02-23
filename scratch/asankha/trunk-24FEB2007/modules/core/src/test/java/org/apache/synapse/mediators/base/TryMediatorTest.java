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

package org.apache.synapse.mediators.base;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.TestMediateHandler;
import org.apache.synapse.mediators.TestMediator;
import org.apache.synapse.mediators.TestUtils;

public class TryMediatorTest extends TestCase {

    private StringBuffer result = new StringBuffer();

    public void testNoErrorsNoFinally() throws Exception {

        TestMediator t1 = new TestMediator();
        t1.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("T1.");
                }
            });
        TestMediator t2 = new TestMediator();
        t2.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("T2.");
                }
            });
        TestMediator t3 = new TestMediator();
        t3.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("T3");
                }
            });
        TestMediator te = new TestMediator();
        te.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("TE");
                }
            });

        TryMediator tryMediator = new TryMediator();
        tryMediator.addChild(t1);
        tryMediator.addChild(t2);
        tryMediator.addChild(t3);
        tryMediator.getErrorHandlerMediators().add(te);

        // invoke transformation, with static enveope
        MessageContext synCtx = TestUtils.getTestContext("<empty/>");

        tryMediator.mediate(synCtx);

        assertTrue("T1.T2.T3".equals(result.toString()));
    }

    public void testNoErrors() throws Exception {

        TestMediator t1 = new TestMediator();
        t1.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("T1.");
                }
            });
        TestMediator t2 = new TestMediator();
        t2.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("T2.");
                }
            });
        TestMediator t3 = new TestMediator();
        t3.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("T3.");
                }
            });
        TestMediator te = new TestMediator();
        te.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("TE");
                }
            });
        TestMediator tf = new TestMediator();
        tf.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("TF");
                }
            });

        TryMediator tryMediator = new TryMediator();
        tryMediator.addChild(t1);
        tryMediator.addChild(t2);
        tryMediator.addChild(t3);
        tryMediator.getErrorHandlerMediators().add(te);
        tryMediator.getFinallyMediators().add(tf);

        // invoke transformation, with static enveope
        MessageContext synCtx = TestUtils.getTestContext("<empty/>");

        tryMediator.mediate(synCtx);

        assertTrue("T1.T2.T3.TF".equals(result.toString()));
    }

    public void testErrors() throws Exception {

        TestMediator t1 = new TestMediator();
        t1.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("T1.");
                }
            });
        TestMediator t2 = new TestMediator();
        t2.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("T2.");
                    throw new SynapseException("test");
                }
            });
        TestMediator t3 = new TestMediator();
        t3.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("T3.");
                }
            });
        TestMediator te = new TestMediator();
        te.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("TE.");
                }
            });
        TestMediator tf = new TestMediator();
        tf.setHandler(
            new TestMediateHandler() {
                public void handle(MessageContext synCtx) {
                    result.append("TF");
                }
            });

        TryMediator tryMediator = new TryMediator();
        tryMediator.addChild(t1);
        tryMediator.addChild(t2);
        tryMediator.addChild(t3);
        tryMediator.getErrorHandlerMediators().add(te);
        tryMediator.getFinallyMediators().add(tf);

        // invoke transformation, with static enveope
        MessageContext synCtx = TestUtils.getTestContext("<empty/>");

        tryMediator.mediate(synCtx);

        assertTrue("T1.T2.TE.TF".equals(result.toString()));
    }
}
