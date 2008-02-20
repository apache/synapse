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

import org.apache.axiom.om.OMException;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.sandesha2.Sandesha2Constants;

/**
 * Class which handles the UsesSequenceSTR header block
 * 
 * Only RM11 namespace supported
 */
public class UsesSequenceSTR implements RMHeaderPart {
	
	public Object fromHeaderBlock(SOAPHeaderBlock headerBlock) throws OMException {
	    // Set that we have processed the must understand
		headerBlock.setProcessed();
		return this;
	}

	public void toHeader(SOAPHeader header) {
		SOAPHeaderBlock sequenceAcknowledgementHeaderBlock = header.addHeaderBlock(
				Sandesha2Constants.WSRM_COMMON.USES_SEQUENCE_STR,Sandesha2Constants.SPEC_2007_02.OM_NS_URI);
		// This header _must_ always be understood
		sequenceAcknowledgementHeaderBlock.setMustUnderstand(true);
	}
}