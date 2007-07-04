/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2.policy;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axis2.wsdl.codegen.CodeGenConfiguration;
import org.apache.axis2.wsdl.codegen.extension.PolicyExtension;
import org.apache.neethi.Policy;
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

    public void addMethodsToStub(Document document, Element element, QName methodName, List assertions) {
        // TODO Auto-generated method stub
    }

	public void init(CodeGenConfiguration codeGenConfiguration) {
		// TODO Auto-generated method stub
	}

}
