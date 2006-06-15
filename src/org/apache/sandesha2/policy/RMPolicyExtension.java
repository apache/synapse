package org.apache.sandesha2.policy;

import javax.xml.namespace.QName;

import org.apache.axis2.modules.PolicyExtension;
import org.apache.ws.policy.Policy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RMPolicyExtension implements PolicyExtension {

	public void addMethodsToStub(Document document, Element element, QName opName, Policy policy) {
		
		Element methods = document.createElement("reliableMessagingMethods");
		
		Element startSequence = document.createElement("createSequence");
		methods.appendChild(startSequence);
		
		Element setLastMessage = document.createElement("setLastMessage");
		methods.appendChild(setLastMessage);
		
		Element endSequence = document.createElement("endSequence");
		methods.appendChild(endSequence);
		
		element.appendChild(methods);
	}

}
