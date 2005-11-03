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
package org.apache.synapse.axis2;



import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.clientapi.Call;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;

import org.apache.axis2.engine.AxisEngine;


import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.util.Utils;


import org.apache.synapse.SynapseException;

public class Sender {
	public static void send(MessageContext messageContext) {
		// TODO this only works for req-resp currently
		try {
			Call call = new Call();
			call.setTo(messageContext.getTo());

			System.out.println(messageContext.getTo().getAddress());
			 
			AxisOperation ao = messageContext.getAxisOperation();
			System.out.println("sending");
			MessageContext result = call.invokeBlocking(ao, messageContext); 
			 
	        

	        
	        MessageContext outMsgContext = Utils.createOutMessageContext(messageContext);
	        AxisEngine ae =
	                new AxisEngine(
	                        messageContext.getOperationContext().getServiceContext().getConfigurationContext());
	        //hack alert
            outMsgContext.setEnvelope(result.getEnvelope());
            if (outMsgContext.getEnvelope().getHeader()==null) outMsgContext.getEnvelope().getBody().insertSiblingBefore(OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope().getHeader());
            result.getEnvelope().serialize(XMLOutputFactory.newInstance().createXMLStreamWriter(System.out));
            //System.out.println("about to receive");
			ae.receive(outMsgContext);
			System.out.println("about to respond");
			ae.send(outMsgContext);
			

		} catch (AxisFault e) {
			throw new SynapseException(e);
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
/* result.setServerSide(true);
Object os = messageContext
.getProperty(MessageContext.TRANSPORT_OUT);
result.setProperty(MessageContext.TRANSPORT_OUT, os);
Object ti = messageContext
.getProperty(HTTPConstants.HTTPOutTransportInfo);
result.setProperty(HTTPConstants.HTTPOutTransportInfo, ti);

//if (result.getReplyTo()==null) result.setReplyTo(new EndpointReference("http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous"));
//result.setAxisService(null);
result.setAxisService()

//result.setAxisOperation(null);
result.setServerSide(true);
//respMC.setProperty("synapseResponse", Boolean.TRUE);
*/