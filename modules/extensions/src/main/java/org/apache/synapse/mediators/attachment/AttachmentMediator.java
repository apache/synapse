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

import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.AbstractMediator;

public class AttachmentMediator extends AbstractMediator {

	private static final Log log = LogFactory.getLog(AttachmentMediator.class);
	
	public static final int BASE64_TO_MTOM = 0;
	public static final int MTOM_TO_BASE64 = 1;
	public static final int SwA_TO_BASE64 = 2;
	public static final int BASE64_TO_SwA = 3;
	public static final int SwA_TO_MTOM = 4;
	public static final int MTOM_TO_SwA = 5;
		
	private AXIOMXPath attachmentPath;
	private int mode = 0;
	private String mimeType;

	public boolean mediate(MessageContext synCtx) {
        if (log.isDebugEnabled()) {
            log.debug("Attachment Mediator, ready to mediate");
        }
        switch(mode) {
		
			case BASE64_TO_MTOM :
				AttachmentConverter.convertBase64_TO_MTOM(attachmentPath, mimeType, synCtx);
				break;
			case MTOM_TO_BASE64 :
				AttachmentConverter.convertMTOM_TO_Base64(attachmentPath, mimeType, synCtx);
				break;	
			default :
				throw new SynapseException("Invalid Attachment Type Convertion");
		}	
		return true;
	}
	
	
	public AXIOMXPath getAttachmentPath() {
		return attachmentPath;
	}

	public void setAttachmentPath(AXIOMXPath attachmentPath) {
		this.attachmentPath = attachmentPath;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

}
