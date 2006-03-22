package org.apache.synapse.synapseobject;

import java.util.Date;

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
 * <p>
 * This class stores configuration values in an Object hierarchy.So instead 
 * of name value pairs it can store complex data types. This will be used by the 
 * Synapse mediators for gaining access to configuration details reguired for its functioning
 * eg.
 * </p>
 * <p>
 * &lt;SynapseObject name="SLA"><br>
 * &nbsp;&nbsp;&nbsp; &lt;attribute name="service" type="String">http://www.google.com/searchService&lt;/attribute><br>
 * &nbsp;&nbsp;&nbsp; &lt;SynapseObject name="consumer-id"><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &lt;attribute name="ip" type="string">192.168.6.127&lt;/attribute><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &lt;attribute name="priority" type="string">3&lt;/attribute><br>
 * &nbsp;&nbsp;&nbsp; &lt;/SynapseObject><br>
 * &lt;/SynapseObject>
 * </p>
 * There are utility methods for manipulating attribute and contained objects as well
 * as search methods
 */
public class SynapseObject extends Hashtable implements Serializable {
    SynapseObjectArray object = new SynapseObjectArray();
    String objectName;
    String createdBy;
    String creationDate;
/**
   * 
   * Default constructure takes a SynapseObject name
   */
    public SynapseObject(String objectName) {
        this.objectName = objectName;
    }
/**
   * 
   * This method takes an xslt(String) as a parameter and returns a SynapseObject 
   * transformed to the suggested format by the xslt
   */
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
/**
   * 
   * This method returns a SynapseObject Name
   */
    public String getSynapseObjectName() {
        return this.objectName;
    }

    /**
     * Get contained SynapseObjects
     *
     * @return SynapseObject
     */
    public SynapseObject[] getChildren() {
        return this.object.getSynapseObjects();
    }

    /**
     * Add a SynapseObject (aggregation)
     *
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
    
    public void setDate(String attributeName, Date attributeValue)
    {
      long date = attributeValue.getTime();
      GenericObject go = new GenericObject(attributeName, "DATE",String.valueOf(date));
      this.put(attributeName, go);
    }

    public Long getLong(String attributeName) {
        GenericObject go = (GenericObject) this.get(attributeName);
        return new Long(go.getValue());
    }
    
    public String getString(String attributeName)
    {
        GenericObject go = (GenericObject) this.get(attributeName);
        return go.getValue();      
    }
    
    public Boolean getBoolean(String attributeName)
    {
        GenericObject go = (GenericObject) this.get(attributeName);
        String value = go.getValue();
        if(value.equalsIgnoreCase("true"))
        return Boolean.TRUE;
        else
        return Boolean.FALSE;
    }    
    public Integer getInteger(String attributeName)
    {
        GenericObject go = (GenericObject) this.get(attributeName);
        return new Integer(go.getValue());      
    }    
    public Float getFloat(String attributeName)
    {
        GenericObject go = (GenericObject) this.get(attributeName);
        return new Float(go.getValue());      
    }    
    public Date getDate(String attributeName)
    {
        GenericObject go = (GenericObject) this.get(attributeName);
        return new Date((Long.parseLong(go.getValue())));
    }      
/**
   * 
   * This method returns the SynapseObject in an xml format
   */
    public String getXMLFragment() {
        String xmlFragStart = "";
        String xmlFragEnd = "</SynapseObject>";
        String x3 = recurse(this, xmlFragStart);
        xmlFragStart += xmlFragEnd;
        return x3;
    }
    private String recurse(SynapseObject bo, String xmlFrag) {

        xmlFrag = xmlFrag + "<SynapseObject name=\"" + bo.getSynapseObjectName() + "\">\n";
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
/**
   * 
   * This method returns a contained SynapseObject by providing an attribute value.
   * In case there are more than one matches it returns the first object found.
   * TODO: This needs to be modified to return all the objects that were found
   * eg SynapseObject[]
   */
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
    public SynapseObject findSynapseObjectByAttributeValueStartingWith(String attributeValue) {
        SynapseObject bo = new SynapseObject("Not Found");
        if (this.hasAttributeValueStartingWith(attributeValue)) {
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
    public boolean hasAttributeValueStartingWith(String attr) {
        SynapseObject bo = this;
        Enumeration enumeration = bo.elements();
        while (enumeration.hasMoreElements()) {
            GenericObject go = (GenericObject) enumeration.nextElement();
            String value = go.getValue();
            if (attr.startsWith(value)) {
                return true;
            }
        }
        return false;
    }    
/**
   * 
   * This method returns a contained SynapseObject by providing an SynapseObject name.
   * In case there are more than one matches it returns the first object found.
   * TODO: This needs to be modified to return all the objects that were found
   * eg SynapseObject[]
   */
    public SynapseObject findSynapseObjectByName(String name) {
        SynapseObject bo = new SynapseObject("Not Found");
        if (this.getSynapseObjectName().equalsIgnoreCase(name)) {
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
/**
   * 
   * This method returns contained SynapseObjects by providing an attribute name.
   * In case there are more than one matches it returns an array of SynapseObjects.
   */
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
/**
   * 
   * This method returns contained SynapseObject by providing an attribute name.
   * In case there are more than one matches it returns an array of SynapseObjects.
   */
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

/**
   * 
   * This method returns contained SynapseObject by providing an attribute value.
   * In case there are more than one matches it returns an array of SynapseObjects.
   */
    public SynapseObject[] findSynapseObjectsByAttributeValue(String attributeValue) {

        SynapseObjectArray boArray = new SynapseObjectArray();

        if (this.findChildrenWithAttributeValue(attributeValue) != null)
            boArray.addSynapseObjects(this.findChildrenWithAttributeValue(attributeValue));

        Enumeration enumeration = this.object.elements();
        while (enumeration.hasMoreElements()) {
            SynapseObject bo = (SynapseObject)enumeration.nextElement();
            if (bo.findSynapseObjectsByAttributeValue(attributeValue) != null) {
                boArray.addSynapseObjects(bo.findSynapseObjectsByAttributeValue(attributeValue));
            }
        }

        if (boArray.size() > 0) {
            return boArray.getSynapseObjects();
        } else {
            return null;
        }
    }
    
/**
   * 
   * This method returns contained SynapseObject by providing an attribute value.
   * In case there are more than one matches it returns an array of SynapseObjects.
   */
    public SynapseObject[] findSynapseObjectsByAttributeValueStartingWith(String attributeValue) {

        SynapseObjectArray boArray = new SynapseObjectArray();

        if (this.findChildrenWithAttributeValueStartingWith(attributeValue) != null)
            boArray.addSynapseObjects(this.findChildrenWithAttributeValueStartingWith(attributeValue));

        Enumeration enumeration = this.object.elements();
        while (enumeration.hasMoreElements()) {
            SynapseObject bo = (SynapseObject)enumeration.nextElement();
            if (bo.findSynapseObjectsByAttributeValueStartingWith(attributeValue) != null) {
                boArray.addSynapseObjects(bo.findSynapseObjectsByAttributeValueStartingWith(attributeValue));
            }
        }

        if (boArray.size() > 0) {
            return boArray.getSynapseObjects();
        } else {
            return null;
        }
    }
    
/**
   * 
   * This method returns contained SynapseObject by providing an attribute value.
   * In case there are more than one matches it returns an array of SynapseObjects.
   */
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
    
/**
   * 
   * This method returns contained SynapseObject by providing an attribute value.
   * In case there are more than one matches it returns an array of SynapseObjects.
   */
    public SynapseObject[] findChildrenWithAttributeValueStartingWith(String attributeValue) {
        SynapseObjectArray boa = new SynapseObjectArray();
        Enumeration enumeration = this.object.elements();
        while (enumeration.hasMoreElements()) {
            SynapseObject bo = (SynapseObject)enumeration.nextElement();
            if (bo.hasAttributeValueStartingWith(attributeValue)) {
                boa.addSynapseObject(bo);
            }
        }
        if (boa.size() > 0) {
            return boa.getSynapseObjects();
        } else {
            return null;
        }
    }    
/**
   * 
   * This method returns a boolean value of true or false depending on 
   * whether a SynapseObject contains an attribute as provided by the parameter
   */
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
  
  /**
   * 
   * @return 
   * @param value
   */
  public SynapseObjectArray getSOsByAttributeValue(String value)
  {
    SynapseObjectArray soa = new SynapseObjectArray();
    getSOsByAttributeValue(soa,this,value);
    return soa;
  }
  protected SynapseObjectArray getSOsByAttributeValue(SynapseObjectArray soa,SynapseObject so,String value)
  {
    if(so.locateAttributeValue(so,value))
    {
      System.out.println(value);
      soa.addSynapseObject(so);
    }
    SynapseObjectArray soac = so.getImmediateChildSOs();
    Enumeration enumChildren = soac.elements();
    
    while(enumChildren.hasMoreElements())
    {
      SynapseObject soic = (SynapseObject)enumChildren.nextElement();
      soic.getSOsByAttributeValue(soa,soic,value);
    }
    return soa;
  }
  
  public SynapseObjectArray getImmediateChildSOs()
  {
    return this.object;
  }
  
  public boolean locateAttributeValue(SynapseObject so, String attributeValue)
  {
    Enumeration attributes = so.elements();
    boolean returnValue = false;
    while(attributes.hasMoreElements())
    {
      GenericObject go = (GenericObject) attributes.nextElement();
      String currentValue = go.getValue();
      System.out.println(currentValue);
      if(currentValue.equals(attributeValue))
      {
        System.out.println(currentValue);
        returnValue = true;
      }
    }
    return returnValue;
  }
  /**
   * Search with filter startsWith
   */
  public SynapseObjectArray getSOsByAttributeValueStartingWith(String value)
  {
    SynapseObjectArray soa = new SynapseObjectArray();
    getSOsByAttributeValueStartingWith(soa,this,value);
    return soa;
  }
  protected SynapseObjectArray getSOsByAttributeValueStartingWith(SynapseObjectArray soa,SynapseObject so,String value)
  {
    if(so.locateAttributeValueStartingWith(so,value))
    {
      System.out.println(value);
      soa.addSynapseObject(so);
    }
    SynapseObjectArray soac = so.getImmediateChildSOs();
    Enumeration enumChildren = soac.elements();
    
    while(enumChildren.hasMoreElements())
    {
      SynapseObject soic = (SynapseObject)enumChildren.nextElement();
      soic.getSOsByAttributeValueStartingWith(soa,soic,value);
    }
    return soa;
  }
  public boolean locateAttributeValueStartingWith(SynapseObject so, String attributeValue)
  {
    Enumeration attributes = so.elements();
    boolean returnValue = false;
    while(attributes.hasMoreElements())
    {
      GenericObject go = (GenericObject) attributes.nextElement();
      String currentValue = go.getValue();
      if(currentValue.startsWith(attributeValue))
      {
        returnValue = true;
      }
    }
    return returnValue;
  }
}