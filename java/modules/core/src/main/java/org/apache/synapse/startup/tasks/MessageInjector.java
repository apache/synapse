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

package org.apache.synapse.startup.tasks;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.task.Task;
import org.apache.synapse.util.PayloadHelper;

/**
 * Injects a Message in to the Synapse environment
 */
public class MessageInjector implements Task, ManagedLifecycle {

    /**
     * Holds the logger for logging purposes
     */
    private Log log = LogFactory.getLog(MessageInjector.class);

    /**
     * Holds the Message to be injected
     */
    private OMElement message = null;

    /**
     * Holds the to address for the message to be injected
     */
    private String to = null;

    /**
     * Could be one of either "soap11" | "soap12" | "pox" | "get"
     */
    private String format = null;

    /**
     * SOAPAction of the message to be set, in case of the format is soap11
     */
    private String soapAction = null;

    /**
     * Holds the SynapseEnv to which the message will be injected
     */
    private SynapseEnvironment synapseEnvironment;

    public final static String SOAP11_FORMAT = "soap11";
    public final static String SOAP12_FORMAT = "soap12";
    public final static String POX_FORMAT = "pox";
    public final static String GET_FORMAT = "get";

    /**
     * Initializes the Injector
     *
     * @param se
     *          SynapseEnvironment of synapse
     */
    public void init(SynapseEnvironment se) {
		synapseEnvironment = se;
	}

    /**
     * Set the message to be injected
     *
     * @param elem
     *          OMElement describing the message
     */
    public void setMessage(OMElement elem) {
		log.debug("set message " + elem.toString());
		message = elem;
	}

    /**
     * Set the to address of the message to be injected
     *
     * @param url
     *          String containing the to address
     */
    public void setTo(String url) {
		to = url;
	}

    /**
     * Sets the format of the message
     *
     * @param format could be one of either "soap11" | "soap12" | "pox" | "get"
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Sets the SOAPAction and valid only when the format is given as soap11
     * 
     * @param soapAction SOAPAction header value to be set
     */
    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    /**
     * This will be invoked by the schedular to inject the message
     * in to the SynapseEnvironment
     */
    public void execute() {
		log.debug("execute");
		if (synapseEnvironment == null) {
			log.error("Synapse Environment not set");
			return;
		}
		if (message == null) {
			log.error("message not set");
			return;

		}
		if (to == null) {
			log.error("to address not set");
			return;

		}

        MessageContext mc = synapseEnvironment.createMessageContext();
//        AspectHelper.setGlobalAudit(mc);    TODO
        mc.pushFaultHandler(new MediatorFaultHandler(mc.getFaultSequence()));
        mc.setTo(new EndpointReference(to));
        if (format == null) {
            PayloadHelper.setXMLPayload(mc, message.cloneOMElement());
        } else {
            try {
                if (SOAP11_FORMAT.equalsIgnoreCase(format)) {
                    mc.setEnvelope(OMAbstractFactory.getSOAP11Factory().createSOAPEnvelope());
                } else if (SOAP12_FORMAT.equalsIgnoreCase(format)) {
                    mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
                } else if (POX_FORMAT.equalsIgnoreCase(format)) {
                    mc.setDoingPOX(true);
                } else if (GET_FORMAT.equalsIgnoreCase(format)) {
                    mc.setDoingGET(true);
                }
                PayloadHelper.setXMLPayload(mc, message.cloneOMElement());
            } catch (AxisFault axisFault) {
                String msg = "Error in setting the message payload : " + message;
                log.error(msg, axisFault);
                throw new SynapseException(msg, axisFault);
            }
        }
        if (soapAction != null) {
            mc.setSoapAction(soapAction);
        }
        synapseEnvironment.injectMessage(mc);

	}

    /**
     * Destroys the Injector
     */
    public void destroy() {
	}

}
