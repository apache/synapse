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
package org.apache.synapse.mediators.bsf.convertors;

import junit.framework.TestCase;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.mediators.TestUtils;

public class JSOMElementConvertorTest extends TestCase {
    
    public static final String XML = "<a><b>petra</b></a>";

    public void testToAndFromScript() {
        JSOMElementConvertor convertor = new JSOMElementConvertor();
        Object o = convertor.toScript(TestUtils.createOMElement(XML));
        OMElement om = convertor.fromScript(o);
        assertEquals(XML, om.toString());
    }

}
