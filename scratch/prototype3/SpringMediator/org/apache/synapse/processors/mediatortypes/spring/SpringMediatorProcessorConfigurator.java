package org.apache.synapse.processors.mediatortypes.spring;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;



import org.apache.synapse.xml.AbstractProcessorConfigurator;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ByteArrayResource;



public class SpringMediatorProcessorConfigurator extends AbstractProcessorConfigurator {
	private static final QName tagName = new QName(Constants.SYNAPSE_NAMESPACE+"/spring", "springmediator");
	public Processor compile(SynapseEnvironment se, OMElement el) {
		SpringMediatorProcessor smp = new SpringMediatorProcessor();
		super.compile(se,el,smp);
		
		OMAttribute bean = el.getAttribute(new QName("bean"));
		if (bean == null) throw new SynapseException("missing bean attribute on "+el.toString());
		
		smp.setBeanName(bean.getAttributeValue().trim());
		
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
		ctx.setClassLoader(se.getClassLoader());
		ctx.refresh();
		smp.setContext(ctx);
		return smp;
		
		
		
	}

	public QName getTagQName() {
		
		return tagName;
	}

}
