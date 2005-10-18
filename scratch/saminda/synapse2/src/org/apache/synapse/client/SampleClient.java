package org.apache.synapse.client;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMNamespace;
import org.apache.axis2.clientapi.MessageSender;
import org.apache.axis2.Constants;
import org.apache.axis2.AxisFault;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 10, 2005
 * Time: 8:10:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class SampleClient {
    private static EndpointReference targetEPR = new EndpointReference(
            "http://localhost:8081/");

    public static void main(String[] args) {
        try {


            MessageSender msgSender = new MessageSender();
            msgSender.setTo(targetEPR);
            msgSender.setSenderTransport(Constants.TRANSPORT_HTTP);

            msgSender.send("mediate", new ClientUtil().getPingOMElement());

        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }


    }

    public static class ClientUtil {

        public OMElement getPingOMElement() {
            OMFactory fac = OMAbstractFactory.getOMFactory();
            OMNamespace omNs = fac.createOMNamespace(
                    "http://synapse.org/synapse", "synapse");
            OMElement method = fac.createOMElement("ping", omNs);
            OMElement value = fac.createOMElement("text", omNs);
            value.addChild(fac.createText(value, "Synpase Ping String "));
            method.addChild(value);

            return method;
        }
    }
}
