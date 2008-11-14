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

package sandesha2.samples.userguide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMText;
import org.apache.axis2.AxisFault;

public class RMSampleService {

	private static Map<String, String> sequenceStrings = new HashMap<String, String>();  //TODO make this non static
	private final String applicationNamespaceName = "http://tempuri.org/"; 
	private final String Text = "Text";
	private final String Sequence = "Sequence";
	private final String echoStringResponse = "echoStringResponse";
	private final String EchoStringReturn = "EchoStringReturn";
	private final String Attachment = "Attachment";
	private final String DESTINATION_IMAGE_FILE = "mtom-image1.jpg";

  public void init(org.apache.axis2.context.ServiceContext serviceContext) {

  }

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
	
	public void MTOMPing(OMElement in) throws Exception  {
		OMElement attachmentElem = in.getFirstChildWithName(new QName(applicationNamespaceName, Attachment));
		if (attachmentElem == null)
			throw new AxisFault("'Attachment' element is not present as a child of the 'Ping' element");

		OMText binaryElem = (OMText) attachmentElem.getFirstOMChild();

		binaryElem.setOptimize(true);
		DataHandler dataHandler = (DataHandler) binaryElem.getDataHandler();

		try {
			
			File destinationFile = new File(DESTINATION_IMAGE_FILE);
			if (destinationFile.exists())
				destinationFile.delete();

			FileOutputStream fileOutputStream = new FileOutputStream(DESTINATION_IMAGE_FILE);

			InputStream inputStream = dataHandler.getDataSource().getInputStream();
			byte[] bytes = new byte[5000];
			int length = inputStream.read(bytes);
			fileOutputStream.write(bytes, 0, length);
			fileOutputStream.close();

		} catch (Exception e) {
			throw AxisFault.makeFault(e);
		}
	}
}
