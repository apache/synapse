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

package org.apache.synapse.mediators.attachment;

import java.util.List;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.jaxen.JaxenException;

public class AttachmentConverter {

	private static final Log log = LogFactory.getLog(AttachmentConverter.class);
	private static SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();;
	
	/**
	 * If MTOM is set to false in axis2.xml this method is really not need
	 * However if MTOM is ebabled then this method should help 
	 */
	public static void convertMTOM_TO_Base64(AXIOMXPath xpath, String mimeType, MessageContext synCtx) throws SynapseException{
        if (log.isDebugEnabled()) {
            log.debug("Converting MTOM to Base64");
        }

        // This should do the trick, however currently it has no effect
		// we need to manually turn off MTOM in axis2.xml
		synCtx.setDoingMTOM(false);				
	}

	/**
	 * Even if MTOM is enabled we need to specify that the content needs to be optimized 
	 */
	public static void convertBase64_TO_MTOM(AXIOMXPath xpath, String mimeType, MessageContext synCtx) throws SynapseException{
        if (log.isDebugEnabled()) {
            log.debug("Converting Base64 to MTOM");
        }

        SOAPBody soapBody = synCtx.getEnvelope().getBody();
		//OMElementUtils.addNameSpaces(xpath, soapBody.getFirstElement(), log);		
		OMElement attachmentNode = getMatchingElement(xpath, soapBody);
		
		OMText binaryNode = factory.createOMText(attachmentNode.getText(),mimeType,true);
		attachmentNode.addChild(binaryNode);		
	}
	
	
	private static OMElement getMatchingElement(AXIOMXPath xpath, SOAPBody soapBody){
		try {			
            Object result = xpath.evaluate(soapBody);
            
            if(result instanceof OMElement){
                 return (OMElement)result;
            } else if (result instanceof List && !((List) result).isEmpty()) {
            	return (OMElement) ((List) result).get(0);  // Always fetches *only* the first
            } else{
            	 throw new SynapseException("Error in XPath expression, no matching element found");
            }

        } catch (JaxenException je) {
        	String error = "Evaluation of the XPath expression " + xpath.toString() +
                " resulted in an error";
            log.error(error,je);
            
            throw new SynapseException(error,je);        	
        }
	}
}
