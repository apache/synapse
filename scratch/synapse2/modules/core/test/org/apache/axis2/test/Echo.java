package org.apache.axis2.test;

import org.apache.axis2.AxisFault;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMFactory;
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

public class Echo {
    public OMElement echo(OMElement element) {
        System.out.println(
                "This is the actual service which has been redirected");
        element.build();
        element.detach();
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace(
                "urn:text-body", "ns");
        OMElement responseText = fac.createOMElement("response_text", omNs);
        responseText.addChild(
                fac.createOMText(responseText, "Synapse Testing String_Response"));
        return responseText;
    }
    public OMElement fault(OMElement element) throws AxisFault {
        throw new AxisFault("Native End Point Throws an Exception");
    }
    public OMElement echo_addressing(OMElement element) {
        System.out.println(
                "This is the actual service which has been redirected with addressing");
        element.build();
        element.detach();
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace(
                "urn:text-body", "ns");
        OMElement responseText = fac.createOMElement("response_text_addressing", omNs);
        responseText.addChild(
                fac.createOMText(responseText, "Synapse Testing String_Response_With_Addressing"));
        return responseText;
    }

    public void ping(OMElement element) {
        System.out.println(
                "This is the actual service which has been pinged");
    }


    public OMElement simple_resources(OMElement element){
        System.out.println("This is the actual resource provider");
        element.build();
        element.detach();

        OMFactory fac = OMAbstractFactory.getOMFactory();

        OMElement ele1 = fac.createOMElement("ele1","","");
        OMElement ele2 = fac.createOMElement("ele2","","");
        OMElement ele3 = fac.createOMElement("ele3","","");
        element.addChild(ele1);
        element.addChild(ele2);
        element.addChild(ele3);
        return element;

    }

}
