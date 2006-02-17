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

package org.apache.synapse.mediators.sla;

import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.InputStream;


public class SLAConfigurator {
    private static Map SLAConfig;
    private Log log = LogFactory.getLog(getClass());


       public SLAConfigurator(InputStream inStream) {

           try {
               Map configMap = generateMap(inStream);
               setConfig(configMap);
           } catch (Exception e) {
               log.info(e);
           }
       }

       private Map generateMap(InputStream inStream) throws Exception {


           StAXOMBuilder staxOMBuilder;
           staxOMBuilder = new StAXOMBuilder(inStream);
           Map generatedMap = new HashMap();
           OMElement config = staxOMBuilder.getDocumentElement();
           config.build();
           Iterator clientItr = config.getChildrenWithName(new QName("slaRequest"));
           while (clientItr.hasNext()) {
               OMElement clientEle = (OMElement) clientItr.next();
               String enabled = clientEle.getAttributeValue(new QName("enabled"));
               boolean isClientEnabled = enabled.equalsIgnoreCase("true");
               if(isClientEnabled){

                   String clientKey = clientEle.getAttributeValue(new QName("ip"));
                   Iterator serviceItr = clientEle.getChildrenWithName(new QName("serviceURL"));

                   Map dataMap = new HashMap();

                   while (serviceItr.hasNext()) {
                       OMElement serviceEle = (OMElement) serviceItr.next();
                       OMElement priorityEle = serviceEle.getFirstChildWithName(new QName("priority"));
                       String serviceEnabled = serviceEle.getAttributeValue(new QName("enabled"));
                       boolean isServiceEnabled = serviceEnabled.equalsIgnoreCase("true");
                       if(isServiceEnabled) {
                           String serviceKey = serviceEle.getAttributeValue(new QName("url"));
                           String priority = priorityEle.getText();
                           dataMap.put(serviceKey,priority);
                       }
                   }
                   generatedMap.put(clientKey, dataMap);
               }
           }
           return generatedMap;
       }

       public int getPriority(String ip, EndpointReference to) {

           int priority=-1;
           Map clientMap = (Map) SLAConfig.get(ip);
           if(clientMap!=null){
               log.info("address" + to.getAddress());
               String priorityValue = (String)clientMap.get(to.getAddress());
                if(priorityValue!=null){
                    priority = Integer.parseInt(priorityValue);
                }
           }
           return priority;
       }

       public void setConfig(Map configMap) {

           SLAConfig = configMap;

       }
}