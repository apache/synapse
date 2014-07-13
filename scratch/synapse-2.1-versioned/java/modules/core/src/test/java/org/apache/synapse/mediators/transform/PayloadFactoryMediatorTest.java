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

package org.apache.synapse.mediators.transform;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.util.xpath.SynapseXPath;

public class PayloadFactoryMediatorTest extends TestCase {

    private static final String SOURCE =
            "<m:CheckPriceRequest xmlns:m=\"http://services.samples\"><m:Code>IBM</m:Code></m:CheckPriceRequest>";

    private static final String FORMAT =
            "<m:getQuote xmlns:m=\"http://services.samples\"><m:request><m:symbol>$1</m:symbol></m:request>" +
            "</m:getQuote>";

    public void testStaticArgs() throws Exception {
        
        PayloadFactoryMediator mediator = new PayloadFactoryMediator();
        mediator.setFormat(FORMAT);
        PayloadFactoryMediator.Argument arg = new PayloadFactoryMediator.Argument();
        arg.setValue("IBM");
        mediator.addArgument(arg);

        testTransformation(mediator);
    }

    public void testExpressionArgs() throws Exception {

        PayloadFactoryMediator mediator = new PayloadFactoryMediator();
        mediator.setFormat(FORMAT);
        PayloadFactoryMediator.Argument arg = new PayloadFactoryMediator.Argument();
        SynapseXPath expression = new SynapseXPath("//m:Code");
        expression.addNamespace("m", "http://services.samples");
        arg.setExpression(expression);
        mediator.addArgument(arg);

        testTransformation(mediator);
    }

    private void testTransformation(PayloadFactoryMediator mediator) throws Exception {

        MessageContext synCtx = TestUtils.getTestContext(SOURCE);

        assertTrue(mediator.mediate(synCtx));

        SynapseXPath xpath = new SynapseXPath("//m:getQuote/m:request/m:symbol");
        xpath.addNamespace("m", "http://services.samples");
        xpath.stringValueOf(synCtx);

        assertEquals("IBM", xpath.stringValueOf(synCtx));
    }

}
