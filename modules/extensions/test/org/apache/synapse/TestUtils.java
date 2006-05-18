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
package org.apache.synapse;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import java.io.StringReader;

public class TestUtils {

    public static TestMessageContext getTestContext(String bodyText) throws Exception {

        // create a test synapse context
        TestMessageContext synCtx = new TestMessageContext();

        SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc = OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);

        XMLStreamReader parser = XMLInputFactory.newInstance().
            createXMLStreamReader(new StringReader(bodyText));
        StAXOMBuilder builder = new StAXOMBuilder(parser);

        // set a dummy static message
        envelope.getBody().addChild(builder.getDocumentElement());

        synCtx.setEnvelope(envelope);
        return synCtx;
    }
}
