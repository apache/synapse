package org.apache.synapse.synapseobject;

/**
 * @author
 */
public class Sample {
    public Sample() {
    }

    public static void main(String[] args) {
        SynapseObject slaConfig = new SynapseObject("slaConfig");
        SynapseObject slaRequest = new SynapseObject("slaRequest");
        SynapseObject serviceURL = new SynapseObject("serviceURL");
        slaRequest.setString("ip", "192.168.1.127");
        slaRequest.setBoolean("enabled", "true");
        serviceURL.setString("url", "http://www.webservicex.net/stockquote.asmx");
        serviceURL.setBoolean("enabled", "true");
        serviceURL.setInteger("priority", "0");
        slaConfig.addChild(slaRequest);
        slaRequest.addChild(serviceURL);
        SynapseObject serviceURL1 = new SynapseObject("serviceURL1");
        serviceURL1.setString("url", "http://www.webservicex.net/stockquote.asmx");
        slaRequest.addChild(serviceURL1);
        //String xsl = "XSL";
        //String xml = slaConfig.translate(xsl);
        //System.out.println("XML****\n" + xml);
        /*BusinessObject[] bo = slaConfig.findChildrenWithAttributeValue("192.168.1.127");

        for (int i = 0; i < bo.length; i++) {
            System.out.println("*******");
            System.out.println(bo[i].getXMLFragment());
        }
        */
        System.out.println(slaConfig.getXMLFragment());

    }
}