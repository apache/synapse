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
package samples.userguide.log;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.OMNamespace;


public class LoggingClient {
    private static EndpointReference targetEpr = new EndpointReference(
            "http://127.0.0.1:8080/synapse/services/mock_service");

    public static void main(String[] args) {
        try {
            ServiceClient serviceClient = new ServiceClient();
            Options co = new Options();
            co.setTo(targetEpr);
            serviceClient.setOptions(co);
            serviceClient.fireAndForget(payload());
        } catch (AxisFault axisFault) {
            axisFault
                    .printStackTrace();
        }
    }

    public static OMElement payload() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace(
                "urn:synapse-body", "ns");
        OMElement method = fac.createOMElement("service", omNs);
        OMElement value = fac.createOMElement("text", omNs);
        value.addChild(
                fac.createText(value, "Synapse Sample String"));
        method.addChild(value);
        return method;
    }
}
