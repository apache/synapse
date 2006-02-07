package com.infravio.core;

import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * @author
 */
public class SynapseObject extends Hashtable implements Serializable {
    SynapseObjectArray object = new SynapseObjectArray();
    String objectName;
    String createdBy;
    String creationDate;

    public SynapseObject(String objectName) {
        this.objectName = objectName;
    }

    public String translate(String xsl) {
        String out_xml = "";
        String xml = this.getXMLFragment();
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

    public String getBOName() {
        return this.objectName;
    }

    /**
     * Get contained object
     *
     * @return SynapseObject
     */
    public SynapseObject[] getChildren() {
        return this.object.getSynapseObjects();
    }

    /**
     * Set object
     *
     * @param SynapseObject
     */
    public void addChild(SynapseObject SynapseObject) {

        this.object.add(SynapseObject);
    }

    public void setLong(String attributeName, String attributeValue) {
        GenericObject go = new GenericObject(attributeName, (Long.TYPE).getName(), attributeValue);
        this.put(attributeName, go);
    }

    public void setInteger(String attributeName, String attributeValue) {
        GenericObject go = new GenericObject(attributeName, (Integer.TYPE).getName(), attributeValue);
        this.put(attributeName, go);
    }

    public void setFloat(String attributeName, String attributeValue) {
        GenericObject go = new GenericObject(attributeName, (Float.TYPE).getName(), attributeValue);
        this.put(attributeName, go);
    }

    public void setString(String attributeName, String attributeValue) {
        GenericObject go = new GenericObject(attributeName, "STRING", attributeValue);
        this.put(attributeName, go);
    }

    public void setBoolean(String attributeName, String attributeValue) {
        GenericObject go = new GenericObject(attributeName, (Boolean.TYPE).getName(), attributeValue);
        this.put(attributeName, go);
    }

    public Long getLong(String attributeName) {
        GenericObject go = (GenericObject) this.get(attributeName);
        return new Long(go.getValue());
    }

    public String getXMLFragment() {
        String xmlFragStart = "";
        String xmlFragEnd = "</SynapseObject>";
        String x3 = recurse(this, xmlFragStart);
        xmlFragStart += xmlFragEnd;
        return x3;
    }
    private String recurse(SynapseObject bo, String xmlFrag) {

        xmlFrag = xmlFrag + "<SynapseObject name=\"" + bo.getBOName() + "\">\n";
        xmlFrag = bo.getAttributes(bo, xmlFrag);
        Enumeration enumeration = bo.object.elements();
        while (enumeration.hasMoreElements()) {
            SynapseObject boj = (SynapseObject)enumeration.nextElement();
            xmlFrag = boj.recurse(boj, xmlFrag);
        }
        xmlFrag += "</SynapseObject>\n";
        return xmlFrag;
    }

    private String getAttributes(SynapseObject bo, String xmlReturn) {

        Enumeration enumeration = bo.elements();
        while (enumeration.hasMoreElements()) {
            GenericObject go = (GenericObject) enumeration.nextElement();
            String name = go.getName();
            String type = go.getType();
            String value = go.getValue();
            xmlReturn += "<attribute name=\"" + name + "\" " + "type=\"" + type + "\">" + value + "</attribute>\n";
        }
        return xmlReturn;
    }

    public SynapseObject findSynapseObjectByAttributeValue(String attributeValue) {
        SynapseObject bo = new SynapseObject("Not Found");
        if (this.hasAttributeValue(attributeValue)) {
            return this;
        } else {
            Enumeration enumeration = this.object.elements();
            while (enumeration.hasMoreElements()) {
                SynapseObject boj = (SynapseObject)enumeration.nextElement();
                bo = boj.findSynapseObjectByAttributeValue(attributeValue);
            }
        }
        return bo;
    }

    public boolean hasAttributeValue(String attr) {
        SynapseObject bo = this;
        Enumeration enumeration = bo.elements();
        while (enumeration.hasMoreElements()) {
            GenericObject go = (GenericObject) enumeration.nextElement();
            String value = go.getValue();
            if (attr.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public SynapseObject findSynapseObjectByName(String name) {
        SynapseObject bo = new SynapseObject("Not Found");
        if (this.getBOName().equalsIgnoreCase(name)) {
            return this;
        } else {
            Enumeration enumeration = this.object.elements();
            while (enumeration.hasMoreElements()) {
                SynapseObject boj = (SynapseObject)enumeration.nextElement();
                bo = boj.findSynapseObjectByName(name);
            }
        }
        return bo;
    }

    public SynapseObject[] findSynapseObjectsByAttributeName(String attributeName) {

        SynapseObjectArray boArray = new SynapseObjectArray();

        if (this.findChildrenWithAttributeName(attributeName) != null)
            boArray.addSynapseObjects(this.findChildrenWithAttributeName(attributeName));

        Enumeration enumeration = this.object.elements();
        while (enumeration.hasMoreElements()) {
            SynapseObject bo = (SynapseObject)enumeration.nextElement();
            if (bo.findSynapseObjectsByAttributeName(attributeName) != null) {
                boArray.addSynapseObjects(bo.findSynapseObjectsByAttributeName(attributeName));
            }
        }

        if (boArray.size() > 0) {
            return boArray.getSynapseObjects();
        } else {
            return null;
        }
    }

    public SynapseObject[] findChildrenWithAttributeName(String attributeName) {
        SynapseObjectArray boa = new SynapseObjectArray();
        Enumeration enumeration = this.object.elements();
        while (enumeration.hasMoreElements()) {
            SynapseObject bo = (SynapseObject)enumeration.nextElement();
            if (bo.hasAttribute(attributeName)) {
                boa.addSynapseObject(bo);
            }
        }
        if (boa.size() > 0) {
            return boa.getSynapseObjects();
        } else {
            return null;
        }
    }


    public SynapseObject[] findSynapseObjectsByAttributeValue(String attributeValue) {

        SynapseObjectArray boArray = new SynapseObjectArray();

        if (this.findChildrenWithAttributeName(attributeValue) != null)
            boArray.addSynapseObjects(this.findChildrenWithAttributeName(attributeValue));

        Enumeration enumeration = this.object.elements();
        while (enumeration.hasMoreElements()) {
            SynapseObject bo = (SynapseObject)enumeration.nextElement();
            if (bo.findSynapseObjectsByAttributeName(attributeValue) != null) {
                boArray.addSynapseObjects(bo.findSynapseObjectsByAttributeName(attributeValue));
            }
        }

        if (boArray.size() > 0) {
            return boArray.getSynapseObjects();
        } else {
            return null;
        }
    }

    public SynapseObject[] findChildrenWithAttributeValue(String attributeValue) {
        SynapseObjectArray boa = new SynapseObjectArray();
        Enumeration enumeration = this.object.elements();
        while (enumeration.hasMoreElements()) {
            SynapseObject bo = (SynapseObject)enumeration.nextElement();
            if (bo.hasAttributeValue(attributeValue)) {
                boa.addSynapseObject(bo);
            }
        }
        if (boa.size() > 0) {
            return boa.getSynapseObjects();
        } else {
            return null;
        }
    }


    public boolean hasAttribute(String attr) {
        SynapseObject bo = this;
        Enumeration enumeration = bo.elements();
        while (enumeration.hasMoreElements()) {
            GenericObject go = (GenericObject) enumeration.nextElement();
            String name = go.getName();
            if (attr.equals(name)) {
                return true;
            }
        }
        return false;
  }
}