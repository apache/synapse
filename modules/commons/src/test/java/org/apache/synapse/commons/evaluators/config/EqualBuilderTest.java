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

package org.apache.synapse.commons.evaluators.config;

import junit.framework.TestCase;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.commons.evaluators.EvaluatorConstants;
import org.apache.synapse.commons.evaluators.EqualEvaluator;

public class EqualBuilderTest extends TestCase {

    private static final String SOURCE = "foo";
    private static final String VALUE = "bar";

    private EqualFactory fac = new EqualFactory();

    public void testHeaderMatch() {
        String input = "<equal type=\"header\" source=\"" + SOURCE +
                "\" value=\"" + VALUE + "\"/>";

        try {
            EqualEvaluator eval = (EqualEvaluator) fac.create(AXIOMUtil.stringToOM(input));
            assertEquals(eval.getType(), EvaluatorConstants.TYPE_HEADER);
            assertEquals(eval.getSource(), SOURCE);
            assertEquals(eval.getValue(), VALUE);
        } catch (Exception e) {
            fail("Error while parsing the input XML");
        }
    }

    public void testParameterMatch() {
        String input = "<equal type=\"param\" source=\"" + SOURCE +
                "\" value=\"" + VALUE + "\"/>";

        try {
            EqualEvaluator eval = (EqualEvaluator) fac.create(AXIOMUtil.stringToOM(input));
            assertEquals(eval.getType(), EvaluatorConstants.TYPE_PARAM);
            assertEquals(eval.getSource(), SOURCE);
            assertEquals(eval.getValue(), VALUE);
        } catch (Exception e) {
            fail("Error while parsing the input XML");
        }
    }

    public void testURLMatch() {
        String input = "<equal type=\"url\" value=\"" + VALUE + "\"/>";

        try {
            EqualEvaluator eval = (EqualEvaluator) fac.create(AXIOMUtil.stringToOM(input));
            assertEquals(eval.getType(), EvaluatorConstants.TYPE_URL);
            assertNull(eval.getSource());
            assertEquals(eval.getValue(), VALUE);
        } catch (Exception e) {
            fail("Error while parsing the input XML");
        }
    }
}
