package org.apache.synapse.synapseobject;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

public class TestSample {
    public static void main(String[] args) {
        String xml = "<SynapseObject name=\"SLA\">\n" +
                "    <attribute name=\"service\" type=\"String\">SomeService</attribute>\n" +
                "    <SynapseObject name=\"consumer\">\n" +
                "        <attribute name=\"ip\" type=\"string\">someIP</attribute>\n" +
                "    </SynapseObject>\n" +
                "</SynapseObject>";
        SynapseObject bo = Utils.xmlToSynapseObject(xml);
        bo.setDate("Date",new Date(System.currentTimeMillis()));
        System.out.println("Date "+bo.getDate("Date"));
        System.out.println("The XML : \n" + bo.getXMLFragment());
        
        try
        {
          FileInputStream file = new FileInputStream("C:/synapse-test/src/sampleMediators/ci/META-INF/ci.xml");
          bo = Utils.xmlToSynapseObject(file);
          System.out.println("name  : "+bo.getSynapseObjectName());
          SynapseObjectArray soa = bo.getSOsByAttributeValueStartingWith("192");
          
          System.out.println("Length "+soa.size());
          file.close();
        }
        catch (Exception e)
        {
          
        }

    }
}
