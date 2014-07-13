package org.apache.synapse.engine;

import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
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
* @author : Deepal Jayasinghe (deepal@apache.org)
*
*/

public class SynapseDeployer {

    private File synpseConfig;

    public SynapseDeployer(File synpseConfig) {
        this.synpseConfig = synpseConfig;
    }

    public SynapseConfiguration populteConfig() throws SynapseException {
        SynapseConfiguration synConfig = new SynapseConfiguration();
        try {
            InputStream in = new FileInputStream(synpseConfig);
            XMLStreamReader xmlReader =
                    XMLInputFactory.newInstance().createXMLStreamReader(in);
            OMFactory fac = OMAbstractFactory.getOMFactory();
            StAXOMBuilder staxOMBuilder = new StAXOMBuilder(fac, xmlReader);
            OMElement element = staxOMBuilder.getDocumentElement();
            element.build();
            Iterator disElements = element.getChildElements();
            while (disElements.hasNext()) {
                OMElement omElement = (OMElement) disElements.next();
                OMAttribute nameatt = omElement.getAttribute(new QName("name"));
                if (nameatt != null) {
                    if (SynapseConstants.DIRECTION_IN.equals(nameatt.getAttributeValue())) {
                        Iterator rulitr = omElement.getChildrenWithName(new QName("ruleSet"));
                        while (rulitr.hasNext()) {
                            OMElement rulset = (OMElement) rulitr.next();
                            OMAttribute stageAtt = rulset.getAttribute(new QName("stage"));
                            if (stageAtt != null) {
                                String satge = stageAtt.getAttributeValue();
                                if (SynapseConstants.STAGE_IN.equals(satge)) {
                                    synConfig.setIncomingPreStageRuleSet(rulset);
                                } else if (SynapseConstants.STAGE_PROCESS.equals(satge)) {
                                    synConfig.setIncomingProcessingStageRuleSet(rulset);
                                } else if (SynapseConstants.STAGE_OUT.equals(satge)) {
                                    synConfig.setIncomingPostStageRuleSet(rulset);
                                }
                            }
                        }
                    } else if (SynapseConstants.DIRECTION_OUT.equals(nameatt.getAttributeValue())) {
                        Iterator rulitr = omElement.getChildrenWithName(new QName("ruleSet"));
                        while (rulitr.hasNext()) {
                            OMElement rulset = (OMElement) rulitr.next();
                            OMAttribute stageAtt = rulset.getAttribute(new QName("stage"));
                            if (stageAtt != null) {
                                String satge = stageAtt.getAttributeValue();
                                if (SynapseConstants.STAGE_IN.equals(satge)) {
                                    synConfig.setOutgoingPreStageRuleSet(rulset);
                                } else if (SynapseConstants.STAGE_PROCESS.equals(satge)) {
                                    synConfig.setOutgoingProcessingStageRuleSet(rulset);
                                } else if (SynapseConstants.STAGE_OUT.equals(satge)) {
                                    synConfig.setOutgoingPostStageRuleSet(rulset);
                                }
                            }
                        }
                    }
                } else {
                    throw new SynapseException("direction name requird");
                }
            }
        } catch (FileNotFoundException e) {
            throw new SynapseException(e);
        } catch (XMLStreamException e) {
            throw new SynapseException(e);
        }
        return synConfig;
    }
}
