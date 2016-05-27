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

package org.apache.synapse.util.xpath;

import junit.framework.TestCase;
import org.jaxen.Context;
import org.jaxen.ContextSupport;

import java.util.ArrayList;
import java.util.List;

public class Base64DecodeFunctionTest extends TestCase {

    public void testBase64DecodeFunctionWithCharset() throws Exception {
        String encodedString = "U3luYXBzZQ==";
        Base64DecodeFunction base64DecodeFunction = new Base64DecodeFunction();
        List<String> params = new ArrayList<String>();
        params.add(encodedString);
        params.add("UTF-8");
        Context context = new Context(null);
        context.setContextSupport(new ContextSupport());
        String decodedString = (String) base64DecodeFunction.call(context, params);
        assertEquals("Wrong decoded value found", "Synapse", decodedString);

    }

    public void testBase64DecodeFunctionWithoutCharset() throws Exception {
        String encodedString = "U3luYXBzZQ==";
        Base64DecodeFunction base64DecodeFunction = new Base64DecodeFunction();
        List<String> params = new ArrayList<String>();
        params.add(encodedString);
        Context context = new Context(null);
        context.setContextSupport(new ContextSupport());
        String decodedString = (String) base64DecodeFunction.call(context, params);
        assertEquals("Wrong decoded value found", "Synapse", decodedString);
    }

    public void testBase64DecodeFunctionWithoutParameters() throws Exception {
        Base64DecodeFunction base64DecodeFunction = new Base64DecodeFunction();
        List<String> params = new ArrayList<String>();
        Context context = new Context(null);
        context.setContextSupport(new ContextSupport());
        String decodedString = (String) base64DecodeFunction.call(context, params);
        assertEquals("Wrong decoded value found", "", decodedString);
    }
}
