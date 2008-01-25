/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.wsrm;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMException;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.Constants;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;

/**
 * Only RM11 namespace supported
 */
public class MessagePending implements RMHeaderPart {

	boolean pending = false;

	public Object fromHeaderBlock(SOAPHeaderBlock messagePendingElement) throws OMException,
			SandeshaException {
		
		String value = messagePendingElement.getAttributeValue(new QName (Sandesha2Constants.WSRM_COMMON.PENDING));
		if (Constants.VALUE_TRUE.equals(value))
			pending = true;
		else if (Constants.VALUE_FALSE.equals(value))
			pending = false;
		else {
			String message = "Attribute 'pending' must have value 'true' or 'false'. Value was:"+value;
			throw new SandeshaException (message);
		}
		
		// Mark this element as processed
		messagePendingElement.setProcessed();

		return messagePendingElement;
	}

	public boolean isPending() {
		return pending;
	}

	public void setPending(boolean pending) {
		this.pending = pending;
	}

	public void toHeader(SOAPHeader header){		
		SOAPHeaderBlock headerBlock = header.addHeaderBlock(Sandesha2Constants.WSRM_COMMON.MESSAGE_PENDING,Sandesha2Constants.SPEC_2007_02.OM_MC_NS_URI);
		headerBlock.addAttribute(Sandesha2Constants.WSRM_COMMON.PENDING, Boolean.valueOf (pending).toString(), null);
	}	
}