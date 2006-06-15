/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *  
 */

package org.apache.sandesha2;

import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;

public class MessageValidator {

	public static void validateMessage (RMMsgContext rmMsg,StorageManager storageManager) throws SandeshaException {
		
		if (rmMsg.getMessageType()!=Sandesha2Constants.MessageTypes.CREATE_SEQ 
				&& rmMsg.getMessageType()!=Sandesha2Constants.MessageTypes.UNKNOWN) {
			
			String sequenceID = SandeshaUtil.getSequenceIDFromRMMessage(rmMsg);
			
			if (sequenceID!=null) {
				String rmVersionOfSequence = SandeshaUtil.getSequenceProperty(sequenceID,Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION,storageManager);
				String addressingNamespaceOfSequence = SandeshaUtil.getSequenceProperty(sequenceID,Sandesha2Constants.SequenceProperties.ADDRESSING_NAMESPACE_VALUE,storageManager);
				
				String rmNamespaceOfMsg = rmMsg.getRMNamespaceValue();
				String rmNamespaceOfSequence = null;
				if (rmVersionOfSequence!=null)
					rmNamespaceOfSequence = SpecSpecificConstants.getRMNamespaceValue(rmVersionOfSequence);
				String addressingNamespaceOfMsg = rmMsg.getAddressingNamespaceValue();
				
				if (rmNamespaceOfSequence!=null && !rmNamespaceOfSequence.equals(rmNamespaceOfMsg)) {
					String message = "Validation failed. The RM namespace of the message does not match with the sequence";
					throw new SandeshaException (message);
				}
				
				if (addressingNamespaceOfSequence!=null && !addressingNamespaceOfSequence.equals(addressingNamespaceOfMsg)) {
					String message = "Validation failed. The Addressing namespace of the message does not match with the sequence";
					throw new SandeshaException (message);
				}
				
				//TODO do validation based on states
			}
		}
	}
}
