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

package org.apache.synapse.transport.fix.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.context.MessageContext;

public class FIXMessageFormatterTest extends TestCase {

	public void testWriteTo() throws Exception {

		String input = "8=FIX.4.0\u00019=105\u000135=D\u000134=2\u000149=BANZAI\u0001" +
                "52=20080711-06:42:26\u000156=SYNAPSE\u000111=1215758546278\u000121=1\u0001" +
                "38=90000000\u000140=1\u000154=1\u000155=DEL\u000159=0\u000110=121\u0001";

		MessageContext msgCtx = new MessageContext();
		FIXMessageBuilder builder = new FIXMessageBuilder();
		OMElement element = builder.processDocument(new ByteArrayInputStream(input.getBytes()),
                "fix/j", msgCtx);
        assertNotNull(element);

        FIXMessageFormatter fixMessageFormatter = new FIXMessageFormatter();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		SOAPFactory factory = OMAbstractFactory.getSOAP12Factory();
		SOAPEnvelope env = factory.getDefaultEnvelope();
		env.getBody().addChild(element);
        msgCtx.setEnvelope(env);

		OMOutputFormat myOutputFormat = new OMOutputFormat();
        fixMessageFormatter.writeTo(msgCtx, myOutputFormat, output, false);
        assertTrue(output.toByteArray().length > 0);
	}

}
