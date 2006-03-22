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

package org.apache.synapse.mediators.failover;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class FailoverConfigurator {
    private Log log = LogFactory.getLog(getClass());
    /*
    *** MAP STRUCTURE ***
    The config map contains the parameter-sets for all the services and also a default
    parameter set. The key to this map is the endPoint reference of the service and the
    value associated with it is the parameter map corresponding to it.

    <failoverService Id="ServiceEPR to distnguish this set when the request comes in">
        <timeoutEnabled>
            A boolean to denote if failover happens on Timeout
        </timeoutEnabled>
        <soapFaultEnabled>
            A boolean to denote if failover happens on soapFault
        </soapFaultEnabled>
        <networkErrorEnabled>
            A boolean, denotes if failover happens on networkError
        </networkErrorEnabled>
        <parameterSet>
            <service>SomeServiceEPR</service>
            <primary>A boolean to denote if this the primary service</primary>
            <active>A boolean to denote if the service is active</active>
            <timeout>Timeout Value for the service in milliseconds</timeout>
        </parameterSet>
    </failoverService>

    This should map to
    key is ("serviceEPR")
    value is (map of parameterSet and the extra mets-data) i.e.
        map with key (parameter name), value (parameter value).
    */

    public Map failoverConfig;
    private Map configMap;

    public FailoverConfigurator(InputStream instream) {

        //Read from some source, probably a file and set the config map here.
        //If the source is service specific, we can use getConfig() and only access
        //service based parameters else make a general config and filter out the needed parameters
        //and create a Map in the getConfig(EndpointReference to)

        try {
            configMap = generateMap(instream);
            setConfig(configMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map generateMap(InputStream instream) throws Exception {

        log.info("Generating map for failover");

        StAXOMBuilder staxOMBuilder;

        staxOMBuilder = new StAXOMBuilder(instream);

        //Read the source and generate a map of the
        Map generatedMap = new HashMap();

        OMElement config = staxOMBuilder.getDocumentElement();
        config.build();
        Iterator serviceItr = config.getChildrenWithName(new QName(FailoverConstants.CFG_FAILOVER_SERVICE));

        while (serviceItr.hasNext()) {
            OMElement serviceEle = (OMElement) serviceItr.next();
            String serviceKey = serviceEle.getAttributeValue(new QName("Id"));
            Iterator paramItr = serviceEle.getChildrenWithName(new QName(FailoverConstants.CFG_PARAMSET));
            Map dataMap = new HashMap();
            int counter = 0;
            while (paramItr.hasNext()) {
                OMElement paramEle = (OMElement) paramItr.next();
                Iterator dataItr = paramEle.getChildElements();

                while (dataItr.hasNext()) {
                    OMElement dataEle = (OMElement) dataItr.next();
                    String dataName = dataEle.getLocalName() + "[" + counter + "]";
                    String dataValue = dataEle.getText();
                    dataMap.put(dataName, dataValue);
                }

                counter++;
            }

            String isTimeOutEnabled = serviceEle.getFirstChildWithName(new QName
                    (FailoverConstants.CFG_FAILOVER_ON_TIMEOUT)).getText();
            /*String isSOAPFaultEnabled = serviceEle.getFirstChildWithName(new QName
                    (FailoverConstants.CFG_FAILOVER_ON_SOPAFAULT)).getText();
            */
            String isNTWRK_ErrorEnabled = serviceEle.getFirstChildWithName(new QName
                    (FailoverConstants.CFG_FAILOVER_ON_NTWRK_ERROR)).getText();

            dataMap.put(FailoverConstants.CFG_FAILOVER_ON_NTWRK_ERROR, Boolean.valueOf(isNTWRK_ErrorEnabled));
            //dataMap.put(FailoverConstants.CFG_FAILOVER_ON_SOPAFAULT, Boolean.valueOf(isSOAPFaultEnabled));
            dataMap.put(FailoverConstants.CFG_FAILOVER_ON_TIMEOUT, Boolean.valueOf(isTimeOutEnabled));
            generatedMap.put(serviceKey, dataMap);

        }

        return generatedMap;
    }

    public Map getConfig(EndpointReference to) {

        //Filter out the required parameters and generate the ConfigMap for this service
        //This is done by using the EndPointReference as a key into the configMap.
        //The value object corresponding to it is the required deprecationMap.
        log.info("Giving failover map for " + to.getAddress());

        failoverConfig = (Map) configMap.get(to.getAddress());

        return failoverConfig;
    }

    public void setConfig(Map configMap) {

        //In case we go for service-specific impl then the configMap will contain details
        // for the single service alone, hence this line.
        //If an aggregate approach is taken [i.e. all service data in a single source file]
        // then this method should not assign configMap to failoverConfig.

        this.failoverConfig = configMap;

    }

}