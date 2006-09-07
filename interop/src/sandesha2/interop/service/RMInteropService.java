/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package sandesha2.interop.service;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;

public class RMInteropService {

	private static Map sequenceStrings = new HashMap();  //TODO make this non static
	private final String applicationNamespaceName = "http://tempuri.org/"; 
	private final String Text = "Text";
	private final String Sequence = "Sequence";
	private final String echoStringResponse = "echoStringResponse";
	private final String EchoStringReturn = "EchoStringReturn";
	
	public OMElement echoString(OMElement in) throws Exception {
		
		OMElement textElem = in.getFirstChildWithName(new QName (applicationNamespaceName,Text));
		OMElement sequenceElem = in.getFirstChildWithName(new QName (applicationNamespaceName,Sequence));
		
		if (textElem==null)
			throw new Exception ("'Text' element is not present as a child of the 'echoString' element");
		if (sequenceElem==null)
			throw new Exception ("'Sequence' element is not present as a child of the 'echoString' element");
		
		String textStr = textElem.getText();
		String sequenceStr = sequenceElem.getText();
		
		System.out.println("'EchoString' service got text '" + textStr + "' for the sequence '" + sequenceStr + "'");
		
		String previousText = (String) sequenceStrings.get(sequenceStr);
		String resultText = (previousText==null)?textStr:previousText+textStr;
		sequenceStrings.put(sequenceStr,resultText);
		
		
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace applicationNamespace = fac.createOMNamespace(applicationNamespaceName,"ns1");
		OMElement echoStringResponseElem = fac.createOMElement(echoStringResponse, applicationNamespace);
		OMElement echoStringReturnElem = fac.createOMElement(EchoStringReturn, applicationNamespace);
		
		echoStringReturnElem.setText(resultText);
		echoStringResponseElem.addChild(echoStringReturnElem);
		
		return echoStringResponseElem;
	}
  
	public void ping(OMElement in) throws Exception  {
		OMElement textElem = in.getFirstChildWithName(new QName (applicationNamespaceName,Text));
		if (textElem==null)
			throw new Exception ("'Text' element is not present as a child of the 'Ping' element");
		
		String textValue = textElem.getText();
		
		System.out.println("ping service got text:" + textValue);
	}
	
}
