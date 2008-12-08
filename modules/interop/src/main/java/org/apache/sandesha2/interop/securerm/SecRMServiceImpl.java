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

package org.apache.sandesha2.interop.securerm;

import java.util.HashMap;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;

public class SecRMServiceImpl {

	HashMap<String, String> sequenceTextMap = new HashMap<String, String>();
	String namespaceValue = "http://tempuri.org/"; 
	String Ping = "Ping";
	String Text = "Text";
	String echoString = "echoString";
	String Sequence = "Sequence";
	String echoStringResponse = "echoStringResponse";
	String EchoStringReturn = "EchoStringReturn";
	
	public OMElement EchoString(OMElement echoStringElement) throws Exception {
		if (echoStringElement==null)
			throw new Exception ("Payload is null");
		
		OMElement textElement = echoStringElement.getFirstChildWithName(new QName (namespaceValue,Text));
		OMElement sequenceElement = echoStringElement.getFirstChildWithName(new QName (namespaceValue,Sequence));
		
		if (textElement==null)
			throw new Exception ("'Text' element is null");
		
		if (sequenceElement==null)
			throw new Exception ("'Sequence' element is null");
		
		String text = textElement.getText();
		String sequence = sequenceElement.getText();
		
		System.out.println("'EchoString' got text '" + text + "' for the sequence '" + sequence + "'.");
		
		String oldReturnValue = (String) sequenceTextMap.get(sequence);
		String newReturnValue = oldReturnValue==null?text:oldReturnValue+text;
		
		sequenceTextMap.put(sequence, newReturnValue);
		
		OMFactory factory = echoStringElement.getOMFactory();
		OMNamespace namespace = factory.createOMNamespace(namespaceValue, "ns1");
		OMElement echoStringResponseElement = factory.createOMElement(echoStringResponse,namespace);
		OMElement echoStringReturnElement = factory.createOMElement(EchoStringReturn, namespace);
		echoStringReturnElement.setText(newReturnValue);
		echoStringResponseElement.addChild(echoStringReturnElement);
		
		return echoStringResponseElement;
	}

	public void Ping(OMElement pingElement) throws Exception {
		if (pingElement==null)
			throw new Exception ("Payload is null");
		
		OMElement textElement = pingElement.getFirstChildWithName(new QName (namespaceValue,Text));
		String text = textElement.getText();
		
		System.out.println("'Ping' got text '" + text + "'.");
	}

}
