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

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Endpoint;
import org.apache.synapse.config.XMLToObjectMapper;

import javax.xml.namespace.QName;

/**
 * Creates an Endpoint instance using the XML fragment specification
 * 
 * <endpoint name="string" address="url" [force="soap|pox"] [optimize="mtom|swa"]>
 *  .. extensibility ..
 * 
 * <enableRM [policy="key"]/>+ <enableSec [policy="key"]/>+ <enableAddressing
 * separateListener="true|false"/>+
 * 
 * 
 * </endpoint>
 */
public class EndpointFactory implements XMLToObjectMapper {

	private static Log log = LogFactory.getLog(EndpointFactory.class);

	private static final EndpointFactory instance = new EndpointFactory();

	private EndpointFactory() {
	}

	public static Endpoint createEndpoint(OMElement elem,
			boolean anonymousEndpoint) {

		OMAttribute name = elem.getAttribute(new QName(
				Constants.NULL_NAMESPACE, "name"));
		OMAttribute address = elem.getAttribute(new QName(
				Constants.NULL_NAMESPACE, "address"));
		OMAttribute force = elem.getAttribute(new QName(
					Constants.NULL_NAMESPACE, "force"));
        OMAttribute optimize = elem.getAttribute(new QName(
					Constants.NULL_NAMESPACE, "optimize"));

        Endpoint endpoint = new Endpoint();
		if (!anonymousEndpoint) {
			if (name == null) {
				handleException("The 'name' attribute is required for a named endpoint definition");
			} else {
				endpoint.setName(name.getAttributeValue());
			}
			if (address != null) {
				endpoint.setAddress(address.getAttributeValue());
			} else {
				// right now an address is *required*
				handleException("The 'address' attribute is required for an endpoint");
			}
		} else {
			OMAttribute reference = elem.getAttribute(new QName(
					Constants.NULL_NAMESPACE, "ref"));
			if (reference != null) {
				endpoint.setRef(reference.getAttributeValue());
			} else if (address != null) {
				endpoint.setAddress(address.getAttributeValue());
			} else {
				handleException("One of the 'address' or 'ref' attributes are required in an "
						+ "anonymous endpoint");
			}
		}
		
		
		if (force != null)
		{
			String forceValue = force.getAttributeValue().trim().toLowerCase();
			if (forceValue.equals("pox")) {
				endpoint.setForcePOX(true);
			} else if (forceValue.equals("soap")) {
				endpoint.setForceSOAP(true);
			} else {
				handleException("force value -\""+forceValue+"\" not yet implemented");
			}
		}

        if (optimize != null && optimize.getAttributeValue().length() > 0) {
            String method = optimize.getAttributeValue().trim();
            if ("mtom".equalsIgnoreCase(method)) {
                endpoint.setUseMTOM(true);
            } else if ("swa".equalsIgnoreCase(method)) {
                endpoint.setUseSwa(true);
            }            
        }

        OMElement wsAddr = elem.getFirstChildWithName(new QName(
				Constants.SYNAPSE_NAMESPACE, "enableAddressing"));
		if (wsAddr != null) {
			endpoint.setAddressingOn(true);
			String useSepList = wsAddr.getAttributeValue(new QName(
					"separateListener"));
			if (useSepList != null) {
				if (useSepList.trim().toLowerCase().startsWith("tr")
						|| useSepList.trim().startsWith("1")) {
					endpoint.setUseSeparateListener(true);
				}
			}
		}

		OMElement wsSec = elem.getFirstChildWithName(new QName(
				Constants.SYNAPSE_NAMESPACE, "enableSec"));
		if (wsSec != null) {
			endpoint.setSecurityOn(true);
			OMAttribute policy = wsSec.getAttribute(new QName(
					Constants.NULL_NAMESPACE, "policy"));
			if (policy != null) {
				endpoint.setWsSecPolicyKey(policy.getAttributeValue());
			}
		}
		OMElement wsRm = elem.getFirstChildWithName(new QName(
				Constants.SYNAPSE_NAMESPACE, "enableRM"));
		if (wsRm != null) {
			endpoint.setReliableMessagingOn(true);
			OMAttribute policy = wsRm.getAttribute(new QName(
					Constants.NULL_NAMESPACE, "policy"));
			if (policy != null) {
				endpoint.setWsRMPolicyKey(policy.getAttributeValue());
			}
		}

		return endpoint;
		// }
	}

	private static void handleException(String msg) {
		log.error(msg);
		throw new SynapseException(msg);
	}

	private static void handleException(String msg, Exception e) {
		log.error(msg, e);
		throw new SynapseException(msg, e);
	}

	public Object getObjectFromOMNode(OMNode om) {
		if (om instanceof OMElement) {
			return createEndpoint((OMElement) om, false);
		} else {
			handleException("Invalid XML configuration for an Endpoint. OMElement expected");
		}
		return null;
	}

	public static EndpointFactory getInstance() {
		return instance;
	}
}
