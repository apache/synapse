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

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;

/**
 * <p> This interface defines a message flowing through Synapse
 * <p> The aim of this is to capture the Message Addressing Properties
 * (aka Message Information Headers) and the SOAP Envelope.
 * The core To/From/FaultTo/ReplyTo/MessageID/RelatesTo stuff is here. <p>
 * In addition this has get/setEnvelope.
 * <p/>
 * There is also a bag of properties<p>
 * There are markers for whether we support REST, MTOM and also if this is a response or not
 */
public interface SynapseMessage {

    /**
     * Get the SOAP envelope of this message
     * @return the SOAP envelope of the message
     */
    public SOAPEnvelope getEnvelope();

    /**
     * Sets the given envelope as the current SOAPEnvelope for this message
     * @param envelope the envelope to be set
     * @throws AxisFault on exception
     */
    public void setEnvelope(SOAPEnvelope envelope) throws AxisFault;

    public EndpointReference getFaultTo();

    public void setFaultTo(EndpointReference reference);

    public EndpointReference getFrom();

    public void setFrom(EndpointReference reference);

    public String getMessageID();

    public void setMessageID(String string);

    public RelatesTo getRelatesTo();

    public void setRelatesTo(RelatesTo[] reference);

    public EndpointReference getReplyTo();

    public void setReplyTo(EndpointReference reference);

    public EndpointReference getTo();

    public void setTo(EndpointReference reference);

    /**
     * Sets the WSAAction
     * @param actionURI the WSAAction
     */
    public void setWSAAction(String actionURI);

    /**
     * Returns the WSAAction
     * @return the WSAAction
     */
    public String getWSAAction();

    /**
     * Returns the SOAPAction of the message
     * @return the SOAPAction
     */
    public String getSoapAction();

    /**
     * Set the SOAPAction
     * @param string the SOAP Action
     */
    public void setSoapAction(String string);

    /**
     * Set the message if
     * @param messageID
     */
    public void setMessageId(String messageID);

    /**
     * Gets the message id
     * @return the message id
     */
    public String getMessageId();

    /**
     * If this message using MTOM?
     * @return true if using MTOM
     */
    public boolean isDoingMTOM();

    /**
     * Marks as using MTOM
     * @param b true to mark as using MTOM
     */
    public void setDoingMTOM(boolean b);

    /**
     * Is this message over REST?
     * @return true if over REST
     */
    public boolean isDoingREST();

    /**
     * Marks this message as over REST
     * @param b true to mark as REST
     */
    public void setDoingREST(boolean b);

    /**
     * Is this message a SOAP 1.1 message?
     * @return true if this is a SOAP 1.1 message
     */
    public boolean isSOAP11();

    /**
     * Mark this message as a response or not.
     * @see org.apache.synapse.SynapseMessage#isResponse()
     * @param b true to set this as a response
     */
    public void setResponse(boolean b);

    /**
     * Is this message a response to a synchronous message sent out through Synapse?
     * @return true if this message is a response message
     */
    public boolean isResponse();

    /**
     * Marks this message as a fault response
     * @see org.apache.synapse.SynapseMessage#isFaultResponse()
     * @param b true to mark this as a fault response
     */
    public void setFaultResponse(boolean b);

    /**
     * Is this message a response to a fault message?
     * @return true if this is a response to a fault message
     */
    public boolean isFaultResponse();

    /**
     * Return a reference to the SynapseContext
     * @return the SynapseContext
     */
    public SynapseContext getSynapseContext();

    /**
     * Set the reference to the SynapseContext
     * @param env the SynapseContext
     */
    public void setSynapseContext(SynapseContext env);
}
