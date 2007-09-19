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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.config.xml.OMElementUtils;
import org.apache.synapse.config.xml.AbstractMediatorFactory;
import org.jaxen.JaxenException;

public class AttachmentMediatorFactory extends AbstractMediatorFactory {

	private static final Log log = LogFactory.getLog(AttachmentMediatorFactory.class);
    private static final QName TAG_NAME    = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "attachments");
	
	public Mediator createMediator(OMElement elem) {	
		AttachmentMediator mediator = new AttachmentMediator();
		
		OMAttribute attMode   = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "mode"));
        OMAttribute attMimeType = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "mimeType"));
        OMElement omPath = elem.getFirstChildWithName(new QName(XMLConfigConstants.NULL_NAMESPACE, "attachmentPath"));
        OMElement omNS = elem.getFirstChildWithName(new QName(XMLConfigConstants.NULL_NAMESPACE, "attachmentNS"));
        
        if(attMode != null && attMode.getAttributeValue().equals("MTOM_TO_BASE64")){
        	mediator.setMode(AttachmentMediator.MTOM_TO_BASE64);
        }
        
        if (attMimeType != null){
        	mediator.setMimeType(attMimeType.getAttributeValue());        	
        }
        
        if (omPath == null){
        	throw new SynapseException("Please specify the XPath to the base64 element");
        }else{
        	AXIOMXPath xp;
			try {
				xp = new AXIOMXPath(omPath.getText());
				OMElementUtils.addNameSpaces(xp, elem, log);
				
				if(omNS != null){
					String prefix = omNS.getAttributeValue(new QName(XMLConfigConstants.NULL_NAMESPACE, "prefix"));
					String name = omNS.getAttributeValue(new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));
					xp.addNamespace(prefix,name);		
				}
				
				mediator.setAttachmentPath(xp);
			} catch (JaxenException e) {
				 log.error("Error creating XPath for Base64 element", e);
			     throw new SynapseException("Error creating XPath for Base64 element", e);
			}        	
        }
        
		return mediator;
	}

	public QName getTagQName() {
		return TAG_NAME;
	}

}
