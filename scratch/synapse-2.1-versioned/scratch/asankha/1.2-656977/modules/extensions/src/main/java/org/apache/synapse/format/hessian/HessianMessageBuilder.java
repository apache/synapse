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

package org.apache.synapse.format.hessian;

import org.apache.axiom.om.*;
import org.apache.axis2.AxisFault;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.util.SynapseBinaryDataSource;

import javax.activation.DataHandler;
import java.io.IOException;
import java.io.InputStream;

/**
 * Enables a message encoded using the Hessian binary protocol to be received by axis2/synapse
 * and this builds the HessianDataSource to represent the hessian message inside the SOAP info-set
 *
 * @see org.apache.axis2.builder.Builder
 * @see org.apache.synapse.util.SynapseBinaryDataSource
 */
public class HessianMessageBuilder implements Builder {

	private static final Log log = LogFactory.getLog(HessianMessageBuilder.class);

	/**
	 * Returns an OMElement from a Hessian encoded message
	 *
	 * @param inputStream stream containg the hessian message to be built
	 * @param contentType content type of the message
	 * @param messageContext message to which the hessian message has to be attached
	 * @return OMElement containing Hessian data handler keeping the message
	 * @throws AxisFault in case of a failure in building the hessian message
     *
     * @see org.apache.axis2.builder.Builder#processDocument(java.io.InputStream,
     * String, org.apache.axis2.context.MessageContext)
	 */
	public OMElement processDocument(InputStream inputStream, String contentType,
        MessageContext messageContext) throws AxisFault {

        if (log.isDebugEnabled()) {
            log.debug("Start building the hessian message in to a HessianDataSource");
        }

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace(HessianConstants.HESSIAN_NAMESPACE_URI,
                HessianConstants.HESSIAN_NS_PREFIX);
        OMElement element = factory.createOMElement(
                HessianConstants.HESSIAN_ELEMENT_LOCAL_NAME, ns);

        try {

            Parameter synEnv = messageContext.getConfigurationContext()
                    .getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_ENV);

            DataHandler dataHandler;
            if (synEnv != null && synEnv.getValue() != null) {
                dataHandler = new DataHandler(new SynapseBinaryDataSource(
                        inputStream, contentType, (SynapseEnvironment) synEnv.getValue()));
            } else {
                // add Hessian data inside a data handler
                dataHandler = new DataHandler(
                        new SynapseBinaryDataSource(inputStream,contentType));
            }
            OMText textData = factory.createOMText(dataHandler, true);
            element.addChild(textData);

        } catch (IOException e) {
            String msg = "Unable to create the HessianDataSource";
            log.error(msg, e);
            throw new AxisFault(msg, e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Building the hessian message using HessianDataSource is successful");
        }

        return element;
    }

}
