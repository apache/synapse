package org.apache.synapse.synapseobject;

import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.SynapseException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by IntelliJ IDEA.
 * User: Vikas
 * Date: Feb 7, 2006
 * Time: 10:32:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestOMElementSo {

    static File file = new File("C:\\DUMP\\mediator-xmls\\policy.xml");
    static InputStream is;
    public static OMElement getElement()
    {
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
         StAXOMBuilder builder;
            try {
                builder = new StAXOMBuilder(is);

            } catch (XMLStreamException e1) {
                throw new SynapseException(
                        "Trouble parsing XML ", e1);

            }
       OMElement element = builder.getDocumentElement();

       return element;
    }


    public static void main(String[] arg){
        OMElement omElement = getElement();

        /* OMElement result = OMUtil.getOMElementWithAttributeName(omElement,"url");
        try {
            System.out.println("RESULT:\n" + result.toStringWithConsume());
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        */

        /*OMElement[] result = OMUtil.getOMElementsWithAttributeName(omElement,"url");
         try {
            int length = result.length;
            for(int i=0; i<length;i++)
            System.out.println("RESULT[ "+i+" ]\n" + result[i].toStringWithConsume());
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        */

        /*OMElement result = OMUtil.getOMElementWithAttribute(omElement,"type","STRING");
        try {
            System.out.println("RESULT:\n" + result.toStringWithConsume());
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        */

        OMElement[] result = OMUtil.getOMElementsWithAttribute(omElement,"type","STRING");
        try {
            int length = result.length;
            for(int i=0; i<length;i++)
            System.out.println("RESULT[ "+i+" ]\n" + result[i].toStringWithConsume());
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

    }
}
