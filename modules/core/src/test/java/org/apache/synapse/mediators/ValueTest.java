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

package org.apache.synapse.mediators;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.util.xpath.SynapseXPath;

/**
 * Tests for {@link Value#evalutePropertyExpression(MessageContext)}, which evaluates a dynamic
 * ("{{...}}") template parameter once and re-compiles the resulting string as a new XPath
 * expression. The re-compiled expression keeps working for ordinary functions, but a
 * {@code document()} or {@code evaluate()} call in it is reported as an error instead of being
 * turned into an evaluatable expression.
 */
public class ValueTest extends TestCase {

    /**
     * Builds an escaped-expression {@link Value} whose first evaluation returns exactly
     * {@code recompiledExpression}. The inner text of the {{...}} form is wrapped as an XPath
     * string literal, so the result does not depend on any property or message content.
     *
     * @param recompiledExpression the XPath text the first evaluation should yield
     * @return a Value that re-compiles {@code recompiledExpression} on evaluation
     */
    private Value escapedValueYielding(String recompiledExpression) {
        return new Value("{'" + recompiledExpression + "'}");
    }

    private void assertRejected(String recompiledExpression) throws Exception {
        MessageContext synCtx = TestUtils.getTestContext("<test/>");
        try {
            escapedValueYielding(recompiledExpression).evalutePropertyExpression(synCtx);
            fail("expected a SynapseException for re-compiled expression: " + recompiledExpression);
        } catch (SynapseException expected) {
            // the re-compiled expression calls a restricted function and must not be evaluated
        }
    }

    /** A re-compiled expression without a restricted function must still be returned. */
    public void testBenignExpressionIsStillRecompiled() throws Exception {
        MessageContext synCtx = TestUtils.getTestContext("<test/>");
        Object result =
                escapedValueYielding("concat(\"F010\",\"-\",\"PWN\")").evalutePropertyExpression(synCtx);
        assertTrue("a re-compiled expression without a restricted function must be returned as "
                + "a SynapseXPath", result instanceof SynapseXPath);
    }

    public void testDocumentFunctionIsRejected() throws Exception {
        assertRejected("document(\"http://example.com/f010\")");
    }

    public void testDocumentFunctionTwoArgFormIsRejected() throws Exception {
        assertRejected("document(\"http://example.com/f010\", /)");
    }

    public void testEvaluateFunctionIsRejected() throws Exception {
        assertRejected("evaluate(\"count(//a)\")");
    }

    public void testDocumentNestedInFunctionArgumentIsRejected() throws Exception {
        assertRejected("concat(document(\"http://example.com/f010\"), \"\")");
    }

    public void testDocumentWrappedInFunctionIsRejected() throws Exception {
        assertRejected("string(document(\"http://example.com/f010\"))");
    }

    public void testDocumentInStepPredicateIsRejected() throws Exception {
        assertRejected("//a[document(\"http://example.com/f010\")]");
    }

    /**
     * Only the unprefixed document()/evaluate() are restricted; a namespaced function that
     * happens to share the local name is a different function and is left alone.
     */
    public void testDocumentWithUnboundPrefixIsRejected() throws Exception {
        assertRejected("ns:document(\"http://example.com/f010\")");
    }

    public void testEvaluateWithUnboundPrefixIsRejected() throws Exception {
        assertRejected("foo:evaluate(\"count(//a)\")");
    }
}
