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
package org.apache.synapse.config.xml;

import org.apache.synapse.config.SynapseConfiguration;

import java.io.FileInputStream;
import java.util.Iterator;

public class XMLConfigurationSerializer {

    public void serializeConfiguration(SynapseConfiguration synCfg) {

        Iterator iter = synCfg.getNamedSequences().keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            System.out.println(MediatorSerializerFinder.getInstance().getSerializer(synCfg.getNamedSequence(name))
                .serializeMediator(null, synCfg.getNamedSequence(name)));
        }

        System.out.println(MediatorSerializerFinder.getInstance().getSerializer(synCfg.getMainMediator())
            .serializeMediator(null, synCfg.getMainMediator()));
    }

    public static void main(String[] args) throws Exception {
        XMLConfigurationBuilder xmlBuilder = new XMLConfigurationBuilder();
        SynapseConfiguration synCfg = xmlBuilder.getConfiguration(
            new FileInputStream("C:\\Code\\Synapse\\repository\\conf\\sample\\synapse_sample_1.xml"));

        XMLConfigurationSerializer xmlSer = new XMLConfigurationSerializer();
        xmlSer.serializeConfiguration(synCfg);
    }
}
