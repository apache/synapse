/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2.policy.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.PolicyEngineData;
import org.apache.sandesha2.policy.RMPolicyToken;
import org.apache.sandesha2.policy.RMProcessorContext;

public class RetransmissionItervalProcessor {
	private boolean initializedRetransmissionInterval = false;

	private Log logger = LogFactory.getLog(this.getClass().getName());

	public void initializeRetranmissionIterval(RMPolicyToken spt)
			throws NoSuchMethodException {
		logger.debug("RetransmissionIntervalProcessor:initializeRetransmissionInterval");;

	}

	public Object doRetransmissionInterval(RMProcessorContext rmpc) {
		RMPolicyToken rmpt = rmpc.readCurrentRMToken();
		switch (rmpc.getAction()) {

		case RMProcessorContext.START:
			if (!initializedRetransmissionInterval) {
				try {
					initializeRetranmissionIterval(rmpt);
					initializedRetransmissionInterval = true;
				} catch (NoSuchMethodException e) {
					logger.error(SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.policyProcessingException,
							e.toString(),
							"RetransmissionInterval"), e);
					return new Boolean(false);
				}
			}

		case RMProcessorContext.COMMIT:

			// //////////////////
			PolicyEngineData ped = rmpc.readCurrentPolicyEngineData();
			String text = rmpc.getAssertion().getStrValue();
			ped.setRetransmissionInterval(Long.parseLong(text));
			// /////////////////

			break;
		case RMProcessorContext.ABORT:
			break;
		}
		return new Boolean(true);
	}
}
