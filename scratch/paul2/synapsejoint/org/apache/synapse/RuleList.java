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
package org.apache.synapse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import javax.xml.stream.XMLStreamReader;


import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMNamespace;

import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.synapse.axis2.Expression;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ByteArrayResource;

public class RuleList extends ArrayList {
	// private namespaceContext

	private String name = null;

	private static final long serialVersionUID = 6108743941694238422L;

	public static final String namespace = "http://ws.apache.org/synapse/ns/rulelist/1";

	public static final QName ruleQ = new QName(namespace, "rule");

	public static final QName rulelistQ = new QName(namespace, "rulelist");

	public static final QName nameQ = new QName("", "name");

	public static final QName xpathQ = new QName("", "xpath");

	public static final QName mediatorQ = new QName("", "mediator");

	public static final QName secureQ = new QName("", "secure");

	public static final QName reliableQ = new QName("", "reliable");

	public static final QName transactionalQ = new QName("", "transactional");

	public RuleList(InputStream in, ClassLoader cl) {
		XMLInputFactory xif = XMLInputFactory.newInstance();
		try {
			XMLStreamReader xsr = xif.createXMLStreamReader(in);
			StAXOMBuilder builder = new StAXOMBuilder(xsr);
			OMElement rulelist = builder.getDocumentElement();

			if (!rulelist.getQName().equals(rulelistQ))
				throw new Exception("not a " + rulelistQ.toString()
						+ " element");

			if (rulelist.getAttribute(nameQ) == null)
				throw new Exception("no " + nameQ.toString() + " attribute");
			String rlname = rulelist.getAttribute(nameQ).getAttributeValue();
			this.setName(rlname);

			Iterator ruleIt = rulelist.getChildrenWithName(ruleQ);

			while (ruleIt.hasNext()) {
				OMElement rule = (OMElement) ruleIt.next();
				Rule r = new Rule();
				if (rule.getAttribute(xpathQ) == null)
					throw new Exception("missing " + xpathQ.toString()
							+ " attribute");

				Expression expr = new Expression(rule.getAttribute(xpathQ)
						.getAttributeValue());
				Iterator i = rulelist.getAllDeclaredNamespaces();
				while (i.hasNext()) {
					OMNamespace n = (OMNamespace) i.next();
					expr.addNamespace(n.getPrefix(), n.getName());
				}

				r.setExpression(expr);
				if (rule.getAttribute(mediatorQ) == null)
					throw new Exception("missing " + mediatorQ.toString()
							+ " attribute");
				r.setMediatorName(rule.getAttribute(mediatorQ)
						.getAttributeValue());
				if (rule.getAttribute(reliableQ) != null)
					r.setReliable(isTrue(rule.getAttribute(reliableQ)
							.getAttributeValue()));
				if (rule.getAttribute(secureQ) != null)
					r.setSecure(isTrue(rule.getAttribute(secureQ)
							.getAttributeValue()));
				if (rule.getAttribute(transactionalQ) != null)
					r.setTransactional(isTrue(rule.getAttribute(transactionalQ)
							.getAttributeValue()));

				Iterator it2 = rule.getChildElements();
				while (it2.hasNext()) {
					OMElement el = (OMElement) it2.next();
					if (el.getLocalName().equals("beans")) {

						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						baos
								.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN//EN\" \"http://www.springframework.org/dtd/spring-beans.dtd\">"
										.getBytes());

						XMLStreamWriter xsw = XMLOutputFactory.newInstance()
								.createXMLStreamWriter(baos);
						
						xsw.setDefaultNamespace(el.getNamespace().getName());
						el.serialize(xsw);
						baos.close();
						GenericApplicationContext ctx = new GenericApplicationContext();
						XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(
								ctx);
						byte[] bytes = baos.toByteArray();
						System.out.println(new String(bytes));
						xbdr.loadBeanDefinitions(new ByteArrayResource(bytes));
						ctx.setClassLoader(cl);
						ctx.refresh();

						r.setSpringBeanFactory(ctx);
						continue;
					}
				}

				this.add(r);
			}

		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	private boolean isTrue(String att) {
		char c = att.toLowerCase().charAt(0);
		return c == 't' || c == 'y';
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[rulelist('" + name + "')]\n");
		int i = 1;
		Iterator it = this.iterator();
		while (it.hasNext()) {
			Rule r = (Rule) it.next();
			sb.append(i++);
			sb.append('.');
			sb.append(' ');
			sb.append(r.toString());
			sb.append('\n');
		}
		return sb.toString();
	}

}
