package org.apache.synapse.mediators;

import java.io.ByteArrayOutputStream;

import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.MediatorConfiguration;
import org.apache.synapse.MediatorConfigurator;
import org.apache.synapse.SynapseException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ByteArrayResource;

public class SpringMediatorConfigurator implements MediatorConfigurator {
	
	
	public MediatorConfiguration parse(OMElement el, ClassLoader cl) {
		SpringMediatorConfiguration mc = new SpringMediatorConfiguration();
		OMAttribute name = el.getAttribute(new QName("", "name"));
		if (name == null) throw new SynapseException("missing name attribute on "+el.toString());
		mc.setMediatorName(name.getAttributeValue());
		OMAttribute bean = el.getAttribute(new QName("", "bean"));
		if (name == null) throw new SynapseException("missing bean attribute on "+el.toString());
		mc.setBeanName(bean.getAttributeValue());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN//EN\" \"http://www.springframework.org/dtd/spring-beans.dtd\">"
							.getBytes());
			XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(baos);
			OMElement beans = null;
			Iterator it = el.getChildElements();
			while (it.hasNext()) {
				OMElement ths = (OMElement)it.next();
				if (ths.getLocalName().toLowerCase().equals("beans")) {
					beans = ths;
					break;
				}
			}
			if (beans==null) throw new SynapseException("<beans> element not found in "+el.toString());
			xsw.setDefaultNamespace(beans.getNamespace().getName());
			beans.serialize(xsw);
		} catch (Exception e) {
			throw new SynapseException(e);
		} 
				
		GenericApplicationContext ctx = new GenericApplicationContext();
		XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(
				ctx);
		xbdr.setValidating(false);
		xbdr.loadBeanDefinitions(new ByteArrayResource(baos.toByteArray()));
		ctx.setClassLoader(cl);
		ctx.refresh();
		
		
		mc.setMediatorElement(el);
		mc.setApplicationContext(ctx);
		
		return mc;
	}



}
