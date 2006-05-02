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
package org.apache.synapse.config;

import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

public class SynapseConfigurationFactory {

    public SynapseConfigurationFactory() {

    }

    public static SynapseConfiguration getConfiguration(String fileName) throws XMLStreamException, IOException {

        SynapseConfiguration config = new SynapseConfiguration();

        OMElement root =
            new StAXOMBuilder(XMLInputFactory.newInstance().
                createXMLStreamReader(new FileReader(fileName)))
                .getDocumentElement();
        root.build();

        OMContainer definitions = root.getFirstChildWithName(Constants.DEFINITIONS_ELT);
        if (definitions != null) {
            Iterator iter = definitions.getChildrenWithName(Constants.SEQUENCE_ELT);
            while (iter.hasNext()) {
                OMElement elt = (OMElement) iter.next();
                defineSequence(elt);
            }
        }

        return null;
    }

    private static void defineSequence(OMElement ele) {
        //Mediator med = MediatorFactoryFinder.getMediator(synapseEnv, ele);
        System.out.println("med");
    }
}
