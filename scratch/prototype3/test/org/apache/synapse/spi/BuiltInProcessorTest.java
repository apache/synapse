package org.apache.synapse.spi;

import junit.framework.TestCase;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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

public class BuiltInProcessorTest extends TestCase {

    private String synapsexml =
            "<synapse xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "<stage name=\"logall\">\n" +
                    "    <log/>\n" +
                    "</stage>\n"+
             "</synapse>";
    private SynapseEnvironment env;

    public void testSynapseEnvironment() throws Exception {

        env = new Axis2SynapseEnvironment(
                getSynapseConfigElement(),
                Thread.currentThread().getContextClassLoader());

     }


    public OMElement getSynapseConfigElement() throws XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newInstance()
                .createXMLStreamReader(
                        new ByteArrayInputStream(synapsexml.getBytes()));
        OMFactory fac = OMAbstractFactory.getOMFactory();
        StAXOMBuilder staxBuilder = new StAXOMBuilder(fac, parser);
        OMElement config = staxBuilder.getDocumentElement();
        return config;

    }

}
