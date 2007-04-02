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

package samples.userguide;

import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.AxisFault;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;

import javax.xml.namespace.QName;

public class ServiceInvoker extends Thread {

    private String invokerName = "anonymous";
    private String operation = null;
    private ServiceClient client = null;
    private OMElement msg = null;
    private long iterations = 10;
    private boolean statefull = false;

    private OMFactory fac = null;

    private long runningTime = 0;

    public ServiceInvoker(String epr, String operation) {

        this.operation = operation;

        Options options = new Options();
        options.setTo(new EndpointReference(epr));
        options.setAction(operation);

        try {
            ConfigurationContext configContext = ConfigurationContextFactory.
                    createConfigurationContextFromFileSystem("client_repo", null);

            client = new ServiceClient(configContext, null);
            options.setTimeOutInMilliSeconds(10000000);

            client.setOptions(options);
            client.engageModule("addressing");

        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }

        fac = OMAbstractFactory.getOMFactory();
        msg = fac.createOMElement("sleepTime", null);
        msg.setText("1000");
    }

    public String getInvokerName() {
        return invokerName;
    }

    public void setInvokerName(String invokerName) {
        this.invokerName = invokerName;

        if (statefull) {
            client.getOptions().setManageSession(true);
            client.getOptions().setAction("setClientName");

            OMElement cName = fac.createOMElement("cName", null);
            cName.setText(invokerName);

            try {
                OMElement response = client.sendReceive(cName);
                System.out.println(response.getText());
            } catch (AxisFault axisFault) {
                axisFault.printStackTrace();
            }
        }
    }

    public long getRunningTime() {
        return runningTime;
    }

    public void setMessage(String msgText) {
        msg.setText(msgText);
    }

    public void setClientSessionID(String id) {

        SOAPFactory soapFactory = OMAbstractFactory.getSOAP12Factory();

        OMNamespace synNamespace = soapFactory.
                createOMNamespace("http://ws.apache.org/namespaces/synapse", "syn");
        SOAPHeaderBlock header = soapFactory.createSOAPHeaderBlock("ClientID", synNamespace);
        header.setText(id);

        client.addHeader(header);
    }

    public void setIterations(long i) {
        iterations = i;
    }

    public void setStatefull(boolean state) {
        this.statefull = state;
    }

    public void run() {

        client.getOptions().setAction(operation);

        try {

            long t1 = System.currentTimeMillis();

            for (long i=0; i < iterations; i++) {
                OMElement response2 = client.sendReceive(msg);
                System.out.println(invokerName + ": " + response2.toString());
            }

            long t2 = System.currentTimeMillis();

            System.out.println("================================================================");
            System.out.println(invokerName + " completed requests.");
            System.out.println("================================================================");
            runningTime = t2 - t1;

        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
