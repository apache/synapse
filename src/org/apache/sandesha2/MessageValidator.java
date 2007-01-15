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

import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SpecSpecificConstants;

/**
 * Used to validate RM messages. This include both application messages and RM
 * control messages.
 */
public class MessageValidator {

	public static void validateMessage(RMMsgContext rmMsg, StorageManager storageManager) throws SandeshaException {

		if (rmMsg.getMessageType() != Sandesha2Constants.MessageTypes.CREATE_SEQ
				&& rmMsg.getMessageType() != Sandesha2Constants.MessageTypes.UNKNOWN) {

			String sequenceID = SandeshaUtil.getSequenceIDFromRMMessage(rmMsg);

			if (sequenceID != null) {
				String rmVersionOfSequence = null;
				
				RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, sequenceID);
				if (rmsBean != null)
					rmVersionOfSequence = rmsBean.getRMVersion();
				else {
					RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceID);
					if (rmdBean != null)
						rmVersionOfSequence = rmdBean.getRMVersion();
				}

				String rmNamespaceOfMsg = rmMsg.getRMNamespaceValue();
				String rmNamespaceOfSequence = null;
				if (rmVersionOfSequence != null)
					rmNamespaceOfSequence = SpecSpecificConstants.getRMNamespaceValue(rmVersionOfSequence);

				if (rmNamespaceOfSequence != null && !rmNamespaceOfSequence.equals(rmNamespaceOfMsg)) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.rmNamespaceNotMatchSequence,
							rmNamespaceOfMsg, rmNamespaceOfSequence, sequenceID);
					throw new SandeshaException(message);
				}

				// TODO do validation based on states
			}
		}
	}
}
