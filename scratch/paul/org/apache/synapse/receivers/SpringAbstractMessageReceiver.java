/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.receivers;

import java.lang.reflect.Method;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.soap.SOAP11Constants;
import org.apache.axis2.soap.SOAP12Constants;
import org.apache.axis2.soap.SOAPFactory;
import org.springframework.beans.factory.BeanFactory;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.xml.namespace.QName;

public abstract class SpringAbstractMessageReceiver implements MessageReceiver {
    public static final String SERVICE_SPRING_XML = "SpringXmlURL";
    public static final String SERVICE_SPRING_INLINE_XML = "SpringXMLInline";
    public static final String SERVICE_SPRING_BEANNAME = "SpringBeanName";
    public static final String SCOPE = "scope";

    protected SOAPFactory fac;

    /**
     * Method makeNewServiceObject
     *
     * @param msgContext
     * @return
     * @throws AxisFault
     */
    protected Object makeNewServiceObject(MessageContext msgContext)
        throws AxisFault {
        try {

            String nsURI = msgContext.getEnvelope().getNamespace().getName();
            if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(nsURI)) {
                fac = OMAbstractFactory.getSOAP12Factory();
            } else if (
                SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(nsURI)) {
                fac = OMAbstractFactory.getSOAP11Factory();
            } else {
                throw new AxisFault(Messages.getMessage("invalidSOAPversion"));
            }

            ServiceDescription service =
                msgContext
                    .getOperationContext()
                    .getServiceContext()
                    .getServiceConfig();
                        
            Parameter implInlineParam = service.getParameter(SERVICE_SPRING_INLINE_XML);
            Parameter implXMLParam = service.getParameter(SERVICE_SPRING_XML);
            Parameter implBeanParam = service.getParameter(SERVICE_SPRING_BEANNAME);
            
            String beanName = ((String) implBeanParam.getValue()).trim();
            if (implXMLParam != null && implBeanParam != null) {
            	ClassLoader cl  = service.getClassLoader();
            	String xmlFile = ((String) implXMLParam.getValue()).trim();
            	
            	ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] {xmlFile}, false);
            	ctx.setClassLoader(cl);
            	ctx.refresh();
            	Object bean = ((BeanFactory)ctx).getBean(beanName);
            	System.out.println(bean);
            	System.out.println(bean.getClass());
                return bean;
            //} else if (implInlineParam != null && implBeanParam != null){
            //	String xmlInline = ((String) implInlineParam.getValue()).trim();
                //XmlBeanFactory xbf = new XmlBeanFactory(new ByteArrayResource(xmlInline.getBytes()));
                //return xbf.getBean(beanName);
            } else {
                throw new AxisFault(
                    Messages.getMessage(
                        "paramIsNotSpecified",
                        "SERVICE_CLASS"));
            }
            
        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        }
    }

    /**
     * Method getTheImplementationObject
     *
     * @param msgContext
     * @return
     * @throws AxisFault
     */
    protected Object getTheImplementationObject(MessageContext msgContext)
        throws AxisFault {
        ServiceDescription service =
            msgContext
                .getOperationContext()
                .getServiceContext()
                .getServiceConfig();

        Parameter scopeParam = service.getParameter(SCOPE);	
        QName serviceName = service.getName();
        if (scopeParam != null
            && Constants.SESSION_SCOPE.equals(scopeParam.getValue())) {
            SessionContext sessionContext = msgContext.getSessionContext();
            synchronized (sessionContext) {
                Object obj =
                    sessionContext.getProperty(serviceName.getLocalPart());
                if (obj == null) {
                    obj = makeNewServiceObject(msgContext);
                    sessionContext.setProperty(serviceName.getLocalPart(), obj);
                }
                return obj;
            }
        } else if (
            scopeParam != null
                && Constants.APPLICATION_SCOPE.equals(scopeParam.getValue())) {
            ConfigurationContext globalContext = msgContext.getSystemContext();
            synchronized (globalContext) {
                Object obj =
                    globalContext.getProperty(serviceName.getLocalPart());
                if (obj == null) {
                    obj = makeNewServiceObject(msgContext);
                    globalContext.setProperty(serviceName.getLocalPart(), obj);
                }
                return obj;
            }
        } else {
            return makeNewServiceObject(msgContext);
        }
    }

    public SOAPFactory getSOAPFactory() {
        return fac;
    }
    

}
