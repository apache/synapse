package org.apache.synapse.mediators.ci;

import java.io.InputStream;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.synapseobject.SynapseObject;
import org.apache.synapse.synapseobject.Utils;
import org.apache.synapse.synapseobject.SynapseObjectArray;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.axis2.util.*;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;


/**
 * 
 * @version 1.0
 * @author Soumadeep Sen
 */
public class CIMediator implements Mediator, EnvironmentAware
{
  private ClassLoader classLoader;
    private InputStream inStream = null;
  private SynapseObject ciObject = null;
  private SynapseObject authenticationInfo = new SynapseObject("AuthenticationInfo");
  public CIMediator()
  {
  }
  public boolean mediate(SynapseMessage synapseMessageContext) {
    boolean returnValue=false;
     MessageContext mc = ((Axis2SynapseMessage)synapseMessageContext).getMessageContext();
     String resource = CIConstants.CFG_XML_FOLDER + "/" + CIConstants.CFG_CI_XML;
     inStream = classLoader.getResourceAsStream(resource);
     Map map = (Map)mc.getProperty(MessageContext.TRANSPORT_HEADERS);
     System.out.println(map);
     // For identifying the source IP the getAddress method is being used
     // Later it will be taken directly from the message context, the flip
     // side to the current approach is that the client has to set the value 
     String fromAddress = synapseMessageContext.getFrom().getAddress();
    // Get the CI.xml and load it 
    if(ciObject == null){
      ciObject = Utils.xmlToSynapseObject(inStream);
    }
    // Find out out the sercurity info sent with the Rquest
      checkIPRange(fromAddress);  
      checkWSSec(mc.getEnvelope()); 
      checkHTTPToken(map);  
      checkCert(mc.getEnvelope());
      
    System.out.println(authenticationInfo.getXMLFragment());
    
    if(authenticationInfo.getBoolean(CIConstants.HTTP_USER_TOKEN).booleanValue() ||
    authenticationInfo.getBoolean(CIConstants.IP_TOKEN).booleanValue() ||
    authenticationInfo.getBoolean(CIConstants.USER_CERT).booleanValue() ||
    authenticationInfo.getBoolean(CIConstants.WS_SEC_USER_TOKEN).booleanValue()
    )
    {
      returnValue = true;
    }
    return returnValue;
  }
  public void setSynapseEnvironment(SynapseEnvironment se) {
  /*  this.se = se;
    if(se!=null){
        log.info("ENVIRONMENT NOT NULL IN CI");
    }*/
  }

  public void setClassLoader(ClassLoader cl) {
      this.classLoader = cl;
  }
  private boolean checkHTTPToken(Map map)
  {
    boolean returnValue = false;
    String obj = (String)map.get("authorization");
    int inte = obj.indexOf(" ");
    obj = obj.substring(inte+1);
    byte[] bytes = Base64.decode(obj);
    String finalStr = new String(bytes);
    finalStr += ":";
    StringTokenizer st = new StringTokenizer(finalStr,":");
    System.out.print("Tokens  "+st.countTokens());
    String userName = st.nextToken();
    String password = st.nextToken();
    System.out.println("Username "+userName+" Password "+password);
    if(ciObject == null){
      ciObject = Utils.xmlToSynapseObject(inStream);
    }
    SynapseObjectArray so = ciObject.getSOsByAttributeValueStartingWith(userName);
    if(so.size() > 0)
    {
      returnValue = true;
    }
    authenticationInfo.setBoolean(CIConstants.HTTP_USER_TOKEN,String.valueOf(returnValue));
    return returnValue;
  }
  
  boolean checkWSSec(SOAPEnvelope env)
  {
    boolean returnValue = false;
    authenticationInfo.setBoolean(CIConstants.WS_SEC_USER_TOKEN,String.valueOf(returnValue));
    return returnValue;
  }
     /**
     * Format expected is
     * <p>
     * <SynapseObject name="ConsumerIdentification">
     *  <SynapseObject name="consumer0">
     *    <Attribute name="CONSUMER_TYPE" type="String">GOLD</Attribute>
     *    <Attribute name="IP_ADDRESS_FROM" type="String">192.168.6.0</Attribute>
     *    <Attribute name="IP_ADDRESS_TO" type="String">192.168.6.255</Attribute>
     *    <Attribute name="HTTP_AUTH_USERNAME" type="String">john</Attribute>
     *    <Attribute name="WS_SEC_USERNAME" type="String">john</Attribute>
     *  </SynapseObject>
     *  <SynapseObject name="consumer1">
     *    <Attribute name="CONSUMER_TYPE" type="String">SILVER</Attribute>
     *    <Attribute name="IP_ADDRESS_FROM" type="String">192.168.6.100</Attribute>
     *    <Attribute name="IP_ADDRESS_TO" type="String">192.168.6.255</Attribute>
     *    <Attribute name="HTTP_AUTH_USERNAME" type="String">mary</Attribute>
     *    <Attribute name="WS_SEC_USERNAME" type="String">mary</Attribute>
     *  </SynapseObject>       
     * </SynapseObject>
     * </p>
     */
  boolean checkIPRange(String fromAddress)
  {
    boolean returnValue = false;
    if(ciObject == null){
      ciObject = Utils.xmlToSynapseObject(inStream);
    }
    SynapseObjectArray so = ciObject.getSOsByAttributeValueStartingWith(fromAddress);
    if(so.size() > 0)
    {
      returnValue = true;
    }
    authenticationInfo.setBoolean(CIConstants.IP_TOKEN,String.valueOf(returnValue));    
    return returnValue;
  }
  boolean checkCert(SOAPEnvelope env)
  {
    boolean returnValue = true;
    //if(returnValue == false){
      System.out.println("Check Cert failed");
   // }
    authenticationInfo.setBoolean(CIConstants.USER_CERT,String.valueOf(returnValue));    
    return returnValue;
  }
}