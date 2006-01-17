package org.apache.synapse.spi.processors;

import junit.framework.TestCase;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.processors.builtin.xslt.XSLTProcessor;
import org.apache.synapse.util.Axis2EnvSetup;
import org.apache.synapse.axis2.Axis2SynapseMessage;

import java.io.ByteArrayInputStream;
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
*
*/

public class XSLTProcessorTest extends TestCase {
    private String xsl = "<xsl:stylesheet version='1.0'\n" +
            "                xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>\n" +
            "    <xsl:template match=\"/\">\n" +
            "        <transformedText>\n" +
            "            <xsl:value-of select=\"//text\"/>\n" +
            "            <xsl:text>Test Being Transformed</xsl:text>" +
            "        </transformedText>\n" +
            "    </xsl:template>\n" +
            "</xsl:stylesheet>";

    public void testXSLTProcessor() throws Exception {
        SynapseMessage sm = new Axis2SynapseMessage(
                Axis2EnvSetup.axis2Deployment("target/synapse-repository"));
        XSLTProcessor pro = new XSLTProcessor();
        pro.setXSLInputStream(new ByteArrayInputStream(xsl.getBytes()));
        pro.setIsBody(true);
        boolean result = pro.process(null, sm);
        assertTrue(result);
    }
}
