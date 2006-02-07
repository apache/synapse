package com.infravio.utils;

import com.infravio.core.SynapseObject;

public class TestSample {
    public static void main(String[] args) {
        String xml = "<SynapseObject name=\"SLA\">\n" +
                "    <attribute name=\"service\" type=\"String\">http://myhost:myport/Service</attribute>\n" +
                "    <SynapseObject name=\"consumer\">\n" +
                "        <attribute name=\"ip\" type=\"string\">192.9.2.11</attribute>\n" +
                "    </SynapseObject>\n" +
                "</SynapseObject>";
        SynapseObject so = Utils.xmlToSynapseObject(xml);
        System.out.println("The XML Frag : \n" + so.getXMLFragment());
        SynapseObject result[] = so.findSynapseObjectsByAttributeName("ip");
        System.out.println("The XML for SynapseObject with 'ip' : \n" + result[0].getXMLFragment());
        boolean hasAttrib = so.hasAttribute("service");
        System.out.println("Has Attrib service : \n" + hasAttrib);

    }
}
