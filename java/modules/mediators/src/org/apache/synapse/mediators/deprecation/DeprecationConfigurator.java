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

package sampleMediators.deprecation;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DeprecationConfigurator {

    /*
    *** MAP STRUCTURE ***
    The config map contains the parameter-sets for all the services and also a default
    parameter set. The key to this map is the endPoint reference of the service and the
    value associated with it is the parameter map corresponding to it.

    <deprecationConfig>
        <service Id = "the serviceEPR">
                <parameterSet>
                    <fromDate>value</fromDate>
                    <toDate>value</toDate>
                </parameterSet>
                <parameterSet>
                    <fromDate>value</fromDate>
                    <toDate>value</toDate>
                </parameterSet>*
        </service>*
    </deprecationConfig>

    This should map to
    key is ("serviceEPR")
    value is (map of parameterSet) i.e.
        map with key (parameter name), value (parameter value).
    */

    public Map deprecationConfig;
    private Map configMap;

    public DeprecationConfigurator(InputStream inStream) {

        //Read from some source, probably a file and set the config map here.
        //If the source is service specific, we can use getConfig() and only access
        //service based parameters else make a general config and filter out the needed parameters
        //and create a Map in the getConfig(EndpointReference to)
        try {
            configMap = generateMap(inStream);
            setConfig(configMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map generateMap(InputStream inStream) throws Exception {


        StAXOMBuilder staxOMBuilder;

        staxOMBuilder = new StAXOMBuilder(inStream);

        //Read the source and generate a map of the
        Map generatedMap = new HashMap();

        OMElement config = staxOMBuilder.getDocumentElement();
        config.build();
        Iterator serviceItr = config.getChildrenWithName(new QName(DeprecationConstants.CFG_DEPRECATION_SERVICE));

        while (serviceItr.hasNext()) {
            OMElement serviceEle = (OMElement) serviceItr.next();
            String serviceKey = serviceEle.getAttributeValue(new QName("Id"));
            Iterator paramItr = serviceEle.getChildElements();
            int counter = 0;
            Map dataMap = new HashMap();

            while (paramItr.hasNext()) {
                OMElement paramEle = (OMElement) paramItr.next();
                Iterator dataItr = paramEle.getChildElements();

                while (dataItr.hasNext()) {
                    OMElement dataEle = (OMElement) dataItr.next();
                    String dataName = dataEle.getLocalName() + "[" + counter + "]";
                    String dataValue = dataEle.getText();
                    dataMap.put(dataName, dataValue);
                }

                //Will be of use if multiple services are facaded by a single serviceEPR
                dataMap.put(DeprecationConstants.CFG_DEPRECATION_SERVICE + "[" + counter + "]", serviceKey);
                counter++;

            }
            generatedMap.put(serviceKey, dataMap);
        }
        return generatedMap;
    }

    public Map getConfig(EndpointReference to) {

        //Filter out the required parameters and generate the ConfigMap for this service
        //This is done by using the EndPointReference as a key into the configMap.
        //The value object corresponding to it is the required deprecationMap.

        deprecationConfig = (Map) configMap.get(to.getAddress());

        return deprecationConfig;
    }

    public void setConfig(Map configMap) {

        //In case we go for service-specific impl then the configMap will contain details
        // for the single service alone, hence this line.
        //If an aggregate approach is taken [i.e. all service data in a single source file]
        // then this method should not assign configMap to deprecationConfig.

        this.deprecationConfig = configMap;

    }

}
