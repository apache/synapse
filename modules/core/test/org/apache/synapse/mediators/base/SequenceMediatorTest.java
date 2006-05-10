/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse.mediators.base;

import junit.framework.TestCase;
import junit.framework.Test;
import org.apache.synapse.mediators.TestMediator;
import org.apache.synapse.mediators.TestMediateHandler;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.SynapseMessageContext;

public class SequenceMediatorTest extends TestCase {

    private StringBuffer result = new StringBuffer();

    public void testSequenceMediator() throws Exception {

        TestMediator t1 = new TestMediator();
        t1.setHandler(
            new TestMediateHandler() {
                public void handle(SynapseMessageContext synCtx) {
                    result.append("T1.");
                }
            });
        TestMediator t2 = new TestMediator();
        t2.setHandler(
            new TestMediateHandler() {
                public void handle(SynapseMessageContext synCtx) {
                    result.append("T2.");
                }
            });
        TestMediator t3 = new TestMediator();
        t3.setHandler(
            new TestMediateHandler() {
                public void handle(SynapseMessageContext synCtx) {
                    result.append("T3");
                }
            });

        SequenceMediator seq = new SequenceMediator();
        seq.addChild(t1);
        seq.addChild(t2);
        seq.addChild(t3);

        // invoke transformation, with static enveope
        SynapseMessageContext synCtx = TestUtils.getTestContext("<empty/>");
        seq.mediate(synCtx);

        assertTrue("T1.T2.T3".equals(result.toString()));
    }
}
