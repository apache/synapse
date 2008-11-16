/*
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
package sandesha2.samples.userguide;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.MessageContext;

public class TestCallback implements AxisCallback {

	private final static String applicationNamespaceName = "http://tempuri.org/"; 
	private final static String echoStringResponse = "echoStringResponse";
	private final static String EchoStringReturn = "EchoStringReturn";
	
	String name = null;
	boolean complete = false;
	
	public TestCallback () {
		
	}
	
	public TestCallback (String name) {
		this.name = name;
	}
	
	public void onError (Exception e) {
		System.out.println("Error reported for test call back");
		e.printStackTrace();
	}

	public void onComplete() {
		complete = true;
	}

	public boolean isComplete(){
		return complete;
	}
	
	public void onFault(MessageContext arg0) {
		SOAPBody body = arg0.getEnvelope().getBody();
		SOAPFault sf = body.getFault();
		System.out.println("Callback '" + name +  "' got fault:" + sf.toString());	
	}
	
	public void onMessage(MessageContext arg0) {
		SOAPBody body = arg0.getEnvelope().getBody();
		
		OMElement echoStringResponseElem = body.getFirstChildWithName(new QName (applicationNamespaceName,echoStringResponse));
		if (echoStringResponseElem==null) { 
			System.out.println("Error: SOAPBody does not have a 'echoStringResponse' child");
			return;
		}
		
		OMElement echoStringReturnElem = echoStringResponseElem.getFirstChildWithName(new QName (applicationNamespaceName,EchoStringReturn));
		if (echoStringReturnElem==null) { 
			System.out.println("Error: 'echoStringResponse' element does not have a 'EchoStringReturn' child");
			return;
		}
		
		String resultStr = echoStringReturnElem.getText();
		System.out.println("Callback '" + name +  "' got result:" + resultStr);
	}
}
