package com.infravio.utils;

import com.infravio.core.SynapseObject;
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
import java.io.StringReader;

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
            parent.setBoolean(goName, goValue);
        } else if (goType.equalsIgnoreCase("Float")) {
            parent.setFloat(goName, goValue);
        } else if (goType.equalsIgnoreCase("Integer")) {
            parent.setInteger(goName, goValue);
        } else if (goType.equalsIgnoreCase("Long")) {
            parent.setLong(goName, goValue);
        } else if (goType.equalsIgnoreCase("String")) {
            parent.setString(goName, goValue);
        } else {
            throw new Exception("TYPE NOT SUPPORTED!");
        }
        return parent;
    }

    public static String transform(String xml, String xsl) {
        /*String xsl = "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n" +
         "<xsl:output method=\"text\" /> \n" +
         "<xsl:template match=\"/\">\n" +
         "<xsl:apply-templates /> \n" +
         "</xsl:template>\n" +
         "<xsl:template match=\"student_list\">\n" +
         "Student Directory for example.edu \n" +
         "<xsl:apply-templates /> \n" +
         "</xsl:template>\n" +
         "<xsl:template match=\"name\">\n" +
         "Name: \n" +
         "<xsl:apply-templates /> \n" +
         "</xsl:template>\n" +
         "<xsl:template match=\"major\">\n" +
         "Major: \n" +
         "<xsl:apply-templates /> \n" +
         "</xsl:template>\n" +
         "<xsl:template match=\"phone\">\n" +
         "Phone: \n" +
         "<xsl:apply-templates /> \n" +
         "</xsl:template>\n" +
         "<xsl:template match=\"email\">\n" +
         "Email: \n" +
         "<xsl:apply-templates /> \n" +
         "</xsl:template>\n" +
         "</xsl:stylesheet>";

  String xml = "<student_list>\n" +
          "<student>\n" +
          "<name>George Washington</name> \n" +
          "<major>Politics</major> \n" +
          "<phone>312-123-4567</phone> \n" +
          "<email>gw@example.edu</email> \n" +
          "</student>\n" +
          "<student>\n" +
          "<name>Janet Jones</name> \n" +
          "<major>Undeclared</major> \n" +
          "<phone>311-122-2233</phone> \n" +
          "<email>janetj@example.edu</email> \n" +
          "</student>\n" +
          "<student>\n" +
          "<name>Joe Taylor</name> \n" +
          "<major>Engineering</major> \n" +
          "<phone>211-111-2333</phone> \n" +
          "<email>joe@example.edu</email> \n" +
          "</student>\n" +
          "</student_list>";
        */
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

    public static SynapseObject transformToSynapseObject(String xml, String xsl){
        String BOXml = Utils.transform(xml,xsl);
        SynapseObject bObject = Utils.xmlToSynapseObject(BOXml);
        return bObject;
    }
}
