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
package org.apache.synapse.mediators.transform;

import junit.framework.TestCase;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.HeaderType;
import org.apache.synapse.mediators.TestUtils;
import org.apache.axiom.om.xpath.AXIOMXPath;

public class HeaderMediatorTest extends TestCase {

    private static final String TEST_HEADER = "http://server/path";

    public void testSimpleHeaderSetAndRemove() throws Exception {

        HeaderMediator headerMediator = new HeaderMediator();
        headerMediator.setName(HeaderType.STR_TO);
        headerMediator.setValue(TEST_HEADER);

        // invoke transformation, with static enveope
        SynapseMessageContext synCtx = TestUtils.getTestContext("<empty/>");
        headerMediator.mediate(synCtx);

        assertTrue(TEST_HEADER.equals(synCtx.getTo().getAddress()));

        // set the header mediator as a remove-header
        headerMediator.setAction(HeaderMediator.ACTION_REMOVE);
        headerMediator.mediate(synCtx);

        assertTrue(synCtx.getTo() == null);
    }

    public void testSimpleHeaderXPathSetAndRemove() throws Exception {

        HeaderMediator headerMediator = new HeaderMediator();
        headerMediator.setName(HeaderType.STR_TO);
        headerMediator.setExpression(new AXIOMXPath("concat('http://','server','/path')"));

        // invoke transformation, with static enveope
        SynapseMessageContext synCtx = TestUtils.getTestContext("<empty/>");
        headerMediator.mediate(synCtx);

        assertTrue(TEST_HEADER.equals(synCtx.getTo().getAddress()));

        // set the header mediator as a remove-header
        headerMediator.setAction(HeaderMediator.ACTION_REMOVE);
        headerMediator.mediate(synCtx);

        assertTrue(synCtx.getTo() == null);
    }


}
