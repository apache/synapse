package org.apache.synapse.synapseobject;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Utils {

    private static Document xmlDocument;

    public static SynapseObject xmlToSynapseObject(String xml) {
        SynapseObject businessObj;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            InputSource inputSource = new InputSource(new StringReader(xml));
            DocumentBuilder parser = factory.newDocumentBuilder();
            xmlDocument = parser.parse(inputSource);

        } catch (Exception e) {
            e.printStackTrace();
        }
        businessObj = createSynapseObject(xmlDocument);
        return businessObj;
    }

    public static SynapseObject xmlToSynapseObject(InputStream inStream) {
        String xml = "";
        try {
            while (inStream.available() > 0) {
                xml += (char) inStream.read();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //System.out.println(xml);
        return xmlToSynapseObject(xml);
    }

    public static SynapseObject createSynapseObject(Document xmlDocument) {

        SynapseObject businessObj = null;
        NodeList node = xmlDocument.getChildNodes();
        int len = node.getLength();
        int i;

        for (i = 0; i < len; i++) {

            String nodeName = node.item(i).getNodeName();
            if (nodeName.equalsIgnoreCase(UtilConstants.NODE)) {
                businessObj = processBONode(node.item(i));
            }
        }
        return businessObj;
    }

    private static SynapseObject processBONode(Node boNode) {

        String name = "";
        SynapseObject tempObj;
        NamedNodeMap namedNodeMap = boNode.getAttributes();
        int attributeCount = namedNodeMap.getLength();
        for (int counter = 0; counter < attributeCount; counter++) {
            Node attribName = namedNodeMap.item(counter);
            if (attribName.getNodeName().equals(UtilConstants.NAME_ATTRIB)) {
                name = namedNodeMap.getNamedItem(UtilConstants.NAME_ATTRIB).getNodeValue();
            }

        }
        tempObj = new SynapseObject(name);
        NodeList nodeList = boNode.getChildNodes();
        int len = nodeList.getLength();
        int i;

        for (i = 0; i < len; i++) {
            String nodeName = nodeList.item(i).getNodeName();
            if (nodeName.equalsIgnoreCase(UtilConstants.LEAF)) {
                try {
                    tempObj = processGONode(nodeList.item(i), tempObj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (nodeName.equalsIgnoreCase(UtilConstants.NODE)) {
                SynapseObject bObj = processBONode(nodeList.item(i));
                tempObj.addChild(bObj);
            }
        }
        return tempObj;
    }

    private static SynapseObject processGONode(Node goNode, SynapseObject parent) throws Exception {

        String goName = "", goType = "", goValue = "";
        NamedNodeMap namedNodeMap = goNode.getAttributes();
        int attributeCount = namedNodeMap.getLength();
        for (int counter = 0; counter < attributeCount; counter++) {
            Node attribName = namedNodeMap.item(counter);
            if (attribName.getNodeName().equals(UtilConstants.NAME_ATTRIB)) {
                goName = namedNodeMap.getNamedItem(UtilConstants.NAME_ATTRIB).getNodeValue();
            } else if (attribName.getNodeName().equals(UtilConstants.TYPE_ATTRIB)) {
                goType = namedNodeMap.getNamedItem(UtilConstants.TYPE_ATTRIB).getNodeValue();
            }
        }
        if (goNode.hasChildNodes()) {
            if (goNode.getFirstChild().getNodeType() == Node.TEXT_NODE) {
                goValue = goNode.getFirstChild().getNodeValue();
            }
        }
        if (goType.equalsIgnoreCase("Boolean")) {
            parent.setBoolean(goName, goValue.toLowerCase());
        } else if (goType.equalsIgnoreCase("Float")) {
            parent.setFloat(goName, goValue);
        } else if (goType.equalsIgnoreCase("Integer")) {
            parent.setInteger(goName, goValue);
        } else if (goType.equalsIgnoreCase("Long")) {
            parent.setLong(goName, goValue);
        } else if (goType.equalsIgnoreCase("String")) {
            parent.setString(goName, goValue);
        } else if (goType.equalsIgnoreCase("Date")) {
            Calendar calendar = Calendar.getInstance();
            TimeZone timeZone = calendar.getTimeZone();
            DateFormat dateFormat = new SimpleDateFormat("d/M/y:H:m");
            dateFormat.setTimeZone(timeZone);
            Date date = dateFormat.parse(goValue);
            parent.setDate(goName, date);
        } else {
            throw new Exception(goType + " - TYPE NOT SUPPORTED!");
        }
        return parent;
    }

    public static String transform(String xml, String xsl) {
        String out_xml = "";
        TransformerFactory tFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = tFactory.newTransformer(new StreamSource(new StringReader(xsl)));
            try {
                ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
                transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(byteArrayOutput));
                out_xml = byteArrayOutput.toString();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        return out_xml;
    }

    public static SynapseObject transformToSynapseObject(String xml, String xsl) {
        String BOXml = Utils.transform(xml, xsl);
        return Utils.xmlToSynapseObject(BOXml);
   }
}
