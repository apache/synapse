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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Adds the SequenceFault header block.
 * 
 * Either RM10 or RM11 namespace supported
 */
public class SequenceFault implements RMHeaderPart {
	
	private FaultCode faultCode;
	
	private String namespaceValue = null;
	private OMNamespace omNamespace = null;

	public SequenceFault(String namespaceValue) {
		this.namespaceValue = namespaceValue;
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(namespaceValue)) {
			omNamespace = Sandesha2Constants.SPEC_2005_02.OM_NS_URI;
		}else{
			omNamespace = Sandesha2Constants.SPEC_2007_02.OM_NS_URI;
		}
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public Object fromHeaderBlock(SOAPHeaderBlock sequenceFaultPart) throws OMException,SandeshaException {


		if (sequenceFaultPart == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.noSeqFaultInElement,
					null));

		OMElement faultCodePart = sequenceFaultPart
				.getFirstChildWithName(new QName(namespaceValue,Sandesha2Constants.WSRM_COMMON.FAULT_CODE));

		if (faultCodePart != null) {
			faultCode = new FaultCode(namespaceValue);
			faultCode.fromOMElement(sequenceFaultPart);
		}

		sequenceFaultPart.setProcessed();

		return this;
	}


	public void setFaultCode(FaultCode faultCode) {
		this.faultCode = faultCode;
	}

	public FaultCode getFaultCode() {
		return faultCode;
	}
	
	public void toHeader(SOAPHeader header){
		OMElement sequenceFaultElement = header.addHeaderBlock(Sandesha2Constants.WSRM_COMMON.SEQUENCE_FAULT, omNamespace);
		if (faultCode != null)
			faultCode.toOMElement(sequenceFaultElement);
	}
}
