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


import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.MessageInformationHeaders;
import org.apache.axis2.addressing.miheaders.RelatesTo;
import org.apache.axis2.soap.SOAPEnvelope;


public interface SOAPMessageContext {

	public EndpointReference getFaultTo();

	public EndpointReference getFrom();

	public boolean isInFaultFlow();

	public SOAPEnvelope getEnvelope();

	public String getMessageID();

	public RelatesTo getRelatesTo();

	public EndpointReference getReplyTo();

	public EndpointReference getTo();

	public void setFaultTo(EndpointReference reference);

	public void setFrom(EndpointReference reference);

	public void setInFaultFlow(boolean b);

	public void setEnvelope(SOAPEnvelope envelope);

	public void setMessageID(String string);

	public void setProcessingFault(boolean b);

	public void setRelatesTo(RelatesTo reference);

	public void setReplyTo(EndpointReference reference);

	public void setTo(EndpointReference reference);

	public void setWSAAction(String actionURI);

	public String getWSAAction();

	public void setWSAMessageId(String messageID);

	public String getWSAMessageId();

	public MessageInformationHeaders getMessageInformationHeaders();

	public void setMessageInformationHeaders(MessageInformationHeaders collection);

	public Object getProperty(String key, boolean persistent);

	public String getSoapAction();

	public void setSoapAction(String string);

	public boolean isDoingMTOM();

	public void setDoingMTOM(boolean b);

	public boolean isDoingREST();

	public void setDoingREST(boolean b);

	public boolean isSOAP11();

	public boolean isResponse();
	public void setResponse(boolean b);
}
