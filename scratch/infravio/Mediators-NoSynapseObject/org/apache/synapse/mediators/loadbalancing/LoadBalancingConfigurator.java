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

package org.apache.synapse.mediators.loadbalancing;

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


public class LoadBalancingConfigurator {
      private Log log = LogFactory.getLog(getClass());
    /*
    *** MAP STRUCTURE ***
    The config map contains the parameter-sets for all the services and also a default
    parameter set. The key to this map is the endPoint reference of the service and the
    value associated with it is the parameter map corresponding to it.

    <loadBalancingService Id="ServiceEPR to distnguish this set when the request comes in">
        <strategy>
            Specifies the load-balancing criteria. [performance/round-robin]
        </strategy>
        <count>
            number of last requests on which to evaluate performance criteria
        </count>
        <lastService>
            keeps track of the service last called in round-robin fashion
        </lastService>
        <parameterSet>
            <service>SomeServiceEPR</service>
            <active>A boolean to denote if the service is active</active>
            <requestCount>Number of requests it has received till now</requestCount>
            <averageResponseTime>
                Average response time for 'count' requests in milliseconds
            </averageResponseTime>
            <lastResponseTime>
                Last response time for the service in milliseconds
            </lastResponseTime>
        </parameterSet>
    </loadBalancingService>

    This should map to
    key is ("serviceEPR")
    value is (map of parameterSet and the extra mets-data) i.e.
        map with key (parameter name), value (parameter value).
    */

    public Map loadbalancingConfig;

    public static Map getConfigMap() {
        return configMap;
    }

    public static void setConfigMap(Map configMap) {
        LoadBalancingConfigurator.configMap = configMap;
    }

    public static Map configMap;

    public LoadBalancingConfigurator(InputStream instream) {

        log.info("LoadBalancing Configurator created!");
        try {
            configMap = generateMap(instream);
            setConfig(configMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map generateMap(InputStream instream) throws Exception {

        log.info("Generating map for LoadBalancing");
        //Read the source and generate a map of the
        Map generatedMap = new HashMap();

        StAXOMBuilder staxOMBuilder;

        staxOMBuilder = new StAXOMBuilder(instream);

        OMElement config = staxOMBuilder.getDocumentElement();
        config.build();
        Iterator serviceItr = config.getChildrenWithName(new QName(LoadBalancingConstants.CFG_LOADBALANCING_SERVICE));

        while (serviceItr.hasNext()) {
            OMElement serviceEle = (OMElement) serviceItr.next();
            String serviceKey = serviceEle.getAttributeValue(new QName("Id"));
            Iterator paramItr = serviceEle.getChildrenWithName(new QName(LoadBalancingConstants.CFG_PARAMSET));
            Map dataMap = new HashMap();
            int counter=0;
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

            String strategy = serviceEle.getFirstChildWithName(new QName
                                              (LoadBalancingConstants.CFG_LOADBALANCING_STRATEGY)).getText();
            String count = serviceEle.getFirstChildWithName(new QName
                                              (LoadBalancingConstants.CFG_LOADBALANCING_PERF_COUNT)).getText();
            dataMap.put(LoadBalancingConstants.CFG_LOADBALANCING_STRATEGY,strategy);
            dataMap.put(LoadBalancingConstants.CFG_LOADBALANCING_PERF_COUNT,count);
            dataMap.put(LoadBalancingConstants.CFG_LOADBALANCING_LAST_SERVICE,"0");
            generatedMap.put(serviceKey, dataMap);

        }

        return generatedMap;
    }
            

    public Map getConfig(EndpointReference to) {

        //Filter out the required parameters and generate the ConfigMap for this service
        //This is done by using the EndPointReference as a key into the configMap.
        //The value object corresponding to it is the required deprecationMap.
        log.info("Giving loadbalancing map for " + to.getAddress());
        loadbalancingConfig = (Map) configMap.get(to.getAddress());

        return loadbalancingConfig;
    }

    public void setConfig(Map configMap) {

        //In case we go for service-specific impl then the configMap will contain details
        // for the single service alone, hence this line.
        //If an aggregate approach is taken [i.e. all service data in a single source file]
        // then this method should not assign configMap to failoverConfig.

        this.loadbalancingConfig = configMap;

    }

}