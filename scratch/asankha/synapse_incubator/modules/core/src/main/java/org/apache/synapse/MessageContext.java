/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse;


import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

import java.util.Set;


/**
 * The Synapse Message Context is available to all mediators through which it flows. It
 * allows one to call to the underlying SynapseEnvironment (i.e. the SOAP engine
 * - such as Axis2) where required. It also allows one to access the current
 * SynapseConfiguration. Additionally it holds per message properties (i.e. local
 * properties valid for the lifetime of the message), and the current SOAPEnvelope
 */
public interface MessageContext {

    /**
     * Get a reference to the current SynapseConfiguration
     *
     * @return the current synapse configuration
     */
    public SynapseConfiguration getConfiguration();

    /**
     * Set or replace the Synapse Configuration instance to be used. May be used to
     * programatically change the configuration at runtime etc.
     *
     * @param cfg The new synapse configuration instance
     */
    public void setConfiguration(SynapseConfiguration cfg);

    /**
     * Returns a reference to the host Synapse Environment
     * @return the Synapse Environment
     */
    public SynapseEnvironment getEnvironment();

    /**
     * Sets the SynapseEnvironment reference to this context
     * @param se the reference to the Synapse Environment
     */
    public void setEnvironment(SynapseEnvironment se);

    /**
     * Get the value of a custom (local) property set on the message instance
     * @param key key to look up property
     * @return value for the given key
     */
    public Object getProperty(String key);

    /**
     * Get the value of a correlation property set on the message instance
     * @param key key to look up property
     * @return value for the given key
     */
    public Object getCorrelationProperty(String key);

    /**
     * Set a custom (local) property with the given name on the message instance
     * @param key key to be used
     * @param value value to be saved
     */
    public void setProperty(String key, Object value);

    /**
     * Set a local message property with the given name and value, and copy it over
     * to any corresponding response messages for correlation
     * @param key key to be used
     * @param value value to be saved
     */
    public void setCorrelationProperty(String key, Object value);

    /**
     * Returns the Set of keys over the properties on this message context
     * @return a Set of keys over message properties
     */
    public Set getPropertyKeySet();

    /**
     * Returns the Set of keys that should be correlated to a response
     * @return a Set of keys that should be correlated with the response
     */
    public Set getCorrelationPropertyKeySet();

    /**
     * Get the SOAP envelope of this message
     * @return the SOAP envelope of the message
     */
    public SOAPEnvelope getEnvelope();

    /**
     * Sets the given envelope as the current SOAPEnvelope for this message
     * @param envelope the envelope to be set
     * @throws org.apache.axis2.AxisFault on exception
     */
    public void setEnvelope(SOAPEnvelope envelope) throws AxisFault;

    // --- SOAP Message related methods ------
    /** Get the faultTo EPR if available */
    public EndpointReference getFaultTo();

    /** Set the faultTo EPR */
    public void setFaultTo(EndpointReference reference);

    /** Get the from EPR if available */
    public EndpointReference getFrom();

    /** Set the from EPR */
    public void setFrom(EndpointReference reference);

    /** Get the message id if available */
    public String getMessageID();

    /** Set the message id */
    public void setMessageID(String string);

    /** Get the relatesTo of this message */
    public RelatesTo getRelatesTo();

    /**
     * Sets the relatesTo references for this message
     * @param reference the relatesTo references array
     */
    public void setRelatesTo(RelatesTo[] reference);

    /** Set the replyTo EPR */
    public EndpointReference getReplyTo();

    /** Get the replyTo EPR if available */
    public void setReplyTo(EndpointReference reference);

    /** Get the To EPR */
    public EndpointReference getTo();

     /**
     * Set the To EPR
     * @param reference the To EPR
     */
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
    public void setWSAMessageID(String messageID);

    /**
     * Gets the message id
     * @return the WSA MessageID
     */
    public String getWSAMessageID();

    /**
     * If this message using MTOM?
     * @return true if using MTOM
     */
    public boolean isDoingMTOM();

    /**
     * If this message using SWA?
     * @return true if using SWA
     */
    public boolean isDoingSWA();

    /**
     * Marks as using MTOM
     * @param b true to mark as using MTOM
     */
    public void setDoingMTOM(boolean b);

    /**
     * Marks as using SWA
     * @param b true to mark as using SWA
     */
    public void setDoingSWA(boolean b);

    /**
     * Is this message over POX?
     * @return true if over POX
     */
    public boolean isDoingPOX();

    /**
     * Marks this message as over REST
     * @param b true to mark as REST
     */
    public void setDoingPOX(boolean b);

    /**
     * Is this message a SOAP 1.1 message?
     * @return true if this is a SOAP 1.1 message
     */
    public boolean isSOAP11();

    /**
     * Mark this message as a response or not.
     * @see org.apache.synapse.MessageContext#isResponse()
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
     * @see org.apache.synapse.MessageContext#isFaultResponse()
     * @param b true to mark this as a fault response
     */
    public void setFaultResponse(boolean b);

    /**
     * Is this message a response to a fault message?
     * @return true if this is a response to a fault message
     */
    public boolean isFaultResponse();

    /**
     * This is used to check whether the tracing should be enabled on the current mediator or not
     * @return indicate whether tracing is on, off or unset
     */
    public int getTracingState();

    /**
     * This is used to set the value of tracing enable variable
     * @param tracingState Set whether the tracing is enabled or not
     */
    public void setTracingState(int tracingState);

}
