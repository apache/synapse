package org.apache.synapse.synapseobject;

/**
 * @author
 */
public class Sample {
    public Sample() {
    }

    public static void main(String[] args) {

        String sla = "<Consumer_Identification>\n" +
                " <Consumer>\n" +
                "   <CONSUMER_TYPE type=\"xsd:String\">GOLD</CONSUMER_TYPE>\n" +
                "   <IP_ADDRESS_FROM type=\"xsd:String\">192.167.6.0</IP_ADDRESS_FROM>\n" +
                "   <IP_ADDRESS_TO type=\"xsd:String\">192.168.6.255</IP_ADDRESS_TO>\n" +
                "   <HTTP_AUTH_USERNAME type=\"xsd:String\">john</HTTP_AUTH_USERNAME>\n" +
                "   <WS_SEC_USERNAME type=\"xsd:String\">john</WS_SEC_USERNAME>\n" +
                "   <Assigned_Service>\n" +
                "    <Service_ID type=\"xsd:String\">stockQuote1</Service_ID>\n" +
                "   </Assigned_Service>\n" +
                " </Consumer>\n" +
                " <Consumer>\n" +
                "   <CONSUMER_TYPE type=\"xsd:String\">SILVER</CONSUMER_TYPE>\n" +
                "   <IP_ADDRESS_FROM type=\"xsd:String\">192.168.6.255</IP_ADDRESS_FROM>\n" +
                "   <IP_ADDRESS_TO type=\"xsd:String\">192.168.6.255</IP_ADDRESS_TO>\n" +
                "   <HTTP_AUTH_USERNAME type=\"xsd:String\">mary</HTTP_AUTH_USERNAME>\n" +
                "   <WS_SEC_USERNAME type=\"xsd:String\">mary</WS_SEC_USERNAME>\n" +
                "   <Assigned_Service>\n" +
                "    <Service_ID type=\"xsd:String\">stockQuote1</Service_ID>\n" +
                "   </Assigned_Service>\n" +
                " </Consumer>\n" +
                "</Consumer_Identification>";
        SynapseObject slaConfig = Utils.xmlToSynapseObject(sla);
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