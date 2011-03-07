package org.apache.synapse.util;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.synapse.SynapseConstants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: supun
 * Date: 3/4/11
 * Time: 7:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropertyGenTest extends TestCase {

    public void testCreate() {
        //File file = new File("/Users/supun/dev/apache/synapse-1/general_dlc_sequence.xml");
        File file = new File("/Users/supun/dev/apache/synapse-1/ib_gift_voucher_redemptions_result_dlc_sequence.xml");

        InputStream is = null;
        try {
            is = new FileInputStream(file);

            OMElement document = new StAXOMBuilder(is).getDocumentElement();
            document.build();

            document.toString();
            String result = "";
            OMElement switchEle = document.getFirstChildWithName(new QName(SynapseConstants.SYNAPSE_NAMESPACE, "switch"));

            Iterator itr = switchEle.getChildrenWithName(new QName(SynapseConstants.SYNAPSE_NAMESPACE, "case"));
            int no = 0;
            while (itr.hasNext()) {


                OMElement caseElem = (OMElement) itr.next();
                Iterator classItr = caseElem.getChildrenWithName(new QName(SynapseConstants.SYNAPSE_NAMESPACE, "class"));
                while (classItr.hasNext()) {
                    OMElement classElem = (OMElement) classItr.next();

                    if (classElem.getAttribute(new QName("name")).getAttributeValue().equals("com.jkh.tools.MessageStoreMediator")) {
                        Iterator propItr = classElem.getChildrenWithName(new QName(SynapseConstants.SYNAPSE_NAMESPACE, "property"));

                        String sequence = "";
                        String messageStore = "";

                        while (propItr.hasNext()) {
                            OMElement proElm = (OMElement) propItr.next();

                            if (proElm.getAttribute(new QName("name")).getAttributeValue().equals("sequence")) {
                                sequence = proElm.getAttribute(new QName("value")).getAttributeValue();
                            }

                            if (proElm.getAttribute(new QName("name")).getAttributeValue().equals("messageStore")) {
                                messageStore = proElm.getAttribute(new QName("value")).getAttributeValue();
                            }
                        }

                        result += "destination." + messageStore + "_" + sequence + "=direct://amq.direct//" + messageStore + "_" + sequence + "\n";
                        no++;
                    }
                }
            }

            System.out.println(result);
            System.out.println(no);

        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}
