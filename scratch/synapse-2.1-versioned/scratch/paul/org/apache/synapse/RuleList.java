package org.apache.synapse;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;

import javax.xml.stream.XMLStreamReader;


import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;

public class RuleList extends ArrayList {
	// private namespaceContext
	
	private String name = null;

	private static final long serialVersionUID = 6108743941694238422L;
	public static final String namespace = "http://ws.apache.org/synapse/ns/rulelist/1";
	public static final QName ruleQ = new QName(namespace,"rule");
	public static final QName rulelistQ = new QName(namespace,"rulelist");
	public static final QName nameQ = new QName("","name");
	public static final QName xpathQ = new QName("","xpath");
	public static final QName mediatorQ = new QName("","mediator");
	public static final QName secureQ = new QName("","secure");
	public static final QName reliableQ = new QName("","reliable");
	public static final QName transactionalQ = new QName("","transactional");
	
	public RuleList(InputStream in) {
		XMLInputFactory xif = XMLInputFactory.newInstance();
		try {
			XMLStreamReader xsr = xif.createXMLStreamReader(in);
			StAXOMBuilder builder = new StAXOMBuilder(xsr);
			OMElement rulelist = builder.getDocumentElement();
			
		
			
			if (!rulelist.getQName().equals(rulelistQ)) throw new Exception("not a "+rulelistQ.toString()+" element");
			
			if (rulelist.getAttribute(nameQ)==null) throw new Exception("no "+nameQ.toString()+" attribute");
			String rlname = rulelist.getAttribute(nameQ).getValue();
			this.setName(rlname);
			
					
			
			Iterator ruleIt = rulelist.getChildrenWithName(ruleQ);
			
			while (ruleIt.hasNext()) {
				OMElement rule = (OMElement)ruleIt.next();
				Rule r = new Rule();
				if (rule.getAttribute(xpathQ)==null) throw new Exception("missing "+xpathQ.toString()+" attribute");
				r.setXpath(rule.getAttribute(xpathQ).getValue());
				if (rule.getAttribute(mediatorQ)==null) throw new Exception("missing "+mediatorQ.toString()+" attribute");
				r.setMediatorName(rule.getAttribute(mediatorQ).getValue());
				if (rule.getAttribute(reliableQ)!=null) r.setReliable(isTrue(rule.getAttribute(reliableQ).getValue()));
				if (rule.getAttribute(secureQ)!=null) r.setSecure(isTrue(rule.getAttribute(secureQ).getValue()));
				if (rule.getAttribute(transactionalQ)!=null) r.setTransactional(isTrue(rule.getAttribute(transactionalQ).getValue()));
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
		return c=='t' || c=='y';
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[rulelist('"+name+"')]\n");
		int i=1;
		Iterator it = this.iterator();
		while (it.hasNext()) {
			Rule r = (Rule)it.next();
			sb.append(i++);
			sb.append('.');
			sb.append(' ');
			sb.append(r.toString());
			sb.append('\n');
		}
		return sb.toString();
	}
	
}


