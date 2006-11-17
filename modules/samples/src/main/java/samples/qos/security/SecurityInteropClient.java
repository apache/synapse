/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package samples.qos.security;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;

import javax.xml.namespace.QName;

public class SecurityInteropClient {

    private static final String TURL = "http://localhost:8080/";
    private static final String PING1_ACTION = "Ping1";
    private static final String PING3_ACTION = "Ping3";
    private static final String PING_NS = "http://xmlsoap.org/Ping";

    public static void main(String[] args) {
        send("Hello World", TURL, PING1_ACTION);
        //send("Hello World", TURL, PING3_ACTION);
    }

    private static void send(String echoText, String turl, String soapAction) {
        try {
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMNamespace xNs = factory.createOMNamespace(PING_NS, "");
            OMElement ping = factory.createOMElement("Ping", xNs);
            OMElement text = factory.createOMElement("text", xNs);
            text.setText(echoText);
            ping.addChild(text);

            ServiceClient serviceClient = new ServiceClient();
            Options options = new Options();

            options.setProperty(Constants.Configuration.TRANSPORT_URL, turl);
            options.setAction(soapAction);
            serviceClient.setOptions(options);
            OMElement result = serviceClient.sendReceive(ping);

            QName gQR = new QName(PING_NS, "PingResponse");
            OMElement qResp = (OMElement) result.getChildrenWithName(gQR).next();
            System.out.println("Response : " + qResp.getText());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
